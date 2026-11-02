package com.thanh0x.coursedeal.controller

import com.thanh0x.coursedeal.dto.CourseReportRequest
import com.thanh0x.coursedeal.dto.CourseReportResponse
import com.thanh0x.coursedeal.service.CourseReportService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/courses")
@Tag(name = "Course Reports", description = "Course deal report endpoints")
class CourseReportController(
    private val courseReportService: CourseReportService,
) {
    @PostMapping("/{courseId}/reports")
    @Operation(
        operationId = "reportCourse",
        summary = "Report an expired or invalid course deal",
        description = "Submits a user report for a course coupon deal.",
    )
    fun reportCourse(
        @PathVariable courseId: Int,
        @Valid @RequestBody request: CourseReportRequest,
        @Parameter(hidden = true) httpServletRequest: HttpServletRequest,
    ): ResponseEntity<CourseReportResponse> {
        val clientIp =
            httpServletRequest.getHeader("X-Forwarded-For")
                ?.split(",")?.firstOrNull()?.trim()
                ?: httpServletRequest.remoteAddr

        val response = courseReportService.submitReport(courseId, request, clientIp)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
}
