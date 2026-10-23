package com.thanh0x.coursedeal.service

import com.thanh0x.coursedeal.config.logger
import com.thanh0x.coursedeal.dto.CourseReportRequest
import com.thanh0x.coursedeal.dto.CourseReportResponse
import com.thanh0x.coursedeal.model.report.CourseReport
import com.thanh0x.coursedeal.repository.CouponCourseRepository
import com.thanh0x.coursedeal.repository.report.CourseReportRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class CourseReportService(
    private val courseReportRepository: CourseReportRepository,
    private val couponCourseRepository: CouponCourseRepository,
    private val courseScraperService: CourseScraperService,
) {
    private val log = logger()

    @Transactional
    fun submitReport(
        courseId: Int,
        request: CourseReportRequest,
        clientIp: String?,
    ): CourseReportResponse {
        val course =
            couponCourseRepository.findById(courseId).orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found with id: $courseId")
            }

        val report =
            courseReportRepository.save(
                CourseReport(
                    courseId = courseId,
                    reason = request.reason,
                    details = request.details,
                    reporterIp = clientIp,
                    status = "PENDING",
                ),
            )

        val reportId = checkNotNull(report.id) { "Report ID must not be null after persisting" }

        // Asynchronously enqueue background re-validation via existing CourseScraperService JobRunr job
        course.couponUrl?.let { couponUrl ->
            val remoteAddr = clientIp ?: "report-worker"
            log.info("Enqueuing re-validation for reported course: {} (Report ID: {})", courseId, reportId)
            courseScraperService.enqueueScrapingTask(couponUrl, remoteAddr)
        }

        return CourseReportResponse(
            id = reportId,
            courseId = report.courseId,
            reason = report.reason,
            details = report.details,
            status = report.status,
            createdAt = report.createdAt,
        )
    }
}
