# Business Logic & Flow - Course Deal Server

## Module Map
- **`coupon-common`**: Cross-cutting utilities (logging helper, shared exceptions) used by nearly every other module. No framework-specific infra deps of its own beyond what those utilities need.
- **`coupon-domain`**: Pure data layer. Contains DTOs, JPA entities, repositories, and Flyway migrations. No infrastructure or logic dependencies.
- **`coupon-infrastructure`**: Redis caching/config only (`RedisConfig`, `RedisService`, `LastFetchTimeManager`).
- **`notification-service`**: Firebase Admin SDK (FCM) push notifications (`FirebaseConfig`, `NotificationService`).
- **`course-scraper`**: Specialized domain-specific I/O shared by both deployables. Contains the `CourseScraperService` (JobRunr-backed validation pipeline) and `CourseDataExtractor`.
- **`course-external-api`**: Client for Udemy's public course API (`ExternalCourseApiClient`), used only by `coupon-api-service`.
- **`identity-service`**: Dedicated module for Identity and Access Management (IAM). Handles Social Auth, Passkey handshakes, and User Preferences.
- **`coupon-api-service`**: Business REST stack. Focused purely on course searching, filtering, and detail retrieval.
- **`coupon-crawler-service`**: Background discovery workers that periodically find new course deals and hand them off to `course-scraper` via JobRunr.

## High-Level Architecture
- **Discovery Layer** (`coupon-crawler-service`): Periodically pulls URLs from multiple sources. It does not validate URLs itself; it enqueues them into **JobRunr**.
- **Background Processing** (`CourseScraperService`): A persistent background pipeline powered by **JobRunr** that validates coupons. It handles Udemy API interaction and persists results with full audit logging and automatic retries.
- **Modern Auth** (`SocialAuthController`, `PasskeyAuthController`): Secure, password-less authentication supporting Google, Apple, and Biometric Passkeys.
- **Personalized Notifications** (`NotificationService`): Matches newly discovered coupons against user-defined keywords/categories and sends targeted FCM push notifications.

## Coupon Discovery & Validation Pipeline
1. **Discovery:** `CrawlerRunner` fires every 15 minutes.
2. **Collection:** `EnextCrawler` and `RealDiscountCrawler` collect raw URLs.
3. **Queue Handoff:** Discovered URLs are passed to `CourseScraperService.enqueueScrapingTask()`.
4. **Validation (Background Worker):**
   - JobRunr picks up the task from MySQL storage (`jobrunr_jobs` table; see [docs/adr/0001-jobrunr-sql-storage-over-redis.md](adr/0001-jobrunr-sql-storage-over-redis.md)).
   - `CourseDataExtractor` uses Jsoup to pull course metadata.
   - The result is audited in the `scraping_task_logs` table.
   - If a provider is down, JobRunr automatically retries with exponential backoff.
5. **Persistence & Alerts:**
   - Valid coupons are saved to `CouponCourseRepository`.
   - `NotificationService` checks user preferences and sends targeted FCM pushes.

## Coupon API Behavior
- **`GET /api/v1/coupons`**: Returns a paged list of coupons. Supports remote filtering by category, rating, level, etc.
- **Local Filtering/Sorting**: The API response (`CouponSummaryDTO`) includes rich metadata to allow mobile clients to filter and sort locally without additional network calls. These fields include:
    - `language`, `rating`, `students`, `reviews`, `expiredTime`, `newest` (createdAt).

## Authentication System
- **Social Login:** Validates Google/Apple ID tokens and issues a server-side JWT.
- **Passkeys (WebAuthn):** Two-step biometric handshake (Registration & Authentication) using FIDO2 standards.
- **Security:** `TokenAuthenticationFilter` verifies JWTs and manages the security context for personalized requests (like Preference updates).

## Sequence Diagram (JobRunr Flow)
```mermaid
sequenceDiagram
    participant Crawler as CrawlerRunner
    participant API as CouponCourseController
    participant JobDB as MySQL (jobrunr_jobs)
    participant Scraper as JobRunr Worker Pool
    participant Extractor as CourseDataExtractor
    participant DomainDB as MySQL (Domain Tables)
    participant FCM as NotificationService

    Note over Crawler, API: Enqueue Scraping Task
    Crawler->>JobDB: enqueue(url)
    API-->>Client: 202 Accepted (Immediate)
    API->>JobDB: enqueue(url)
    
    Scraper->>JobDB: Poll Job
    Scraper->>DomainDB: Log Task (PENDING in scraping_task_logs)
    Scraper->>Extractor: Extract Metadata (Slow IO)
    Extractor-->>Scraper: CouponCourseData
    Scraper->>DomainDB: Save Coupon & Log (SUCCESS)
    
    Scraper->>FCM: notifyInterestedUsers(coupon)
    FCM->>DomainDB: Query User Preferences
    FCM-->>User: Push Notification
```

## Implementation Touchpoints
- `com.thanh0x.coursedeal.crawlerrunner.CrawlerRunner` (in `coupon-crawler-service`) – URL discovery loop.
- `com.thanh0x.coursedeal.service.CourseScraperService` (in `course-scraper`) – Core background scraping logic.
- `com.thanh0x.coursedeal.controller.CouponCourseController` (in `coupon-api-service`) – REST API for coupons.
- `com.thanh0x.coursedeal.controller.SocialAuthController` (in `identity-service`) – OAuth2 token exchange.
- `com.thanh0x.coursedeal.controller.PasskeyAuthController` (in `identity-service`) – WebAuthn biometric flow.

For a comprehensive breakdown of the 9 modules, key classes, and full system topology, see [docs/architecture.md](architecture.md).
