package com.thanh0x.coursedeal.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.thanh0x.coursedeal.model.coupon.CourseLevel
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Full DTO for coupon detail view.
 */
@Schema(description = "Detailed information for a course coupon")
data class CouponDetailDTO(
    @Schema(description = "Unique identifier of the course")
    val courseId: Int,
    @Schema(description = "Main category of the course")
    val category: String? = null,
    @Schema(description = "Sub-category of the course")
    val subCategory: String? = null,
    @Schema(description = "Title of the course")
    val title: String? = null,
    @Schema(description = "Estimated length of the course content (e.g. in minutes)")
    val contentLength: Int = 0,
    @Schema(description = "Difficulty level of the course")
    val level: CourseLevel? = null,
    @Schema(description = "Author/Instructor of the course")
    val author: String? = null,
    @Schema(description = "Average rating of the course (0-5)")
    val rating: Float = 0f,
    @Schema(description = "Total number of reviews")
    val reviews: Int = 0,
    @Schema(description = "Total number of enrolled students")
    val students: Int = 0,
    @Schema(description = "The coupon code string")
    val couponCode: String? = null,
    @Schema(description = "URL to the course preview image")
    val previewImage: String? = null,
    @Schema(description = "The full Udemy coupon URL")
    val couponUrl: String? = null,
    @Schema(description = "Unix timestamp (seconds) when the coupon expires", example = "1785810480")
    val expiredTime: Long? = null,
    @Schema(description = "Number of remaining redemptions for this coupon")
    val usesRemaining: Int = 0,
    @Schema(description = "Short headline/catchphrase for the course")
    val heading: String? = null,
    @Schema(description = "Full HTML/text description of the course")
    val description: String? = null,
    @Schema(description = "Path/URL to the course preview video")
    val previewVideo: String? = null,
    @Schema(description = "Instructional language of the course")
    val language: String? = null,
    @get:JvmName("isNew")
    @get:JsonProperty("isNew")
    @field:Schema(name = "isNew", description = "Flag indicating if this is a newly discovered coupon")
    val isNew: Boolean = false,
    @Schema(description = "Unix timestamp (seconds) when the coupon was discovered", example = "1785660439")
    val createdAt: Long? = null,
    @Schema(description = "Unix timestamp (seconds) when the coupon data was last updated", example = "1785660439")
    val updatedAt: Long? = null,
)
