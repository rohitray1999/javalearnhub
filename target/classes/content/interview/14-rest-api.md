---
topic: REST API design
order: 14
---
## [0-1] What is REST? Which HTTP methods and status codes should you know?
**REST** is an architectural style: resources identified by URLs (`/orders/42`), manipulated through standard HTTP methods, **stateless** requests, uniform interface, and representations such as JSON.

| Method | Use | Safe | Idempotent |
|---|---|---|---|
| GET | read | yes | yes |
| POST | create / action | no | **no** |
| PUT | replace | no | yes |
| PATCH | partial update | no | not guaranteed |
| DELETE | remove | no | yes |

Common codes: `200 OK`, `201 Created` (+ `Location` header), `204 No Content`, `400 Bad Request`, `401 Unauthorized` (not authenticated), `403 Forbidden` (authenticated but not allowed), `404 Not Found`, `409 Conflict`, `422 Unprocessable Entity`, `429 Too Many Requests`, `500 Internal Server Error`, `503 Service Unavailable`.

**Safe** means no side effects; **idempotent** means repeating the call has the same effect as doing it once.

## [0-1] @Controller versus @RestController? @PathVariable versus @RequestParam versus @RequestBody?
`@RestController` = `@Controller` + `@ResponseBody`: return values are written straight to the response body (JSON), not resolved as view names. `@Controller` is for server-rendered pages (Thymeleaf).

- `@PathVariable`: a part of the URL path: `/users/{id}`.
- `@RequestParam`: a query parameter or form field: `/users?active=true`.
- `@RequestBody`: the request body, deserialised by Jackson into an object (`@Valid` triggers Bean Validation).

```java
@GetMapping("/users/{id}")
User get(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean withOrders) { ... }

@PostMapping("/users")
ResponseEntity<User> create(@Valid @RequestBody CreateUser req) {
    User u = service.create(req);
    return ResponseEntity.created(URI.create("/users/" + u.id())).body(u);
}
```

## [1-3] Walk through the Spring MVC request flow.
1. The servlet container passes the request through **filters** (security, logging).
2. **`DispatcherServlet`** (front controller) receives it.
3. **`HandlerMapping`** finds the controller method (by URL, HTTP method, headers).
4. **Interceptors** (`preHandle`) run.
5. **`HandlerAdapter`** invokes the method; **argument resolvers** and **message converters** bind path variables, parameters and the JSON body, and validation runs.
6. The controller returns data; for `@ResponseBody` an **`HttpMessageConverter`** (Jackson) writes it. For views, a `ViewResolver` renders a template.
7. Exceptions go to **`HandlerExceptionResolver`s** (`@ExceptionHandler`, `@ControllerAdvice`).
8. Interceptor `postHandle` and `afterCompletion`, then the response returns through the filters.

Knowing the filter versus interceptor difference helps: filters are servlet level (before Spring MVC), interceptors are MVC level.

## [1-3] PUT versus PATCH versus POST? How do you make a POST safe to retry?
- **POST** creates or triggers an action; calling it twice may create two resources.
- **PUT** **replaces** the whole resource at a known URL; repeating it gives the same state.
- **PATCH** changes **part** of a resource (JSON Merge Patch or JSON Patch).

**Retry-safe POST:** use an **idempotency key**. The client sends `Idempotency-Key: <uuid>`; the server stores the key with the response (unique constraint) and, on a repeat, returns the **stored result** instead of processing again. Essential for payments and order creation, because network timeouts make clients retry.

## [3-5] What are your best practices for REST API design?
- **Nouns, plural, hierarchical**: `/customers/7/orders`, not `/getOrders`.
- **Versioning** from the start (URL `/v1/...` or header), and **additive, backward compatible** changes.
- **Pagination** on every list (`page`, `size` or **cursor/keyset** for large data), with filtering and sorting parameters. Return total counts or `next` links.
- **Consistent errors** using RFC 9457 `application/problem+json`, with machine-readable codes.
- **Validation** with Bean Validation, returning field-level messages.
- **DTOs**, never expose JPA entities (avoids leaking fields and lazy loading issues).
- **Correct status codes**, `Location` on create, ETags for concurrency (`If-Match`).
- Document with **OpenAPI**; treat the spec as a contract.
- Secure by default (HTTPS, authentication, rate limits).
- Use `Instant` or ISO-8601 dates in UTC.

