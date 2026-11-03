# Spring Boot: Course Deals Backend (Kotlin)

![Kotlin](https://img.shields.io/badge/Kotlin-1.9.21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen)
![Java](https://img.shields.io/badge/Java-17-orange)
![License](https://img.shields.io/badge/License-MIT-yellow)

The **Course Deals Backend** (`course_deals_backend`) is a multi-module Spring Boot 3.5.7 application written in Kotlin that discovers, scrapes, and validates 100% off online course coupons (Udemy, etc.). It powers the REST API, OpenAPI 3.0 contract generation, passwordless & social authentication, targeted FCM push notifications, and background JobRunr workers for the Course Deals multiplatform ecosystem (Android, iOS, watchOS, and Desktop).

## Features

- **Automated Deal Discovery & Smart Refresh**: Discovers course deals from multiple aggregator sources (`EnextCrawler`, `RealDiscountCrawler`) and periodically re-validates active coupons to purge expired deals automatically.
- **Asynchronous Scraping Pipeline**: Non-blocking background validation engine powered by **JobRunr** backed by MySQL with audit logging (`scraping_task_logs`) and automatic retries.
- **Modern Authentication**: Supports **Passwordless Email OTP** (`/api/v1/auth/send-otp`, `/api/v1/auth/verify-otp`), **Google & Apple OAuth2** (`/api/v1/auth/social/login`), **WebAuthn / Passkeys** (`/api/v1/auth/passkey/*`), and Redis-backed active session management with silent JWT refresh.
- **Hot Deals & Personalized Discovery**: Curated `/api/v1/coupons/hot-deals` endpoint with user preference fallback for mobile and Apple Watch companion widgets/complications.
- **Remote App Configuration & Community Reports**: Remote config & minimum version gate (`/api/v1/app/config`) and community deal reporting (`/api/courses/{courseId}/reports`).
- **Targeted Push Notifications**: Personalized **Firebase Cloud Messaging (FCM)** device token registration (`/api/v1/users/device-token`) and topic notifications based on user-defined categories and keywords.
- **Deterministic OpenAPI 3.0 Spec Export**: Stabilized SpringDoc OpenAPI contract exported via `./gradlew exportOpenApiSpec` to `openapi/openapi.json` for KMP Ktor client code generation.
- **Observability & Security**: **Sentry** error tracking (`ErrorTrackingReporter`) across API and crawler services, plus strict **SSRF hardening** (`UrlValidator`).

## Architecture

9 Gradle modules (`settings.gradle.kts`) structured by responsibility; the two deployables (`coupon-api-service`, `coupon-crawler-service`) compose the rest:

- **`modules/coupon-common`**: Cross-cutting utilities, logging helpers, Sentry observability (`ErrorTrackingReporter`), and shared exceptions.
- **`modules/coupon-domain`**: Pure data layer containing JPA entities, Spring Data repositories, DTOs, and Flyway SQL migrations (`src/main/resources/db/migration`).
- **`modules/coupon-infrastructure`**: Redis 7 caching, session management, and fetch-time tracking (`RedisService`, `LastFetchTimeManager`).
- **`modules/notification-service`**: Firebase Admin SDK setup and targeted FCM push notifications (`NotificationService`).
- **`modules/course-scraper`**: Coupon scraping, smart refresh, and validation pipeline shared by both deployables (`CourseScraperService`, `CourseDataExtractor`).
- **`modules/course-external-api`**: Client for Udemy's public course API (`ExternalCourseApiClient`).
- **`modules/identity-service`**: IAM controllers & services: Email OTP, Social OAuth2, Passkeys (WebAuthn), JWT rotation, device tokens, terms acceptance, and user preferences.
- **`modules/coupon-api-service`**: Deployable REST API application (Port `8080` on `local`, Port `8081` on `mobile-local`).
- **`modules/coupon-crawler-service`**: Deployable background discovery and JobRunr worker application (Port `8081` on `local`, Port `8082` on `mobile-local`).

## Quick Start

Requires **Java 17** and **Docker**.

### Option 1: Local Development (from source)
```shell
git clone https://github.com/Huythanh0x/course_deals_backend.git
cd course_deals_backend

# 1. Start local MySQL 8 and Redis 7
docker compose -f docker-compose.local.yml up -d

# 2. Run both API (port 8080) and Crawler (port 8081) with the local profile
./gradlew bootRunLocal
# Or run the isolated Mobile-Local test stack (API on 8081, Crawler on 8082, DB training_coupon_mobile_local):
bash scripts/bootRunMobileLocal.sh
```

### Option 2: Production Stack via Docker Compose
```shell
docker compose -f docker-compose.prod.yml up -d
```
Pulls the pre-built API and crawler Docker images (`huythanh0x/training-coupon-services`) and starts MySQL and Redis alongside them.

## Documentation

- **[AGENTS.md](AGENTS.md)** — Agent instructions, module map, build/verify commands, and multi-agent workflow (`team-flow`, `sprint-flow`).
- **[docs/getting-started.md](docs/getting-started.md)** — Running services from source, `local` vs `mobile-local` profiles, Flyway migrations, auth/mock-auth setup, OpenAPI export, and troubleshooting.
- **[docs/business-logic.md](docs/business-logic.md)** — How the discovery, JobRunr scraping, smart refresh, authentication, and notification pipelines flow.
- **[docs/ci-cd.md](docs/ci-cd.md)** — GitHub Actions CI (`ci-cd.yml`), Release CD (`release-cd.yml`), `scripts/release-build.sh`, and `scripts/autodeploy.sh`.
- **[docs/SPRINT_GUIDE.md](docs/SPRINT_GUIDE.md)** — 1-week solo developer sprint cadence and Backend Definition of Done.
- **[docs/project-management.md](docs/project-management.md)** — Label taxonomy, branching, Conventional Commits, PRs, versioning, and ADRs (`docs/adr/`).
- **[CONTRIBUTING.md](CONTRIBUTING.md)** — Code style (`ktlintCheck`, `detekt`), testing, and PR guidelines.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE.md) file for details.

## Contact

For any inquiries or issues, please open an issue on GitHub or contact <a href="mailto:huythanh0x@gmail.com">huythanh0x@gmail.com</a>.
