---
topic: Collections
order: 4
---
## [0-1] ArrayList versus LinkedList: which should you use?
| Operation | ArrayList | LinkedList |
|---|---|---|
| `get(i)` | O(1) | O(n) |
| `add` at end | amortised O(1) | O(1) |
| `add` or `remove` in the middle | O(n) (shifting) | O(1) *after* you hold the node, but finding it is O(n) |
| Memory | compact array | extra node object plus two pointers per element |

**Default to `ArrayList`.** Its array is cache friendly, so it is faster than `LinkedList` in most real workloads, even for many inserts. Use `LinkedList` mainly as a `Deque`, and even then `ArrayDeque` is usually better.

## [0-1] When do you use List, Set and Map? How do HashSet, LinkedHashSet and TreeSet differ?
- **List**: ordered, allows duplicates, index access.
- **Set**: no duplicates.
- **Map**: key to value, unique keys.

| Implementation | Order | Complexity | Notes |
|---|---|---|---|
| `HashSet` / `HashMap` | none | O(1) average | needs `equals`/`hashCode` |
| `LinkedHashSet` / `LinkedHashMap` | insertion (or access) order | O(1) | slightly more memory |
| `TreeSet` / `TreeMap` | sorted | O(log n) | needs `Comparable` or `Comparator`, no null keys |

## [1-3] How does HashMap work internally?
**put(key, value):**
1. Compute `hash = key.hashCode() ^ (hashCode >>> 16)` (spreads high bits to reduce collisions).
2. Bucket index = `(n - 1) & hash` (`n` is the table size, always a power of two).
3. Empty bucket: store the node. Otherwise walk the bucket: if a key `equals` the new one, replace the value; else append.
4. If a bucket has **8 or more** nodes and the table has at least **64** slots, the chain becomes a red-black tree (O(log n)). With fewer slots the table resizes instead. Trees shrink back at 6.
5. When `size > capacity * 0.75` (load factor), the table **doubles** and entries are redistributed.

**get(key):** same hash and index, then `equals` along the bucket.

Defaults: capacity 16, load factor 0.75. Average `get` and `put` are O(1), worst case O(log n) with trees. It is **not thread safe**, and allows one `null` key.

## [1-3] Comparable versus Comparator? What should you know about sorting?
- **`Comparable<T>`**: the class defines its **natural order** via `compareTo`. One order per class.
- **`Comparator<T>`**: an **external** order, so you can have many (by name, by salary, and so on).

```java
people.sort(Comparator.comparing(Person::city)
                      .thenComparing(Person::age, Comparator.reverseOrder()));
```

Points to add:
- `List.sort` and `Collections.sort` are **stable** (TimSort), so equal elements keep their relative order.
- Do not subtract ints in `compare` (overflow). Use `Integer.compare`.
- `TreeSet` and `TreeMap` use `compare`, not `equals`, to decide equality, so keep them **consistent with equals**.

## [3-5] What are fail-fast and fail-safe iterators?
**Fail-fast** (`ArrayList`, `HashMap`): the iterator checks a `modCount`; if the collection is structurally changed during iteration by anything except the iterator itself, it throws `ConcurrentModificationException`. It is best effort, not a guarantee.

```java
for (String s : list) { if (s.isEmpty()) list.remove(s); }   // CME
list.removeIf(String::isEmpty);                              // correct
```

**Fail-safe** is a loose name for concurrent collections that never throw CME:
- `CopyOnWriteArrayList`: iterates over a **snapshot**; great for read-heavy, rarely changed lists.
- `ConcurrentHashMap`: **weakly consistent** iterators; they may or may not show concurrent updates.

## [3-5] Compare HashMap, Hashtable, synchronizedMap and ConcurrentHashMap.
- **HashMap**: fast, not thread safe.
- **Hashtable**: legacy, every method `synchronized` (one lock), no nulls. Avoid.
- **`Collections.synchronizedMap`**: wrapper with a single lock; iteration still needs manual synchronisation.
- **ConcurrentHashMap**: designed for concurrency. In Java 8+ it uses a node array: **CAS** to insert into an empty bucket, and `synchronized` on the **first node of the bucket** otherwise. Reads are mostly lock-free. Size is tracked with striped counters. No null keys or values.

Compound actions must be atomic. Use `computeIfAbsent`, `merge`, `putIfAbsent`; a `get` then `put` is a race.

