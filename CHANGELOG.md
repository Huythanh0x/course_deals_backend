# Changelog

All notable changes to this project are documented here, following
[Keep a Changelog](https://keepachangelog.com) format. See
[`docs/versioning.md`](docs/versioning.md) for the versioning scheme.

## [Unreleased]
### Added
- `GET /api/v1/coupons/hot-deals` endpoint with user preference matching and global top-student fallback for mobile & Apple Watch companion (#58).
- Sentry error reporting integration (`sentry-spring-boot-starter-jakarta`, `ErrorTrackingReporter`) across API and crawler services (#157).
- Deterministic SpringDoc OpenAPI 3.0 spec export task (`./gradlew exportOpenApiSpec` → `openapi/openapi.json`) with stabilized `operationId`s for KMP client codegen (#49).
- Automated Backend Release CD workflow (`.github/workflows/release-cd.yml` & `scripts/release-build.sh`) publishing executable JARs, multi-arch Docker images, and SHA256 checksums (#211).
- Public remote configuration endpoint (`GET /api/v1/app/config`) for force-update gating and feature flags (#52).
- FCM device token registration and topic subscription (`POST /api/v1/users/device-token`) (#50).
- Passwordless Email OTP authentication (`/api/v1/auth/send-otp`, `/api/v1/auth/verify-otp`), Terms acceptance (`/api/v1/users/me/accept-terms`), account deletion (`DELETE /api/v1/users/me`), and community course reports (`POST /api/courses/{courseId}/reports`).
- Isolated `mobile-local` Spring profile (`scripts/bootRunMobileLocal.sh`) running API on port `8081` and Crawler on port `8082` against `training_coupon_mobile_local`.

### Fixed
- Smart refresh in `CrawlerRunner` / `CourseScraperService` to re-validate active coupons and automatically purge expired deals (#55, #56).
- JobRunr SQL storage configuration prefix (`jobrunr.*`) — ADR 0001.
