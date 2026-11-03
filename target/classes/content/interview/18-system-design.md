---
topic: System design basics
order: 18
---
## [3-5] Design a URL shortener.
**Requirements:** create short links, redirect with low latency, very read-heavy (maybe 100:1), optional expiry and analytics.

**API:** `POST /urls` returns `{shortCode}`; `GET /{shortCode}` returns `301/302` to the long URL. (`302` lets you count every click; `301` is cached by browsers.)

**Key generation:** a 7-character **Base62** code (`[a-zA-Z0-9]`) gives about 3.5 trillion combinations. Options: encode a unique counter or id (no collisions, needs a distributed id generator such as ranges handed to app servers or Snowflake ids), or hash the URL and handle collisions. Random codes should be checked against a unique constraint.

**Storage:** a key-value style table `shortCode -> longUrl, userId, createdAt, expiresAt`. A relational DB with an index works fine at moderate scale; Cassandra or DynamoDB for massive scale.

**Scaling reads:** put a **cache** (Redis) in front (cache-aside, hot keys), CDN for redirects when possible, stateless app servers behind a load balancer. Record clicks **asynchronously** through a queue so redirects stay fast.

Mention abuse protection (rate limits, malicious URL checks) and custom aliases.

## [5+] Design a rate limiter.
**Goal:** limit each client (user, API key, IP) to N requests per time window, across many servers, with minimal latency.

**Algorithms:**
- **Fixed window counter**: simple, but allows bursts at window edges (up to 2x).
- **Sliding window log / counter**: accurate, more memory (counter variant approximates cheaply).
- **Token bucket**: tokens refill at a fixed rate; a request takes a token; allows controlled **bursts**. Very common (used by most APIs).
- **Leaky bucket**: smooths output to a constant rate.

**Distributed implementation:** keep counters in **Redis** with atomic operations: `INCR` + `EXPIRE`, or a **Lua script** (so check and update are atomic) for token bucket. Put the limiter in the **API gateway** or a filter; return **`429`** with `Retry-After` and `X-RateLimit-*` headers.

**Design choices:** key by user plus endpoint, different limits per plan, fail **open or closed** if Redis is down (usually open for availability, closed for security-critical paths), local approximate counters with periodic sync for ultra-low latency, and per-region limits to avoid cross-region latency.

## [5+] Explain caching strategies and the problems they bring.
**Patterns:**
- **Cache-aside (lazy loading)**: app checks the cache, on a miss reads the DB and fills the cache. Most common; the cache holds only requested data.
- **Read-through**: the cache layer loads from the DB itself.
- **Write-through**: writes go to cache and DB together; reads are always fresh, writes are slower.
- **Write-behind**: write to cache and flush to the DB asynchronously; fast, but risks data loss.

**Problems and fixes:**
- **Invalidation and staleness**: use TTLs, invalidate or update on writes, version keys; accept eventual consistency where possible.
- **Cache stampede** (a hot key expires and many requests hit the DB): request coalescing (single-flight lock), **TTL jitter**, early refresh, stale-while-revalidate.
- **Penetration** (requests for keys that do not exist): cache negative results briefly, use a Bloom filter.
- **Hot keys** and uneven load: replicate hot keys, local in-process cache (Caffeine) in front of Redis.
- **Eviction policy** (`allkeys-lru`, `allkeys-lfu`) and memory limits; monitor **hit ratio**.

Cache what is **read often and changes rarely**, and always consider what happens when the cache is cold or down.

## [5+] How do you scale a system? Load balancing, statelessness, consistent hashing and CAP.
**Scale up** (bigger machine) is simple but limited; **scale out** (more machines) needs the design to allow it.

1. **Stateless services**: no session data in memory; keep it in Redis or a signed token, so any instance can serve any request and you can autoscale.
2. **Load balancing**: L4 or L7; algorithms (round robin, least connections, consistent hashing for affinity); health checks; avoid the balancer being a single point of failure.
3. **Database scaling**: indexing and query tuning first, then **read replicas**, caching, **partitioning and sharding** by a well-distributed key.
4. **Consistent hashing**: maps keys to nodes on a ring so adding or removing a node moves only about `1/N` of the keys (caches, sharded stores, Kafka-like partitioning), with virtual nodes for balance.
5. **Asynchrony**: queues to absorb spikes and decouple work; CDN for static content.
6. **Availability**: redundancy across zones, timeouts, retries, circuit breakers, graceful degradation.
7. **CAP and trade-offs**: under a partition choose **consistency or availability**; choose per use case (payments: consistency; feeds: availability), and understand **eventual consistency**.

In interviews: clarify requirements and scale numbers first, sketch a simple design, identify the **bottleneck**, then evolve it, explaining trade-offs at each step.
