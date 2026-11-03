---
topic: Strings and exceptions
order: 3
---
## [0-1] Why is String immutable and what is the string pool?
`String` is immutable: once created, its characters cannot change. Reasons:

- **Security**: strings carry class names, file paths, URLs and credentials; they must not change after validation.
- **Thread safety**: no locks needed.
- **Caching**: the hash code is cached, so strings are fast map keys.
- **String pool**: literals with the same text share one object.

```java
String a = "java";
String b = "java";
String c = new String("java");
System.out.println(a == b);          // true  (same pooled object)
System.out.println(a == c);          // false (new heap object)
System.out.println(a == c.intern()); // true
```

Since Java 7 the pool lives on the heap, so unused pooled strings can be garbage collected.

## [0-1] String versus StringBuilder versus StringBuffer?
| | String | StringBuilder | StringBuffer |
|---|---|---|---|
| Mutable | No | Yes | Yes |
| Thread safe | Yes (immutable) | **No** | Yes (synchronized) |
| Speed for edits | Slow (new object each time) | Fastest | Slower |

Use `StringBuilder` for building text in loops. `StringBuffer` is rarely needed, because shared mutable text is usually a design smell. For a one-line concatenation, `+` is fine: modern javac compiles it to an efficient `invokedynamic` call.

## [1-3] What is the difference between checked and unchecked exceptions?
- **Checked** (`Exception` but not `RuntimeException`): the compiler forces you to handle or declare them. Meant for recoverable conditions the caller can reasonably handle, for example `IOException`.
- **Unchecked** (`RuntimeException`, `Error`): no compile-time requirement. Usually programming errors (`NullPointerException`, `IllegalArgumentException`) or unrecoverable problems (`OutOfMemoryError`).

Practical guidance: modern frameworks (Spring) favour unchecked exceptions, because checked ones leak implementation details through every layer and break lambdas. Wrap low-level checked exceptions into a meaningful domain exception and **keep the cause**:

```java
try { repo.save(order); }
catch (SQLException e) { throw new OrderStorageException("Could not save order " + order.id(), e); }
```

## [1-3] How do try-with-resources and finally behave? Any traps?
`try-with-resources` closes every `AutoCloseable` automatically, in **reverse order** of declaration, even if an exception occurs.

```java
try (var in = new FileInputStream(f); var out = new FileOutputStream(g)) {
    in.transferTo(out);
}   // out closed first, then in
```

If the body *and* `close()` both throw, the body's exception wins and the close exception is attached as **suppressed** (`getSuppressed()`).

`finally` traps:
- A `return` or `throw` inside `finally` **overrides** the one from `try` and can hide the original exception.
- `finally` does not run if the JVM exits (`System.exit`), crashes, or the thread is killed or stuck in an endless loop.

## [3-5] What are the best practices for exception handling in a layered Spring application?
1. **Do not swallow exceptions** (`catch (Exception e) {}`). Handle, translate, or let them propagate.
2. **Log once**, at the boundary that handles it. "Log and rethrow" creates duplicate noise.
3. **Preserve the cause** when wrapping.
4. Throw **specific domain exceptions** (`OrderNotFoundException`) rather than generic `RuntimeException`.
5. Centralise HTTP mapping in a `@RestControllerAdvice` that returns a consistent error body (RFC 9457 `ProblemDetail`).
6. Never leak stack traces, SQL or internal class names to clients; include a **correlation id** instead.
7. Validate early (`@Valid`, preconditions) so you fail fast with a clear message.
8. Keep `@Transactional` rollback rules in mind: by default only unchecked exceptions roll back.

## [5+] Are exceptions expensive? Is it acceptable to use them for control flow?
Throwing is not free: the main cost is **`fillInStackTrace()`**, which walks the call stack when the exception is *created*. In deep stacks (frameworks, proxies) that is a lot of work. Handling an exception also prevents some JIT optimisations on that path.

So:
- Do not use exceptions for ordinary control flow (loop exit, "not found" in a hot path). Return `Optional`, a result type or a boolean.
- For rare, truly exceptional cases they are fine.
- If you must throw frequently (for example a preallocated signal exception), disable stack capture: `super(message, cause, false, false)` or override `fillInStackTrace()`.

Always measure before optimising: in most business code the I/O around the exception costs far more than the exception itself.

## [0-1] What is the difference between throw and throws? Can a try block exist without catch?
- **`throw`** actually throws an exception object: `throw new IllegalArgumentException("bad");`
- **`throws`** appears in a method signature and **declares** which checked exceptions the method may pass to its caller.

A `try` can be used **without `catch`** if it has a `finally` or is a try-with-resources.

```java
void read() throws IOException {          // declares
    if (!file.exists()) throw new FileNotFoundException(file.getName());   // throws
}
```

Multi-catch (`catch (IOException | SQLException e)`) handles several types in one block. Catch the **most specific** exception first, and never catch `Throwable` or `Error` casually.

## [1-3] How do you avoid and debug NullPointerException?
**Debugging:** since Java 14 (on by default from 15) **helpful NPE messages** name the exact null expression, for example `Cannot invoke "String.length()" because "user.name" is null`. Read the stack trace from the top line in your own code.

**Prevention:**
- **Do not return null**: return empty collections or `Optional`.
- Validate inputs at the boundary: `Objects.requireNonNull(x, "x must not be null")`.
- Use `Objects.requireNonNullElse`, `Optional.map/orElse`, and literal-first comparisons (`"ADMIN".equals(role)`).
- Annotate with `@Nullable`/`@NonNull` and let the IDE or static analysis (SpotBugs, NullAway) check them.
- Prefer **immutable objects** whose fields are fully set in the constructor.
- Beware of **unboxing** a null `Integer` and `Map.get` on a missing key.
