---
topic: OOP and design principles
order: 2
---
## [0-1] Explain the four pillars of OOP with a real example.
Use one running example, such as a payment system:

- **Encapsulation**: keep state private, expose behaviour. `Account.balance` is private; `withdraw()` enforces "no overdraft".
- **Inheritance**: reuse and specialise. `UpiPayment extends Payment`.
- **Polymorphism**: one reference type, many runtime behaviours. `Payment p = new CardPayment(); p.pay(500);` calls the card version.
- **Abstraction**: expose *what*, hide *how*. Callers use a `PaymentGateway` interface and never see the HTTP calls behind it.

Say the benefit for each: encapsulation protects invariants, inheritance and polymorphism reduce duplication and `if/else` chains, abstraction lets you swap implementations.

## [0-1] Abstract class versus interface: when do you use which?
| | Abstract class | Interface |
|---|---|---|
| Inheritance | A class extends **one** | A class implements **many** |
| State | Can have instance fields | Only constants (`static final`) |
| Methods | Abstract and concrete, any access | Abstract, `default`, `static`, `private` (Java 9+) |
| Constructor | Yes | No |

**Use an interface** to define a capability or contract (`Comparable`, `Notifier`). **Use an abstract class** when subclasses share state or a partial implementation (template method).

Default methods (Java 8) let interfaces evolve without breaking implementers, but they still cannot hold instance state.

## [1-3] What is the difference between method overloading and overriding?
- **Overloading**: same name, different parameter list, in the same class (or subclass). Resolved at **compile time** using the declared argument types.
- **Overriding**: subclass redefines an inherited instance method with the same signature. Resolved at **runtime** from the actual object type.

Override rules: same name and parameters; return type same or **covariant**; access not more restrictive; cannot throw broader checked exceptions; cannot override `final`, `private` or `static` methods (static methods are *hidden*, not overridden).

```java
class Animal { Animal create() { return new Animal(); } }
class Dog extends Animal { @Override Dog create() { return new Dog(); } } // covariant return
```

Always add `@Override`: the compiler then catches signature typos.

## [1-3] Composition or inheritance: which do you prefer and why?
Prefer **composition** ("has-a") unless there is a true "is-a" relationship and the parent was designed for extension.

Problems with inheritance:
- Tight coupling: parent changes can break children (the fragile base class problem).
- Breaks encapsulation: the child depends on parent internals.
- Single inheritance limits you.

Composition lets you swap behaviour at runtime and test with mocks.

```java
class OrderService {
    private final PaymentGateway gateway;   // composed, injected
    OrderService(PaymentGateway gateway) { this.gateway = gateway; }
}
```

Spring's constructor injection is composition in action.

## [3-5] Explain the SOLID principles with Java examples.
- **S: Single Responsibility.** A class has one reason to change. Split `UserService` that validates, saves, emails and logs into focused collaborators.
- **O: Open/Closed.** Open for extension, closed for modification. Add a new `PaymentStrategy` implementation instead of editing a `switch`.
- **L: Liskov Substitution.** A subtype must be usable wherever the parent is expected, without surprises. A `Square` that extends `Rectangle` and breaks `setWidth/setHeight` violates this.
- **I: Interface Segregation.** Many small interfaces beat one fat one. Do not force `Printer` implementers to implement `fax()`.
- **D: Dependency Inversion.** Depend on abstractions, not concrete classes. `OrderService` depends on `PaymentGateway` (interface), and Spring injects the implementation.

Give a "before and after" for at least one; interviewers want to see you *apply* it, not recite it.

## [3-5] What is the diamond problem and how does Java handle it?
The diamond problem: class `D` inherits the same method from two paths (`B` and `C`, which both extend `A`), and it is unclear which version to use. Java avoids it for **classes** by allowing only single class inheritance.

With interface `default` methods the conflict can return. Java's rule: if two interfaces provide the same default method, the class **must override it**, and can pick one explicitly:

```java
interface A { default String hi() { return "A"; } }
interface B { default String hi() { return "B"; } }
class C implements A, B {
    @Override public String hi() { return A.super.hi(); }
}
```

Resolution order: class method wins over interface default; the more specific interface wins; otherwise you must resolve it.

## [5+] Give an example of a Liskov Substitution violation and how you would catch it in a code review.
Classic: `Square extends Rectangle`. A client does `rect.setWidth(4); rect.setHeight(5); assert rect.area() == 20;` and it fails for `Square`, because setting one side changes the other.

Real-world versions: a `ReadOnlyList` subtype that throws `UnsupportedOperationException` from `add()`, or a subclass that strengthens preconditions or weakens postconditions.

