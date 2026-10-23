package com.thanh0x.coursedeal.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * Request payload for reporting an issue with a course deal.
 */
@Schema(description = "Request body for submitting a course report")
data class CourseReportRequest(
    @field:NotBlank(message = "Reason must not be blank")
    @field:Size(max = 50, message = "Reason cannot exceed 50 characters")
    @Schema(
        description = "Reason for reporting (e.g. NOT_FREE, NO_DATA, EXPIRED, OTHER, or localized text)",
        example = "NOT_FREE",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val reason: String,
    @field:Size(max = 2000, message = "Details cannot exceed 2000 characters")
    @Schema(
        description = "Optional additional details or explanation from reporter",
        example = "Coupon code expired today and price reverted to $84.99",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    )
    val details: String? = null,
)
