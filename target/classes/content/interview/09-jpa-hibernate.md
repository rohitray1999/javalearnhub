---
topic: JDBC, JPA and Hibernate
order: 9
---
## [0-1] What are the steps in JDBC? Statement versus PreparedStatement?
Steps: get a `Connection` (from a `DataSource`), create a statement, execute it, read the `ResultSet`, and **close everything** (use try-with-resources). Since JDBC 4 the driver loads automatically.

```java
try (Connection con = ds.getConnection();
     PreparedStatement ps = con.prepareStatement("SELECT name FROM users WHERE id = ?")) {
    ps.setLong(1, id);
    try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) return rs.getString("name");
    }
}
```

**Prefer `PreparedStatement`:**
- Parameters are sent separately from the SQL, which prevents **SQL injection**.
- The database can cache the execution plan.
- Supports batching (`addBatch`, `executeBatch`) and handles types and escaping correctly.

Never build SQL by string concatenation with user input.

## [1-3] What is the difference between JPA, Hibernate and Spring Data JPA?
- **JPA** (Jakarta Persistence): a **specification**, a set of interfaces and annotations (`@Entity`, `EntityManager`, JPQL).
- **Hibernate**: the most popular **implementation** of JPA, with extras (second-level cache, `@BatchSize`, filters).
- **Spring Data JPA**: a layer **on top** that generates repository implementations (`JpaRepository`), derived queries (`findByEmail`), paging, specifications and auditing.

Stack: your code uses a Spring Data repository, which calls JPA's `EntityManager`, implemented by Hibernate, which uses JDBC, which talks to the database.

## [1-3] Explain entity states, the persistence context and dirty checking.
The **persistence context** (first-level cache) belongs to one `EntityManager` or session, usually one transaction. It ensures one instance per row identity.

Entity states: **transient** (new, not tracked), **managed** (tracked), **detached** (was managed, context closed), **removed**.

**Dirty checking:** for managed entities, Hibernate snapshots state on load and at **flush** compares it. Changed fields produce `UPDATE` statements automatically, so you do not need to call `save()` inside a transaction.

```java
@Transactional
void rename(Long id, String name) {
    User u = repo.findById(id).orElseThrow();
    u.setName(name);        // UPDATE is issued at commit, no save() needed
}
```

`persist` makes a new entity managed; `merge` copies a detached entity's state into a managed one and returns it. Flush happens before queries (auto mode) and at commit.

## [1-3] LAZY versus EAGER fetching, and what is LazyInitializationException?
Defaults: `@ManyToOne` and `@OneToOne` are **EAGER**; `@OneToMany` and `@ManyToMany` are **LAZY**.

- **LAZY** loads the association on first access, through a proxy or a lazy collection.
- **EAGER** loads it with the parent, which can pull in huge graphs and cause extra queries.

`LazyInitializationException` happens when you touch a lazy association **after the session or transaction is closed** (for example while serialising an entity to JSON).

**Fixes (best first):** query exactly what you need with `JOIN FETCH` or `@EntityGraph`; return **DTO projections**; keep the access inside a `@Transactional` service. Making everything EAGER, or relying on Open Session in View, only hides the problem.

## [3-5] What is the N+1 select problem? How do you find and fix it?
Loading N parents with one query, then lazily loading each parent's children triggers **N extra queries**.

```java
List<Order> orders = repo.findAll();                 // 1 query
orders.forEach(o -> o.getItems().size());            // N more queries
```

**Detect:** enable SQL logging or `hibernate.generate_statistics`, use a datasource proxy (such as datasource-proxy) in tests that **asserts the number of queries**, and watch APM traces.

**Fix:**
- `@Query("select o from Order o join fetch o.items")` or `@EntityGraph(attributePaths = "items")`.
- `@BatchSize(size = 50)` or `hibernate.default_batch_fetch_size`, which loads children in batches (`WHERE id IN (...)`).
- **DTO projections** selecting only needed columns.

**Caveat:** fetch-joining a collection with **pagination** makes Hibernate paginate **in memory** (it warns about it). Page the parent ids first, then fetch the children.

## [3-5] How does @Transactional work? Explain propagation, rollback rules and common traps.
Spring wraps the bean in a **proxy**. The proxy begins a transaction before the method, commits on success, and rolls back on failure.

- **Propagation:** `REQUIRED` (default, join or create), `REQUIRES_NEW` (suspend and create a new one), `SUPPORTS`, `MANDATORY`, `NOT_SUPPORTED`, `NEVER`, `NESTED` (savepoint).
- **Rollback:** by default only for **unchecked** exceptions and `Error`. Checked exceptions **commit** unless you set `rollbackFor`.
- **Isolation:** defaults to the database's default.
- `readOnly = true` is a hint (skips dirty checking in Hibernate, can route to replicas).

**Traps:**
1. **Self-invocation**: calling a `@Transactional` method from another method in the **same class** bypasses the proxy, so no transaction.
2. `@Transactional` on `private` or `final` methods, or on non-Spring-managed objects, does nothing.
3. Catching the exception inside the method hides the failure from the proxy, so there is no rollback.
4. Long transactions (HTTP calls inside) hold DB connections and locks.

