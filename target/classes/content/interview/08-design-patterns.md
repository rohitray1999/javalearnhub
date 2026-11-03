---
topic: Design patterns
order: 8
---
## [0-1] How do you implement a Singleton in Java, and how is it different from a Spring singleton?
Options, from worst to best:
1. **Eager** static instance: simple and thread safe, but created even if unused.
2. **`synchronized` getInstance**: safe but locks on every call.
3. **Double-checked locking** with a `volatile` field.
4. **Holder class** (lazy, thread safe, no locks):

```java
public final class Config {
    private Config() {}
    private static class Holder { static final Config INSTANCE = new Config(); }
    public static Config get() { return Holder.INSTANCE; }
}
```
5. **Enum singleton**: also safe against reflection and serialization attacks.

Classic singletons can be broken by reflection, serialization (fix with `readResolve`) and cloning.

A **Spring singleton** is one instance **per ApplicationContext**, not per JVM, and it is managed and injected, so it is easy to mock in tests. Prefer that to a hand-written static singleton.

## [1-3] What is the difference between Factory Method, Abstract Factory and Builder?
- **Factory Method**: one method decides which concrete class to create and returns a common type. `PaymentFactory.of("UPI")`.
- **Abstract Factory**: creates **families** of related objects (for example UI widgets for a theme, or `Connection`, `Statement`, `Dialect` for a database vendor).
- **Builder**: constructs one **complex object** step by step, with optional fields and validation in `build()`. Fixes telescoping constructors and keeps objects immutable.

Rule of thumb: choose the factory when the question is "*which* class?", and a builder when the question is "*how do I assemble* this object?".

## [1-3] How does the Strategy pattern remove if-else chains? How does Spring help?
Define an interface for the varying behaviour, one implementation per variant, and select at runtime.

```java
interface DiscountStrategy { String type(); double apply(double amount); }

@Component class FestiveDiscount implements DiscountStrategy { ... }
@Component class LoyaltyDiscount implements DiscountStrategy { ... }

@Service
class PricingService {
    private final Map<String, DiscountStrategy> byType;
    PricingService(List<DiscountStrategy> strategies) {
        this.byType = strategies.stream().collect(toMap(DiscountStrategy::type, s -> s));
    }
    double price(String type, double amount) { return byType.get(type).apply(amount); }
}
```

Adding a new discount means adding one class, with no changes to `PricingService` (Open/Closed Principle). Spring injects all implementations as a `List` or `Map`.

## [3-5] Compare Decorator, Proxy, Adapter and Facade.
All wrap another object, but the **intent** differs:
- **Adapter**: converts one interface to another that the client expects (legacy API to new interface).
- **Decorator**: adds behaviour while keeping the **same interface**, and can be stacked (`new BufferedReader(new InputStreamReader(...))`).
- **Proxy**: same interface, **controls access**: lazy loading, security, caching, remote calls, transactions. Spring AOP (`@Transactional`) and Hibernate lazy entities are proxies.
- **Facade**: offers a **simpler interface** over a complex subsystem. A service method that calls validation, pricing and persistence is a facade.

## [3-5] What is the Template Method pattern, and how does it differ from Strategy?
**Template Method**: a base class defines the skeleton of an algorithm and lets subclasses fill in steps (**inheritance**). **Strategy** swaps a whole behaviour by injecting an object (**composition**).

```java
abstract class ReportJob {
    public final void run() { fetch(); transform(); publish(); }   // fixed skeleton
    protected abstract void transform();                           // varies
}
```

Spring's `JdbcTemplate`, `RestTemplate` and `TransactionTemplate` use the callback flavour: the template handles resource management and the exceptions; you supply the changing part as a lambda. Prefer Strategy when you need to switch behaviour at runtime or avoid deep inheritance.

## [5+] Which design patterns does Spring use internally, and how do you avoid pattern overuse?
In Spring: **Singleton** (default scope), **Factory** (`BeanFactory`, `ApplicationContext`), **Proxy** (AOP, `@Transactional`, `@Async`), **Template Method** (`JdbcTemplate`), **Observer** (`ApplicationEvent`, `@EventListener`), **Front Controller** (`DispatcherServlet`), **Strategy** (`HandlerMapping`, `ViewResolver`), **Decorator** (servlet wrappers), **Builder** (`ResponseEntity`, `HttpSecurity`).

Avoiding overuse:
- Start with the simplest code and **refactor to a pattern** when duplication or change pressure appears.
- A pattern is justified by a **concrete force** (a second implementation, a variation point), not "we might need it".
- Watch for names like `AbstractFactoryProviderManager`: abstraction layers with a single implementation.
- Judge patterns by readability and testability for your team.

In design discussions, name the **problem** first, then the pattern as shorthand for the solution.

## [0-1] What are design patterns, and how are they categorised?
Design patterns are **proven, reusable solutions** to common design problems, described in the Gang of Four book (23 patterns). They give a shared vocabulary ("use a Strategy here").

- **Creational** (how objects are created): Singleton, Factory Method, Abstract Factory, Builder, Prototype.
- **Structural** (how classes and objects are composed): Adapter, Decorator, Proxy, Facade, Composite, Bridge, Flyweight.
- **Behavioural** (how objects interact and share responsibility): Strategy, Observer, Command, Template Method, State, Iterator, Chain of Responsibility.

A pattern is a **guideline, not a copy-paste template**. Using one without a real problem makes code more complex.

## [1-3] Explain the Observer and Command patterns with examples.
**Observer:** a subject notifies many observers when its state changes, without knowing who they are (loose coupling).
```java
interface OrderListener { void onPlaced(Order o); }
class OrderService {
    private final List<OrderListener> listeners = new CopyOnWriteArrayList<>();
    void placeOrder(Order o) { save(o); listeners.forEach(l -> l.onPlaced(o)); }
}
```
In Spring: `ApplicationEventPublisher.publishEvent(new OrderPlaced(o))` with `@EventListener` (use `@TransactionalEventListener` to react after commit). Also the basis of messaging.

**Command:** turns a request into an **object** so you can queue, log, retry or undo it.
```java
interface Command { void execute(); void undo(); }
```
Examples: an undo stack in an editor, task queues (`Runnable` is a command), job schedulers, CQRS commands in services.

## [3-5] DAO versus Repository versus DTO versus Service: how do they differ?
- **Entity**: a persistent domain object mapped to a table.
- **DAO** (Data Access Object): encapsulates **database access** for one table or entity, usually technical: `findById`, `insert`, SQL.
- **Repository**: a **domain-oriented collection abstraction** for aggregates (`orderRepository.findOverdue()`); hides persistence details and speaks the domain language. Spring Data repositories are this.
- **DTO** (Data Transfer Object): a plain object that **carries data across boundaries** (API requests and responses), shaped for the consumer, not the database. It stops entities leaking and avoids lazy loading and over-posting problems.
- **Service**: **business logic and orchestration**, transaction boundaries, calls to repositories and other systems.

Typical flow: `Controller` (DTO in) to `Service` (maps to entity, applies rules) to `Repository` (persistence) and back, mapping to a DTO (MapStruct) for the response.
