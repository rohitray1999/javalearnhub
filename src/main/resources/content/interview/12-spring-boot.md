---
topic: Spring Boot
order: 12
---
## [0-1] What is Spring Boot, and what do starters and @SpringBootApplication do?
Spring Boot makes it fast to create production-ready Spring applications: **auto-configuration** (sensible defaults from the classpath), **starters** (one dependency that pulls a tested set of libraries), an **embedded server** (Tomcat by default, so you run a plain JAR), and **Actuator** for operations.

- Starter example: `spring-boot-starter-web` brings Spring MVC, Jackson and Tomcat; `spring-boot-starter-data-jpa` brings Hibernate and Spring Data.
- `@SpringBootApplication` = `@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan`.

You still write normal Spring code; Boot removes the boilerplate configuration.

## [1-3] How does auto-configuration work?
At startup, `@EnableAutoConfiguration` loads the candidate classes listed in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (inside each starter or library). Each one is guarded by **conditions**:

- `@ConditionalOnClass`: only if a class is on the classpath (for example `DataSource`).
- `@ConditionalOnMissingBean`: **back off** if you define your own bean.
- `@ConditionalOnProperty`: toggled by configuration.

So defining your own `DataSource` bean replaces the default one. To see what applied and why, run with `--debug` (the **conditions evaluation report**) or use the Actuator `conditions` endpoint. Exclude one with `@SpringBootApplication(exclude = ...)` or `spring.autoconfigure.exclude`.

## [1-3] Explain profiles and externalised configuration.
Configuration comes from many sources; **higher precedence wins**. Simplified order: command-line arguments, then OS environment variables, then profile-specific files (`application-prod.yml`), then `application.yml`, then defaults.

- **Profiles** (`spring.profiles.active=prod`) switch settings and beans (`@Profile("dev")`).
- `@ConfigurationProperties(prefix = "app.mail")` binds a group of properties to a **typed, validated** class (preferred). `@Value("${...}")` is fine for single values.
- Use **environment variables or a secrets manager** (Vault, Kubernetes Secrets) for passwords. Never commit secrets.
- Relaxed binding: `APP_MAIL_HOST` maps to `app.mail.host`.

```java
@ConfigurationProperties(prefix = "app.mail")
@Validated
record MailProps(@NotBlank String host, @Positive int port) {}
```

## [3-5] How do you use Actuator for monitoring and Kubernetes health checks?
Actuator exposes operational endpoints: `/actuator/health`, `metrics`, `info`, `loggers`, `prometheus`, `threaddump`, `heapdump`, and more. **Only `health` is exposed over HTTP by default**; choose what to expose with `management.endpoints.web.exposure.include`.

- Metrics use **Micrometer**, which has a registry per backend (Prometheus, Datadog). Add custom metrics with `MeterRegistry` counters, timers and gauges.
- **Probes:** `management.endpoint.health.probes.enabled=true` gives `/actuator/health/liveness` (should the container restart?) and `/readiness` (should it receive traffic?). Keep liveness cheap and independent of external dependencies.
- **Secure** Actuator (separate management port or authentication); `heapdump` and `env` expose sensitive data.
- Pair with distributed tracing (Micrometer Tracing / OpenTelemetry).

## [3-5] How do you implement global exception handling in a REST API?
Use `@RestControllerAdvice` and return a consistent body. Spring 6 supports **RFC 9457 `ProblemDetail`**.

```java
@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    ProblemDetail notFound(OrderNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Order not found");
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalid(MethodArgumentNotValidException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage()).toList());
        return pd;
    }
}
```

Also: log unexpected errors once with a **correlation id**, never expose stack traces, and add a catch-all handler returning 500.

## [3-5] How do you reduce Spring Boot startup time and memory use?
- **Trim the classpath** and exclude unneeded auto-configurations; avoid heavy component scans.
- `spring.main.lazy-initialization=true` (beans created on first use; moves failures to runtime, so use it carefully).
- Use `proxyBeanMethods = false` in your own configuration classes.
- **Class Data Sharing (AppCDS)** and tuned JVM flags (`-XX:TieredStopAtLevel=1` for short-lived apps).
- **Spring AOT and GraalVM native image**: very fast start and small memory, with build-time and reflection constraints.
- **Project CRaC** (checkpoint and restore) is another option.
- Measure with `--debug` startup logging and the Actuator `startup` endpoint (`BufferingApplicationStartup`).

Remember: for long-running services, peak throughput matters more than startup.

## [5+] What is your production-readiness checklist for a Spring Boot service?
**Reliability**
- Graceful shutdown (`server.shutdown=graceful`), readiness and liveness probes.
- Timeouts everywhere (HTTP clients, DB, queues); retries with backoff only for idempotent calls; circuit breakers.
- Bounded thread pools and connection pools sized from measurements.

**Observability**
- Structured JSON logs with correlation ids, metrics (RED: rate, errors, duration), distributed tracing, meaningful alerts and dashboards.

**Security**
- Secrets outside the image, HTTPS, least-privilege credentials, dependency scanning, secure Actuator, security headers, input validation.

**Operations**
- Database migrations with **Flyway or Liquibase** (never `ddl-auto=update` in production).
- Config per environment, resource limits and `MaxRAMPercentage` in containers, zero-downtime rollout and rollback plan.
- Load testing, capacity limits and a runbook.

## [5+] How would you build a custom Spring Boot starter?
Split into two modules (or at least two layers):
1. **`xyz-spring-boot-autoconfigure`**: contains the auto-configuration class and a `@ConfigurationProperties` class.
2. **`xyz-spring-boot-starter`**: an empty module with the dependencies.

