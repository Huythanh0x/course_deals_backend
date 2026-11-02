package com.thanh0x.coursedeal.service

import com.thanh0x.coursedeal.config.logger
import com.thanh0x.coursedeal.crawlerrunner.CourseDataExtractor
import com.thanh0x.coursedeal.model.audit.ScrapingTaskLog
import com.thanh0x.coursedeal.model.coupon.CouponCourseHistory
import com.thanh0x.coursedeal.observability.ErrorTrackingReporter
import com.thanh0x.coursedeal.observability.SentryErrorTrackingReporter
import com.thanh0x.coursedeal.repository.CouponCourseHistoryRepository
import com.thanh0x.coursedeal.repository.CouponCourseRepository
import com.thanh0x.coursedeal.repository.ExpiredCouponRepository
import com.thanh0x.coursedeal.repository.audit.ScrapingTaskLogRepository
import org.jobrunr.scheduling.JobScheduler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for handling background scraping of course coupons using JobRunr.
 */
@Service
@Suppress("LongParameterList")
class CourseScraperService(
    private val couponCourseRepository: CouponCourseRepository,
    private val expiredCouponRepository: ExpiredCouponRepository,
    private val couponCourseHistoryRepository: CouponCourseHistoryRepository,
    private val notificationService: NotificationService,
    private val scrapingTaskLogRepository: ScrapingTaskLogRepository,
    private val jobScheduler: JobScheduler,
    private val couponCleanupService: CouponCleanupService,
    @Autowired(required = false)
    errorTrackingReporter: ErrorTrackingReporter? = null,
) {
    private val log = logger()
    private val reporter: ErrorTrackingReporter = errorTrackingReporter ?: SentryErrorTrackingReporter()

    companion object {
        private val KNOWN_WORKER_SOURCES = setOf("crawler", "crawler-refresh", "report-worker", "unknown")

        internal fun sanitizeSourceTag(remoteAddr: String): String {
            val trimmed = remoteAddr.trim()
            return when {
                trimmed.isEmpty() -> "unknown"
                trimmed in KNOWN_WORKER_SOURCES -> trimmed
                else -> "api-submission"
            }
        }
    }

    /**
     * Enqueues a scraping task into JobRunr.
     */
    fun enqueueScrapingTask(
        couponUrl: String,
        remoteAddr: String,
    ) {
        jobScheduler.enqueue { validateAndSaveCoupon(couponUrl, remoteAddr) }
    }

    /**
     * Core validation logic. This is executed by the JobRunr worker.
     *
     * Must not let any exception escape: an uncaught exception here would fail the JobRunr worker
     * itself rather than just this one task, so any failure - network, parsing, or DB - is caught,
     * logged, and recorded in the audit log instead.
     */
    @Suppress("TooGenericExceptionCaught")
    @Transactional
    fun validateAndSaveCoupon(
        couponUrl: String,
        remoteAddr: String,
    ) {
        log.info("Starting background validation for coupon: {} from {}", couponUrl, remoteAddr)

        var auditLog =
            scrapingTaskLogRepository.save(
                ScrapingTaskLog(
                    url = couponUrl,
                    remoteAddr = remoteAddr,
                    status = "PENDING",
                ),
            )

        try {
            val extractor = CourseDataExtractor(couponUrl)
            val couponData = extractor.getFullCouponCodeData()

            if (couponData == null) {
                handleExpiredOrInvalidExtraction(couponUrl, extractor.courseId, auditLog)
                return
            }

            val existedBefore =
                couponCourseRepository.findByCouponUrl(couponUrl) != null ||
                    expiredCouponRepository.findByCouponUrl(couponUrl) != null
            couponData.isNew = !existedBefore

            val saved = couponCourseRepository.save(couponData)

            couponCourseHistoryRepository.save(
                CouponCourseHistory(
                    courseId = saved.courseId,
                    title = saved.title,
                    couponUrl = saved.couponUrl ?: "",
                    status = if (existedBefore) "reactivated" else "new",
                ),
            )

            log.info("Successfully validated and saved: {} (ID: {})", saved.title, saved.courseId)

            updateAuditLog(auditLog, "SUCCESS", null, saved.courseId)

            // Notify interested users about the new deal
            notificationService.notifyInterestedUsers(saved)
        } catch (e: Exception) {
            log.error("Error in async scraping for {}: {}", couponUrl, e.message, e)
            reporter.reportException(
                throwable = e,
                tags =
                    mapOf(
                        "service" to "course-scraper",
                        "job_type" to "validate_and_save_coupon",
                        "source" to sanitizeSourceTag(remoteAddr),
                    ),
            )
            updateAuditLog(auditLog, "FAILED", e.javaClass.simpleName + ": " + e.message)
        }
    }

    private fun handleExpiredOrInvalidExtraction(
        couponUrl: String,
        extractedCourseId: Int,
        auditLog: ScrapingTaskLog,
    ) {
        log.warn("Failed to extract valid data for URL: {}", couponUrl)
        val existing =
            couponCourseRepository.findByCouponUrl(couponUrl)
                ?: if (extractedCourseId > 0) {
                    couponCourseRepository.findByCourseId(extractedCourseId)
                } else {
                    null
                }
        if (existing != null) {
            couponCleanupService.archiveAndPurgeSingleCoupon(existing)
        } else {
            couponCleanupService.recordExpiredUrl(
                url = couponUrl,
                courseId = if (extractedCourseId > 0) extractedCourseId else null,
            )
        }
        updateAuditLog(auditLog, "FAILED", "Extractor returned null data (expired or invalid coupon)")
    }

    private fun updateAuditLog(
        auditLog: ScrapingTaskLog,
        status: String,
        error: String?,
        courseId: Int? = null,
    ) {
        auditLog.status = status
        auditLog.errorMessage = error
        auditLog.courseId = courseId
        scrapingTaskLogRepository.save(auditLog)
    }
}