**Review signals:**
- Overrides that throw, do nothing, or return `null` where the parent never did.
- `instanceof` checks in client code to special case a subtype.
- Subclass tightening input validation.

**Fix:** model the relationship differently (shared interface, immutable shapes, composition), or make the parent contract honest about what varies.

## [5+] Anemic versus rich domain model: which do you choose in a Spring application?
- **Anemic model**: entities are data holders; all logic lives in `*Service` classes. Easy to start, but rules scatter across services, invariants can be bypassed and services become large.
- **Rich model**: behaviour sits with the data (`order.addItem()`, `account.withdraw()`), and the object guards its own invariants.

My default: keep **simple CRUD** straightforward, but put **real business rules** into the domain objects (or aggregates), and use services only for orchestration, transactions and calls to other systems.

Trade-offs to mention: JPA entities with rich behaviour need care (no-arg constructors, lazy proxies, not exposing collections for mutation). For complex domains, aggregates, value objects and domain events from DDD help a lot; for small apps they are overkill.

## [0-1] What are constructors? What are constructor overloading and chaining?
A **constructor** initialises a new object. It has the class name and no return type. If you write none, the compiler adds a default no-argument constructor.

- **Overloading**: several constructors with different parameters.
- **Chaining**: `this(...)` calls another constructor in the same class; `super(...)` calls the parent's. It must be the first statement.

```java
class Employee {
    private final String name; private final double salary;
    Employee(String name)                { this(name, 30000); }          // chaining
    Employee(String name, double salary) { this.name = name; this.salary = salary; }
}
```

Constructors are **not inherited** and cannot be `abstract`, `static` or `final`. If the parent has no no-arg constructor, the child must call a parent constructor explicitly.

## [1-3] What is the difference between association, aggregation and composition?
All describe "has-a" relationships, with different **lifecycle ownership**:
- **Association**: objects know each other; independent lifecycles (`Teacher` and `Student`).
- **Aggregation**: a weak whole-part relation; the part can **exist without** the whole (`Department` has `Professor`s; the professor remains if the department closes).
- **Composition**: a strong whole-part relation; the part **cannot outlive** the whole (`House` and `Room`, `Order` and `OrderLine`).

In code, composition usually means the whole creates and owns the parts, often as private final fields that are never shared. In JPA, composition maps to `cascade = ALL` with `orphanRemoval = true`.

## [1-3] What are DRY, KISS, YAGNI and the Law of Demeter?
- **DRY** (Don't Repeat Yourself): one authoritative place for each piece of knowledge. Duplicate *logic*, not just duplicate text, is the problem; forced sharing of unrelated code is also harmful.
- **KISS**: prefer the simplest design that works.
- **YAGNI** (You Aren't Gonna Need It): do not build features or abstractions for hypothetical future needs.
- **Law of Demeter** ("talk only to your friends"): a method should call methods on itself, its fields, its parameters and objects it creates, not reach through chains like `order.getCustomer().getAddress().getCity()`. Chains couple you to the whole structure.

Fix for a Demeter violation: ask the owning object (`order.shippingCity()`), "Tell, don't ask".

## [3-5] How do you approach a low-level design (LLD) question such as a parking lot?
**Process**
1. **Clarify requirements**: vehicle types, floors, pricing, payment, entry and exit points, concurrency.
2. **Identify entities** (nouns): `ParkingLot`, `Floor`, `Spot`, `Vehicle`, `Ticket`, `Payment`.
3. **Relationships**: a lot has floors, a floor has spots, a ticket links a vehicle and a spot.
4. **Behaviour** (verbs) and **patterns** where there is variation: pricing and spot allocation are **Strategies**; creating vehicles can use a **Factory**; a single lot instance can be managed by DI.
5. Write the core classes, then discuss **extensibility** (new vehicle types, EV charging) and **concurrency** (two cars must not get the same spot).

```java
enum VehicleType { BIKE, CAR, TRUCK }
record Ticket(String id, String spotId, VehicleType type, Instant entry) {}

interface PricingStrategy { double fee(Ticket t, Instant exit); }
interface SpotAllocationStrategy { Optional<Spot> pick(List<Floor> floors, VehicleType type); }

class ParkingLot {
    private final SpotAllocationStrategy allocator;
    synchronized Ticket park(Vehicle v) {          // atomic allocate-and-mark
        Spot s = allocator.pick(floors, v.type()).orElseThrow(LotFullException::new);
        s.occupy(v);
        return new Ticket(newId(), s.id(), v.type(), Instant.now());
    }
}
```
For scale, replace `synchronized` with per-spot atomic compare-and-set or a DB row lock.
