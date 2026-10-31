package com.thanh0x.coursedeal.repository.specification

import com.thanh0x.coursedeal.dto.CouponSearchCriteriaDTO
import com.thanh0x.coursedeal.model.coupon.CouponCourseData
import com.thanh0x.coursedeal.model.coupon.CourseLevel
import jakarta.persistence.criteria.Predicate
import org.springframework.data.jpa.domain.Specification
import java.time.Instant

object CouponCourseSpecifications {
    fun withCriteria(
        criteria: CouponSearchCriteriaDTO,
        now: Instant,
    ): Specification<CouponCourseData> {
        return Specification { root, _, cb ->
            val predicates = mutableListOf<Predicate>()

            // 1. Only active (unexpired) coupons
            val expiredDateNull = cb.isNull(root.get<Instant>("expiredDate"))
            val notExpired = cb.greaterThan(root.get("expiredDate"), now)
            predicates.add(cb.or(expiredDateNull, notExpired))

            // 2. Full-text search q (matches title, heading, or description)
            criteria.q?.trim()?.takeIf { it.isNotEmpty() }?.let { q ->
                val pattern = "%${q.lowercase()}%"
                val titleMatch = cb.like(cb.lower(root.get("title")), pattern)
                val headingMatch = cb.like(cb.lower(root.get("heading")), pattern)
                val descMatch = cb.like(cb.lower(root.get("description")), pattern)
                predicates.add(cb.or(titleMatch, headingMatch, descMatch))
            }

            // 3. Category filter
            criteria.category?.trim()?.takeIf { it.isNotEmpty() }?.let { cat ->
                predicates.add(cb.like(cb.lower(root.get("category")), "%${cat.lowercase()}%"))
            }

            // 4. Instructor / Author filter
            criteria.instructor?.trim()?.takeIf { it.isNotEmpty() }?.let { inst ->
                predicates.add(cb.like(cb.lower(root.get("author")), "%${inst.lowercase()}%"))
            }

            // 5. Min Rating filter
            criteria.minRating?.let { minRating ->
                if (minRating > 0f) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), minRating))
                }
            }

            // 6. Language filter
            criteria.language?.trim()?.takeIf { it.isNotEmpty() }?.let { lang ->
                predicates.add(cb.like(cb.lower(root.get("language")), "%${lang.lowercase()}%"))
            }

            // 7. Course Level filter
            criteria.level?.trim()?.takeIf { it.isNotEmpty() }?.let { lvl ->
                runCatching { CourseLevel.valueOf(lvl.uppercase()) }.getOrNull()?.let { courseLevel ->
                    predicates.add(cb.equal(root.get<CourseLevel>("level"), courseLevel))
                }
            }

            @Suppress("SpreadOperator")
            cb.and(*predicates.toTypedArray())
        }
    }
}
