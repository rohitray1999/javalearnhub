---
topic: Redis and caching
order: 22
---
## [0-1] What is Redis? Which data types does it support, and where do you use them?
Redis is an **in-memory data store** (key-value with rich data structures) used as a cache, database, message broker and more. It is very fast (sub-millisecond) because data lives in RAM and the command execution is simple.

| Type | Typical use |
|---|---|
| **String** | cache values, counters (`INCR`), tokens, locks |
| **Hash** | objects (user profile fields) |
| **List** | queues, recent items |
| **Set** | unique items, tags, "who liked this" |
| **Sorted set (ZSET)** | **leaderboards**, rate limiting windows, scheduled jobs |
| **Stream** | durable event log with consumer groups |
| **Bitmap / HyperLogLog** | flags, approximate unique counts |
| **Pub/Sub** | simple fire-and-forget messaging |

Common uses: caching, **session storage**, rate limiting, leaderboards, distributed locks, queues. Always set a **TTL** on cache keys.

## [1-3] How does Redis persist data? What are eviction policies? Is Redis single-threaded?
**Persistence**
- **RDB snapshots**: periodic point-in-time dumps. Compact and fast to restore, but you can lose the data since the last snapshot.
- **AOF** (append-only file): logs every write; configurable `fsync` (`always`, `everysec`, `no`). Safer, larger files, slower restart.
- Production often combines both. For a **pure cache**, persistence can be off.

**Eviction** (when `maxmemory` is reached): `noeviction` (reject writes), `allkeys-lru`, `allkeys-lfu`, `volatile-lru` (only keys with TTL), `volatile-ttl`, `allkeys-random`. For caches, `allkeys-lru` or `allkeys-lfu` are typical.

**Threading:** command execution is **single-threaded** (so each command is atomic and there are no locks), while newer versions can use extra threads for network I/O. Therefore one **slow command** (`KEYS *`, a huge `SMEMBERS`, big Lua scripts) blocks everyone: use `SCAN` and avoid giant keys.

## [3-5] How do you use Redis with Spring Boot? What are the good practices?
Add `spring-boot-starter-data-redis` (the default client is **Lettuce**, which is thread safe and non-blocking).

```java
@Bean
RedisCacheManager cacheManager(RedisConnectionFactory cf) {
    RedisCacheConfiguration cfg = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(10))
        .serializeValuesWith(SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
    return RedisCacheManager.builder(cf).cacheDefaults(cfg).build();
}
```
Then use `@Cacheable` on service methods, or `StringRedisTemplate` for direct operations.

**Practices:**
- A **TTL on every key** (with jitter) and bounded memory.
- **JSON serialisation** instead of Java serialisation (readable, safer, versionable).
- Key naming such as `app:entity:id`; versioned prefixes for deployments.
- Never use `KEYS` in production; use `SCAN`.
- Avoid **big keys** and **hot keys**; keep values small.
- Set **timeouts**, handle Redis downtime gracefully (the app should degrade to the database, not crash).
- Monitor hit ratio, memory, evictions, latency, and connected clients.

## [5+] How do you implement a distributed lock with Redis? What about high availability: Sentinel versus Cluster?
**Lock:** acquire atomically with a unique token and a timeout:
```
SET lock:order:42 <random-token> NX PX 30000
```
Release **only if you still own it**, atomically, with a Lua script (otherwise you might delete someone else's lock after yours expired):
```lua
if redis.call("get", KEYS[1]) == ARGV[1] then return redis.call("del", KEYS[1]) else return 0 end
```
**Caveats:** if the work takes longer than the TTL the lock expires while you still run (use a **watchdog** or lease renewal, as **Redisson** does); a failover can lose the lock; process pauses (GC) can break assumptions. For **correctness-critical** exclusion use **fencing tokens** checked by the resource, or rely on the **database** (row lock or unique constraint) or a consensus system (ZooKeeper, etcd). Redis locks are fine for efficiency (avoid duplicate work), not for hard safety guarantees.

**High availability:**
- **Replication** with **Sentinel**: one primary, replicas, Sentinels monitor and **promote** a replica on failure. Data is not sharded; good for moderate data size.
- **Redis Cluster**: data is **sharded** across nodes using **16,384 hash slots**, with replicas per shard; it scales memory and throughput. Multi-key operations must stay in one slot (use hash tags `{user:1}`).
- Replication is asynchronous, so a failover can lose recent writes; size memory with headroom for replication and persistence.
