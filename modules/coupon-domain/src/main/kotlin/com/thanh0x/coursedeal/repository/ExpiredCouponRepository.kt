package com.thanh0x.coursedeal.repository

import com.thanh0x.coursedeal.model.coupon.ExpiredCourseData
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ExpiredCouponRepository : JpaRepository<ExpiredCourseData, String> {
    @Query("SELECT e.couponUrl FROM ExpiredCourseData e WHERE e.couponUrl IS NOT NULL")
    fun findAllCouponUrls(): Set<String>

    @Query("SELECT e.couponUrl FROM ExpiredCourseData e WHERE e.updatedAt >= :updatedAfter AND e.couponUrl IS NOT NULL")
    fun findRecentlyCheckedExpiredUrls(
        @Param("updatedAfter") updatedAfter: LocalDateTime,
    ): Set<String>

    fun findByCouponUrl(couponUrl: String): ExpiredCourseData?

    @Modifying
    @Transactional
    @Query("UPDATE ExpiredCourseData e SET e.updatedAt = CURRENT_TIMESTAMP WHERE e.couponUrl IN :couponUrls")
    fun updateUpdatedAtForUrls(
        @Param("couponUrls") couponUrls: Set<String>,
    )

    @Query("SELECT e.courseId FROM ExpiredCourseData e WHERE e.couponUrl = :couponUrl")
    fun findCourseIdByCouponUrl(
        @Param("couponUrl") couponUrl: String,
    ): Int?
}
