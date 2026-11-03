---
topic: Kafka and messaging
order: 16
---
## [1-3] What are Kafka's core concepts: topics, partitions, offsets and consumer groups?
- **Topic**: a named, append-only log of records.
- **Partition**: a topic is split into partitions; each is an **ordered** sequence. Partitions give parallelism and scale.
- **Offset**: a record's position within a partition.
- **Producer** writes; records with the same **key** go to the same partition (hash of the key), which preserves per-key order.
- **Consumer group**: consumers sharing a group id split the partitions; **each partition is read by one consumer in the group**, so parallelism is capped at the partition count. Different groups each get all the messages (publish and subscribe).
- **Broker** and **replication**: each partition has a leader and followers on other brokers.

Kafka stores messages for a **retention period** regardless of consumption, so consumers can **replay** from an offset.

## [1-3] How is Kafka different from a traditional queue such as RabbitMQ?
- **Kafka** is a distributed, durable **log**: messages persist and can be **re-read**; consumers track their own offsets; very high throughput; ordering per partition; best for event streaming, event sourcing, data pipelines.
- **RabbitMQ** is a **message broker** with rich routing (exchanges, queues): messages are removed once acknowledged; per-message features (priorities, TTL, delayed delivery), flexible patterns (work queues, request-reply); lower throughput.

Choose Kafka when you need replay, many independent consumers or huge volume; choose a queue broker for task distribution and complex routing.

## [3-5] What delivery guarantees does Kafka provide, and how do you avoid message loss and duplicates?
Semantics: **at-most-once**, **at-least-once** (default in practice), **exactly-once** (within Kafka, with limits).

**Avoid loss:**
- Producer: `acks=all`, retries on, and **`enable.idempotence=true`** (no duplicates from producer retries).
- Broker: replication factor 3, `min.insync.replicas=2`.
- Consumer: **commit offsets after processing**, not before (otherwise a crash loses the message).

**Duplicates** still occur with at-least-once (a crash after processing but before the commit), so make consumers **idempotent**: dedupe by message id, use upserts or unique constraints.

**Exactly-once** inside Kafka (read, process, write) uses **transactions** (`transactional.id`, `isolation.level=read_committed`). Effects on external systems (DB, email) still need idempotency or the outbox pattern.

## [3-5] How do you handle consumer failures, rebalancing, retries and a dead letter topic?
- **Rebalance**: when consumers join, leave or time out, partitions are reassigned. Avoid long processing between polls (`max.poll.interval.ms`), use **cooperative sticky** assignment, and keep processing fast or hand it to a worker pool.
- **Poison messages** (always fail) must not block a partition. Use **retries with backoff** for transient errors, then publish to a **dead letter topic (DLT)** with the error details for inspection and replay. Spring Kafka offers `DefaultErrorHandler` with `DeadLetterPublishingRecoverer` and retry topics.
- **Ordering vs parallelism**: parallel retries can reorder messages; keep order-sensitive flows on the same key and partition.
- Monitor **consumer lag** (the most important Kafka health metric) and alert on it.

## [5+] How do you design topics, partitions and keys for scale and ordering?
- **Key choice** defines ordering and balance: use the entity id (order id, account id) so all events for one entity are ordered on one partition. A skewed key (one huge customer) creates a **hot partition**.
- **Partition count** sets max consumer parallelism and throughput. Plan for growth: increasing partitions later **changes the key-to-partition mapping**, which breaks per-key ordering for existing keys.
- **Retention and compaction**: time or size based deletion for event streams; **log compaction** keeps the latest value per key for state-like topics (changelogs).
- **Schema management**: Avro or Protobuf with a **schema registry** and compatibility rules (backward or forward) so producers and consumers evolve safely.
- Topic naming and ownership conventions, ACLs per team, and quotas.
- Capacity: size by throughput per partition (often tens of MB/s), replication cost, and retention storage.
- Design events as **facts** (`OrderPlaced`) with an id, timestamp and version, not as RPC in disguise.
