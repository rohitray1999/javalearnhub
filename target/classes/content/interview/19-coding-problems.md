---
topic: Coding problems in Java
order: 19
---
## [0-1] Reverse a string and check for a palindrome.
```java
static String reverse(String s) {
    return new StringBuilder(s).reverse().toString();
}

// Palindrome: two pointers, ignore case and non-alphanumerics. O(n) time, O(1) space.
static boolean isPalindrome(String s) {
    int i = 0, j = s.length() - 1;
    while (i < j) {
        while (i < j && !Character.isLetterOrDigit(s.charAt(i))) i++;
        while (i < j && !Character.isLetterOrDigit(s.charAt(j))) j--;
        if (Character.toLowerCase(s.charAt(i++)) != Character.toLowerCase(s.charAt(j--))) return false;
    }
    return true;
}
```
Talking points: strings are immutable, so a manual reverse uses a `char[]` and swaps from both ends. Do not build the answer with `+=` in a loop (O(n squared)). Mention surrogate pairs if asked about emojis (`StringBuilder.reverse()` handles them).

## [0-1] Write FizzBuzz and a prime number check.
```java
for (int i = 1; i <= 100; i++) {
    if (i % 15 == 0)      System.out.println("FizzBuzz");   // check 15 first
    else if (i % 3 == 0)  System.out.println("Fizz");
    else if (i % 5 == 0)  System.out.println("Buzz");
    else                  System.out.println(i);
}

static boolean isPrime(int n) {
    if (n < 2) return false;
    for (int i = 2; (long) i * i <= n; i++) {      // only up to sqrt(n)
        if (n % i == 0) return false;
    }
    return true;
}
```
`isPrime` is **O(sqrt n)**. For many numbers up to N, use the **Sieve of Eratosthenes** (O(n log log n)). The common FizzBuzz mistake is testing 3 and 5 before 15.

## [0-1] Check whether two strings are anagrams.
Two strings are anagrams if they have the same characters with the same counts.

```java
static boolean isAnagram(String a, String b) {
    if (a.length() != b.length()) return false;
    int[] count = new int[26];                       // assuming lowercase a-z
    for (int i = 0; i < a.length(); i++) {
        count[a.charAt(i) - 'a']++;
        count[b.charAt(i) - 'a']--;
    }
    for (int c : count) if (c != 0) return false;
    return true;
}
```
**O(n)** time, O(1) space. The simpler alternative is to **sort both** char arrays and compare (O(n log n)). For Unicode use a `HashMap<Character, Integer>`. Ask whether case and spaces matter.

## [0-1] Find the first non-repeated character in a string. Write Fibonacci in different ways.
```java
static Character firstUnique(String s) {
    Map<Character, Integer> counts = new LinkedHashMap<>();    // keeps insertion order
    for (char c : s.toCharArray()) counts.merge(c, 1, Integer::sum);
    for (var e : counts.entrySet()) if (e.getValue() == 1) return e.getKey();
    return null;
}
```
**Fibonacci**
```java
static long fibIterative(int n) {            // O(n) time, O(1) space
    long a = 0, b = 1;
    for (int i = 0; i < n; i++) { long t = a + b; a = b; b = t; }
    return a;
}
static long fibRecursive(int n) { return n < 2 ? n : fibRecursive(n - 1) + fibRecursive(n - 2); } // O(2^n)

static long fibMemo(int n, long[] memo) {    // O(n)
    if (n < 2) return n;
    if (memo[n] != 0) return memo[n];
    return memo[n] = fibMemo(n - 1, memo) + fibMemo(n - 2, memo);
}
```
The plain recursion recomputes subproblems exponentially. A `long` overflows after `fib(92)`; use `BigInteger` beyond that.

## [1-3] Count word frequency and find the top 3 words using streams.
```java
Map<String, Long> freq = Arrays.stream(text.toLowerCase().split("\\W+"))
        .filter(w -> !w.isBlank())
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

List<String> top3 = freq.entrySet().stream()
        .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                         .thenComparing(Map.Entry.comparingByKey()))   // stable tie-break
        .limit(3)
        .map(Map.Entry::getKey)
        .toList();
```
Complexity: counting is O(n); sorting all entries is O(m log m) for m distinct words. For a very large m, keep a **min-heap of size k** (O(m log k)). Normalise case and punctuation, and say how you handle ties.

