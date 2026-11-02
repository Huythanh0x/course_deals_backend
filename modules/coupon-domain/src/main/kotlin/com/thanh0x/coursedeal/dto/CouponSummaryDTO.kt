package com.thanh0x.coursedeal.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.thanh0x.coursedeal.model.coupon.CourseLevel
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Optimized DTO for coupon list view.
 */
@Schema(description = "Summary information for a course coupon")
data class CouponSummaryDTO(
    @Schema(description = "Unique identifier of the course")
    val courseId: Int,
    @Schema(description = "Title of the course")
    val title: String? = null,
    @Schema(description = "Main category of the course")
    val category: String? = null,
    @Schema(description = "Difficulty level of the course")
    val level: CourseLevel? = null,
    @Schema(description = "Instructional language of the course")
    val language: String? = null,
    @Schema(description = "Author/Instructor of the course")
    val author: String? = null,
    @Schema(description = "Average rating of the course (0-5)")
    val rating: Float = 0f,
    @Schema(description = "Total number of reviews")
    val reviews: Int = 0,
    @Schema(description = "Total number of enrolled students")
    val students: Int = 0,
    @Schema(description = "URL to the course preview image")
    val previewImage: String? = null,
    @Schema(description = "Unix timestamp (seconds) when the coupon expires", example = "1785810480")
    val expiredTime: Long? = null,
    @Schema(description = "Unix timestamp (seconds) when the coupon was discovered", example = "1785660439")
    val createdAt: Long? = null,
    @get:JvmName("isNew")
    @get:JsonProperty("isNew")
    @field:Schema(name = "isNew", description = "Flag indicating if this is a newly discovered coupon")
    val isNew: Boolean = false,
)
