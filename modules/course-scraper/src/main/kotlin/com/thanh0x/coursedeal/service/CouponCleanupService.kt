package com.thanh0x.coursedeal.service

import com.thanh0x.coursedeal.config.logger
import com.thanh0x.coursedeal.model.coupon.CouponCourseData
import com.thanh0x.coursedeal.model.coupon.ExpiredCourseData
import com.thanh0x.coursedeal.repository.CouponCourseRepository
import com.thanh0x.coursedeal.repository.ExpiredCouponRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Service responsible for identifying, archiving, and purging expired coupons.
 */
@Service
class CouponCleanupService(
    private val couponCourseRepository: CouponCourseRepository,
    private val expiredCouponRepository: ExpiredCouponRepository,
) {
    private val log = logger()

    /**
     * Finds all coupons whose expiredDate is before or equal to [threshold], archives their
     * metadata into expired_course_data, and removes them from coupon_course_data.
     *
     * @param threshold the expiration cutoff
     * @return the number of purged coupons
     */
    @Transactional
    fun archiveAndPurgeExpiredCoupons(): Int = archiveAndPurgeExpiredCoupons(Instant.now())

    @Transactional
    fun archiveAndPurgeExpiredCoupons(threshold: Instant): Int {
        val expiredCoupons = couponCourseRepository.findByExpiredDateLessThanEqual(threshold)
        if (expiredCoupons.isEmpty()) {
            log.debug("No expired coupons found to purge (threshold: {})", threshold)
            return 0
        }

        log.info("Found {} expired coupons to archive and purge", expiredCoupons.size)
        archiveCoupons(expiredCoupons)

        val urlsToDelete = expiredCoupons.mapNotNull { it.couponUrl }.toSet()
        if (urlsToDelete.isNotEmpty()) {
            couponCourseRepository.deleteAllCouponsByUrl(urlsToDelete)
        }

        log.info("Successfully purged and archived {} expired coupons", expiredCoupons.size)
        return expiredCoupons.size
    }

    /**
     * Archives a single coupon when it is determined to be expired or invalid during scraping,
     * and deletes it from active coupon storage if present.
     */
    @Transactional
    fun archiveAndPurgeSingleCoupon(coupon: CouponCourseData) {
        val url = coupon.couponUrl
        if (!url.isNullOrBlank()) {
            archiveSingleUrl(url, coupon.courseId, coupon.title)
            couponCourseRepository.deleteByCouponUrl(url)
            log.info("Archived and removed invalid/expired coupon: {} (ID: {})", coupon.title, coupon.courseId)
        }
    }

    /**
     * Records a known-invalid/expired coupon URL directly into expired_course_data.
     */
    @Transactional
    fun recordExpiredUrl(
        url: String,
        courseId: Int? = null,
        title: String? = null,
    ) {
        archiveSingleUrl(url, courseId, title)
    }

    private fun archiveSingleUrl(
        url: String,
        courseId: Int?,
        title: String?,
    ) {
        val existing = expiredCouponRepository.findByCouponUrl(url)
        if (existing != null) {
            if (courseId != null) existing.courseId = courseId
            if (title != null) existing.title = title
            expiredCouponRepository.updateUpdatedAtForUrls(setOf(url))
        } else {
            expiredCouponRepository.save(ExpiredCourseData(couponUrl = url, courseId = courseId, title = title))
        }
    }

    private fun archiveCoupons(coupons: List<CouponCourseData>) {
        val existingUrls = expiredCouponRepository.findAllCouponUrls()
        val toSave = mutableListOf<ExpiredCourseData>()
        val toUpdateUrls = mutableSetOf<String>()

        for (c in coupons) {
            val url = c.couponUrl ?: continue
            if (existingUrls.contains(url)) {
                toUpdateUrls.add(url)
            } else {
                toSave.add(
                    ExpiredCourseData(
                        couponUrl = url,
                        courseId = c.courseId,
                        title = c.title,
                    ),
                )
            }
        }

        if (toSave.isNotEmpty()) {
            expiredCouponRepository.saveAll(toSave)
        }
        if (toUpdateUrls.isNotEmpty()) {
            expiredCouponRepository.updateUpdatedAtForUrls(toUpdateUrls)
        }
    }
}
