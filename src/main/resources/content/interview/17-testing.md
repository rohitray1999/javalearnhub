---
topic: Testing
order: 17
---
## [0-1] What are unit, integration and end-to-end tests? What are the JUnit 5 basics?
- **Unit test**: one class or method in isolation, with dependencies mocked. Fast, many of them.
- **Integration test**: several components together (service + real database, controller + JSON). Slower, fewer.
- **End-to-end test**: the whole system through the UI or public API. Slowest and most fragile, so keep few.

```java
class PriceCalculatorTest {
    @Test
    void appliesTenPercentDiscount() {
        assertEquals(90.0, new PriceCalculator().apply(100.0, 10), 0.001);
    }

    @ParameterizedTest
    @CsvSource({"100,10,90", "200,50,100"})
    void discounts(double price, int pct, double expected) { ... }
}
```

JUnit 5 annotations: `@Test`, `@BeforeEach`, `@AfterEach`, `@ParameterizedTest`, `@Disabled`, `@Nested`; assertions such as `assertThrows`, `assertAll`. Name tests by behaviour, use arrange-act-assert, and test one thing per test.

## [1-3] How do you use Mockito? What is the difference between a mock and a spy?
A **mock** is a fake object whose methods do nothing unless you stub them. A **spy** wraps a **real** object: real methods run unless stubbed.

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock PaymentGateway gateway;
    @InjectMocks OrderService service;

    @Test
    void chargesCustomer() {
        when(gateway.charge(any(), eq(500.0))).thenReturn(Receipt.ok());

        service.checkout(order);

        verify(gateway).charge(any(), eq(500.0));
        verifyNoMoreInteractions(gateway);
    }
}
```

Useful: `ArgumentCaptor` to inspect arguments, `doThrow(...).when(mock)` to simulate failures, `BDDMockito` (`given/then`), strict stubs that flag unused stubbing.

Guidance: **mock your own boundaries** (gateways, repositories), not value objects or the class under test; too many mocks mean the design is too coupled or the test is checking implementation instead of behaviour.

## [3-5] In Spring Boot, when do you use @SpringBootTest, @WebMvcTest, @DataJpaTest and Testcontainers?
- **`@SpringBootTest`**: loads the **full context**. Use for a few end-to-end style integration tests (with `webEnvironment = RANDOM_PORT` and `TestRestTemplate` or `WebTestClient`). Slow if overused.
- **`@WebMvcTest(UserController.class)`**: only the **web layer**; services replaced with `@MockitoBean` (Spring Boot 3.4+; `@MockBean` before). Test routing, JSON, validation and security with `MockMvc`.
- **`@DataJpaTest`**: only the JPA slice with an embedded database by default, transactional with rollback per test. Test repositories and queries.
- **Testcontainers**: run a **real PostgreSQL, Kafka or Redis** in Docker for tests. Embedded H2 behaves differently from production (dialects, types, locking), so use the real database for query and migration tests.

```java
@Testcontainers @DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
class OrderRepositoryTest {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16");
}
```
Reuse containers and keep slices small to keep the suite fast.

## [5+] How do you design a test strategy for a large system? Flaky tests, contract tests and TDD.
**Shape:** a pyramid, with many fast unit tests, a solid layer of integration tests on real infrastructure, a few end-to-end smoke tests for critical journeys. Test **behaviour at boundaries**, not private details.

**Flaky tests** (pass and fail randomly) destroy trust. Typical causes: shared state and test order, real time (`Thread.sleep`, `now()`), async races, network and external services. Fix by controlling the clock (inject `Clock`), using Awaitility instead of sleeps, isolating data per test, and **quarantining and fixing** flakes quickly rather than rerunning.

**Microservices:** use **consumer-driven contract tests** (Pact, Spring Cloud Contract) so a provider cannot break a consumer without a failing build, instead of slow shared end-to-end environments.

**Quality signals:** coverage is a hint, not a goal. **Mutation testing** (PIT) shows whether tests really detect changes. Add performance and load tests (Gatling, k6) and test failure modes (timeouts, retries).

**TDD** (red, green, refactor) is excellent for logic-heavy code and gives design feedback; apply it where it pays off rather than dogmatically. Keep the build fast and run tests in CI on every change.

## [1-3] What are the types of test doubles? What is the difference between TDD and BDD?
**Test doubles** replace real dependencies in tests:
- **Dummy**: passed but never used.
- **Stub**: returns **canned answers** (`when(repo.find(1)).thenReturn(user)`).
- **Mock**: a stub that also **verifies interactions** (`verify(mailer).send(...)`).
- **Spy**: wraps the real object and records calls.
- **Fake**: a working lightweight implementation (in-memory repository).

Prefer fakes or stubs when you care about **state and output**; use mocks when the **interaction itself** is the behaviour (an email must be sent).

**TDD** (Test-Driven Development): write a failing test, make it pass, refactor (red, green, refactor). It is a **design and developer practice**. **BDD** (Behaviour-Driven Development) describes behaviour in business language (**Given, When, Then**, Cucumber or well-named tests), to align developers, QA and business around shared examples.

## [3-5] How do you test REST controllers and security with MockMvc?
`@WebMvcTest` plus `MockMvc` tests the web layer (routing, JSON, validation, status codes, security) **without a server**, mocking the service.

```java
@WebMvcTest(OrderController.class)
class OrderControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean OrderService service;

    @Test
    @WithMockUser(roles = "USER")
    void createsOrder() throws Exception {
        when(service.create(any())).thenReturn(new OrderDto(7L, "NEW"));

        mvc.perform(post("/orders").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"A1\",\"qty\":2}"))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void rejectsAnonymous() throws Exception {
        mvc.perform(get("/orders")).andExpect(status().isUnauthorized());
    }
}
```
Also test: validation failures return `400` with the right body, an authenticated user without the role gets `403`, and error handling (`@RestControllerAdvice`). For a full stack with a real database use `@SpringBootTest` plus Testcontainers.