## [3-5] How do you handle concurrent updates and large payloads in an API?
**Lost updates:** use **optimistic concurrency**. Return an `ETag` (or `version` field) on GET; the client sends `If-Match` on update; if the version changed, respond `412 Precondition Failed` (or `409`). Backed by JPA `@Version`.

**Large payloads and long operations:**
- Paginate or stream; support `Accept-Encoding: gzip`.
- For long-running work return `202 Accepted` with a **status URL** (`/jobs/123`) and let the client poll, or use a webhook.
- Uploads: stream to object storage (pre-signed URLs) rather than through your app servers; set size limits.
- Set server and client **timeouts** and request size limits so one slow client cannot hold resources.

## [5+] How do you make an API fast, scalable and evolvable?
**Performance**
- Remove N+1 queries; use projections and indexes; cache.
- **HTTP caching**: `Cache-Control`, `ETag` (conditional GET returns `304`); a CDN for public reads.
- Application caching (Caffeine, Redis) with clear invalidation.
- Compression, connection pooling, HTTP/2, keep payloads small (sparse fields).
- Move slow work **off the request path** (queues, async processing).

**Scalability**
- Keep services **stateless**, scale horizontally behind a load balancer.
- **Rate limiting** and quotas per client, back-pressure and timeouts.
- Protect dependencies with bulkheads and circuit breakers.

**Evolvability**
- Add fields instead of changing meaning; tolerate unknown fields (be liberal in what you accept).
- Deprecate with `Sunset` headers and telemetry on version use.
- **Consumer-driven contract tests** (Pact or Spring Cloud Contract) catch breaking changes before release.

## [5+] REST, gRPC or GraphQL: how do you choose?
- **REST/JSON**: universal, cacheable with HTTP, easy to debug; best for **public and browser-facing** APIs. Can over-fetch or need many round trips.
- **gRPC** (Protobuf over HTTP/2): compact, fast, strongly typed, **streaming**, code generation. Ideal for **internal service-to-service** calls. Weaker browser support, less human-readable.
- **GraphQL**: clients **ask for exactly the fields** they need from one endpoint; good for varied front ends (mobile and web) and aggregating many backends. Costs: query complexity and cost control, caching is harder, N+1 on the server (use DataLoader), authorisation per field.

A common architecture: GraphQL or REST at the edge (gateway or BFF), gRPC or messaging between services. Choose based on consumers, performance needs and team skills.

## [1-3] How does Jackson serialise JSON? Which annotations and settings matter?
Jackson converts Java objects to JSON and back through `ObjectMapper` (Spring Boot configures one for you).

Useful annotations: `@JsonProperty("user_id")` (rename), `@JsonIgnore` and `@JsonIgnoreProperties` (hide fields such as passwords), `@JsonInclude(NON_NULL)`, `@JsonFormat(pattern = "...")` for dates, `@JsonCreator` for immutable constructors, `@JsonView` for different projections.

Settings worth knowing (Spring Boot properties):
- `spring.jackson.deserialization.fail-on-unknown-properties=false` (be tolerant of new fields from other services; Boot already defaults to this).
- Write dates as **ISO-8601**, not timestamps: `spring.jackson.serialization.write-dates-as-timestamps=false` with the `jackson-datatype-jsr310` module (included by Boot).

**Security note:** never enable **polymorphic default typing** (`enableDefaultTyping`) for untrusted input; it has caused many deserialisation vulnerabilities. Never serialise entities with lazy relations directly; use DTOs.

## [1-3] How do you implement pagination and sorting in a REST API? Page versus Slice versus keyset?
Spring Data supports `Pageable` as a controller argument: `GET /orders?page=0&size=20&sort=createdAt,desc`.

```java
@GetMapping("/orders")
Page<OrderDto> list(@PageableDefault(size = 20, sort = "createdAt", direction = DESC) Pageable p) {
    return repo.findAll(p).map(mapper::toDto);
}
```
- **`Page`**: content plus total count and total pages (an extra `COUNT` query, which is slow on huge tables).
- **`Slice`**: only knows whether a next page exists; cheaper.
- **Offset paging** (`LIMIT 20 OFFSET 100000`) gets slower as the offset grows and can skip or repeat rows when data changes.
- **Keyset (cursor) paging**: `WHERE (created_at, id) < (:lastCreated, :lastId) ORDER BY created_at DESC, id DESC LIMIT 20`; fast at any depth and stable. Return an opaque `nextCursor` token.

Always **cap `size`** (for example at 100) so clients cannot request a million rows, and whitelist sortable fields.
