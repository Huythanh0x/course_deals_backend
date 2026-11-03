---
name: kmp-ktor-networking
description: Ktor 3 client rules for Course Deals `:core:network` across Android/Desktop (`ktor-client-okhttp`) and iOS (`ktor-client-darwin`), plus OpenAPI codegen (`generateCourseDealsApi`). Covers `requestTimeoutMillis` + `socketTimeoutMillis`, URL-safe user error mapping, `CancellationException`-safe `runSuspendCatching`, non-2xx status handling, and host-scoped Bearer Auth refresh.
source: https://github.com/filippkowalski/kmp-skills-by-filip/blob/main/skills/kmp-ktor-networking/SKILL.md (Aligned to Course Deals)
---

# Course Deals Ktor Networking (`:core:network` & OpenAPI Codegen)

Aligned to `course_deals_ComposeMultiplatform` (`:core:network`, Ktor 3 `ktor-client-okhttp` on JVM/Android/Desktop, `ktor-client-darwin` on iOS, `kotlinx.serialization`, and `./gradlew generateCourseDealsApi`).

## 1. OpenAPI Single Source of Truth (`generateCourseDealsApi`)

- `core/network/openapi/openapi.json` is synced from `../course_deals_backend/openapi/openapi.json`.
- Run `./gradlew generateCourseDealsApi` whenever backend REST paths or DTOs change to regenerate multiplatform Ktor APIs & `@Serializable` models under `core/network/src/commonMain/gen/`.
- Never hand-edit generated files under `src/commonMain/gen/`.

## 2. Set Both `requestTimeoutMillis` AND `socketTimeoutMillis` on OkHttp

- **Symptom:** A slow endpoint (>10s) fails on Android/Desktop with `SocketTimeoutException` even when `requestTimeoutMillis = 30_000` is set, while iOS (`Darwin` engine, 60s default) succeeds.
- **Cause:** Ktor's `OkHttp` engine maps OkHttp's read/write timeout only from `socketTimeoutMillis` and otherwise keeps OkHttp's 10s idle read default.
- **Fix:** Always configure `connectTimeoutMillis`, `requestTimeoutMillis`, and `socketTimeoutMillis` together in `:core:network`'s `HttpTimeout` plugin.

## 3. Never Expose Raw Ktor `error.message` in Release UI

- **Symptom:** Error banners or dialogs leak internal hostnames, `/api/v1/...` paths, and query parameters (`Socket timeout has expired [url=http://...]`).
- **Fix:** Map network and HTTP exceptions in `:data:*` repositories to domain error models or safe user-facing messages before updating `UiState.errorMessage`.

## 4. Rethrow `CancellationException` in Suspend Catch Blocks

- **Symptom:** Navigating away from a screen or cancelling a search query logs a bogus failure or emits an error state from `runCatching`.
- **Cause:** Standard `runCatching { ... }` catches `CancellationException`, breaking structured concurrency.
- **Fix:** Always rethrow `CancellationException` first:
  ```kotlin
  suspend inline fun <T> runSuspendCatching(block: () -> T): Result<T> = try {
      Result.success(block())
  } catch (e: kotlin.coroutines.cancellation.CancellationException) {
      throw e
  } catch (e: Throwable) {
      Result.failure(e)
  }
  ```

## 5. Host-Scoped Bearer Auth & Token Refresh

- Scope `Authorization: Bearer <token>` and `X-App-Version` headers in `:core:network` to the Course Deals API host so external URLs (e.g. Udemy course images loaded by Coil `coil-network-ktor3`) never receive backend auth headers.
- Keep all `io.ktor` artifacts (`ktor-client-core`, `ktor-client-okhttp`, `ktor-client-darwin`, `ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json`, `ktor-client-mock`) pinned to the single `ktor` version in `gradle/libs.versions.toml`.

## Checklist
- [ ] `./gradlew generateCourseDealsApi` run after any backend OpenAPI schema update
- [ ] `HttpTimeout` sets both `requestTimeoutMillis` and `socketTimeoutMillis`
- [ ] Raw `Throwable.message` from Ktor never shown directly in production UI
- [ ] `CancellationException` rethrown in all `try`/`catch` and `runCatching` wrappers around `suspend` calls
- [ ] All `io.ktor` dependencies share one version in `gradle/libs.versions.toml`
