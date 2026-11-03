---
topic: Spring Security
order: 13
---
## [1-3] How does Spring Security work? Authentication versus authorization, and the filter chain.
- **Authentication**: who are you? (login, token validation)
- **Authorization**: what are you allowed to do? (roles, permissions)

Spring Security is a chain of **servlet filters** in front of your controllers. A request passes through filters such as `SecurityContextHolderFilter`, `CsrfFilter`, `UsernamePasswordAuthenticationFilter` / `BearerTokenAuthenticationFilter`, `ExceptionTranslationFilter` and `AuthorizationFilter`. After authentication, an `Authentication` object is stored in the **`SecurityContext`** (thread local) and used for authorization.

In Spring Security 6 you configure it with a `SecurityFilterChain` **bean** (the old `WebSecurityConfigurerAdapter` is gone):

```java
@Bean
SecurityFilterChain chain(HttpSecurity http) throws Exception {
    return http
        .authorizeHttpRequests(a -> a
            .requestMatchers("/public/**").permitAll()
            .requestMatchers("/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated())
        .formLogin(Customizer.withDefaults())
        .build();
}
```
Order matters: specific rules first, `anyRequest()` last.

## [1-3] How should you store passwords?
Never store plain text or fast hashes (MD5, SHA-1, plain SHA-256). Use a **slow, salted, adaptive** hash: **BCrypt**, **Argon2** or **PBKDF2/scrypt**. Each hash includes a unique **salt**, which defeats rainbow tables, and a **work factor** that you can raise as hardware improves.

```java
@Bean PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();   // "{bcrypt}..." prefix
}
```

`DelegatingPasswordEncoder` stores the algorithm id with the hash, so you can **migrate algorithms** without breaking existing users. Also: rate limit and lock out login attempts, require sensible password length, and never log credentials.

## [3-5] How does JWT authentication work, and what are its pitfalls?
A JWT has three Base64URL parts: **header**, **payload (claims)**, **signature**. Flow: the user logs in, the server issues a signed token (`sub`, `exp`, roles), the client sends it as `Authorization: Bearer <token>`, and the server verifies the **signature and expiry** with no session lookup (stateless).

**Pitfalls:**
- The payload is **encoded, not encrypted**. Never put secrets in it.
- Tokens are hard to **revoke** before expiry. Use **short-lived access tokens** (minutes) plus **refresh tokens** (rotated, stored server side, revocable) and a deny list when needed.
- Validate `exp`, `iss`, `aud` and force the expected algorithm (reject `alg: none` and algorithm confusion HS256 versus RS256).
- Browser storage: `localStorage` is exposed to XSS; an `HttpOnly`, `Secure`, `SameSite` cookie is safer but then needs CSRF protection.
- Keep signing keys in a vault and rotate them (use `kid` and JWKS).

## [3-5] What are CSRF and CORS? When can you disable CSRF?
- **CSRF**: a malicious site makes the victim's browser send a **state-changing request** to your site, and the browser attaches the victim's **session cookie** automatically. Defence: a **CSRF token** that attackers cannot read, plus `SameSite` cookies.
- **CORS**: a **browser** rule that blocks a page from one origin reading responses from another unless the server allows it with headers (`Access-Control-Allow-Origin`). It is **not** a server-side security control; it does not stop curl or other servers.

**Disabling CSRF** is acceptable for **stateless APIs** that authenticate with a bearer token in the `Authorization` header and use **no cookie-based authentication**, because the browser does not attach that header automatically. Keep CSRF **enabled** for cookie or session based apps (including server-rendered Thymeleaf forms).

CORS: allow **specific** origins, never `*` together with credentials.

## [3-5] How do you secure methods with @PreAuthorize?
Enable it with `@EnableMethodSecurity`. Then protect business methods, not just URLs:

```java
@PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
public Profile getProfile(Long userId) { ... }

@PostAuthorize("returnObject.owner == authentication.name")
public Document load(Long id) { ... }
```

- It works through **AOP proxies**, so the same self-invocation limit applies.
- It enforces **object-level** rules (can *this* user see *this* record), which URL rules cannot.
- Roles get the `ROLE_` prefix (`hasRole('ADMIN')` checks `ROLE_ADMIN`); `hasAuthority` uses the exact string.
- Test it with `@WithMockUser`.

## [5+] Explain OAuth 2.0 and OpenID Connect. Which flow would you use, and how does Spring fit?
**OAuth 2.0** is about **delegated authorization**: a client gets limited access to a resource on behalf of the user, without seeing the password. **OpenID Connect** adds an **identity layer** (ID token, `userinfo`) so the client knows *who* logged in.

Roles: **resource owner** (user), **client** (app), **authorization server** (Keycloak, Okta, Entra ID), **resource server** (your API).

Flows:
- **Authorization Code + PKCE**: for web, SPA and mobile apps (the standard choice; PKCE protects public clients).
- **Client Credentials**: service-to-service, no user.
- Avoid **implicit** and **password** grants (deprecated).

Spring: the API is an **OAuth2 Resource Server** (`spring-boot-starter-oauth2-resource-server`, point to `issuer-uri`, JWTs validated through JWKS) and the web app uses `oauth2Login` / `oauth2Client`. Map scopes or claims to authorities with a `JwtAuthenticationConverter`. Use short-lived tokens, validate audience, and do not roll your own auth server unless you must.

## [5+] How do you defend a Spring application against the OWASP Top 10?
- **Broken access control (IDOR)**: check **ownership** on every object (`@PreAuthorize`, query by `id AND ownerId`), deny by default.
- **Injection**: parameterised queries / JPA parameters, never concatenate SQL; validate and encode output.
- **Cryptographic failures**: TLS everywhere, strong password hashing, no sensitive data in logs or URLs, rotate keys.
- **Insecure design**: threat model, rate limiting, abuse cases, secure defaults.
- **Security misconfiguration**: secure Actuator, disable debug and stack traces in production, security headers (HSTS, CSP, `X-Content-Type-Options`).
- **Vulnerable components**: dependency scanning (OWASP Dependency-Check, Dependabot), keep Spring and the JDK patched.
- **Authentication failures**: MFA, lockout, secure session handling (`HttpOnly`, `Secure`, session fixation protection, which Spring does by default).
- **Integrity failures** (deserialisation, unsigned updates): avoid Java native serialisation of untrusted data; configure Jackson safely, sign artefacts.
- **Logging and monitoring**: audit logs for auth events, alerts on anomalies.
- **SSRF**: allow-list outbound URLs, block internal IP ranges.

## [1-3] How do you implement login with a database, roles and UserDetailsService?
1. Store users with a **BCrypt-hashed** password and roles.
2. Implement `UserDetailsService.loadUserByUsername` to load the user and return a `UserDetails` with authorities.
3. Define a `PasswordEncoder` bean and a `SecurityFilterChain`. Spring wires an `AuthenticationProvider` (`DaoAuthenticationProvider`) automatically.

```java
@Service
class DbUserDetailsService implements UserDetailsService {
    private final UserRepository users;
    public UserDetails loadUserByUsername(String email) {
        AppUser u = users.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException(email));
        return User.withUsername(u.getEmail())
                   .password(u.getPasswordHash())
                   .roles(u.getRoles().toArray(String[]::new))
                   .build();
    }
}
```
Good practice: return the **same generic error** for "unknown user" and "wrong password" (avoid user enumeration), throttle attempts, and keep the roles in the database, not hard coded. For access inside code use `@AuthenticationPrincipal` or `SecurityContextHolder`.
