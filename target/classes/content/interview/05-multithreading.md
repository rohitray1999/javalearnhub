---
topic: Multithreading and concurrency
order: 5
---
## [0-1] What are the ways to create a thread? What is the difference between start() and run()?
Ways:
1. Extend `Thread` and override `run()`. (Uses up your single inheritance.)
2. Implement `Runnable` and pass it to a `Thread`. (Preferred over 1.)
3. Implement `Callable<V>`, which **returns a value and can throw**, and submit it to an `ExecutorService`, which returns a `Future<V>`.
4. In real code, you rarely create raw threads: use an executor, `CompletableFuture`, or (Java 21) virtual threads.

`start()` asks the JVM to create a **new thread** that then calls `run()`. Calling `run()` directly just executes the method on the **current thread**, with no concurrency. Calling `start()` twice throws `IllegalThreadStateException`.

## [0-1] What are the states in a thread's lifecycle?
`NEW` (created, not started) then `RUNNABLE` (running or ready to run) then, depending on what it does:
- `BLOCKED`: waiting to acquire a monitor lock (`synchronized`).
- `WAITING`: waiting indefinitely (`wait()`, `join()`, `LockSupport.park()`).
- `TIMED_WAITING`: waiting with a timeout (`sleep(ms)`, `wait(ms)`, `join(ms)`).
- `TERMINATED`: finished.

A useful follow-up: `jstack` thread dumps show these states, and many threads `BLOCKED` on the same lock point to contention or a deadlock.

## [1-3] What does volatile do? How is it different from synchronized and Atomic classes?
`volatile` gives **visibility** and ordering: a write to a volatile variable is visible to every thread that later reads it, and it stops certain reorderings. It does **not** give atomicity for compound actions: `count++` is read-modify-write and is still a race.

| Tool | Visibility | Atomicity of compound actions | Blocks? |
|---|---|---|---|
| `volatile` | yes | no | no |
| `synchronized` / `ReentrantLock` | yes | yes (mutual exclusion) | yes |
| `AtomicInteger` and friends | yes | yes for single variable (CAS) | no (lock-free) |

Use `volatile` for flags (`running`), `AtomicXxx` for counters, locks for multi-variable invariants. Under heavy contention `LongAdder` beats `AtomicLong`.

## [1-3] How do wait(), notify() and sleep() differ?
- `sleep(ms)` pauses the current thread and **keeps** any locks it holds.
- `wait()` **releases** the object's monitor and waits until another thread calls `notify()` or `notifyAll()` on the same object.
- `wait`, `notify` and `notifyAll` must be called inside a `synchronized` block on that object (else `IllegalMonitorStateException`).

Always wait in a **loop**, to handle spurious wakeups and changed conditions:

```java
synchronized (queue) {
    while (queue.isEmpty()) queue.wait();
    item = queue.remove();
}
```

Prefer higher level tools (`BlockingQueue`, `CountDownLatch`, `Condition`) over raw `wait/notify`.

## [3-5] How does ThreadPoolExecutor work, and how do you size a pool?
Parameters: `corePoolSize`, `maximumPoolSize`, `keepAliveTime`, a **work queue**, a thread factory, and a rejection handler.

Order of events when a task is submitted:
1. If fewer than `core` threads exist, start a new thread.
2. Otherwise put the task in the **queue**.
3. Only if the queue is **full**, start extra threads up to `max`.
4. If that fails, the **rejection policy** applies: `AbortPolicy` (throws, default), `CallerRunsPolicy` (back-pressure), `DiscardPolicy`, `DiscardOldestPolicy`.

**Trap:** `Executors.newFixedThreadPool` uses an **unbounded** queue (memory can explode), and `newCachedThreadPool` allows unbounded threads. Build `ThreadPoolExecutor` yourself with a bounded queue and a named thread factory.

**Sizing:** CPU-bound work: about number of cores (+1). I/O-bound work: `cores * (1 + wait time / compute time)`. Then **measure**; separate pools per workload so a slow dependency cannot starve everything.

