---
topic: JVM internals and performance
order: 7
---
## [0-1] What is garbage collection? Can you force it?
Garbage collection automatically frees memory of objects that are **no longer reachable** from any **GC root** (thread stacks, static fields, JNI references). You do not call `free()`.

`System.gc()` is only a **hint**; the JVM may ignore it. It is almost never right in application code, and a full GC pause can hurt latency. If memory is being retained, the fix is to remove the unwanted references (a leak), not to call `gc()`.

Another point: finalizers are deprecated for removal. Use `try-with-resources` or `Cleaner` for non-memory resources.

## [1-3] What are the JVM memory areas?
- **Heap** (shared): all objects. Split into **young** (Eden + two survivor spaces) and **old** generation.
- **Metaspace** (native memory): class metadata. It replaced PermGen in Java 8 and grows dynamically.
- **Thread stacks** (one per thread): frames with local variables and partial results; primitives live here, objects live on the heap.
- **PC register**, **native method stacks**.
- **Code cache**: JIT-compiled machine code.
- **Direct memory**: `ByteBuffer.allocateDirect`, used by NIO and Netty.

Total process memory is more than `-Xmx`: add metaspace, stacks, code cache, direct buffers and GC structures. Remember this when sizing containers.

## [1-3] What is the difference between StackOverflowError and OutOfMemoryError? What kinds of OOM exist?
- **StackOverflowError**: a thread's stack is exhausted, typically by runaway recursion. Fix the recursion or increase `-Xss` only as a last resort.
- **OutOfMemoryError**: the JVM cannot allocate. The message tells you which area:
  - `Java heap space`: live data is bigger than the heap, or there is a leak.
  - `GC overhead limit exceeded`: GC runs constantly and frees almost nothing.
  - `Metaspace`: too many classes (class loader leak, dynamic proxies, hot redeploy).
  - `unable to create native thread`: OS or container limit reached.
  - `Direct buffer memory`.

## [3-5] How does generational garbage collection work? Compare G1 and ZGC.
Most objects die young, so the heap is split into generations:
1. New objects go to **Eden**. A **minor GC** copies survivors to a survivor space; objects that survive enough cycles are **promoted** to the old generation.
2. The old generation is collected less often (mark, sweep or compact; in G1 via mixed collections).

| | G1 (default since Java 9) | ZGC |
|---|---|---|
| Layout | Heap divided into regions | Region based, colored pointers |
| Goal | Balance throughput with a **pause target** (`-XX:MaxGCPauseMillis`) | Very **low pause** (sub-millisecond to few ms) on large heaps |
| Cost | Pauses grow with live set | A little more CPU and memory overhead |
| Use | General services | Latency-sensitive services, big heaps |

Choosing: start with G1, read GC logs (`-Xlog:gc*`), and move to ZGC only if pauses break your SLO. Tune heap size before exotic flags.

## [3-5] Explain class loading and the difference between ClassNotFoundException and NoClassDefFoundError.
Class loaders form a hierarchy: **Bootstrap** (core JDK), **Platform** (JDK extensions), **Application** (your classpath), plus custom ones (web servers, plugins). They use **parent delegation**: ask the parent first, so core classes cannot be replaced.

Steps for a class: **load**, **link** (verify, prepare, resolve), **initialise** (static blocks run once).

- `ClassNotFoundException` (checked): an explicit load by name failed (`Class.forName("x.Y")`). The class is not on the classpath.
- `NoClassDefFoundError` (an Error): the class **was present at compile time** but is missing at runtime, or its static initialiser failed earlier (`ExceptionInInitializerError`). Typical causes: a missing dependency or version conflicts.

## [5+] A production service is slowly running out of memory. How do you investigate?
1. **Confirm** the pattern in metrics: heap after GC trending up (a leak) versus a high but flat plateau (undersized heap).
2. Enable evidence: `-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=...` and GC logs `-Xlog:gc*`.
3. Take a **heap dump** (`jcmd <pid> GC.heap_dump`) and open it in Eclipse MAT or VisualVM. Look at the **dominator tree** and **path to GC roots** for the biggest retained objects.
4. Use **JFR** (`jcmd <pid> JFR.start`) for allocation profiling with low overhead.
5. Check usual suspects: unbounded caches or maps, static collections, **`ThreadLocal`** values in pooled threads, listeners never removed, unclosed resources, large result sets loaded into memory, class loader leaks.
6. Reproduce with a load test, fix, and add a **bounded** structure (size limit, TTL) plus an alert.

Also differentiate heap from native memory: if heap looks fine but RSS grows, check direct buffers, thread count and metaspace (`-XX:NativeMemoryTracking`).

## [5+] How does the JIT compiler work, and what matters for running Java in containers?
**Tiered compilation:** code starts in the **interpreter**, hot methods are compiled by **C1** (fast, lightly optimised) and the hottest by **C2** (heavy optimisation). Key optimisations: **inlining**, **escape analysis** (stack allocation or scalar replacement of non-escaping objects), loop unrolling, dead code elimination. The JVM can **deoptimise** if assumptions break.

Practical effects:
- **Warm-up**: the first requests are slower; do not benchmark cold code (use JMH).
- Startup-sensitive workloads (serverless) can use **CDS/AppCDS**, **CRaC**, or **GraalVM native image** (fast start and low memory, but closed-world limits on reflection and lower peak throughput).

**Containers:**
- The JVM reads cgroup limits. Prefer `-XX:MaxRAMPercentage=70` over a fixed `-Xmx`, leaving room for non-heap memory.
- Give at least 2 CPUs for sensible GC and JIT behaviour; with a very low CPU limit, compilation and GC threads compete with your requests.
- Set `-XX:+ExitOnOutOfMemoryError` so Kubernetes restarts the pod cleanly.

## [3-5] What are strong, soft, weak and phantom references? Where are they used?
- **Strong** (normal): never collected while reachable.
- **`SoftReference`**: collected only when memory is **low**. Used for memory-sensitive caches (used less these days).
- **`WeakReference`**: collected at the **next GC** once no strong references remain. `WeakHashMap` uses weak keys, so entries disappear when the key is no longer used elsewhere (canonicalising maps, listener registries).
- **`PhantomReference`**: the object is already finalised; used with a `ReferenceQueue` for **post-mortem cleanup** (the basis of `Cleaner`).

Caution: a good cache needs size limits and eviction policy (Caffeine); relying on GC and soft references makes behaviour unpredictable.

## [3-5] Which JVM flags do you use most, and how do you size the heap?
Common flags:
- `-Xms` / `-Xmx`: initial and maximum heap (often equal in servers to avoid resizing). In containers prefer `-XX:MaxRAMPercentage=70`.
- `-Xss`: thread stack size.
- `-XX:+UseG1GC` / `-XX:+UseZGC`: choose the collector.
- `-Xlog:gc*:file=gc.log`: GC logging (always on in production).
- `-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=...` and `-XX:+ExitOnOutOfMemoryError`.
- `-XX:MaxMetaspaceSize` (a safety cap), `-XX:+AlwaysPreTouch` (reserve memory up front).

**Sizing the heap:** run a realistic load, look at the **live set after full or mixed GC** and set the heap to about **2 to 3 times the live set** to keep GC overhead low. Leave room outside the heap (metaspace, thread stacks, direct buffers, code cache) in the container limit. Verify with GC logs (pause times, frequency), not guesswork, and change **one setting at a time**.
