package com.thanh0x.coursedeal.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Filter and search criteria for multi-faceted coupon search.
 */
@Schema(description = "Criteria for searching and filtering coupons with multi-faceted parameters")
data class CouponSearchCriteriaDTO(
    @Schema(description = "Full-text search query over title, heading, and description", example = "Python")
    val q: String? = null,
    @Schema(description = "Filter by course category (case-insensitive substring/match)", example = "Development")
    val category: String? = null,
    @Schema(description = "Filter by course author / instructor", example = "Jose Portilla")
    val instructor: String? = null,
    @Schema(description = "Minimum rating threshold (e.g. 4.0, 4.5)", example = "4.5")
    val minRating: Float? = null,
    @Schema(description = "Course language (case-insensitive match/contains)", example = "English")
    val language: String? = null,
    @Schema(description = "Filter by course level (BEGINNER, INTERMEDIATE, etc.)", example = "BEGINNER")
    val level: String? = null,
    @Schema(
        description = "Sort order: 'relevance', 'newest', 'expiring_soon', 'rating', 'students', 'reviews'",
        defaultValue = "newest",
    )
    val sort: String = "newest",
    @Schema(description = "Sort direction: 'asc' or 'desc'", allowableValues = ["asc", "desc"], defaultValue = "desc")
    val sortDirection: String = "desc",
    @Schema(description = "Zero-indexed page number", defaultValue = "0")
    val page: Int = 0,
    @Schema(description = "Number of items per page (max 1000)", defaultValue = "20")
    val size: Int = 20,
)
