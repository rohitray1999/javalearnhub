---
topic: Java 8 and modern Java
order: 6
---
## [0-1] What is a lambda expression and a functional interface?
A **lambda** is a short anonymous function: `(a, b) -> a + b`. It can be used wherever a **functional interface** (an interface with exactly one abstract method) is expected. `@FunctionalInterface` makes the compiler enforce that.

| Interface | Method | Use |
|---|---|---|
| `Predicate<T>` | `boolean test(T)` | filtering |
| `Function<T,R>` | `R apply(T)` | mapping |
| `Consumer<T>` | `void accept(T)` | side effects |
| `Supplier<T>` | `T get()` | lazy values |
| `Comparator<T>` | `int compare(T,T)` | ordering |

Lambdas can capture only **effectively final** local variables. A method reference (`String::length`) is a shorthand for a lambda that just calls one method.

## [1-3] What are intermediate and terminal operations? What is the difference between map and flatMap?
A stream pipeline has a source, **intermediate** operations (lazy, return a stream: `filter`, `map`, `sorted`, `distinct`, `limit`) and one **terminal** operation (starts execution: `collect`, `count`, `forEach`, `reduce`, `toList`). Nothing runs until the terminal operation.

- `map`: one input to one output.
- `flatMap`: one input to **a stream** of outputs, which are flattened into a single stream.

```java
List<List<String>> nested = List.of(List.of("a", "b"), List.of("c"));
nested.stream().flatMap(List::stream).toList();   // [a, b, c]

Map<String, List<Order>> byCity =
    orders.stream().collect(Collectors.groupingBy(Order::city));
```

A stream can be consumed only once.

## [1-3] How should you use Optional? What are the anti-patterns?
`Optional<T>` makes "may be absent" explicit **in return types**.

Good:
```java
String name = repo.findById(id).map(User::name).orElse("unknown");
repo.findById(id).orElseThrow(() -> new NotFoundException(id));
```

Anti-patterns:
- Calling `get()` without checking (use `orElseThrow`).
- `Optional` as a **field**, **parameter** or in collections (use overloads, or empty collections).
- `if (opt.isPresent()) return opt.get();` (use `map`, `orElse`).
- `orElse(expensiveCall())` evaluates **always**; use `orElseGet(() -> expensiveCall())` for lazy evaluation.
- Returning `null` from a method that returns `Optional`.

## [3-5] What are the pitfalls of parallel streams?
`parallelStream()` splits work across the shared `ForkJoinPool.commonPool()`. Problems:

- **Shared pool**: a slow or blocking task can starve other parallel work in the JVM (including other requests in a web app).
- **Overhead**: for small data or cheap operations it is slower than sequential.
- **Bad sources**: `LinkedList`, `Stream.iterate` split poorly; `ArrayList`, arrays and ranges split well.
- **Shared mutable state** in lambdas causes races (`list.add` in `forEach`).
- Ordering: `forEach` is unordered; `forEachOrdered` costs parallelism.
- Boxing (`Stream<Integer>`) hurts; prefer `IntStream`.

Use it only for **large, CPU-bound, stateless** pipelines, and **measure**. For I/O, use a proper executor or virtual threads.

## [3-5] Explain records, sealed classes and pattern matching.
- **Record (Java 16)**: an immutable data carrier; the compiler generates the constructor, accessors, `equals`, `hashCode` and `toString`. Supports compact constructors for validation.
- **Sealed types (Java 17)**: restrict which classes may extend or implement a type (`permits`). The compiler then knows all subtypes.
- **Pattern matching (`instanceof` 16, `switch` 21)**: test and bind in one step, and with sealed types the switch can be **exhaustive** (no `default` needed).

```java
sealed interface Shape permits Circle, Rect {}
record Circle(double r) implements Shape {}
record Rect(double w, double h) implements Shape {}

double area(Shape s) {
    return switch (s) {
        case Circle c -> Math.PI * c.r() * c.r();
        case Rect r   -> r.w() * r.h();
    };
}
```
Together they model "one of these cases" data (algebraic data types) cleanly, replacing visitor boilerplate.

