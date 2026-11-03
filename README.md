# JavaLearnHub

Java / Spring Boot / Microservices learning site. Spring Boot 3.5 + Thymeleaf + Bootstrap 5.3.8 (CDN).

## Run

Requirements: JDK 21+, Maven 3.9+

```bash
mvn spring-boot:run
# open http://localhost:8080
```

Pehli baar chalane par starter content (9 lessons, 27 quiz questions, 3 paths) automatically DB mein seed ho jata hai.
Interview content (218 questions, 23 topics) har start par Markdown files se sync hota hai. Default DB: file based H2 (`./data/learnhub`).

### PostgreSQL

```bash
docker run -d --name learnhub-pg -p 5432:5432 \
  -e POSTGRES_DB=learnhub -e POSTGRES_USER=learnhub -e POSTGRES_PASSWORD=learnhub postgres:16
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

## Features

- Stages, lessons (Markdown), sidebar navigation, prev/next
- Quiz per lesson (answers server par check hote hain, sahi jawab page ke saath nahi jata)
- Register / login (BCrypt, Spring Security form login)
- Mark complete, progress bars, streak, daily goal, estimated finish date
- Bookmarks, personal notes, search, learning paths, interview questions
- Dark mode, responsive layout

## Structure

```
config/   Security, UserDetails, DataSeeder (starter content)
domain/   JPA entities
repo/     Spring Data repositories
service/  Content, Progress, Quiz, User services
web/      MVC controllers
resources/templates, static/css, static/js
```

## Interview questions add/edit karna

Files: `src/main/resources/content/interview/*.md` (ek file = ek topic). Format:

```
---
topic: Core Java
order: 1
---
## [0-1] Question text?
Answer in Markdown (code blocks, tables, lists)...

## [5+] Senior level question?
...
```

Levels: `[0-1]`, `[1-3]`, `[3-5]`, `[5+]` (years of experience). App restart par sync hota hai:
naya question add, badla hua update, file se hataya hua DB se delete. DB delete karne ki zarurat nahi.
Page par filters hain: experience level, topic, aur search box.

## Naya lesson add karna

`DataSeeder` mein `lesson(...)` + `quiz(...)` ka pattern copy karo. Seeder sirf tab chalta hai jab DB khaali ho,
isliye existing DB mein naya content ke liye `./data` folder delete karo ya admin/import feature banao (next step).

## Next steps

1. Lessons ko Markdown files (`resources/content/*.md`) se import karna
2. Admin page: lesson/quiz add-edit
3. Code runner (Judge0/Piston) aur AI mentor (backend se Anthropic API)
4. Interview flashcards with spaced repetition, certificates, Razorpay courses