```java
@AutoConfiguration
@ConditionalOnClass(AuditClient.class)
@EnableConfigurationProperties(AuditProperties.class)
public class AuditAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean          // lets users override
    AuditClient auditClient(AuditProperties props) { return new AuditClient(props.url()); }
}
```

Register it in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`. Follow the naming convention (`acme-spring-boot-starter`, not `spring-boot-starter-acme`), add `spring-boot-configuration-processor` for IDE metadata, always use `@ConditionalOnMissingBean`, keep it opt-in via a property if risky, and test it with `ApplicationContextRunner`.

## [0-1] How do you create and run a Spring Boot application? What is application.properties?
1. Generate a project at **start.spring.io** (Maven or Gradle, Java version, dependencies such as Web, JPA, Validation).
2. The generated class with `@SpringBootApplication` and `main()` starts the app.
3. Run with `mvn spring-boot:run`, from the IDE, or build a **fat JAR** (`mvn package`) and `java -jar app.jar`. The embedded Tomcat starts on port **8080**.

**`application.properties` / `application.yml`** hold configuration: port (`server.port`), datasource URL, logging levels, your own settings. Environment-specific files use profiles: `application-dev.yml`, `application-prod.yml`.

Handy extras: **Spring Boot DevTools** (automatic restart), **Actuator** (health and metrics), and `spring.jpa.show-sql=true` while learning.

## [1-3] How do you validate request data in Spring Boot, and write a custom validator?
Add `spring-boot-starter-validation`. Put Bean Validation annotations on the DTO and trigger them with `@Valid`.

```java
record CreateUser(@NotBlank String name,
                  @Email String email,
                  @Min(18) int age,
                  @Pattern(regexp = "[0-9]{10}") String phone) {}

@PostMapping("/users")
User create(@Valid @RequestBody CreateUser req) { ... }
```
A failure throws `MethodArgumentNotValidException`, which you convert to a `400` with a `@RestControllerAdvice`. Use `@Validated` on a class to validate `@PathVariable` and `@RequestParam` (throws `ConstraintViolationException`).

**Custom validator:**
```java
@Target(FIELD) @Retention(RUNTIME) @Constraint(validatedBy = GstValidator.class)
@interface ValidGst { String message() default "Invalid GSTIN"; Class<?>[] groups() default {}; Class<? extends Payload>[] payload() default {}; }

class GstValidator implements ConstraintValidator<ValidGst, String> {
    public boolean isValid(String v, ConstraintValidatorContext c) { return v == null || v.matches("..."); }
}
```
Validation is not a replacement for **database constraints** or for business rules that need data access.

## [3-5] RestTemplate, RestClient, WebClient and OpenFeign: how do you call other services?
- **`RestTemplate`**: classic blocking client; in **maintenance mode**, so avoid it in new code.
- **`RestClient`** (Spring 6.1 / Boot 3.2+): modern **blocking** fluent client; the recommended replacement for `RestTemplate` in MVC applications.
- **`WebClient`**: **non-blocking, reactive** client (WebFlux); also usable in MVC for async calls and streaming.
- **OpenFeign** (Spring Cloud) or **HTTP interface clients** (`@HttpExchange`): declare an interface and get an implementation, giving very readable code.

```java
RestClient client = RestClient.builder().baseUrl("http://inventory").build();
Stock s = client.get().uri("/stock/{sku}", sku).retrieve().body(Stock.class);
```
Whatever you choose, always configure **connect and read timeouts**, a pooled HTTP client, retries only for idempotent calls, a circuit breaker, error mapping and **trace header propagation**.

## [3-5] How does caching work in Spring Boot with @Cacheable?
Enable it with `@EnableCaching`, pick a provider (Caffeine for local, Redis for shared) and annotate service methods:

```java
@Cacheable(value = "products", key = "#id")
Product get(Long id) { ... }               // result stored; the method body is skipped on a hit

@CachePut(value = "products", key = "#p.id")
Product update(Product p) { ... }          // always runs and updates the cache

@CacheEvict(value = "products", key = "#id")
void delete(Long id) { ... }
```
Points to mention: it is **proxy based** (self-invocation bypasses it); cache **small, serialisable, immutable** values; always configure **TTL and size limits**; `null` results and `unless` conditions; the **stale data** risk, so decide your invalidation strategy; and monitor the **hit ratio**. For several instances, use a distributed cache or accept short-lived inconsistency with a local one.

## [5+] Spring MVC versus WebFlux: when would you choose reactive?
**Spring MVC** uses the **thread-per-request** model with blocking I/O: simple, easy to debug, wide library support (JDBC, JPA).

**WebFlux** uses an **event loop** with non-blocking I/O and `Mono`/`Flux` (Reactor): a few threads handle many connections, with **back-pressure** and streaming support.

Choose **WebFlux** when: very high concurrency with mostly I/O waiting, streaming (SSE, WebSockets), or you call many non-blocking services, and the **whole stack is non-blocking** (R2DBC, reactive drivers).

Stay with **MVC** when: you use JPA or JDBC (blocking), the team is not experienced with reactive, or you have typical CRUD services. One blocking call on an event-loop thread can stall the application.

Since **virtual threads** (Java 21, `spring.threads.virtual.enabled=true`), MVC can scale blocking I/O to very high concurrency with simple code, so the case for reactive is now narrower: streaming and back-pressure pipelines. Reactive code is also harder to read, debug and profile.
