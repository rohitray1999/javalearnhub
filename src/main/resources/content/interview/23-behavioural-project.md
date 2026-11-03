---
topic: Behavioural and project discussion
order: 23
---
## [0-1] "Tell me about yourself": how should a fresher answer?
Keep it to about **60 to 90 seconds**, structured as **present, past, future**:

1. **Who you are**: degree, college, and the area you focus on (Java, backend, Spring Boot).
2. **Proof**: one or two projects or an internship, with **what you built and the technologies**, and one concrete detail (for example "a REST API with JWT login and PostgreSQL; I added pagination and unit tests").
3. **Skills you are building**: Core Java, collections, multithreading basics, SQL, Git.
4. **Why this role**: what you want to learn and contribute.

Avoid reciting your CV, avoid personal details irrelevant to the job, and do not say anything you cannot explain in depth, because the interviewer will pick one of your lines and drill into it.

## [1-3] How do you explain your current project and your role?
Prepare a **two minute story** with this structure:
1. **Business problem**: who uses it and why it matters (for example "a municipal payment system that generates challans and receipts for citizens").
2. **Architecture**: modules, main flows, technologies (Spring Boot, database, messaging, front end), deployment.
3. **Your contribution**: specific features, APIs, queries or fixes **you** owned. Say "I", not only "we".
4. **Scale and constraints**: users, transactions per day, data size, performance or compliance needs.
5. **Challenges and results**: a problem you solved and the measurable outcome ("reduced report time from 40 s to 6 s by adding an index and replacing N+1 queries").

Be ready to draw the architecture and to explain **why** each choice was made, the **trade-offs**, and what you would improve today.

## [1-3] Describe a difficult bug you fixed. How do you debug a production issue?
Use the **STAR** format: **Situation**, **Task**, **Action**, **Result**.

Example outline: *Situation*: duplicate receipts were generated under load. *Task*: find the cause quickly without stopping payments. *Action*: reproduced with a concurrency test, found a check-then-insert race, added a unique constraint and an idempotency key, added logging. *Result*: duplicates dropped to zero, plus a regression test.

**Systematic debugging process:**
1. **Reproduce** or gather evidence: logs, metrics, traces, a recent deployment or data change.
2. **Narrow down**: where does it fail, and for whom? Compare with working cases.
3. **Form a hypothesis** and test it (one change at a time).
4. **Fix the root cause**, not the symptom, and add a **test**.
5. **Prevent**: alerts, validation, documentation.

## [1-3] Why are you looking for a change, and what questions do you ask the interviewer?
**Why change:** be **positive and forward-looking**: what you want more of (bigger scale, modern stack, ownership, learning), not complaints about your employer or manager. Be honest and consistent with your CV.

**Good questions to ask:** how the team is structured and how work is planned; what the **first 90 days** look like; what the tech stack and deployment process are; how code review, testing and on-call work; what the biggest technical challenge is right now; how growth and feedback are handled. Avoid questions answered on the careers page, and avoid leading with salary or leave in a technical round (discuss compensation with the recruiter when appropriate).

## [3-5] How do you handle disagreements with teammates, including in code reviews?
- **Separate the person from the issue**; focus on the code and the **goal** (maintainability, performance, delivery).
- **Listen first** and restate their view to confirm you understood it.
- Bring **evidence**: benchmarks, requirements, incidents, documentation, not opinions.
- Discuss **trade-offs** openly and consider if both options are acceptable; many disagreements are about style.
- If you disagree after a fair discussion, **escalate to the tech lead** or follow the agreed decision, then **commit** to it.
- In reviews, comment kindly and specifically ("could we extract this so it is testable?"), distinguish **must fix** from **nitpick**, and when receiving feedback, assume good intent and ask questions.

Give a real example in which you changed your mind, because it shows maturity.

## [3-5] How do you estimate work and deal with tight deadlines or changing scope?
**Estimation:** break the work into small tasks, include **testing, review, deployment and unknowns**, give a **range** instead of a single number, and mention assumptions and risks. Use past velocity as evidence. Spike first on unknown parts.

**Tight deadlines:**
- **Clarify the real priority**: what is the minimum valuable scope (MVP)?
- Communicate **early** if the plan is at risk, with options: cut scope, move the date, add people (rarely helps), or accept risk with a mitigation.
- **Do not silently cut quality** (tests, security, error handling); make technical debt visible and schedule the payback.
- Protect focus, remove blockers, and keep stakeholders updated with short, honest status.

**Changing scope:** ask what is **dropped or delayed** in exchange; track changes; keep releases small.

## [5+] How do you mentor junior developers and raise engineering quality in a team?
- **Mentoring**: pair on real tasks, give **context** (why, not just what), let them struggle a bit but unblock quickly, give **specific and timely feedback**, delegate stretch tasks with support, and share credit.
- **Code reviews** as teaching: explain principles, link to references, review small pull requests, and encourage juniors to review seniors' code too.
- **Standards**: coding guidelines, a PR checklist, automated formatting and static analysis, test and coverage expectations in CI, templates for new services.
- **Knowledge sharing**: design docs, brown-bag sessions, runbooks, onboarding guides.
- **Culture**: blameless postmortems, psychological safety, and metrics that the team owns (lead time, change failure rate).

Measure the result: faster onboarding, fewer production defects, more people able to own parts of the system.

## [5+] Tell me about a major production incident you handled. How do you run an incident and a postmortem?
**During the incident:**
1. **Stabilise first**: assess impact, **roll back or mitigate** (feature flag, scale up, fail over) before hunting for the root cause.
2. Assign roles: **incident commander**, communications, and investigators; keep a timeline.
3. **Communicate** regularly to stakeholders and customers with facts, not guesses.
4. Verify recovery with metrics, not assumptions.

**Afterwards: a blameless postmortem** within a few days: timeline, impact, **root causes and contributing factors** (use "five whys"), what went well, what did not, and **action items with owners and due dates** (detection, prevention, runbooks, tests). Focus on systems and process, not individuals, and follow up until the actions are done.

In an interview, tell it as a story: impact, your actions and decisions under pressure, the result, and **what you changed so it cannot recur**.

## [5+] How do you make architecture decisions, document them and manage technical debt?
**Decisions:**
- Start from **requirements and constraints** (scale, latency, team skills, cost, deadlines, compliance), not from technology preferences.
- List **options with trade-offs**, prototype or spike when uncertain, and prefer **reversible** decisions; take extra care with one-way doors (data model, public APIs).
- Involve the people who will build and run it; make the decision explicit.
- Record it in an **ADR** (Architecture Decision Record): context, decision, alternatives, consequences, status. Revisit when assumptions change.

**Technical debt:** make it **visible** (a tracked backlog with impact and cost), distinguish **deliberate** debt (to hit a date, with a payback plan) from accidental debt, allocate a steady share of capacity (for example 10 to 20 percent) to paying it down, and refactor **alongside feature work** in the areas you touch. Argue for debt work in terms of **business risk and delivery speed**, not code beauty.