```java
counts.merge(word, 1, Integer::sum);   // atomic in ConcurrentHashMap
```

## [5+] Implement an LRU cache. What changes when it must be thread safe and fast?
Simplest: `LinkedHashMap` in **access order**.

```java
class LruCache<K, V> extends LinkedHashMap<K, V> {
    private final int max;
    LruCache(int max) { super(16, 0.75f, true); this.max = max; }
    @Override protected boolean removeEldestEntry(Map.Entry<K, V> e) { return size() > max; }
}
```

`get` and `put` are O(1): a hash table plus a doubly linked list ordered by recency (the structure you would hand-build in an interview: `HashMap<K, Node>` plus a doubly linked list with head and tail pointers).

**Concurrency:** `LinkedHashMap` is not thread safe, and even `get` mutates it in access order. Options: wrap with `Collections.synchronizedMap` (simple, contended), or use a purpose-built cache such as **Caffeine**, which gives near-LRU eviction (Window TinyLFU), TTL, size limits, stats and non-blocking reads. In production I would pick Caffeine rather than build my own.

## [5+] What are common HashMap performance and correctness pitfalls in production?
1. **Mutable keys**: changing a field used in `hashCode` after insertion makes the entry unreachable. Use immutable keys.
2. **Bad `hashCode`** (constant, or poorly distributed): everything piles into one bucket; performance degrades to O(log n) at best, and it is also a DoS vector.
3. **Resizing cost**: for a known size, presize: `new HashMap<>((int) (expected / 0.75) + 1)` or `HashMap.newHashMap(expected)` (Java 19+).
4. **Memory**: each entry is an object (about 32+ bytes of overhead); millions of small entries are heavy. Consider primitive collections or different structures.
5. **Concurrent use** of a plain `HashMap` can corrupt it or spin; use `ConcurrentHashMap`.
6. **Iteration order** is not guaranteed and can change between Java versions. Use `LinkedHashMap` or `TreeMap` when order matters.

## [0-1] Array versus ArrayList? Iterator versus for-each?
| | Array | ArrayList |
|---|---|---|
| Size | fixed | grows automatically |
| Element types | primitives or objects | objects only (autoboxing for primitives) |
| Features | `length`, fast | rich API (`add`, `remove`, `contains`, `sort`) |
| Generics | no type safety at runtime for arrays of objects | compile-time checked |

For-each is syntactic sugar over an **`Iterator`**. Use an explicit `Iterator` (or `ListIterator`, which also goes backwards and can `set`/`add`) when you need to **remove while iterating**: `it.remove()`. `Enumeration` is the legacy equivalent.

## [1-3] How does HashSet ensure uniqueness? How do PriorityQueue, ArrayDeque and EnumMap work?
- **`HashSet`** is backed by a `HashMap`: your element becomes the **key** (value is a dummy constant). `add` returns `false` if an element with the same `hashCode` and `equals` already exists.
- **`PriorityQueue`**: a **binary min-heap** in an array. `offer`/`poll` are O(log n), `peek` O(1). Iteration order is **not sorted**. Use a `Comparator` for max-heap behaviour. Not thread safe (`PriorityBlockingQueue` is).
- **`ArrayDeque`**: a resizable circular array; the best general-purpose **stack and queue** (faster than `LinkedList` and `Stack`). No nulls.
- **`EnumMap` / `EnumSet`**: backed by arrays or bit vectors indexed by the enum ordinal; very fast and compact when keys are enum constants.

## [3-5] List.of versus Collections.unmodifiableList versus List.copyOf: what is the difference?
- **`Collections.unmodifiableList(list)`**: a **read-only view** of the original. If someone changes the original list, your view changes too. Not truly immutable.
- **`List.of(...)`** (Java 9+): a truly **immutable** list; throws `UnsupportedOperationException` on change; **no nulls**; fixed contents at creation.
- **`List.copyOf(coll)`** (Java 10+): an immutable **copy** (returns the same instance if the input is already an immutable `List.of`).
- `stream.toList()` (Java 16): unmodifiable, but **allows nulls**.

For defensive copying of inputs, use `List.copyOf` (a snapshot). For exposing internal state read-only without copying, an unmodifiable view works, if you accept that it reflects later changes. `Arrays.asList` is fixed-size but **writable** through to the backing array.
