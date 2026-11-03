---
name: kotlin-backend-jpa-entity-mapping
description: Model Kotlin persistence, Flyway migrations, and transaction boundaries for `course_deals_backend` (`modules:coupon-domain`, MySQL 8, Spring Data JPA, Hibernate, Redis 7, JobRunr). Covers `class` vs `data class` entities, ID-based equality, uniqueness constraints, N+1 fetch plans, and transactional boundaries.
source: https://github.com/Kotlin/kotlin-agent-skills/blob/main/skills/kotlin-backend-jpa-entity-mapping/SKILL.md (Aligned to Course Deals Backend)
---

# Course Deals Backend: JPA Entity Mapping, Flyway & Transactions

Aligned to `course_deals_backend` (Kotlin 1.9.21, Spring Boot 3.5.7, MySQL 8, Flyway in `modules/coupon-domain/src/main/resources/db/migration/`, Redis 7 in `modules:coupon-infrastructure`, JobRunr 8.7.1).

## 1. Entity Design & Identity Rules (`modules:coupon-domain`)

- **Never use `data class` for JPA `@Entity` classes:** Keep `data class` strictly for transport DTOs (`*Request`, `*Response`). Hibernate proxies and dirty tracking break when `equals`/`hashCode` are computed over mutable fields, and `toString()` on a `data class` triggers `LazyInitializationException` on lazy collections.
- **Regular `class` with ID-Based Identity:**
  ```kotlin
  @Entity
  @Table(name = "course_reports")
  class CourseReportEntity(
      @Column(name = "coupon_id", nullable = false)
      val couponId: Long,

      @Column(name = "reason", nullable = false, length = 64)
      var reason: String
  ) {
      @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      var id: Long? = null
          protected set

      override fun equals(other: Any?): Boolean {
          if (this === other) return true
          if (other !is CourseReportEntity) return false
          return id != null && id == other.id
      }

      override fun hashCode(): Int = javaClass.hashCode()

      override fun toString(): String = "CourseReportEntity(id=$id, couponId=$couponId, reason=$reason)"
  }
  ```

## 2. Mandatory Flyway Migration Parity

- Every `@Entity` or `@Column` addition/change in `modules:coupon-domain` **MUST** have a matching Flyway SQL script at:
  `modules/coupon-domain/src/main/resources/db/migration/V<timestamp_or_N>__<description>.sql`
- Include explicit MySQL 8 indexes (`INDEX idx_...`) and `UNIQUE` constraints supporting the repository queries.

## 3. Idempotency & Duplicate Prevention (Two-Layer Guard)

- Enforce uniqueness at **both** layers:
  1. **Database constraint:** `UNIQUE KEY uk_...` in Flyway + `@Table(uniqueConstraints = [...])` to prevent race-condition duplicates.
  2. **Application guard:** Check `repository.existsBy...()` / `findBy...()` in the `@Service` method to throw a clean domain exception (mapped to `400`/`409` by `@RestControllerAdvice`) instead of leaking raw `DataIntegrityViolationException`.

## 4. Transaction Boundaries, Redis & JobRunr Side Effects

- Keep `@Transactional` focused on the database consistency boundary in `@Service` classes (never on `@RestController`).
- **Never perform slow external HTTP scraping (`CourseScraperService` / `ExternalCourseApiClient` / Jsoup) inside an open `@Transactional` DB connection.**
- In `modules:coupon-crawler-service`, discovery crawlers (`EnextCrawler`, `RealDiscountCrawler`) only enqueue URLs into JobRunr; asynchronous JobRunr workers perform validation and persist results.
- Invalidate or update `RedisService` cache keys (`modules:coupon-infrastructure`) after DB mutations commit.

## Checklist
- [ ] JPA `@Entity` is a regular `class` (not `data class`) with ID-based `equals` and `javaClass.hashCode()`
- [ ] Matching Flyway migration exists in `modules/coupon-domain/src/main/resources/db/migration/`
- [ ] Both DB `UNIQUE` constraint and service-level check exist for idempotent writes
- [ ] External HTTP/Jsoup calls and JobRunr enqueues do not hold open DB transactions