## [3-5] Optimistic versus pessimistic locking?
- **Optimistic**: assume conflicts are rare. Add a `@Version` column; on update Hibernate runs `UPDATE ... WHERE id=? AND version=?`. If zero rows are updated, someone else changed it first and you get `OptimisticLockException`; retry or tell the user. No database locks held. Best for most web apps.
- **Pessimistic**: lock the row up front (`SELECT ... FOR UPDATE`) with `@Lock(LockModeType.PESSIMISTIC_WRITE)`. Prevents conflicts but blocks other writers, risks deadlocks and needs short transactions. Use for high-contention data such as inventory counters, or when retries are not acceptable.

Combine with timeouts (`jakarta.persistence.lock.timeout`) and consistent lock ordering.

## [5+] How do you make Hibernate perform well for bulk writes and read-heavy workloads?
**Bulk inserts and updates**
- `GenerationType.IDENTITY` **disables JDBC batching** (Hibernate needs the id immediately). Use `SEQUENCE` with a pooled `allocationSize`.
- Set `hibernate.jdbc.batch_size=50`, `order_inserts=true`, `order_updates=true`.
- Flush and `clear()` the persistence context in chunks to avoid memory growth.
- For massive loads use JDBC batch, `COPY` or `INSERT ... SELECT`, or a bulk JPQL update (which bypasses the context).

**Reads**
- Use DTO or interface projections; avoid loading whole entities for list screens.
- Avoid `EAGER`, and watch for hidden N+1.
- **Second-level cache** (Ehcache, Caffeine through JCache) suits rarely changed reference data; it brings staleness and invalidation concerns in clusters. The **query cache** is rarely worth it.
- Use read replicas and `readOnly = true` transactions.

**Connections**
- Size the HikariCP pool small (often 10 to 30 per instance); more connections rarely mean more throughput. Keep transactions short, and consider disabling Open-Session-in-View (`spring.jpa.open-in-view=false`) so lazy loading cannot happen in the view layer while connections are held.

## [0-1] What is ORM? Which are the basic JPA annotations?
**ORM** (Object-Relational Mapping) maps Java objects to database tables, so you work with objects and the framework generates SQL. It removes JDBC boilerplate, but you must still understand the SQL it produces.

Basic annotations:
- `@Entity`, `@Table(name = "orders")`: a mapped class.
- `@Id`, `@GeneratedValue(strategy = ...)`: primary key and how it is generated (`IDENTITY`, `SEQUENCE`).
- `@Column(nullable = false, length = 100, unique = true)`.
- `@Enumerated(EnumType.STRING)`: store enums as text, **not** the ordinal (reordering enum constants would corrupt data).
- `@Transient`: not persisted.
- `@Embeddable` and `@Embedded`: reuse a value object (for example `Address`).

An entity needs a no-argument constructor (at least protected) and should not be `final`, so Hibernate can create proxies.

## [1-3] Explain @OneToMany, @ManyToOne, mappedBy, cascade and orphanRemoval.
In a bidirectional relation, the **owning side** is the one with the foreign key (`@ManyToOne`). The other side declares `mappedBy` to say "the other side owns this".

```java
@Entity class Order {
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> lines = new ArrayList<>();

    void addLine(OrderLine l) { lines.add(l); l.setOrder(this); }   // keep both sides in sync
}
@Entity class OrderLine {
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "order_id")
    private Order order;
}
```

- **`cascade`**: propagates operations (`PERSIST`, `MERGE`, `REMOVE`, `ALL`) from parent to child. Use it for **composition** (order and its lines), not for shared entities.
- **`orphanRemoval = true`**: deleting a child from the parent's collection deletes the row.
- Without `mappedBy`, `@OneToMany` creates an extra join table, a common mistake.
- Use `LAZY` on `@ManyToOne` explicitly, since the default is EAGER.

## [1-3] How do derived queries, @Query, JPQL, native SQL and Pageable work in Spring Data?
- **Derived queries**: Spring builds the query from the method name: `findByStatusAndCreatedAtAfter(Status s, Instant t)`. Great for simple cases; names get unreadable when long.
- **`@Query`**: write **JPQL** (entity names, portable) or `nativeQuery = true` (real SQL, for vendor features or complex reports). Use named parameters: `where u.email = :email` with `@Param("email")`.
- **Projections**: interfaces or records to fetch only some columns.
- **Paging and sorting**: pass `Pageable` and return `Page<T>` (runs an extra count query) or `Slice<T>` (no count; just "has next"). Prefer `Slice` for infinite scroll.

```java
Page<Order> findByCustomerId(Long id, Pageable pageable);
// call: repo.findByCustomerId(7L, PageRequest.of(0, 20, Sort.by("createdAt").descending()))
```
Whitelist sort fields coming from clients, and for very large tables use keyset pagination.

## [3-5] What are the common JPA mapping mistakes, and how should equals and hashCode be written for entities?
Common mistakes:
- **`@Data` or `@EqualsAndHashCode` on entities**: includes lazy collections (triggers loading) and breaks with changing ids. Also `toString` on bidirectional relations causes **infinite recursion**.
- **EAGER everywhere**, and `@OneToMany` without `mappedBy`.
- Fetching several `List` collections at once throws **`MultipleBagFetchException`**; use `Set`, or fetch in separate queries.
- Storing enums by ordinal.
- Exposing entities directly in REST responses.
- Using `GenerationType.AUTO` and not understanding what it picks.

**equals/hashCode:** the id is `null` until persisted, so an id-only `hashCode` changes after save, which breaks `HashSet`. Options: use a **business key** (a natural unique key like `isbn`, `email`) that never changes; or use the id for `equals` but a **constant** `hashCode` (`getClass().hashCode()`), accepting hash collisions in big sets. Compare through `Hibernate.getClass(o)` to handle proxies.
