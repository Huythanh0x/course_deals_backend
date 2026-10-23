# AGENTS.md

Facts and pointers for working in this repo with Google Antigravity. For process detail,
follow the links into `/docs` rather than expecting it duplicated here.

## Project snapshot

Spring Boot multi-module backend service for the Course Deals platform, written in
Kotlin. Package prefix `com.thanh0x.coursedeal`. Powers the REST API, deals discovery
crawlers, background validation scraping pipeline, identity & authentication (Passkey/OAuth2),
and targeted push notifications.

- Kotlin 1.9.21, Spring Boot 3.5.7, Spring Dependency Management 1.1.0, Gradle 8.x/9.x
  (Java 17 source/target compatibility)
- Persistence: MySQL 8, Spring Data JPA, Hibernate, Flyway migrations (`modules/coupon-domain/src/main/resources/db/migration`)
- Caching: Redis 7 (`modules/coupon-infrastructure`)
- Background jobs: JobRunr 8.7.1 backed by Redis/SQL for coupon validation and retries
- Scrapers: Jsoup, Udemy public API client (`modules/course-external-api`)
- Authentication: Google & Apple ID token verification, WebAuthn / Passkeys (`webauthn-server-core`), JWT (`jjwt 0.11.5`)
- Notifications: Firebase Cloud Messaging (`firebase-admin 9.3.0`)
- Documentation: SpringDoc OpenAPI 2.8.8 (Swagger UI at `/swagger-ui.html`)
- Code quality & linting: Ktlint 1.0.1, Detekt 1.23.4

## Module map

9 modules total (`settings.gradle.kts`), structured by responsibility and deployment target:

| Module | Role |
|---|---|
| `modules:coupon-common` | Cross-cutting utilities, logging helpers, shared exception definitions |
| `modules:coupon-domain` | Pure data layer: JPA entities, repositories, DTOs, and Flyway migration scripts |
| `modules:coupon-infrastructure` | Redis configuration, caching service (`RedisService`), and fetch time tracking |
| `modules:notification-service` | Firebase Admin SDK setup and targeted push notifications (`NotificationService`) |
| `modules:course-scraper` | Background validation pipeline (`CourseScraperService`), Jsoup data extraction (`CourseDataExtractor`) |
| `modules:course-external-api` | Udemy REST API integration client (`ExternalCourseApiClient`) |
| `modules:identity-service` | Auth controllers: Social OAuth2, Passkeys (WebAuthn), JWT handling, user preferences |
| `modules:coupon-api-service` | Deployable application: Public REST API for querying, filtering, and searching coupons |
| `modules:coupon-crawler-service` | Deployable application: Scheduled discovery crawlers (`CrawlerRunner`, `EnextCrawler`, `RealDiscountCrawler`) |

## Architecture & layer conventions

- **Separation of Discovery & Processing**: `coupon-crawler-service` only discovers URLs; it does not perform deep scraping synchronously. It enqueues URLs into JobRunr via `CourseScraperService`.
- **Background Jobs**: Asynchronous scraping and validation tasks run through JobRunr workers with audit logs saved to `scraping_task_logs`.
- **Database Migrations**: All schema updates must be managed via Flyway migration scripts in `modules/coupon-domain/src/main/resources/db/migration/` (`V<timestamp>__<description>.sql`). Never alter JPA entities without matching Flyway migrations.
- **REST Endpoints & Validation**: Use Jakarta Validation annotations on DTOs. Document public endpoints using OpenAPI / Swagger annotations. Return appropriate HTTP status codes (202 Accepted for async enqueue, 400 for invalid inputs, 404 for missing entities).
- **Exceptions**: Use domain exceptions defined in `coupon-common` or domain layers and let `@RestControllerAdvice` format standard error responses.

## Build & verify commands

- Full build: `./gradlew build`
- Run all unit tests: `./gradlew clean test`
- Run module-specific tests: `./gradlew :modules:<module-name>:test` (e.g. `./gradlew :modules:coupon-domain:test`)
- Static analysis (lint & detekt):
  - Lint check: `./gradlew ktlintCheck`
  - Lint auto-format: `./gradlew ktlintFormat`
  - Detekt check: `./gradlew detekt`
  - Both combined: `./gradlew ktlintCheck detekt`
- Run local stack:
  - Infrastructure (MySQL & Redis): `docker compose -f docker-compose.local.yml up -d`
  - API Service: `./gradlew :modules:coupon-api-service:bootRun --args='--spring.profiles.active=local'`
  - Crawler Service: `./gradlew :modules:coupon-crawler-service:bootRun --args='--spring.profiles.active=local'`
  - Run both services simultaneously: `./gradlew bootRunLocal` (or `bash scripts/bootRunLocal.sh`)

## Git / GitHub workflow

One-line version: **issue → branch `<type>/<issue-number>-<slug>` off latest `main` → commits passing Conventional Commits format → PR from `.github/PULL_REQUEST_TEMPLATE.md` with `Closes #N` → CI green → squash-merge → delete branch.**

Full details in `/docs`:
- [`docs/project-management.md`](docs/project-management.md) — label taxonomy (`type`, `priority`, `area`, `size`), Definition of Done
- [`docs/branching.md`](docs/branching.md) — branch naming rules
- [`docs/commit-conventions.md`](docs/commit-conventions.md) — Conventional Commits format
- [`docs/pull-requests.md`](docs/pull-requests.md) — PR lifecycle, checklist, squash-merge process
- [`docs/adr-guide.md`](docs/adr-guide.md) — when and how to write Architecture Decision Records in `docs/adr/`
- [`docs/ci-cd.md`](docs/ci-cd.md) — CI/CD pipelines, Docker deployment

The `ticket-flow` skill runs the full lifecycle as a gated single-agent sequence — `new-ticket` (if needed) → `read-requirements` → `scan-related-code` → `confirm-edge-cases` → `implement-change` → `run-tests` → `capture-evidence` → `ship-pr` — stopping to report and confirm after every step, backed by a resumable per-ticket workspace under `.agents/tickets/` (git-ignored).

The `team-flow` skill runs the same lifecycle using a **5-agent specialized team** across `course_deals_backend` and `course_deals_ComposeMultiplatform` (**PM/Architect**, `backend-engineer`, `android-engineer`, `security-arch-reviewer`, and `qa-release-engineer`) with only **2 human-in-the-loop checkpoints** (1: Approve Spec, API Contract & Edge Cases; 2: Ready to Merge PRs).

The `sprint-flow` skill manages the **1-week solo developer sprint rhythm** (Monday–Saturday build & test, Sunday stop & plan) integrated with GitHub Projects:
- [`docs/SPRINT_GUIDE.md`](docs/SPRINT_GUIDE.md) — 1-week sprint lifecycle, iteration conventions, and solo-dev DoD
- `sprint-flow plan` — Sunday backlog grooming, point budgeting (15–20 pts), and iteration setup
- `sprint-flow retro` — Sunday velocity and completed points inspection

## Evidence capture conventions

For backend changes, automated and manual verification includes:
- Command line execution of `./gradlew ktlintCheck detekt test` with real pass/fail counts.
- `curl -i` requests and JSON response bodies demonstrating endpoint functionality.
- Docker compose container status and Spring Boot application logs verifying startup, database connection, or background job enqueueing.
- If UI or Swagger documentation needs verification, screenshots can be captured and uploaded to the hosted R2 worker via `capture-evidence`.