## [3-5] Which features between Java 9 and Java 21 do you actually use?
- **9**: `List.of`, `Map.of`, private interface methods, the module system (rarely hand-written).
- **10**: `var` for local variables.
- **11 (LTS)**: `HttpClient`, `String.isBlank/strip/lines`, `Files.readString`.
- **14 to 17**: switch expressions, **text blocks**, records, sealed classes, pattern `instanceof`.
- **21 (LTS)**: **virtual threads**, pattern matching for `switch`, record patterns, **sequenced collections** (`getFirst`, `reversed`).
- Under the hood: better GCs (G1 default, ZGC), faster startup, improved container support.

Mention how you applied one: for example, "we replaced a visitor with sealed types and a pattern switch" or "virtual threads simplified our blocking I/O service".

## [5+] How do Collectors work internally? How do you handle duplicate keys and write a custom collector?
A `Collector<T, A, R>` has four parts: **supplier** (create the mutable container), **accumulator** (add an element), **combiner** (merge two containers, used by parallel streams) and **finisher** (convert to the result).

```java
Collector<String, StringJoiner, String> joinWithComma = Collector.of(
    () -> new StringJoiner(","),   // supplier
    StringJoiner::add,             // accumulator
    StringJoiner::merge,           // combiner
    StringJoiner::toString);       // finisher
```

Gotchas:
- `Collectors.toMap` throws `IllegalStateException` on **duplicate keys**; pass a merge function: `toMap(k, v, (a, b) -> a)`.
- `toMap` rejects `null` values.
- `groupingBy` accepts a **downstream collector**: `groupingBy(city, counting())`, `mapping`, `summingInt`.
- `Collectors.teeing` (Java 12) computes two results in one pass.
- `toList()` on a stream (Java 16) returns an unmodifiable list; `Collectors.toList()` makes no promise either way.

## [0-1] What are default and static methods in interfaces, and why were they added?
Before Java 8, adding a method to an interface broke every implementing class. **`default` methods** give an implementation inside the interface so existing classes keep working. This is how `List.sort`, `Collection.stream` and `Map.getOrDefault` were added without breaking the world.

**`static` methods** in interfaces hold utilities tied to the interface (`Comparator.comparing`, `List.of`). Java 9 added **`private` methods** to share code between defaults.

```java
interface Greeter {
    String name();
    default String greet() { return "Hello, " + name(); }
    static Greeter of(String n) { return () -> n; }
}
```
Rules: a class can override a default; if two interfaces conflict, the class must resolve it. Interfaces still cannot have instance state.

## [1-3] Why was the java.time API introduced? How do you use it?
`java.util.Date` and `Calendar` were mutable, not thread safe, had confusing month indexing and poor time zone handling. **`java.time`** (Java 8) is immutable, thread safe and clear:

- `LocalDate`, `LocalTime`, `LocalDateTime`: **no time zone** (a calendar date or wall-clock time).
- `ZonedDateTime`, `OffsetDateTime`: with zone or offset.
- **`Instant`**: a point on the UTC timeline; best for timestamps and storage.
- `Duration` (time-based) and `Period` (date-based), `DateTimeFormatter` for parsing and formatting.

```java
Instant now = Instant.now();
LocalDate due = LocalDate.now().plusDays(30);
ZonedDateTime ist = now.atZone(ZoneId.of("Asia/Kolkata"));
long days = ChronoUnit.DAYS.between(start, end);
```
Good practice: store and transfer **UTC `Instant`s**, convert to the user's zone only for display, and inject a **`Clock`** so time can be controlled in tests.

## [3-5] How do you handle checked exceptions inside lambdas?
Standard functional interfaces (`Function`, `Consumer`) do not declare checked exceptions, so this does not compile:
```java
paths.stream().map(p -> Files.readString(p));   // IOException
```
Options:
1. **Handle inside** the lambda (try/catch) and return a default or wrap in an unchecked exception (`UncheckedIOException`).
2. Extract a **helper method** that wraps and rethrows; keeps the stream readable.
3. Define your own functional interface that `throws Exception` and a small adapter:

```java
@FunctionalInterface interface ThrowingFunction<T, R> { R apply(T t) throws Exception; }

static <T, R> Function<T, R> unchecked(ThrowingFunction<T, R> f) {
    return t -> { try { return f.apply(t); } catch (Exception e) { throw new RuntimeException(e); } };
}
```
4. If error handling is the main logic (collect failures separately), use a loop or a result type instead of forcing a stream.
