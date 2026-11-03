package com.thanh0x.coursedeal.service

import com.thanh0x.coursedeal.model.audit.ScrapingTaskLog
import com.thanh0x.coursedeal.observability.ErrorTrackingReporter
import com.thanh0x.coursedeal.repository.CouponCourseHistoryRepository
import com.thanh0x.coursedeal.repository.CouponCourseRepository
import com.thanh0x.coursedeal.repository.ExpiredCouponRepository
import com.thanh0x.coursedeal.repository.audit.ScrapingTaskLogRepository
import org.jobrunr.scheduling.JobScheduler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class CourseScraperErrorReportingTest {
    private val couponCourseRepository = mock(CouponCourseRepository::class.java)
    private val expiredCouponRepository = mock(ExpiredCouponRepository::class.java)
    private val couponCourseHistoryRepository = mock(CouponCourseHistoryRepository::class.java)
    private val notificationService = mock(NotificationService::class.java)
    private val scrapingTaskLogRepository = mock(ScrapingTaskLogRepository::class.java)
    private val jobScheduler = mock(JobScheduler::class.java)
    private val couponCleanupService = mock(CouponCleanupService::class.java)

    private var capturedThrowable: Throwable? = null
    private var capturedTags: Map<String, String>? = null
    private val reporter =
        ErrorTrackingReporter { throwable, tags ->
            capturedThrowable = throwable
            capturedTags = tags
        }

    private val scraperService =
        CourseScraperService(
            couponCourseRepository = couponCourseRepository,
            expiredCouponRepository = expiredCouponRepository,
            couponCourseHistoryRepository = couponCourseHistoryRepository,
            notificationService = notificationService,
            scrapingTaskLogRepository = scrapingTaskLogRepository,
            jobScheduler = jobScheduler,
            couponCleanupService = couponCleanupService,
            errorTrackingReporter = reporter,
        )

    @BeforeEach
    fun setUp() {
        capturedThrowable = null
        capturedTags = null
        `when`(scrapingTaskLogRepository.save(any(ScrapingTaskLog::class.java))).thenAnswer { invocation ->
            invocation.getArgument<ScrapingTaskLog>(0)
        }
    }

    @Test
    fun `validateAndSaveCoupon reports unexpected worker failure with safe crawler source tag`() {
        val invalidUrl = "https://invalid.udemy.local/course/test"
        val simulatedError = IllegalStateException("Database write failure during expired cleanup")
        `when`(couponCourseRepository.findByCouponUrl(invalidUrl)).thenThrow(simulatedError)

        scraperService.validateAndSaveCoupon(invalidUrl, "crawler")

        assertSame(simulatedError, capturedThrowable)
        assertEquals(
            mapOf(
                "service" to "course-scraper",
                "job_type" to "validate_and_save_coupon",
                "source" to "crawler",
            ),
            capturedTags,
        )
    }

    @Test
    fun `validateAndSaveCoupon scrubs client IP address into api-submission source tag`() {
        val invalidUrl = "https://invalid.udemy.local/course/test"
        val simulatedError = RuntimeException("Unexpected scraping error")
        `when`(couponCourseRepository.findByCouponUrl(invalidUrl)).thenThrow(simulatedError)

        scraperService.validateAndSaveCoupon(invalidUrl, "203.0.113.42")

        assertSame(simulatedError, capturedThrowable)
        assertEquals(
            mapOf(
                "service" to "course-scraper",
                "job_type" to "validate_and_save_coupon",
                "source" to "api-submission",
            ),
            capturedTags,
        )
    }
}
