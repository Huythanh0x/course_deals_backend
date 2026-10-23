package com.thanh0x.coursedeal.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * Response payload returned after a course report is created.
 */
@Schema(description = "Response payload after submitting a course report")
data class CourseReportResponse(
    @Schema(description = "ID of the created report", example = "1")
    val id: Long,
    @Schema(description = "ID of the reported course", example = "123456")
    val courseId: Int,
    @Schema(description = "Reason submitted", example = "NOT_FREE")
    val reason: String,
    @Schema(description = "Optional details submitted", example = "Expired coupon")
    val details: String?,
    @Schema(description = "Processing status of report", example = "PENDING")
    val status: String,
    @Schema(description = "Creation timestamp")
    val createdAt: LocalDateTime?,
)
