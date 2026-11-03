package com.thanh0x.coursedeal.controller

import com.thanh0x.coursedeal.dto.CourseDetailDTO
import com.thanh0x.coursedeal.dto.CourseReviewsDTO
import com.thanh0x.coursedeal.dto.CurriculumDTO
import com.thanh0x.coursedeal.dto.RelatedCourseDTO
import com.thanh0x.coursedeal.service.CourseDetailService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * REST controller for course detail endpoints.
 * Provides comprehensive course information including reviews, curriculum, and related courses.
 */
@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/api/v1/coupons")
@Tag(name = "Course Details", description = "API endpoints for detailed course information")
class CourseDetailController(private val courseDetailService: CourseDetailService) {
    /**
     * Gets comprehensive course details including reviews summary, curriculum, and related courses.
     *
     * @param courseId the course ID
     * @param couponCode optional coupon code
     * @return CourseDetailDTO with all course information
     */
    @GetMapping("/{courseId}/details")
    @Operation(
        operationId = "getCourseDetails",
        summary = "Get comprehensive course details",
        description = "Returns detailed course information including reviews, curriculum, pricing, and related courses",
    )
    fun getCourseDetails(
        @Parameter(description = "Course ID", required = true)
        @PathVariable courseId: Int,
        @Parameter(description = "Optional coupon code")
        @RequestParam(required = false) couponCode: String?,
    ): ResponseEntity<CourseDetailDTO> {
        val details = courseDetailService.getCourseDetails(courseId, couponCode)
        return details?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
    }

    /**
     * Gets paginated course reviews.
     *
     * @param courseId the course ID
     * @param page the page number (default: 1)
     * @return CourseReviewsDTO with paginated reviews
     */
    @GetMapping("/{courseId}/reviews")
    @Operation(
        operationId = "getCourseReviews",
        summary = "Get course reviews",
        description = "Returns paginated course reviews with instructor responses",
    )
    fun getCourseReviews(
        @Parameter(description = "Course ID", required = true)
        @PathVariable courseId: Int,
        @Parameter(description = "Page number (1-indexed)")
        @RequestParam(defaultValue = "1") page: Int,
    ): ResponseEntity<CourseReviewsDTO> {
        val reviews = courseDetailService.getCourseReviews(courseId, page)
        return reviews?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
    }

    /**
     * Gets course curriculum/syllabus.
     *
     * @param courseId the course ID
     * @param couponCode optional coupon code
     * @return CurriculumDTO with course sections
     */
    @GetMapping("/{courseId}/curriculum")
    @Operation(
        operationId = "getCourseCurriculum",
        summary = "Get course curriculum",
        description = "Returns the course curriculum/syllabus with all sections and lectures",
    )
    fun getCourseCurriculum(
        @Parameter(description = "Course ID", required = true)
        @PathVariable courseId: Int,
        @Parameter(description = "Optional coupon code")
        @RequestParam(required = false) couponCode: String?,
    ): ResponseEntity<CurriculumDTO> {
        val curriculum = courseDetailService.getCourseCurriculum(courseId, couponCode)
        return curriculum?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
    }

    /**
     * Gets related/recommended courses.
     *
     * @param courseId the course ID
     * @return List of RelatedCourseDTO
     */
    @GetMapping("/{courseId}/related")
    @Operation(
        operationId = "getRelatedCourses",
        summary = "Get related courses",
        description = "Returns a list of related/recommended courses",
    )
    fun getRelatedCourses(
        @Parameter(description = "Course ID", required = true)
        @PathVariable courseId: Int,
    ): ResponseEntity<List<RelatedCourseDTO>> {
        val relatedCourses = courseDetailService.getRelatedCourses(courseId)
        return ResponseEntity.ok(relatedCourses)
    }
}
