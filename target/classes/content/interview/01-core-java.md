---
topic: Core Java
order: 1
---
## [0-1] What is the difference between JDK, JRE and JVM?
**Short answer:** the JVM runs bytecode, the JRE is the JVM plus the standard libraries, and the JDK is the JRE plus development tools.

- **JVM**: loads classes, verifies bytecode, runs it (interpreter plus JIT) and manages memory with the garbage collector. It is platform specific.
- **JRE**: JVM + core libraries (`java.lang`, `java.util` and so on). Enough to *run* a program.
- **JDK**: JRE + `javac`, `jar`, `jshell`, `jcmd`, `jlink` and other tools. You need it to *build* programs.

Since Java 11 there is no separate JRE download. You ship the JDK, or build a trimmed runtime with `jlink`.

**Interviewers also ask:** "Where does `javac` fit?" It compiles `.java` into `.class` bytecode. It is part of the JDK, not the JVM.

## [0-1] Why is Java called platform independent? Is the JVM platform independent?
Java source compiles to **bytecode**, which is not tied to any CPU or operating system. Each platform has its own JVM that translates bytecode to native instructions. So:

- **Bytecode** is platform independent ("write once, run anywhere").
- **The JVM is platform dependent**: there is a different JVM build for Windows, Linux and macOS.

A good extra point: the JIT compiler turns frequently used bytecode into optimised machine code at runtime, so portability does not cost much speed.

## [0-1] What is the difference between == and equals(), and what is the Integer cache trap?
`==` compares **primitive values** or, for objects, whether two references point to the **same object**. `equals()` compares **logical content**, if the class overrides it (`String`, `Integer`, `LocalDate` do; plain `Object` does not).

```java
Integer a = 127, b = 127;
Integer c = 128, d = 128;
System.out.println(a == b);       // true  (cached)
System.out.println(c == d);       // false (two objects)
System.out.println(c.equals(d));  // true
```

`Integer.valueOf` caches values from **-128 to 127**. Outside that range autoboxing creates new objects, so `==` fails. Always use `equals()` for wrapper objects.

## [1-3] Is Java pass by value or pass by reference?
Java is **always pass by value**. For objects, the value that is copied is the **reference**, not the object.

```java
void change(StringBuilder sb, int n) {
    sb.append("!");          // modifies the SAME object: caller sees it
    sb = new StringBuilder("new");  // only the local copy of the reference changes
    n = 99;                  // caller's int is unchanged
}
```

- You **can** mutate the object through the copied reference.
- You **cannot** make the caller's variable point somewhere else.
- This is why a classic `swap(a, b)` method cannot work in Java.

## [1-3] What is the contract between equals() and hashCode()?
Rules:

1. If `a.equals(b)` is true, then `a.hashCode() == b.hashCode()` must be true.
2. Equal hash codes do **not** imply equal objects (collisions are allowed).
3. `equals` must be reflexive, symmetric, transitive, consistent, and `x.equals(null)` is false.

If you override one without the other, hash based collections break: a `HashMap.get()` looks in the wrong bucket and returns `null` for a key that "is" in the map.

```java
@Override public boolean equals(Object o) {
    return o instanceof Customer c && id.equals(c.id);
}
@Override public int hashCode() { return Objects.hash(id); }
```

**Pitfall:** never base `hashCode` on mutable fields if the object is used as a map key. If the field changes after insertion, the entry becomes unreachable.

## [1-3] What are autoboxing and unboxing, and what can go wrong?
Autoboxing converts a primitive to its wrapper (`int` to `Integer`) automatically; unboxing does the reverse.

- **NullPointerException**: `int x = map.get("missing");` unboxes a `null`.
- **Performance**: `Long sum = 0L; for (...) sum += i;` creates a new `Long` on every iteration. Use `long`.
- **Identity trap**: `==` on wrappers (see the Integer cache above).
- **Overload surprises**: `list.remove(int index)` versus `list.remove(Object o)` with a `List<Integer>`.

Rule of thumb: use primitives for computation, wrappers only when you need generics, collections or null.

## [3-5] In what order do static blocks, instance blocks and constructors run with inheritance?
1. Parent **static** init (once, when the class loads)
2. Child **static** init (once)
3. Parent **instance** init blocks and field initialisers, then parent constructor
4. Child **instance** init blocks and field initialisers, then child constructor

```java
class A { static { p("A static"); } { p("A init"); } A() { p("A ctor"); } }
class B extends A { static { p("B static"); } { p("B init"); } B() { p("B ctor"); } }
new B();
// A static, B static, A init, A ctor, B init, B ctor
```

**Classic bug:** calling an overridable method from a parent constructor. The child's override runs *before* the child's fields are initialised, so it sees defaults (`null`, `0`).

## [3-5] How do you design an immutable class in Java? Why would you?
Steps:

1. Make the class `final` (or use a private constructor and factories).
2. Make all fields `private final`.
3. No setters.
4. **Defensive copies** of mutable inputs in the constructor and of mutable outputs in getters.
5. Do not let `this` escape from the constructor.

```java
public final class Order {
    private final String id;
    private final List<String> items;
    public Order(String id, List<String> items) {
        this.id = id;
        this.items = List.copyOf(items);   // defensive, unmodifiable copy
    }
    public List<String> items() { return items; }
}
```

**Why:** thread safe without locks, safe as map keys, easy to reason about and cache. In modern Java, a `record` gives you most of this (still copy mutable components yourself).

