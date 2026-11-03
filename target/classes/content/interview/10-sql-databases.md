---
topic: SQL and databases
order: 10
---
## [0-1] What is the difference between DELETE, TRUNCATE and DROP? What are the JOIN types?
- **DELETE**: removes rows (with `WHERE`), logged per row, fires triggers, can be rolled back.
- **TRUNCATE**: removes **all** rows quickly, resets identity in most databases, minimal logging (transactional in PostgreSQL, implicit commit in some others).
- **DROP**: removes the table itself (structure and data).

Joins:
- `INNER JOIN`: only matching rows.
- `LEFT JOIN`: all left rows plus matches (nulls when no match).
- `RIGHT JOIN`: the mirror image.
- `FULL OUTER JOIN`: all rows from both.
- `CROSS JOIN`: Cartesian product.

Also: a **primary key** is unique and not null (one per table); a **unique** constraint allows nulls (database specific) and you can have many.

## [1-3] How do indexes work, and when are they not used?
Most indexes are **B-trees**: sorted structures that let the database find rows in O(log n) instead of scanning the table.

- A **composite index** `(a, b, c)` follows the **leftmost prefix rule**: it helps `WHERE a=?`, `a=? AND b=?`, but not `WHERE b=?` alone.
- Put equality columns first, then range columns.
- A **covering index** contains all columns the query needs, so the table is never touched.

**An index is skipped when:** you wrap the column in a function (`WHERE LOWER(email)=?`, unless you create a functional index), use a leading wildcard (`LIKE '%abc'`), compare with an implicit type cast, or the table or filter is so small or unselective that a scan is cheaper.

**Cost:** every index slows writes and uses space. Index for real queries, verify with `EXPLAIN`, and drop unused ones.

## [1-3] Explain ACID and the transaction isolation levels.
**ACID:** Atomicity (all or nothing), Consistency (constraints hold), Isolation (concurrent transactions do not interfere), Durability (committed data survives crashes).

| Level | Dirty read | Non-repeatable read | Phantom read |
|---|---|---|---|
| Read uncommitted | possible | possible | possible |
| Read committed | no | possible | possible |
| Repeatable read | no | no | possible (prevented in MySQL InnoDB with gap locks) |
| Serializable | no | no | no |

- **Dirty read**: seeing uncommitted data. **Non-repeatable**: the same row changes between two reads. **Phantom**: new rows appear in a repeated range query.
- Defaults: PostgreSQL and Oracle use **read committed**; MySQL InnoDB uses **repeatable read**.
- Higher isolation means more locking or retries. Pick the lowest level that is correct for the use case.

## [3-5] How do you find and tune a slow query?
1. **Measure first**: slow query log, `pg_stat_statements`, APM. Find the queries with the highest total time, not only the slowest single run.
2. Run **`EXPLAIN (ANALYZE)`**: look for sequential scans on big tables, wrong join order, large row estimate mismatches (stale statistics), sorts spilling to disk.
3. Add or fix **indexes** (composite, covering, partial) and keep statistics fresh (`ANALYZE`).
4. Rewrite: select only needed columns, filter early, avoid functions on indexed columns, replace correlated subqueries with joins, avoid `OR` across columns.
5. **Pagination:** `OFFSET 100000` scans and discards rows. Use **keyset pagination**: `WHERE id > :last ORDER BY id LIMIT 50`.
6. Reduce round trips (batching, fix N+1) and consider caching or read replicas.

Always verify the improvement with the same `EXPLAIN` and realistic data volume.

## [3-5] Find the second highest salary. How do window functions help?
Several approaches:

```sql
-- Subquery
SELECT MAX(salary) FROM employee
WHERE salary < (SELECT MAX(salary) FROM employee);

-- Window function (handles ties well, generalises to Nth)
SELECT salary FROM (
  SELECT salary, DENSE_RANK() OVER (ORDER BY salary DESC) AS rnk FROM employee
) t WHERE rnk = 2;
```

- `ROW_NUMBER()`: unique sequence, ties broken arbitrarily.
- `RANK()`: ties share a rank, then **gaps** appear (1, 1, 3).
- `DENSE_RANK()`: ties share a rank, **no gaps** (1, 1, 2).

Window functions also give running totals (`SUM(x) OVER (ORDER BY d)`), top N per group (`PARTITION BY dept`), and `LAG`/`LEAD` comparisons without self joins.

## [3-5] Normalisation versus denormalisation: how do you decide?
**Normalisation** (1NF to 3NF) removes redundancy so each fact is stored once. It avoids update anomalies and keeps data consistent, and is the right default for **transactional (OLTP)** systems.

**Denormalisation** deliberately duplicates data (a stored total, a copied customer name) to **avoid joins** and speed up reads. It suits reporting, search, read-heavy paths and microservices that must not join across service databases.

