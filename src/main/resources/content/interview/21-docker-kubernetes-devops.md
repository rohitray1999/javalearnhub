---
topic: Docker, Kubernetes and DevOps
order: 21
---
## [0-1] What is Docker? What is the difference between an image and a container?
**Docker** packages an application with its dependencies so it runs the same everywhere.

- **Image**: a read-only, layered **template** (OS libraries, JDK, your JAR). Built from a `Dockerfile`.
- **Container**: a **running instance** of an image, isolated using Linux namespaces and cgroups. It shares the host kernel, so it is lighter than a virtual machine.
- **Registry**: where images are stored (Docker Hub, ECR, GCR).

```dockerfile
FROM eclipse-temurin:21-jre
COPY target/app.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```
Commands: `docker build -t myapp .`, `docker run -p 8080:8080 myapp`, `docker ps`, `docker logs <id>`, `docker exec -it <id> sh`. Container data is **ephemeral**; use volumes for persistence.

## [1-3] How do you write a good Dockerfile for a Spring Boot application?
- Use a **multi-stage build**: build with a JDK and Maven image, then copy only the JAR into a small **JRE** image.
- Use **layered JARs** (`java -Djarmode=tools extract --layers` or Spring Boot's layertools) so dependency layers are cached and only your code layer changes on each build.
- Run as a **non-root user**.
- Add a `.dockerignore` (exclude `target/`, `.git`).
- Set JVM flags for containers: `-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError`.
- Pin base image versions (not `latest`) and scan images for vulnerabilities (Trivy).
- Handle signals: use the **exec form** of `ENTRYPOINT` so the JVM receives `SIGTERM` for graceful shutdown.

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml . 
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

FROM eclipse-temurin:21-jre
RUN useradd -r app
USER app
COPY --from=build /src/target/*.jar /app.jar
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "/app.jar"]
```
Alternative: `mvn spring-boot:build-image` (Cloud Native Buildpacks) creates an optimised image without a Dockerfile.

## [1-3] Git: merge versus rebase, reset versus revert, and branching strategies.
- **`git merge`**: joins histories with a **merge commit**; preserves the true history. Safe for shared branches.
- **`git rebase`**: **replays** your commits on top of another branch for a linear history. It rewrites commits, so **never rebase commits that others have pulled**.
- **`git cherry-pick`**: copy one commit onto the current branch (hotfix backports).
- **`git reset`**: moves the branch pointer (`--soft` keeps changes staged, `--mixed` unstaged, `--hard` discards). Rewrites history, so use it only on local work.
- **`git revert`**: creates a **new commit that undoes** an earlier one; safe for shared branches.
- **`git stash`** saves uncommitted work temporarily; `git reflog` helps recover "lost" commits.

**Branching:** **Gitflow** (develop, release and hotfix branches; heavier, suits versioned releases) versus **trunk-based development** (short-lived branches, frequent merges to main, feature flags; suits continuous delivery). Keep branches short, pull requests small, and resolve conflicts by understanding both changes.

## [3-5] Explain the core Kubernetes objects and how a Java service runs on them.
- **Pod**: the smallest unit; one or more containers sharing network and storage.
- **Deployment** (manages **ReplicaSets**): declares the desired number of replicas, handles **rolling updates** and rollbacks.
- **Service**: a stable virtual IP and DNS name that load balances across pods (`ClusterIP`, `NodePort`, `LoadBalancer`).
- **Ingress**: HTTP(S) routing from outside to services (host and path rules, TLS).
- **ConfigMap** and **Secret**: configuration and sensitive data injected as environment variables or files.
- **Namespace**: logical separation of teams or environments.
- **Probes**: **liveness** (restart if dead), **readiness** (send traffic only when ready), **startup** (slow starters).
- **Resources**: `requests` (used for scheduling) and `limits` (hard cap). Memory over the limit means the container is killed (OOMKilled); CPU over the limit is **throttled**.
- **HPA**: autoscaling on CPU, memory or custom metrics.

For Spring Boot: map readiness and liveness to Actuator probes, use graceful shutdown (`server.shutdown=graceful`) with a `terminationGracePeriodSeconds`, and keep the app stateless.

## [3-5] What does a CI/CD pipeline look like? Compare rolling, blue-green and canary deployments.
**Pipeline:** commit, then build and unit tests, static analysis (Sonar, dependency scan), package and push the image, deploy to a test environment, integration and smoke tests, then promote to production (manual approval for continuous delivery, automatic for continuous deployment). Keep it fast, and make the same artefact flow through every environment.

**Deployment strategies:**
- **Rolling**: replace instances gradually; no extra capacity needed; old and new versions run together, so changes must be **backward compatible** (especially the database).
- **Blue-green**: run a full new environment, then switch traffic at once; instant rollback by switching back; needs double capacity.
- **Canary**: send a small percentage of traffic to the new version, watch metrics, then increase. The safest for risky changes; needs good observability.

Add **feature flags** (decouple deployment from release), automated rollback on failed health checks or error-rate alerts, and database migrations that follow expand and contract.

## [5+] A Java service in Kubernetes keeps restarting. How do you troubleshoot it?
1. `kubectl get pods` and `kubectl describe pod <p>`: read the **last state, exit code and events**.
   - **OOMKilled (exit 137)**: the container exceeded its memory limit. The heap may be fine but **total** memory (metaspace, threads, direct buffers) is too big. Use `MaxRAMPercentage`, review the limit, and check for native memory growth.
   - **Exit 143**: terminated by SIGTERM (normal rollout or eviction).
   - **CrashLoopBackOff**: the app fails at startup; read `kubectl logs --previous`.
2. **Probe failures**: a liveness probe that is too strict or that checks external dependencies kills healthy pods; a startup probe or longer `initialDelaySeconds` helps slow JVM start-up.
3. **CPU throttling**: tiny CPU limits slow JIT and GC and cause latency; check throttling metrics and raise the limit (or drop CPU limits and keep requests).
4. **Evictions and node pressure**: `kubectl describe node`, `kubectl top pod`.
5. **Deeper**: take a heap dump or thread dump with `kubectl exec` and `jcmd` (or an **ephemeral debug container** `kubectl debug`), look at GC logs, and compare with metrics and traces from before the restart.
6. Fix the cause, add a meaningful alert, and write down the runbook.

## [5+] What are SLI, SLO, SLA and error budgets? How do they guide engineering decisions?
- **SLI** (Service Level Indicator): a **measurement** of user-visible health, such as the proportion of requests under 300 ms, or the success rate.
- **SLO** (Service Level Objective): the **target** for an SLI, for example "99.9% of requests succeed over 30 days".
- **SLA** (Service Level Agreement): a **contractual** promise with consequences (credits); usually looser than the internal SLO.
- **Error budget**: `100% minus SLO`. At 99.9% over 30 days you can "spend" about 43 minutes of failure.

**Use:** if the budget is healthy, ship features faster; if it is burning, **slow releases and prioritise reliability work**. Alert on **burn rate** (how fast the budget is consumed) rather than on every spike, which cuts noise. Choose SLIs from the user's perspective (latency, availability, correctness, freshness), and review them after incidents with **blameless postmortems**.
