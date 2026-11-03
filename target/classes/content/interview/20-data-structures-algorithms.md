---
topic: Data structures and algorithms
order: 20
---
## [0-1] What is Big-O notation? What are the common complexities?
Big-O describes how the **running time or memory grows** as the input size `n` grows, ignoring constants. It is usually the **worst case**.

| Complexity | Name | Example |
|---|---|---|
| O(1) | constant | array index, `HashMap.get` (average) |
| O(log n) | logarithmic | binary search, balanced tree, heap push |
| O(n) | linear | scan a list |
| O(n log n) | linearithmic | merge sort, quicksort (average) |
| O(n squared) | quadratic | nested loops, bubble sort |
| O(2^n) | exponential | naive recursive Fibonacci, subsets |

Rules: drop constants (`2n` is O(n)) and lower terms (`n squared + n` is O(n squared)). Analyse **time and space**, and know the difference between worst, average and **amortised** cost (`ArrayList.add` is amortised O(1)).

## [0-1] Array versus linked list versus stack versus queue?
- **Array**: contiguous memory; O(1) index access; insert or delete in the middle is O(n); fixed size (an `ArrayList` resizes for you).
- **Linked list**: nodes with pointers; O(1) insert or delete **if you hold the node**; access by index is O(n); extra memory per node; poor cache locality.
- **Stack**: **LIFO** (last in, first out): `push`, `pop`, `peek`. Used for undo, recursion, expression parsing, bracket matching.
- **Queue**: **FIFO**: `offer`, `poll`. Used for scheduling, BFS, buffering. A **deque** works at both ends.

In Java use `ArrayDeque` for both stack and queue, and `ArrayList` as the default list.

## [1-3] Compare the common sorting algorithms.
| Algorithm | Best | Average | Worst | Space | Stable |
|---|---|---|---|---|---|
| Bubble / insertion | O(n) | O(n squared) | O(n squared) | O(1) | yes |
| Selection | O(n squared) | O(n squared) | O(n squared) | O(1) | no |
| Merge sort | O(n log n) | O(n log n) | O(n log n) | O(n) | yes |
| Quick sort | O(n log n) | O(n log n) | O(n squared) | O(log n) | no |
| Heap sort | O(n log n) | O(n log n) | O(n log n) | O(1) | no |

- **Quick sort** is fast in practice; a bad pivot gives the worst case (fixed with random or median-of-three pivots).
- **Merge sort** is predictable and stable; good for linked lists and external sorting.
- Java: `Arrays.sort(int[])` uses **dual-pivot quicksort**; `Collections.sort`/`List.sort` and `Arrays.sort(Object[])` use **TimSort** (a stable merge and insertion hybrid, O(n) on nearly sorted data).
- For small integer ranges, **counting sort** is O(n + k).

## [1-3] What are the tree traversals, and what is a BST? What is the difference between BFS and DFS?
**Binary tree traversals:** **in-order** (left, node, right; gives a sorted sequence for a BST), **pre-order** (node, left, right; copying a tree), **post-order** (left, right, node; deleting a tree), and **level-order** (using a queue).

```java
void inorder(Node n) { if (n == null) return; inorder(n.left); visit(n); inorder(n.right); }
```

**BST**: left keys are smaller, right keys are larger. Search, insert and delete are O(log n) when **balanced**, but O(n) when degenerate (inserting sorted data). Self-balancing trees (AVL, red-black; `TreeMap` is red-black) guarantee O(log n).

**BFS** explores level by level with a **queue**: shortest path in an unweighted graph. **DFS** goes deep with a **stack or recursion**: cycle detection, topological sort, connected components, backtracking. Both are O(V + E).

## [3-5] How do you approach dynamic programming problems?
Dynamic programming applies when a problem has **overlapping subproblems** and **optimal substructure**.

Steps: (1) define the **state** (what do the parameters mean?), (2) write the **recurrence**, (3) set **base cases**, (4) choose **memoisation** (top-down recursion plus a cache) or **tabulation** (bottom-up table), (5) optimise space if only the previous row is needed.

Example: **minimum coins** to make `amount`:
```java
static int coinChange(int[] coins, int amount) {
    int[] dp = new int[amount + 1];
    Arrays.fill(dp, amount + 1);                  // "infinity"
    dp[0] = 0;
    for (int a = 1; a <= amount; a++)
        for (int c : coins)
            if (c <= a) dp[a] = Math.min(dp[a], dp[a - c] + 1);
    return dp[amount] > amount ? -1 : dp[amount];  // O(amount * coins)
}
```
Classic problems: Fibonacci, climbing stairs, 0/1 knapsack, longest common subsequence, edit distance, house robber. Say what the state means out loud: that is what interviewers listen for.

## [3-5] Explain the two-pointer and sliding window techniques with an example.
**Two pointers**: two indexes move through a sequence (from both ends, or one fast and one slow) to avoid a nested loop. Examples: pair sum in a sorted array, palindrome check, removing duplicates in place, cycle detection in a linked list (fast and slow).

**Sliding window**: maintain a window `[left, right]` over an array or string, expand `right`, and shrink `left` when the window breaks a rule. It turns O(n squared) into **O(n)**.

```java
static int longestUniqueSubstring(String s) {
    Map<Character, Integer> lastSeen = new HashMap<>();
    int best = 0, left = 0;
    for (int right = 0; right < s.length(); right++) {
        char c = s.charAt(right);
        if (lastSeen.containsKey(c) && lastSeen.get(c) >= left) left = lastSeen.get(c) + 1;
        lastSeen.put(c, right);
        best = Math.max(best, right - left + 1);
    }
    return best;
}
```
Pattern recognition tip: "longest or shortest subarray or substring with a property" often means a sliding window.

## [3-5] How do you detect a cycle in a graph, and what is topological sorting?
**Undirected graph:** DFS with a `visited` set; a visited neighbour that is not the parent means a cycle. (Union-Find also works.)

**Directed graph:** DFS with three states: *unvisited, in the current recursion path, done*. Reaching a node that is **in the current path** means a cycle.

**Topological sort** orders the vertices of a **directed acyclic graph** so every edge goes from earlier to later. Uses: build and task dependencies, course prerequisites, bean initialisation order. **Kahn's algorithm**: compute in-degrees, repeatedly take nodes with in-degree 0 (queue), and decrease neighbours' in-degrees. If you processed fewer nodes than exist, there is a cycle. O(V + E).

## [5+] How do you choose data structures for real-world problems?
Think in terms of **operations and scale**:
- **Leaderboard / rank queries**: a sorted structure (skip list, balanced tree; Redis **sorted set**), not re-sorting a list on every read.
- **Autocomplete**: a **trie** (prefix tree), or a search engine with n-grams.
- **"Have I seen this before?" at huge scale**: a **Bloom filter** (small memory, no false negatives, some false positives) in front of a database.
- **Top K in a stream**: a bounded **min-heap**; approximate heavy hitters with **count-min sketch**.
- **Cache with eviction**: hash map plus doubly linked list (LRU) or TinyLFU (Caffeine).
- **Scheduling by time**: a **priority queue** or a hashed timing wheel for millions of timers.
- **Range sums and updates**: prefix sums, Fenwick tree, segment tree.
- **Distributing keys over nodes**: **consistent hashing**.
- **Graph problems** (routes, dependencies, recommendations): adjacency lists, BFS, Dijkstra, topological sort.

Also weigh **memory layout** (arrays beat pointer-heavy structures in practice), concurrency needs (concurrent collections), persistence (B-trees and LSM trees in databases), and whether **approximate** answers are acceptable.
