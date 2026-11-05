package com.thanh0x.coursedeal.repository

import com.thanh0x.coursedeal.model.coupon.CouponCourseData
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDateTime

@Repository
@Suppress("TooManyFunctions")
interface CouponCourseRepository : JpaRepository<CouponCourseData, Int> {
    @Query(
        """
        SELECT c FROM CouponCourseData c WHERE 
        (c.expiredDate IS NULL OR c.expiredDate > :now) AND 
        c.usesRemaining > 0 AND 
        c.couponCode IS NOT NULL AND 
        c.couponCode <> ''
        """,
    )
    fun findAllActive(
        @Param("now") now: Instant,
        pageable: Pageable,
    ): Page<CouponCourseData>

    @Query(
        """
        SELECT c FROM CouponCourseData c WHERE 
        (c.expiredDate IS NULL OR c.expiredDate > :now) AND 
        c.usesRemaining > 0 AND 
        c.couponCode IS NOT NULL AND 
        c.couponCode <> '' AND 
        c.rating >= :minRating 
        ORDER BY c.students DESC
        """,
    )
    fun findHotDeals(
        @Param("minRating") minRating: Float,
        @Param("now") now: Instant,
        pageable: Pageable,
    ): Page<CouponCourseData>

    @Query(
        """
        SELECT c FROM CouponCourseData c WHERE 
        (c.expiredDate IS NULL OR c.expiredDate > :now) AND 
        c.usesRemaining > 0 AND 
        c.couponCode IS NOT NULL AND 
        c.couponCode <> '' AND 
        c.rating >= :minRating AND (
            LOWER(c.category) LIKE LOWER(CONCAT('%', :preference, '%')) OR 
            LOWER(c.title) LIKE LOWER(CONCAT('%', :preference, '%'))
        ) 
        ORDER BY c.students DESC
        """,
    )
    fun findHotDealsByPreference(
        @Param("preference") preference: String,
        @Param("minRating") minRating: Float,
        @Param("now") now: Instant,
        pageable: Pageable,
    ): Page<CouponCourseData>

    @Query(
        """
        SELECT c FROM CouponCourseData c WHERE 
        (c.expiredDate IS NULL OR c.expiredDate > :now) AND 
        c.usesRemaining > 0 AND 
        c.couponCode IS NOT NULL AND 
        c.couponCode <> '' AND (
            LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%')) OR 
            LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%')) OR 
            LOWER(c.heading) LIKE LOWER(CONCAT('%', :query, '%'))
        )
        """,
    )
    fun findByQueryAndActive(
        @Param("query") query: String,
        @Param("now") now: Instant,
        pageable: Pageable,
    ): Page<CouponCourseData>

    @Query(
        """
        SELECT c FROM CouponCourseData c WHERE 
        (c.expiredDate IS NULL OR c.expiredDate > :now) AND 
        c.usesRemaining > 0 AND 
        c.couponCode IS NOT NULL AND 
        c.couponCode <> '' AND 
        c.rating > :rating AND 
        c.contentLength > :contentLength AND 
        (:level = '' OR CAST(c.level AS string) LIKE CONCAT('%', :level, '%')) AND 
        (:category = '' OR LOWER(c.category) LIKE LOWER(CONCAT('%', :category, '%'))) AND 
        (:language = '' OR LOWER(c.language) LIKE LOWER(CONCAT('%', :language, '%')))
        """,
    )
    @Suppress("LongParameterList")
    fun findWithStructuredFilters(
        @Param("rating") rating: Float,
        @Param("contentLength") contentLength: Int,
        @Param("level") level: String,
        @Param("category") category: String,
        @Param("language") language: String,
        @Param("now") now: Instant,
        pageable: Pageable,
    ): Page<CouponCourseData>

    @Query(
        """
        SELECT c FROM CouponCourseData c
        WHERE c.expiredDate <= :threshold
           OR c.usesRemaining <= 0
           OR c.couponCode IS NULL
           OR c.couponCode = ''
        """,
    )
    fun findByExpiredDateLessThanEqual(
        @Param("threshold") expirationThreshold: Instant,
    ): List<CouponCourseData>

    fun findByCourseId(courseId: Int): CouponCourseData?

    @Modifying
    @Transactional
    @Query("DELETE FROM CouponCourseData ccd WHERE ccd.couponUrl IN :expiredCouponUrls")
    fun deleteAllCouponsByUrl(
        @Param("expiredCouponUrls") expiredCouponUrls: Set<String>,
    )

    @Modifying
    @Transactional
    fun deleteByCouponUrl(couponUrl: String)

    @Query("SELECT c.couponUrl FROM CouponCourseData c WHERE c.couponUrl IS NOT NULL")
    fun findAllCouponUrls(): Set<String>

    fun findByCouponUrl(couponUrl: String): CouponCourseData?

    @Query("SELECT c.courseId FROM CouponCourseData c WHERE c.couponUrl = :couponUrl")
    fun findCourseIdByCouponUrl(
        @Param("couponUrl") couponUrl: String,
    ): Int?

    @Query(
        """
        SELECT DISTINCT c.couponUrl FROM CouponCourseData c WHERE 
        c.couponUrl IS NOT NULL AND (
        (c.expiredDate <= :expirationThreshold) OR 
        (c.usesRemaining < :minUsesRemaining) OR 
        (c.updatedAt < :updatedBefore)
        )
        """,
    )
    fun findCouponUrlsNeedingRefresh(
        @Param("expirationThreshold") expirationThreshold: Instant,
        @Param("minUsesRemaining") minUsesRemaining: Int,
        @Param("updatedBefore") updatedBefore: LocalDateTime,
    ): Set<String>
}
