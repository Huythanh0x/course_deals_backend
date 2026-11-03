---
name: spring-kotlin-code-review
description: Spring Boot + Kotlin API contract, validation, security, and persistence review rules for `course_deals_backend` (`modules:coupon-api-service`, `modules:identity-service`, `modules:coupon-domain`, `modules:course-scraper`).
source: https://github.com/yalishevant/kotlin-backend-agent-skills/blob/main/.agents/skills/spring-kotlin-code-review/SKILL.md (Aligned to Course Deals Backend)
---

# Course Deals Spring + Kotlin API & Code Review Guide

Aligned to `course_deals_backend` (`com.thanh0x.coursedeal`, 9 Gradle modules, Spring Boot 3.5.7 MVC, Jakarta Validation, SpringDoc OpenAPI `openapi/openapi.json`, Ktlint 1.0.1, Detekt 1.23.4).

## 1. Kotlin DTO Validation & Jackson Serialization Rules

- **Always use `@field:` use-site targets** for Jakarta Bean Validation annotations on Kotlin constructor properties:
  ```kotlin
  data class SubmitReportRequest(
      @field:NotBlank(message = "Reason must not be blank")
      @field:Size(max = 500)
      val reason: String
  )
  ```
  *(Without `@field:`, validation annotations attach to constructor parameters and Spring `@Valid` silently ignores them in many paths!)*
- Keep transport DTOs (`data class` in controller/API layers) separate from JPA `@Entity` classes.
- Ensure nullable vs. non-null fields in backend DTOs match `openapi/openapi.json` and the KMP client (`course_deals_ComposeMultiplatform` `./gradlew generateCourseDealsApi`).

## 2. REST Controller & Security Hygiene (`modules:coupon-api-service` & `modules:identity-service`)

- Annotate public controllers with SpringDoc OpenAPI annotations (`@Tag`, `@Operation`, `@SecurityRequirement(name = "bearerAuth")`).
- Verify `SecurityConfig` route matchers explicitly classify public vs. authenticated endpoints (`401 Unauthorized` / `403 Forbidden`).
- Return standard error envelopes via `@RestControllerAdvice` across all 4 quadrants:
  - **Happy Path:** `200 OK` / `201 Created` / `202 Accepted` (for async JobRunr enqueue).
  - **Failed Path (Domain/Validation):** `400 Bad Request` / `404 Not Found` / `409 Conflict` / `429 Too Many Requests`.
  - **Error Path (Infrastructure):** `500 Internal Server Error` with safe message (never raw stack traces).

## 3. Mandatory Verification Gate

Before committing any change in `course_deals_backend`, run:
```bash
export JAVA_HOME="$(${JAVA_HOME:-/usr/libexec/java_home -v 17 2>/dev/null || echo "$HOME/.gradle/jdks/eclipse_adoptium-17-aarch64-os_x/jdk-17.0.20.1+1/Contents/Home"})"
./gradlew ktlintFormat
./gradlew ktlintCheck detekt test
```

## Checklist
- [ ] All Jakarta Validation annotations on Kotlin DTO properties use `@field:` (`@field:NotBlank`, `@field:Valid`, etc.)
- [ ] `openapi/openapi.json` updated when endpoints or DTO schemas change
- [ ] Flyway migration matches `@Entity` changes and is safe for rolling deployment
- [ ] `@Transactional` is on proxied public `@Service` methods (no self-invocation trap)
- [ ] `./gradlew ktlintCheck detekt test` passes 100%