## [1-3] Solve Two Sum, and explain the complexity trade-off.
**Problem:** return indexes of two numbers that add up to `target`.

```java
static int[] twoSum(int[] nums, int target) {
    Map<Integer, Integer> seen = new HashMap<>();        // value -> index
    for (int i = 0; i < nums.length; i++) {
        Integer j = seen.get(target - nums[i]);
        if (j != null) return new int[] { j, i };
        seen.put(nums[i], i);
    }
    return new int[0];
}
```
- **Brute force**: two loops, O(n squared) time, O(1) space.
- **HashMap**: one pass, **O(n) time, O(n) space**.
- If the array is **sorted**, use two pointers (O(n) time, O(1) space).

Edge cases: duplicates (`[3,3]` and target 6), negative numbers, no answer, the same element used twice.

## [1-3] Check for valid parentheses.
Use a **stack**: push openers; on a closer, the top must be its matching opener.

```java
static boolean isValid(String s) {
    Deque<Character> stack = new ArrayDeque<>();
    for (char c : s.toCharArray()) {
        switch (c) {
            case '(', '[', '{' -> stack.push(c);
            case ')' -> { if (stack.isEmpty() || stack.pop() != '(') return false; }
            case ']' -> { if (stack.isEmpty() || stack.pop() != '[') return false; }
            case '}' -> { if (stack.isEmpty() || stack.pop() != '{') return false; }
            default -> { }
        }
    }
    return stack.isEmpty();                    // nothing left unmatched
}
```
**O(n)** time and space. Use `ArrayDeque`, not `Stack` (legacy, synchronised).

## [1-3] Implement binary search, and reverse a singly linked list.
```java
static int binarySearch(int[] a, int target) {       // sorted array, O(log n)
    int lo = 0, hi = a.length - 1;
    while (lo <= hi) {
        int mid = lo + (hi - lo) / 2;                 // avoids int overflow of (lo + hi)
        if (a[mid] == target) return mid;
        if (a[mid] < target) lo = mid + 1; else hi = mid - 1;
    }
    return -1;
}

static Node reverse(Node head) {                      // iterative, O(n) time, O(1) space
    Node prev = null;
    while (head != null) {
        Node next = head.next;      // 1. remember the rest
        head.next = prev;           // 2. flip the pointer
        prev = head;                // 3. advance prev
        head = next;                // 4. advance head
    }
    return prev;
}
```
Mention the recursive version (O(n) stack space) and the classic pitfalls: an off-by-one in the loop condition, and losing the `next` pointer before re-pointing.

## [1-3] Find the missing number in 0..n and detect duplicates in an array.
```java
static int missing(int[] nums) {                      // numbers 0..n with one missing
    int n = nums.length;
    int expected = n * (n + 1) / 2, actual = 0;
    for (int x : nums) actual += x;
    return expected - actual;                         // O(n), O(1) space
}
// Overflow-safe variant: XOR all indexes 0..n with all values.

static Set<Integer> duplicates(int[] nums) {
    Set<Integer> seen = new HashSet<>(), dup = new HashSet<>();
    for (int x : nums) if (!seen.add(x)) dup.add(x);  // add() returns false if already present
    return dup;
}
```
Follow-up: with values in `1..n`, you can find duplicates in **O(1) extra space** by marking visited indexes (negating `nums[abs(x)-1]`) or by cyclic sort, if modifying the input is allowed.

## [3-5] Implement producer-consumer in Java.
The easiest correct solution uses a **`BlockingQueue`**, which handles locking, waiting and signalling.

```java
BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(10);   // bounded = back-pressure
final int POISON = -1;

Thread producer = new Thread(() -> {
    try {
        for (int i = 0; i < 100; i++) queue.put(i);            // blocks if full
        queue.put(POISON);                                      // tell the consumer to stop
    } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
});

Thread consumer = new Thread(() -> {
    try {
        for (int v; (v = queue.take()) != POISON; ) process(v); // blocks if empty
    } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
});
producer.start(); consumer.start();
```
If asked for the low-level version: a shared buffer with `synchronized`, `wait()` in a **`while`** loop while full or empty, and `notifyAll()` after each change. With several consumers use one poison pill per consumer, or an `ExecutorService`.

