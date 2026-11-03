package com.learnhub.config;

import com.learnhub.domain.*;
import com.learnhub.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Starter content. Pehli baar app chalne par DB khaali ho to ye seed hota hai.
 * Asli content badhane ke liye isi pattern mein naye lessons add karo
 * (ya baad mein Markdown files se import karo).
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final StageRepository stages;
    private final LessonRepository lessons;
    private final QuizQuestionRepository quizzes;
    private final LearningPathRepository paths;

    @Override
    @Transactional
    public void run(String... args) {
        if (stages.count() > 0) return;

        Stage basics = stage("java-basics", "Java basics", "Core Java", "Beginner",
                "How Java runs, variables and types: the foundation for everything else.", 1);
        Stage oop = stage("oop", "Object-oriented programming", "Core Java", "Beginner",
                "Classes, objects, inheritance and polymorphism.", 2);
        Stage coll = stage("collections", "Collections", "Core Java", "Intermediate",
                "Lists, sets and maps, and how HashMap works inside.", 3);
        Stage modern = stage("modern-java", "Java 8 and beyond", "Modern Java", "Intermediate",
                "Lambdas and streams: the features that changed Java.", 4);
        Stage boot = stage("spring-boot", "Spring Boot", "Spring", "Intermediate",
                "Build REST APIs with auto-configuration and Spring Data.", 5);
        Stage ms = stage("microservices", "Microservices", "Architecture", "Advanced",
                "Service boundaries, communication and resilience.", 6);

        Lesson l1 = lesson(basics, "how-java-runs", "How Java runs", 8, 6, 1,
                "Source code becomes bytecode, and the JVM runs that bytecode on any machine.", """
## Write once, run anywhere

You write code in a `.java` file. The compiler (`javac`) turns it into **bytecode** stored in a `.class` file.
The **JVM** (Java Virtual Machine) then runs that bytecode. Because every operating system has its own JVM, the same `.class` file runs on Windows, Linux and macOS.

```java
public class Hello {
    public static void main(String[] args) {
        System.out.println("Hello, Java!");
    }
}
```

Compile and run it:

```bash
javac Hello.java
java Hello
```

## JDK, JRE and JVM

| Name | What it is |
|---|---|
| **JVM** | Runs bytecode |
| **JRE** | JVM plus the standard libraries |
| **JDK** | JRE plus tools like `javac`. Install this one. |

## Key points

- `main` is the entry point of a program.
- Java is **compiled to bytecode**, then **interpreted and JIT-compiled** by the JVM.
- The JIT compiler turns hot bytecode into fast machine code while the program runs.
""");
        quiz(l1, "What does the javac command produce?",
                List.of("Machine code for your CPU", "Bytecode in a .class file", "A .jar file", "A .exe file"), 1,
                "javac compiles source into platform-independent bytecode. The JVM runs it.");
        quiz(l1, "Which one do you install to write and compile Java programs?",
                List.of("JRE", "JVM only", "JDK", "JIT"), 2,
                "The JDK includes the compiler and tools. The JRE only runs programs.");
        quiz(l1, "Why can the same .class file run on different operating systems?",
                List.of("Each OS has its own JVM that understands bytecode", "Java source is recompiled on every run", "Bytecode is machine code for all CPUs", "The JDK converts it to .exe"), 0,
                "The JVM is the platform-specific part. Bytecode stays the same.");

        Lesson l2 = lesson(basics, "variables-and-types", "Variables and data types", 8, 7, 2,
                "Primitive types, reference types and how Java stores values.", """
## Primitive types

Java has 8 primitive types. The ones you will use daily:

```java
int age = 28;
long population = 1_400_000_000L;
double price = 99.99;
boolean active = true;
char grade = 'A';
```

## Reference types

Everything else is a **reference type**: `String`, arrays, and your own classes. A reference variable holds the *address* of an object, not the object itself.

```java
String name = "Asha";
String other = name;   // both variables point to the same object
```

## var (Java 10+)

The compiler can infer the type for local variables:

```java
var count = 10;               // int
var names = new ArrayList<String>();
```

## Common trap

`==` compares references for objects. Use `.equals()` to compare content:

```java
String a = new String("java");
String b = new String("java");
System.out.println(a == b);       // false
System.out.println(a.equals(b));  // true
```
""");
        quiz(l2, "How many primitive types does Java have?",
                List.of("4", "6", "8", "10"), 2, "byte, short, int, long, float, double, char and boolean.");
        quiz(l2, "Which comparison checks the content of two Strings?",
                List.of("==", ".equals()", "=", "!="), 1, "== compares references. equals() compares content.");
        quiz(l2, "What does var do (Java 10+)?",
                List.of("Makes the variable dynamic at runtime", "Lets the compiler infer the local variable type", "Declares a constant", "Creates a global variable"), 1,
                "Java stays statically typed. The compiler just infers the type.");

        Lesson l3 = lesson(oop, "classes-and-objects", "Classes and objects", 8, 8, 1,
                "A class is a blueprint. An object is one instance built from it.", """
## Class and object

A **class** defines fields (data) and methods (behaviour). An **object** is an instance of a class.

```java
public class Account {
    private double balance;          // encapsulated state

    public Account(double opening) {  // constructor
        this.balance = opening;
    }

    public void deposit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        balance += amount;
    }

    public double getBalance() { return balance; }
}

Account acc = new Account(500);
acc.deposit(250);
```

## Encapsulation

Keep fields `private` and expose behaviour through methods. This lets you protect rules such as "deposit must be positive".

## Records (Java 16+)

For simple data holders, a record removes the boilerplate:

```java
public record Point(int x, int y) {}
```

The compiler generates the constructor, getters (`x()`, `y()`), `equals`, `hashCode` and `toString`.
""");
        quiz(l3, "What is a class?",
                List.of("A running object", "A blueprint for objects", "A method", "A package"), 1, "Objects are created from a class using new.");
        quiz(l3, "Why make fields private?",
                List.of("To make code run faster", "To protect state and enforce rules through methods", "Because Java requires it", "To save memory"), 1,
                "That is encapsulation: control how data changes.");
        quiz(l3, "What does a record generate for you?",
                List.of("Only a constructor", "Constructor, accessors, equals, hashCode and toString", "A database table", "Only getters and setters"), 1,
                "Records are compact immutable data carriers.");

        Lesson l4 = lesson(oop, "inheritance-polymorphism", "Inheritance and polymorphism", 8, 8, 2,
                "Reuse behaviour with extends, and let one reference type take many forms.", """
## Inheritance

A subclass reuses and extends a parent class.

```java
class Payment {
    void pay(double amount) { System.out.println("Paying " + amount); }
}

class UpiPayment extends Payment {
    @Override
    void pay(double amount) { System.out.println("UPI: " + amount); }
}
```

## Polymorphism

A parent reference can hold any child object. The **actual object** decides which method runs at runtime.

```java
Payment p = new UpiPayment();
p.pay(500);   // prints "UPI: 500.0"
```

## Prefer interfaces

Use an interface when you only need a contract:

```java
interface Notifier { void send(String msg); }
```

## Rule of thumb

Favour **composition over inheritance**. Use `extends` only for a true "is-a" relationship.
""");
        quiz(l4, "In Payment p = new UpiPayment(); p.pay(10); which pay() runs?",
                List.of("Payment.pay", "UpiPayment.pay", "Both", "Compile error"), 1, "Method calls resolve on the actual object type at runtime.");
        quiz(l4, "What does @Override do?",
                List.of("Makes a method static", "Asks the compiler to check you are overriding a parent method", "Hides the parent method", "Speeds up the method"), 1,
                "It catches typos in method signatures at compile time.");
        quiz(l4, "Which design guideline is widely recommended?",
                List.of("Always use deep inheritance", "Favour composition over inheritance", "Never use interfaces", "Make every field public"), 1,
                "Composition is more flexible and easier to change.");

        Lesson l5 = lesson(coll, "hashmap-basics", "HashMap basics", 8, 9, 1,
                "Key-value storage with fast lookups, and what happens inside.", """
## Using a HashMap

```java
Map<String, Integer> marks = new HashMap<>();
marks.put("Asha", 91);
marks.put("Ravi", 78);

int m = marks.get("Asha");               // 91
marks.getOrDefault("Neha", 0);           // 0
marks.merge("Ravi", 5, Integer::sum);    // 83
```

## How it works inside

1. Java calls `key.hashCode()` and picks a **bucket** (array index).
2. If the bucket is empty, the entry is stored there.
3. If another key lands in the same bucket (a **collision**), entries form a small chain.
4. When the map gets about 75 percent full (load factor 0.75), it **resizes** to double the size.
5. Long chains in a bucket (8 or more entries, with a large enough table) become balanced trees.

Average `get` and `put` are **O(1)**.

## The contract

If you use your own class as a key, override **both** `equals()` and `hashCode()`, otherwise lookups fail.

HashMap is **not thread-safe**. Use `ConcurrentHashMap` when threads share a map.
""");
        quiz(l5, "What is the default load factor of HashMap?",
                List.of("0.5", "0.75", "1.0", "2.0"), 1, "When size passes capacity x 0.75, the table doubles.");
        quiz(l5, "What happens when two keys land in the same bucket?",
                List.of("The first key is deleted", "A collision: entries are chained in that bucket", "An exception is thrown", "The map resizes immediately"), 1,
                "Collisions are normal. Chains (or trees) handle them.");
        quiz(l5, "What must you override when using a custom class as a HashMap key?",
                List.of("toString only", "equals and hashCode", "clone", "finalize"), 1, "Both must agree, otherwise get() will not find your key.");

        Lesson l6 = lesson(modern, "lambdas", "Lambdas and functional interfaces", 8, 7, 1,
                "Pass behaviour as a value with short, readable syntax.", """
## Old way versus new way

```java
// Java 7
Collections.sort(words, new Comparator<String>() {
    public int compare(String a, String b) {
        return Integer.compare(a.length(), b.length());
    }
});

// Java 8+
words.sort((a, b) -> Integer.compare(a.length(), b.length()));
words.sort(Comparator.comparingInt(String::length));
```

## Functional interfaces

A lambda can be used wherever an interface with **one abstract method** is expected.

| Interface | Method | Use |
|---|---|---|
| `Predicate<T>` | `boolean test(T)` | filter |
| `Function<T,R>` | `R apply(T)` | transform |
| `Consumer<T>` | `void accept(T)` | do something |
| `Supplier<T>` | `T get()` | produce a value |

```java
Predicate<Integer> isEven = n -> n % 2 == 0;
System.out.println(isEven.test(10));   // true
```

## Method references

`String::length` is shorthand for `s -> s.length()`.
""");
        quiz(l6, "A lambda can replace which kind of interface?",
                List.of("Any interface", "A functional interface (one abstract method)", "Only abstract classes", "Only Comparable"), 1, "Functional interfaces have exactly one abstract method.");
        quiz(l6, "Which interface takes a value and returns a boolean?",
                List.of("Function", "Consumer", "Predicate", "Supplier"), 2, "Predicate<T> is used for conditions such as filter().");
        quiz(l6, "String::length is equivalent to:",
                List.of("s -> s.length()", "() -> length", "s -> length(s, s)", "length -> s"), 0, "A method reference is shorthand for a lambda that calls that method.");

        Lesson l7 = lesson(modern, "stream-api", "Stream API", 8, 8, 2,
                "Process data as a pipeline: source, lazy steps, one terminal step.", """
## A pipeline

```java
List<String> names = orders.stream()
    .filter(o -> o.amount() > 1000)
    .map(Order::customer)
    .distinct()
    .toList();
```

A stream has three parts:

1. **Source**: a collection, array or generator
2. **Intermediate steps** (lazy): `filter`, `map`, `sorted`, `distinct`
3. **Terminal step** (runs everything): `toList`, `collect`, `count`, `forEach`, `sum`

Nothing runs until the terminal step. This is why streams are called **lazy**.

## Grouping

```java
Map<String, List<Order>> byCity = orders.stream()
    .collect(Collectors.groupingBy(Order::city));
```

## Good habits

- Streams do not modify the source collection.
- A stream can be consumed **only once**.
- Keep lambdas short. Extract a method when logic grows.
- Do not use `parallelStream()` without measuring.
""");
        quiz(l7, "When does a stream pipeline actually execute?",
                List.of("When filter() is called", "When a terminal operation is called", "When the stream is created", "Never automatically"), 1, "Intermediate operations are lazy.");
        quiz(l7, "Which is a terminal operation?",
                List.of("map", "filter", "collect", "sorted"), 2, "collect, count, forEach and toList end the pipeline.");
        quiz(l7, "Can you reuse a stream after a terminal operation?",
                List.of("Yes, unlimited times", "No, it throws IllegalStateException", "Only for numbers", "Only with parallel streams"), 1, "A stream is single-use.");

        Lesson l8 = lesson(boot, "first-rest-api", "Your first REST API with Spring Boot", 17, 10, 1,
                "Create a controller, return JSON and understand auto-configuration.", """
## Why Spring Boot

Spring Boot gives you **auto-configuration**, an embedded server (Tomcat) and **starters** (one dependency that brings a whole feature set). You write business code, not XML.

## Create the project

Use start.spring.io with **Spring Web**, **Spring Data JPA** and **H2**.

## A controller

```java
@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService service;

    public BookController(BookService service) {   // constructor injection
        this.service = service;
    }

    @GetMapping
    public List<Book> all() { return service.findAll(); }

    @GetMapping("/{id}")
    public Book one(@PathVariable Long id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Book create(@Valid @RequestBody Book book) { return service.save(book); }
}
```

## The layers

Controller (HTTP) calls Service (business rules) calls Repository (database).

```java
public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByAuthor(String author);
}
```

Spring Data creates the implementation for you at startup.
""");
        quiz(l8, "What does a Spring Boot starter do?",
                List.of("Starts the JVM", "Bundles the dependencies for a feature", "Deploys to the cloud", "Generates HTML"), 1, "Example: spring-boot-starter-web brings Spring MVC, Jackson and Tomcat.");
        quiz(l8, "Which annotation maps an HTTP GET request?",
                List.of("@PostMapping", "@GetMapping", "@Service", "@Entity"), 1, "@GetMapping handles GET requests.");
        quiz(l8, "Who implements JpaRepository interfaces?",
                List.of("You, manually", "Spring Data at runtime", "Hibernate only", "The database"), 1, "Spring Data generates a proxy implementation.");

        Lesson l9 = lesson(ms, "what-are-microservices", "What are microservices?", 17, 9, 1,
                "Small independent services around business capabilities, and the trade-offs that come with them.", """
## Monolith versus microservices

A **monolith** is one deployable application. **Microservices** split the system into small services, each owning one business capability and its own data.

| | Monolith | Microservices |
|---|---|---|
| Deploy | All together | Each service alone |
| Scale | Whole app | Only the busy service |
| Complexity | In the code | In the network |
| Data | One shared DB | Database per service |

## Typical building blocks

- **API Gateway**: single entry point (routing, auth, rate limits)
- **Service discovery**: services find each other by name
- **Messaging** (Kafka, RabbitMQ): asynchronous events
- **Resilience**: timeouts, retries, circuit breakers
- **Saga pattern**: a business transaction across services using compensating steps

## When not to use them

Small team, unclear domain or early-stage product: start with a **modular monolith**. Split a module out only when you feel real pain.
""");
        quiz(l9, "What is the main idea of microservices?",
                List.of("One big deployable app", "Small independently deployable services around business capabilities", "Using only Kafka", "Using no database"), 1, "Each service owns its logic and data.");
        quiz(l9, "What does an API gateway do?",
                List.of("Stores data", "Provides a single entry point and routing for clients", "Compiles code", "Replaces the database"), 1, "It handles routing, authentication and rate limiting at the edge.");
        quiz(l9, "A sensible starting point for a new small product is usually:",
                List.of("100 microservices", "A modular monolith", "Only serverless functions", "No architecture"), 1, "Split services later when real scaling or team pain appears.");

        path("java-from-zero", "Java from zero", "Beginner",
                "New to programming or Java? Start here, one small step at a time.", List.of(l1, l2, l3, l4));
        path("backend-developer", "Job-ready backend developer", "Intermediate",
                "Build and ship REST APIs: Java, collections, Spring Boot and microservices.", List.of(l2, l3, l5, l8, l9));
        path("interview-prep", "Crack the Java interview", "Intermediate",
                "The topics interviewers love: OOP, HashMap, streams and more.", List.of(l4, l5, l6, l7));
    }

    // ---------------- helpers ----------------
    private Stage stage(String slug, String title, String category, String level, String desc, int order) {
        Stage s = new Stage();
        s.setSlug(slug); s.setTitle(title); s.setCategory(category);
        s.setLevelName(level); s.setDescription(desc); s.setOrderNo(order);
        return stages.save(s);
    }

    private Lesson lesson(Stage stage, String slug, String title, int minJava, int minutes, int order,
                          String summary, String md) {
        Lesson l = new Lesson();
        l.setStage(stage); l.setSlug(slug); l.setTitle(title);
        l.setMinJavaVersion(minJava); l.setReadMinutes(minutes); l.setOrderNo(order);
        l.setSummary(summary); l.setContentMd(md);
        return lessons.save(l);
    }

    private void quiz(Lesson lesson, String question, List<String> options, int correct, String explanation) {
        QuizQuestion q = new QuizQuestion();
        q.setLesson(lesson); q.setQuestion(question);
        q.setOptions(new ArrayList<>(options));
        q.setCorrectIdx(correct); q.setExplanation(explanation);
        quizzes.save(q);
    }

    private void path(String slug, String title, String level, String desc, List<Lesson> items) {
        LearningPath p = new LearningPath();
        p.setSlug(slug); p.setTitle(title); p.setLevelName(level); p.setDescription(desc);
        p.setLessons(new ArrayList<>(items));
        paths.save(p);
    }
}