## [3-5] What is a deadlock? How do you prevent and diagnose it?
A deadlock is a cycle of threads each holding a lock another needs. It needs four conditions together: **mutual exclusion, hold and wait, no pre-emption, circular wait**.

```java
// Thread 1: lock(A) then lock(B)     Thread 2: lock(B) then lock(A)
```

**Prevention:**
- Always acquire locks in a **global order**.
- Use `tryLock(timeout)` and back off.
- Hold locks for the shortest time and never call unknown code while holding one.
- Prefer immutable data, concurrent collections and message passing to shared locks.

**Diagnosis:** take a thread dump (`jstack <pid>` or `jcmd <pid> Thread.print`). The JVM reports "Found one Java-level deadlock" with the lock cycle. Also look for threads stuck `BLOCKED` in monitoring.

## [3-5] How do you use CompletableFuture well?
It models asynchronous pipelines.

```java
CompletableFuture<Order> f =
    CompletableFuture.supplyAsync(() -> userClient.get(id), ioPool)
        .thenCompose(user -> orderClient.latest(user))      // async step returning a future
        .thenApply(this::toDto)                              // sync transform
        .exceptionally(ex -> fallbackOrder());
```

- `thenApply` is like `map`; `thenCompose` is like `flatMap`; `thenCombine` joins two independent futures; `allOf` and `anyOf` wait for many.
- **Pass your own executor.** The default is the shared `ForkJoinPool.commonPool()`, which is small and must not run blocking I/O.
- Handle errors with `exceptionally` or `handle`; an unhandled failure is silent until you `join()`.
- Add timeouts: `orTimeout`, `completeOnTimeout`.
- `get()` throws checked exceptions; `join()` throws `CompletionException`.

## [5+] Explain the Java Memory Model: happens-before, safe publication and double-checked locking.
The JMM defines when one thread's writes are guaranteed visible to another. Without a **happens-before** edge, the compiler, JIT and CPU may reorder or cache writes.

Key happens-before rules:
- Unlock of a monitor, then a later lock of the same monitor.
- Write to a `volatile` field, then a later read of it.
- `Thread.start()` before anything in the started thread; everything in a thread before `join()` returns.
- Writes to `final` fields in a constructor are visible after construction (if `this` did not escape).

**Safe publication** means making an object visible to other threads with such an edge (static initialiser, `volatile` or `final` field, a lock, a concurrent collection).

**Double-checked locking** is only correct with `volatile`; otherwise another thread can see a non-null reference to a **partly constructed** object:

```java
private static volatile Config instance;
static Config get() {
    if (instance == null) synchronized (Config.class) {
        if (instance == null) instance = new Config();
    }
    return instance;
}
```
Simpler: the holder-class idiom or an `enum`.

## [5+] What are virtual threads (Java 21)? When do they help and what are the pitfalls?
Virtual threads are **lightweight threads managed by the JVM**, mapped onto a small pool of carrier (platform) threads. A blocking call (socket, JDBC, `sleep`) **unmounts** the virtual thread, so you can run hundreds of thousands of them.

**Helps:** high-concurrency **I/O-bound** servers written in the simple "one thread per request" style, without reactive code.
**Does not help:** CPU-bound work (still limited by cores).

**Guidelines and pitfalls:**
- **Do not pool them.** Create one per task (`Executors.newVirtualThreadPerTaskExecutor()`).
- Limit access to scarce resources (DB connections) with a `Semaphore` or the pool itself; thousands of threads can overwhelm a database.
- **Pinning:** blocking inside `synchronized` (before Java 24) or a native call pins the carrier thread. Prefer `ReentrantLock` on older versions.
- Be careful with large `ThreadLocal` data.
- Spring Boot 3.2+: `spring.threads.virtual.enabled=true`.
- Structured concurrency and scoped values are the companion APIs; check their status for your Java version.

## [0-1] What is the difference between a process and a thread? What is a race condition?
A **process** is a running program with its **own memory space**; a **thread** is a unit of execution **inside** a process that **shares** the process's heap with other threads but has its own stack. Threads are cheaper to create and communicate, but sharing memory creates risk.