## [3-5] Write code that creates a deadlock, then fix it.
```java
Object a = new Object(), b = new Object();

new Thread(() -> { synchronized (a) { sleep(100); synchronized (b) { work(); } } }).start();
new Thread(() -> { synchronized (b) { sleep(100); synchronized (a) { work(); } } }).start();
// Thread 1 holds A and waits for B; thread 2 holds B and waits for A: deadlock.
```
**Fix 1: a global lock order.** Always acquire `a` then `b` in both threads, so a cycle cannot form.

**Fix 2: `tryLock` with a timeout** and back off:
```java
if (lockA.tryLock(1, TimeUnit.SECONDS)) {
    try {
        if (lockB.tryLock(1, TimeUnit.SECONDS)) { try { work(); } finally { lockB.unlock(); } }
    } finally { lockA.unlock(); }
}
```
Diagnose with `jstack`, which prints "Found one Java-level deadlock". In real code the order is often hidden (two accounts in a transfer): order locks by a stable key such as the account id.

## [3-5] Find the top K frequent elements.
Count with a map, then keep a **min-heap of size K**.

```java
static List<Integer> topK(int[] nums, int k) {
    Map<Integer, Integer> counts = new HashMap<>();
    for (int x : nums) counts.merge(x, 1, Integer::sum);

    PriorityQueue<Map.Entry<Integer, Integer>> heap =
        new PriorityQueue<>(Map.Entry.comparingByValue());       // smallest count on top
    for (var e : counts.entrySet()) {
        heap.offer(e);
        if (heap.size() > k) heap.poll();                        // drop the least frequent
    }
    return heap.stream().map(Map.Entry::getKey).toList();
}
```
**O(n + m log k)** time, O(m) space, where m is the number of distinct values. Alternatives: sort all entries (O(m log m)), or **bucket sort** by frequency (O(n)). For an unbounded stream, mention approximate structures like **count-min sketch**.

## [5+] Implement a bounded blocking queue without using BlockingQueue.
```java
class BoundedQueue<T> {
    private final Object[] items;
    private int head, tail, count;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    BoundedQueue(int capacity) { items = new Object[capacity]; }

    void put(T x) throws InterruptedException {
        lock.lock();
        try {
            while (count == items.length) notFull.await();      // loop: spurious wakeups
            items[tail] = x; tail = (tail + 1) % items.length; count++;
            notEmpty.signal();
        } finally { lock.unlock(); }
    }

    @SuppressWarnings("unchecked")
    T take() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0) notEmpty.await();
            T x = (T) items[head]; items[head] = null;          // let GC reclaim it
            head = (head + 1) % items.length; count--;
            notFull.signal();
            return x;
        } finally { lock.unlock(); }
    }
}
```
Two `Condition`s avoid waking the wrong kind of thread (a benefit over `wait/notifyAll`). Always unlock in `finally`. Discuss fairness, `offer` with timeout, and that the JDK `ArrayBlockingQueue` is exactly this design.

## [5+] Write a thread-safe in-memory cache with TTL and a token-bucket rate limiter.
**Cache with expiry** (lazy expiry on read):
```java
class TtlCache<K, V> {
    private record Entry<V>(V value, long expiresAtNanos) {}
    private final ConcurrentHashMap<K, Entry<V>> map = new ConcurrentHashMap<>();

    void put(K key, V value, Duration ttl) {
        map.put(key, new Entry<>(value, System.nanoTime() + ttl.toNanos()));
    }
    V get(K key) {
        Entry<V> e = map.get(key);
        if (e == null) return null;
        if (System.nanoTime() - e.expiresAtNanos() >= 0) { map.remove(key, e); return null; }
        return e.value();
    }
}
```
Add: a **scheduled cleaner** (so unread keys do not leak), a **maximum size** with an eviction policy, `computeIfAbsent` to prevent a stampede, and metrics. In production use **Caffeine**.

**Token bucket:**
```java
class TokenBucket {
    private final long capacity; private final double refillPerNano;
    private double tokens; private long last = System.nanoTime();

    TokenBucket(long capacity, double refillPerSecond) {
        this.capacity = capacity; this.tokens = capacity; this.refillPerNano = refillPerSecond / 1e9;
    }
    synchronized boolean tryAcquire() {
        long now = System.nanoTime();
        tokens = Math.min(capacity, tokens + (now - last) * refillPerNano);
        last = now;
        if (tokens >= 1) { tokens -= 1; return true; }
        return false;
    }
}
```
Use `System.nanoTime()` (monotonic), not wall-clock time. For a cluster, move the state to **Redis with a Lua script**.