Costs: extra write logic, risk of inconsistency, and more storage. Mitigate with triggers, events, or periodic recomputation, and treat the normalised data as the **source of truth**.

## [5+] SQL versus NoSQL, replication and sharding: how do you reason about them?
**Choosing a store**
- **Relational**: strong consistency, transactions, joins, flexible ad hoc queries. Default choice.
- **Document** (MongoDB): flexible schema, aggregates read together.
- **Key-value** (Redis, DynamoDB): very fast simple access, caching, sessions.
- **Wide-column** (Cassandra): huge write throughput, known access patterns.
- **Search** (Elasticsearch), **graph**, **time-series** for specialised queries.

Choose from **access patterns, consistency needs and scale**, not fashion. Many systems use polyglot persistence.

**CAP:** during a network **partition** you must choose consistency or availability. Many systems are tunable (quorum reads and writes).

**Replication:** a primary plus read replicas scales reads; replicas lag, so reads right after a write may be stale (read-your-writes needs routing to the primary).

**Sharding:** split data by key across nodes to scale writes and size. Pick a key with even distribution (avoid hot shards), expect hard cross-shard joins and transactions, and plan resharding (consistent hashing helps). Exhaust indexing, caching and vertical scaling first.

## [0-1] What are SQL constraints? What is the difference between WHERE and HAVING?
**Constraints** enforce data rules inside the database: `PRIMARY KEY`, `FOREIGN KEY`, `UNIQUE`, `NOT NULL`, `CHECK`, `DEFAULT`. Always enforce critical rules (uniqueness, referential integrity) in the **database**, not only in code, because several services or manual scripts may write to it.

**`WHERE` vs `HAVING`:**
- `WHERE` filters **rows before** grouping (it cannot use aggregates).
- `HAVING` filters **groups after** `GROUP BY` (it can use `COUNT`, `SUM`).

```sql
SELECT department, COUNT(*) AS staff, AVG(salary) AS avg_salary
FROM employee
WHERE active = true              -- rows first
GROUP BY department
HAVING COUNT(*) > 5;             -- then groups
```
Logical order: `FROM`, `WHERE`, `GROUP BY`, `HAVING`, `SELECT`, `ORDER BY`, `LIMIT`.

## [1-3] What are views, stored procedures, functions and triggers? When do you use them?
- **View**: a saved query that looks like a table; simplifies complex joins and restricts columns for security. A **materialised view** stores the result and must be refreshed (speeds up reports).
- **Stored procedure**: server-side code called by name; good for batch processing close to the data, and for security (grant execute only). Drawbacks: harder to version, test and debug, ties logic to one database.
- **Function**: returns a value and can be used inside queries.
- **Trigger**: runs automatically on insert, update or delete (audit logs, derived columns). Powerful but **hidden logic**: it surprises developers and can hurt performance.

Modern guidance: keep business logic in the application where it is tested and versioned; use database features for integrity, auditing and heavy set-based work.

## [1-3] Write the common SQL interview queries: duplicates, top N per group, and employees earning more than their manager.
```sql
-- Find duplicate emails
SELECT email, COUNT(*) FROM users GROUP BY email HAVING COUNT(*) > 1;

-- Delete duplicates, keep the lowest id
DELETE FROM users u
USING users d
WHERE u.email = d.email AND u.id > d.id;          -- PostgreSQL syntax

-- Top 3 salaries in each department
SELECT * FROM (
  SELECT e.*, DENSE_RANK() OVER (PARTITION BY department ORDER BY salary DESC) AS r
  FROM employee e
) t WHERE r <= 3;

-- Employees earning more than their manager (self join)
SELECT e.name FROM employee e
JOIN employee m ON e.manager_id = m.id
WHERE e.salary > m.salary;

-- Customers with no orders
SELECT c.* FROM customer c
LEFT JOIN orders o ON o.customer_id = c.id
WHERE o.id IS NULL;
```
Explain your thinking aloud, mention edge cases (ties, nulls), and say which database dialect you assume.

## [5+] How do you perform zero-downtime database schema changes?
Use the **expand and contract** (parallel change) approach, because during a rolling deploy old and new application versions run **at the same time** against one schema.

1. **Expand**: add the new column or table in a backward-compatible way (nullable or with a default; no renames or drops yet).
2. **Deploy code that writes both** old and new places and reads the old one (or reads new with fallback).
3. **Backfill** existing data in **small batches** off-peak, to avoid long locks and replication lag.
4. **Switch reads** to the new structure; verify.
5. **Contract**: after all old versions are gone, stop dual writes and drop the old column in a later release.

Details: adding an index can lock a table; use `CREATE INDEX CONCURRENTLY` (PostgreSQL) or online DDL (MySQL). Set `lock_timeout` for migrations, avoid `ALTER` operations that rewrite big tables, and run migrations with **Flyway or Liquibase** as a separate, reviewed step. Always keep migrations forward only, and rehearse on production-sized data.
