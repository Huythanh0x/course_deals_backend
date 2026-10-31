# Ticket #162: Live deal watchers counter via lightweight Server-Sent Events (SSE)

## Overview
Implemented real-time active viewers counter for course deals via lightweight HTTP Server-Sent Events (SSE) backed by Redis with an in-memory fallback.

## Implementation Details
1. **Domain Layer (`coupon-domain`)**:
   - `LiveWatchersEventDTO`: Payload emitted over SSE containing `activeViewers: Long`.
   - `LiveWatchersTracker`: Service interface for recording heartbeat, removing watcher, and querying active count.
   - `InMemoryLiveWatchersTracker`: ConcurrentHashMap-backed tracker with heartbeat timestamp eviction for standalone or test profiles.

2. **Infrastructure Layer (`coupon-infrastructure`)**:
   - `RedisLiveWatchersTracker`: Redis Sorted Set (`deal:{id}:viewers`) tracker with score-based eviction and 30s TTL per heartbeat.
   - `RedisConfig`: Configured `@Primary LiveWatchersTracker` bean when Redis is enabled.

3. **API Service Layer (`coupon-api-service`)**:
   - `LiveWatchersService`: Manages `SseEmitter` subscriptions, cleans up on completion/timeout/error, and broadcasts active viewer counts across deals every 10 seconds.
   - `LiveWatchersConfig`: Fallback `@ConditionalOnMissingBean` for `InMemoryLiveWatchersTracker`.
   - `CouponCourseController`: Added `GET /api/v1/coupons/{courseId}/live-watchers` (`text/event-stream`).

## Verification & Testing
- Unit tests:
  - `InMemoryLiveWatchersTrackerTest`: Verifies heartbeats, eviction, and removals.
  - `RedisLiveWatchersTrackerTest`: Verifies zset operations, eviction score threshold, and card query.
  - `LiveWatchersServiceTest`: Verifies lifecycle, 404 on missing/expired deals, and stream event emissions.
- Controller WebMvc test:
  - `CouponCourseControllerLiveWatchersTest`: Verifies async SSE start and 404 handling.
- Full build and lint verification:
  - `./gradlew test`: All module test suites pass.
  - `./gradlew ktlintCheck detekt`: Clean with 0 violations.
