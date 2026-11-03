---
topic: Spring Core
order: 11
---
## [0-1] What are IoC and Dependency Injection? Which injection type is best?
**Inversion of Control**: the framework creates and wires objects instead of your code calling `new`. **Dependency Injection** is the mechanism: the container supplies a class's dependencies.

Types:
- **Constructor injection** (preferred): dependencies are `final`, the object is never half built, required dependencies are explicit, and it is trivial to unit test without Spring.
- **Setter injection**: for optional or changeable dependencies.
- **Field injection** (`@Autowired` on a field): short, but hides dependencies, cannot be `final`, and needs reflection or Spring to test. Avoid it.

```java
@Service
class OrderService {
    private final PaymentGateway gateway;
    OrderService(PaymentGateway gateway) { this.gateway = gateway; }  // @Autowired optional with one constructor
}
```

## [0-1] What are @Component, @Service, @Repository and @Controller? How is @Bean different?
All are **stereotype annotations** that register a bean through component scanning. `@Component` is the generic one; the others add meaning for a layer: `@Service` (business logic), `@Repository` (data access; also **translates persistence exceptions** into Spring's `DataAccessException`), `@Controller` and `@RestController` (web layer).

`@Bean` is a method in a `@Configuration` class that **you** call to create the bean. Use it for classes you do not own (third-party libraries) or when construction needs custom logic.

## [1-3] What are the bean scopes? What happens when a prototype bean is injected into a singleton?
Scopes: **singleton** (default, one per container), **prototype** (new instance per request for the bean), and web scopes **request**, **session**, **application**, **websocket**.

If a prototype is injected into a singleton, the injection happens **once** when the singleton is created, so the singleton keeps using the same prototype instance. Fix with:
- `ObjectProvider<MyPrototype>` and `provider.getObject()` each time,
- `@Lookup` method injection,
- or a **scoped proxy** (`@Scope(proxyMode = TARGET_CLASS)`).

Also remember: singleton beans must be **stateless or thread safe**, since one instance serves all threads.

## [1-3] Describe the Spring bean lifecycle.
1. **Instantiate** (constructor or factory method).
2. **Populate** dependencies.
3. **Aware** callbacks (`BeanNameAware`, `ApplicationContextAware`).
4. `BeanPostProcessor.postProcessBeforeInitialization`.
5. **Initialise**: `@PostConstruct`, `InitializingBean.afterPropertiesSet`, `@Bean(initMethod)`.
6. `BeanPostProcessor.postProcessAfterInitialization`, where **AOP proxies are created**.
7. The bean is in use.
8. **Destroy** on context shutdown: `@PreDestroy`, `DisposableBean.destroy`.

`BeanFactoryPostProcessor` runs earlier and can change bean definitions (for example `PropertySourcesPlaceholderConfigurer` resolving `${...}`).

## [3-5] How does Spring AOP work? JDK proxy versus CGLIB, and what are the limits?
Spring AOP weaves behaviour (transactions, caching, security, logging) around beans using **runtime proxies**.

- If the target implements an interface, Spring can use a **JDK dynamic proxy**; otherwise it uses a **CGLIB** subclass. Spring Boot defaults to CGLIB (`proxyTargetClass=true`).
- Advice types: `@Before`, `@After`, `@AfterReturning`, `@AfterThrowing`, `@Around`.

**Limits:**
- **Self-invocation** is not intercepted (the call does not go through the proxy).
- `final` classes and methods cannot be proxied by CGLIB; `private` methods are not intercepted.
- Only Spring-managed beans and only method execution join points. For field access or constructors you need AspectJ weaving.

```java
@Around("@annotation(Timed)")
Object time(ProceedingJoinPoint pjp) throws Throwable {
    long t = System.nanoTime();
    try { return pjp.proceed(); }
    finally { log.info("{} took {} ms", pjp.getSignature(), (System.nanoTime() - t) / 1_000_000); }
}
```

## [3-5] How does @Autowired resolve beans? What about circular dependencies?
Resolution: first by **type**. If several candidates exist, Spring uses `@Primary`, then `@Qualifier`, then falls back to matching the **parameter or field name** with the bean name. No match gives `NoSuchBeanDefinitionException`; several unresolved gives `NoUniqueBeanDefinitionException`.

**Circular dependency** (`A` needs `B` needs `A`): with constructor injection Spring cannot create either and throws `BeanCurrentlyInCreationException`. With setter or field injection singletons used to work via early references, but **Spring Boot 2.6+ prohibits circular references by default**.

The right fix is to **redesign**: extract the shared logic into a third bean, use events, or invert one dependency. `@Lazy` on an injection point is only a workaround.

## [5+] What does @Configuration do internally, and how does component scanning work?
`@SpringBootApplication` combines `@Configuration`, `@EnableAutoConfiguration` and `@ComponentScan` (which scans the package of the main class and below).

**Component scanning** uses a classpath scanner to read class metadata (ASM, without loading classes) and registers bean definitions for stereotype-annotated classes.

**Full versus lite configuration:**
- In a `@Configuration` class (default `proxyBeanMethods = true`) Spring generates a **CGLIB subclass**, so calling one `@Bean` method from another returns the **same singleton** from the container instead of creating a new object.
- With `proxyBeanMethods = false` (or `@Component` with `@Bean` methods, called "lite" mode), no proxy is created. It starts faster and uses less memory, but inter-bean method calls produce new instances. Inject dependencies as method parameters instead.

```java
@Configuration(proxyBeanMethods = false)
class AppConfig {
    @Bean Client client(Settings settings) { return new Client(settings); }  // parameter injection
}
```
Auto-configuration and Spring Boot's own classes use `proxyBeanMethods = false` for speed.

## [0-1] What is the Spring Framework? ApplicationContext versus BeanFactory?
Spring is a framework built around the **IoC container**, with modules for the web (Spring MVC), data access (JDBC, JPA, transactions), AOP, security, testing and more. Its aim is to make enterprise Java code loosely coupled and testable.

- **`BeanFactory`**: the basic container; creates beans lazily.
- **`ApplicationContext`**: extends it with **eager singleton creation**, event publishing, internationalisation (`MessageSource`), resource loading and automatic registration of `BeanPostProcessor`s. This is what you use in practice (Spring Boot creates one for you).

Eager creation means wiring problems fail at **startup**, not at the first request, which is an advantage.

## [1-3] How do @Value, @Profile, @Primary, @Qualifier and @Conditional work?
- **`@Value("${app.timeout:30}")`**: injects a property with an optional default (`:30`). For groups of properties prefer `@ConfigurationProperties`.
- **`@Profile("dev")`**: registers a bean only when that profile is active.
- **`@Primary`**: the default choice when several beans of one type exist.
- **`@Qualifier("name")`**: picks a specific bean at an injection point.
- **`@Conditional`** (and Boot's `@ConditionalOnProperty`, `@ConditionalOnMissingBean`, `@ConditionalOnClass`): register a bean only if a condition holds; the foundation of auto-configuration.

```java
@Bean @Primary DataSource main() { ... }
@Bean @Profile("test") DataSource inMemory() { ... }
class Report { Report(@Qualifier("main") DataSource ds) { ... } }
```

## [3-5] How do Spring events, @Async and @Scheduled work, and what are the pitfalls?
**Events:** `publisher.publishEvent(new OrderPlaced(id))` and `@EventListener`. Listeners run **synchronously in the publisher's thread and transaction** by default. Use `@TransactionalEventListener(phase = AFTER_COMMIT)` to react only after a successful commit (for example to send an email).

**`@Async`** (needs `@EnableAsync`) runs a method on a task executor and returns `void` or a `CompletableFuture`. Pitfalls: it is **proxy based**, so self-invocation is not async; the default executor settings may be unbounded or unsuitable, so define a bounded `ThreadPoolTaskExecutor`; exceptions in `void` methods need an `AsyncUncaughtExceptionHandler`; and the **`SecurityContext` and transaction do not propagate** automatically.

**`@Scheduled`** (needs `@EnableScheduling`): `fixedRate`, `fixedDelay` or `cron`. Pitfalls: the default scheduler uses **one thread** (long jobs delay others), and with several application instances the job runs **on each instance**; use ShedLock or Quartz with a database lock to run once per cluster.