A **race condition** is when the result depends on the **timing** of threads accessing shared mutable data. Example: two threads doing `count++` (read, add, write) can lose an update.

Fixes: make the data immutable, confine it to one thread, or protect it with `synchronized`, a `Lock`, or atomic classes.

## [1-3] ExecutorService: submit versus execute, Future, and shutdown versus shutdownNow?
- `execute(Runnable)`: fire and forget; exceptions go to the thread's uncaught handler.
- `submit(Runnable/Callable)`: returns a **`Future`**; exceptions are **captured** and rethrown from `future.get()` as `ExecutionException`. A common bug is never calling `get()` and silently losing errors.
- `Future.get()` blocks (use a timeout); `cancel(true)` interrupts.
- `shutdown()`: stop accepting tasks, **let queued tasks finish**.
- `shutdownNow()`: try to stop running tasks (interrupt) and return the queued ones.

Standard shutdown:
```java
pool.shutdown();
if (!pool.awaitTermination(30, TimeUnit.SECONDS)) pool.shutdownNow();
```
Always shut pools down; non-daemon pool threads keep the JVM alive.

## [3-5] CountDownLatch, CyclicBarrier, Semaphore and Phaser: when do you use each?
- **`CountDownLatch(n)`**: one-shot; threads wait until the count reaches 0. Use for "wait until N services have started" or "main waits for N workers". **Cannot be reset**.
- **`CyclicBarrier(n)`**: n threads wait **for each other** at a barrier, then all proceed; **reusable**. Use for phased parallel computation.
- **`Semaphore(permits)`**: limits **concurrent access** to a resource (connection pool, rate limit). `acquire`/`release`.
- **`Phaser`**: a flexible, reusable barrier with a dynamic number of parties and multiple phases.

Also know `Exchanger`, and the higher-level `BlockingQueue` for producer-consumer hand-off, which is often the simplest tool.

## [3-5] What is ThreadLocal, and what are the dangers?
`ThreadLocal<T>` gives each thread its **own copy** of a variable. Typical uses: per-request context (user id, trace id), non-thread-safe helpers like `SimpleDateFormat`, transaction context (Spring uses it for `SecurityContext` and transactions).

**Dangers:**
- **Leaks in thread pools**: pooled threads live forever, so values stay after the request. Always call `remove()` in a `finally` (or a filter's cleanup).
- **Stale data** from the previous request on a reused thread.
- Does **not propagate** to child or async threads (use `InheritableThreadLocal`, a `TaskDecorator`, or Micrometer context propagation).
- With **virtual threads**, there can be millions of them, so heavy `ThreadLocal` values are costly. Prefer explicit parameters, or **`ScopedValue`** (Java 21+ preview, check your version).

## [5+] How do you diagnose thread pool starvation, lock contention and high CPU in production?
**High CPU**
1. Find the hot thread: `top -H -p <pid>`, convert the thread id to hex, then match it to the `nid=0x...` entry in `jstack`.
2. Or capture a **JFR** recording or an async-profiler flame graph to see where CPU goes (GC, JIT, a tight loop, regex backtracking, serialisation).

**Thread pool starvation**
- Symptoms: latency grows while CPU is low; threads in the pool are all `WAITING` or blocked on I/O; the queue grows.
- Check pool metrics (active, queue size, rejected) and thread dumps. Causes: blocking calls with no timeouts, tasks waiting on other tasks in the **same pool** (deadlock by starvation), pools sized too small, a slow downstream service.
- Fixes: timeouts, separate pools per dependency (bulkhead), bounded queues with back-pressure, async or virtual threads for I/O.

**Lock contention**
- Many threads `BLOCKED` on the same monitor in repeated thread dumps. JFR has lock contention events.
- Fixes: shorter critical sections, finer-grained locks or lock striping, `ConcurrentHashMap`, `LongAdder`, immutable or copy-on-write data.

Take **several dumps** a few seconds apart; one snapshot can mislead.