## [5+] What do you check when migrating a large application from Java 8 to Java 17 or 21?
**Language and library**
- Removed APIs: `javax.xml.bind`, `javax.activation`, Nashorn, parts of CORBA. Add the Jakarta artifacts back as dependencies if needed.
- **Strong encapsulation** of JDK internals: reflection into `sun.*` or `java.base` internals fails. Fix libraries, or use `--add-opens` as a temporary bridge.
- Deprecated for removal: `SecurityManager`, finalization.

**Frameworks**
- Spring Boot 3 requires Java 17+ and moves from `javax.*` to **`jakarta.*`** packages.
- Upgrade Lombok, Mockito, Byte Buddy, Jackson and your build plugins first.

**Runtime**
- G1 is the default GC; re-test GC settings and heap sizes, drop old CMS flags.
- JVM is container aware; prefer `-XX:MaxRAMPercentage` over fixed `-Xmx`.

**Process:** upgrade in steps (8, then 11, then 17, then 21), run with `-Xlint` and `jdeps` to find internal API use, keep CI green at each step, and load test before release. Gains: records, sealed types, text blocks, better GC, virtual threads (21).

## [0-1] What do the keywords static, final, this and super mean?
- **`static`**: belongs to the **class**, not to an object. There is one shared copy. A static method cannot directly use instance fields (there is no `this`). A `static {}` block runs once when the class is loaded.
- **`final`**: a variable can be assigned only once; a method cannot be overridden; a class cannot be extended (`String` is final). A `final` reference still allows the **object** to change (`final List<String> l` can still `l.add(...)`).
- **`this`**: the current object. `this(...)` calls another constructor of the same class.
- **`super`**: the parent class. `super.method()` calls the parent's version; `super(...)` calls a parent constructor.

```java
class Counter {
    static int created = 0;          // shared by all objects
    private final int id;            // set once per object
    Counter() { this.id = ++created; }
}
```

## [0-1] What is the difference between final, finally and finalize?
- **`final`**: a keyword for constants, non-overridable methods and non-extendable classes.
- **`finally`**: a block after `try/catch` that runs whether or not an exception occurred; used for cleanup (prefer try-with-resources).
- **`finalize()`**: an old `Object` method the GC could call before reclaiming an object. It is unpredictable, slow and **deprecated for removal** (Java 18). Use `try-with-resources` or `java.lang.ref.Cleaner` instead.

Interviewers like the follow-up "does `finally` always run?" See the try-with-resources answer: not if the JVM exits or crashes.

## [1-3] What is the difference between shallow copy and deep copy? How does clone() work?
- **Shallow copy**: copies the object, but nested objects are **shared** between original and copy.
- **Deep copy**: copies the whole graph, so changes to nested objects do not affect the original.

`Object.clone()` (needs `implements Cloneable`) is a shallow field-by-field copy, it is `protected`, bypasses constructors and is widely considered **broken by design**.

Better options:
- A **copy constructor** or static factory: `new Order(other)` that copies mutable parts explicitly.
- **Immutable objects** (records, `List.copyOf`): no copying needed.
- A mapping library (MapStruct) or serialisation round trip for complex graphs (slower).

## [3-5] Explain generics, wildcards and type erasure.
Generics add **compile-time type safety** and remove casts: `List<String>` cannot accept an `Integer`.

**Type erasure:** the compiler removes type arguments, so at runtime `List<String>` and `List<Integer>` are the same class. Consequences: you cannot write `new T()`, `new T[10]`, `x instanceof List<String>`, or overload `m(List<String>)` and `m(List<Integer>)`.

**Wildcards and PECS ("Producer Extends, Consumer Super"):**
- `List<? extends Number>`: you can **read** Numbers; you cannot add (except null).
- `List<? super Integer>`: you can **add** Integers; reads give `Object`.

```java
static double sum(List<? extends Number> src) { ... }          // producer
static void fill(List<? super Integer> dst) { dst.add(1); }    // consumer
static <T extends Comparable<T>> T max(List<T> l) { ... }      // bounded type
```
Also know **heap pollution** (raw types bypass checks) and why generic arrays are unsafe.

## [3-5] What are annotations, and how are they processed?
Annotations are **metadata** attached to code. Key settings:
- `@Retention`: `SOURCE` (compiler only), `CLASS` (in the class file), `RUNTIME` (readable by reflection).
- `@Target`: where it may be used (type, method, field, parameter).

Processing happens in two ways:
1. **Compile time**: annotation processors generate or check code (**Lombok**, **MapStruct**, Dagger).
2. **Runtime**: frameworks read them with **reflection** (Spring `@Service`, JPA `@Entity`, JUnit `@Test`).

```java
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
public @interface Timed { String value() default ""; }
```
Reflection has startup and memory costs, which is why Spring AOT and GraalVM move this work to **build time**.

## [5+] What are source and binary compatibility, and how do you evolve a public library API safely?
- **Source compatible**: client code still **compiles** against the new version.
- **Binary compatible**: already **compiled** clients still link and run without recompiling.

Usually safe: adding a class, adding a method to a **class**, adding a `default` method to an interface, widening access, deprecating.

Breaking: removing or renaming a public member, changing a signature, return type or checked exceptions, adding an **abstract** method to an interface, moving a class between packages, or changing documented behaviour.

**Practices:** semantic versioning, `@Deprecated(since="2.1", forRemoval=true)` with a migration path and a long window, keep implementation in non-exported packages (`module-info` `exports`), prefer interfaces and factories to extensible classes, use `sealed` or `final` to limit extension, and run **japicmp** or **Revapi** in CI to catch breaking changes automatically.
