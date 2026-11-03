---
topic: Microservices
order: 15
---
## [1-3] Monolith versus microservices: what are the trade-offs?
**Monolith:** one deployable. Simple to develop, test, debug and deploy; in-process calls; ACID transactions. Pain appears with a **large team or codebase**: slow builds, risky releases, one failure or scaling need affects everything.

**Microservices:** small services around **business capabilities**, each owning its data, deployed independently.
- Benefits: independent deployment and scaling, team autonomy, technology choice, fault isolation.
- Costs: **distributed system complexity** (network failures, latency, eventual consistency), harder debugging and testing, more operations (CI/CD, observability, service discovery), data consistency problems.

Advice: start with a well-structured **modular monolith** unless you have clear scale or team reasons. Do not adopt microservices only because it is popular. A **distributed monolith** (services that must deploy together) gets the costs without the benefits.

## [3-5] How do services communicate? Sync versus async, API gateway and service discovery.
**Synchronous** (REST, gRPC): simple request and response, but it **couples availability and latency**: if B is down or slow, A suffers. Needs timeouts, retries and circuit breakers.

**Asynchronous** (Kafka, RabbitMQ, SQS): publish events or commands; **loose coupling**, buffering and better resilience; the cost is eventual consistency and harder tracing.

Guideline: use sync for **queries** where the user waits for data; use async for **commands and events** (order placed, payment completed) and workflows.

- **API gateway**: single entry point for clients: routing, authentication, rate limiting, TLS, aggregation (Spring Cloud Gateway, Kong, cloud gateways).
- **Service discovery**: services find each other by logical name (Eureka, Consul). In **Kubernetes**, built-in DNS and Services already do this.

## [3-5] How do you make service calls resilient?
Failures are normal; design for them.
- **Timeouts** on every remote call (the most important rule).
- **Retries** only for **idempotent** operations, with **exponential backoff and jitter**, and a retry budget, or retries amplify an outage (retry storm).
- **Circuit breaker**: after many failures, stop calling for a while (open), then probe (half-open). Gives the dependency time to recover and fails fast.
- **Bulkhead**: separate thread or connection pools per dependency so one slow service cannot exhaust everything.
- **Fallback / graceful degradation**: cached data or a reduced feature.
- **Rate limiting and load shedding** to protect yourself.

In Spring: **Resilience4j** (`@CircuitBreaker`, `@Retry`, `@Bulkhead`, `@TimeLimiter`). Test failures with chaos experiments.

## [3-5] How do you handle configuration and observability across services?
**Configuration:** externalise it (Kubernetes ConfigMaps and Secrets, Spring Cloud Config, Vault); same artefact for every environment; support refresh without rebuild where safe.

**The three pillars of observability:**
- **Logs**: structured JSON, with a **correlation or trace id** propagated across calls, shipped to a central store (ELK, Loki).
- **Metrics**: RED (rate, errors, duration) per endpoint, plus saturation metrics; Prometheus and Grafana; alerts based on SLOs.
- **Distributed tracing**: follow one request through all services (OpenTelemetry, Micrometer Tracing, Jaeger, Tempo).

Also: health endpoints, dashboards per service, and standardised tooling (a service template) so every team gets observability by default.

## [5+] How do you handle distributed transactions? Explain Saga and the Outbox pattern.
You cannot rely on one ACID transaction across services; **two-phase commit** is usually avoided (blocking, poor availability, not supported by most brokers and NoSQL stores).

**Saga:** a business transaction as a sequence of **local transactions**; if a step fails, run **compensating actions** for completed steps (for example `PaymentFailed` triggers `ReleaseStock`).
- **Choreography**: services react to each other's events. Simple and decoupled for small flows; hard to follow as it grows.
- **Orchestration**: a central coordinator (Temporal, Camunda, a custom orchestrator) drives the steps. Easier to see and change.

**Dual-write problem:** updating the DB and publishing a message are two systems, so one can succeed and the other fail. Use the **Transactional Outbox**: write the event to an `outbox` table in the **same DB transaction**, and a relay (polling or CDC such as Debezium) publishes it to the broker.

Consumers must be **idempotent** (deduplicate by event id), because delivery is at-least-once. Design compensations that tolerate retries, and expect intermediate states to be visible.

## [5+] How do you manage data across services? What about queries that span services?
Principle: **database per service** (or at least private schemas); no shared tables. Other services get data only through **APIs or events**.

For cross-service reads:
- **API composition**: a gateway or BFF calls several services and merges results. Simple, but latency and partial failures grow.
- **CQRS with a read model**: services publish events; a read-side service builds a denormalised **materialised view** for queries. Fast reads, eventually consistent.
- **Data replication by events**: keep a local copy of the few fields you need (for example customer name), updated from events.

Accept **eventual consistency**; design the UX around it (pending states, idempotent commands). Use **reference ids** instead of foreign keys across services, and version your event schemas (Avro or Protobuf with a schema registry).

## [5+] How would you move a monolith to microservices?
Use incremental migration, not a big-bang rewrite.
1. **Find boundaries** with Domain-Driven Design: bounded contexts, not technical layers. Start by modularising inside the monolith.
2. **Strangler Fig pattern**: put a gateway or facade in front, route one capability at a time to a new service, and retire the old code gradually.
3. Extract the **least coupled, most valuable** module first (or the one with distinct scaling needs). Avoid starting with the core, most entangled part.
4. Break shared database coupling: introduce APIs or events, then split the schema; use change data capture or dual-run with verification during the transition.
5. Build the platform first: CI/CD, observability, config and security templates.
6. Measure success (deployment frequency, lead time, failure rate) and stop when the benefits flatten.

Pitfalls: too many tiny services (nano-services), chatty synchronous calls, shared libraries that couple deployments, and no ownership.

## [1-3] What is the 12-factor app, and what does Spring Cloud versus Kubernetes provide?
The **12-factor** methodology describes cloud-friendly applications. The ones interviewers ask about most: **config in the environment** (not in code), **stateless processes**, **backing services as attached resources** (DB, queue via URL), **logs as event streams** (write to stdout), **dev/prod parity**, **disposability** (fast start, graceful stop), and one codebase, many deploys.

**Spring Cloud** provides library-level building blocks: Config Server, Eureka discovery, Gateway, LoadBalancer, OpenFeign, Resilience4j integration, Stream.

**Kubernetes** now provides many of these at the platform level: **service discovery and load balancing** (Services and DNS), **config and secrets** (ConfigMaps, Secrets), **health-based restarts** (probes), **scaling** (HPA), and **rollouts**. A common modern setup: Kubernetes for discovery and config, Spring Boot with Micrometer and Resilience4j, plus a gateway at the edge, and less of the older Eureka and Config Server stack.

## [3-5] How do you handle authentication and authorisation between microservices?
**User identity:** authenticate at the **API gateway** (OAuth2/OIDC), then forward the **JWT** to downstream services. Each service acts as a resource server and **validates the token itself** (signature, `exp`, audience) and enforces its own authorisation, because the network is not a trust boundary. Do not trust a plain `X-User-Id` header unless it is set by a trusted gateway and the service is unreachable from outside.

**Service identity:** for calls with no user (batch, back-end jobs) use the **client credentials** flow, and **mTLS** (service mesh such as Istio or Linkerd) to authenticate and encrypt service-to-service traffic.

**Other points:**
- Propagate tokens carefully (token relay) and prefer **narrow scopes**, or use **token exchange** for downstream calls.
- Keep fine-grained authorisation close to the data owner.
- Use short-lived tokens, rotate keys with JWKS, keep secrets in a vault, and audit access.
