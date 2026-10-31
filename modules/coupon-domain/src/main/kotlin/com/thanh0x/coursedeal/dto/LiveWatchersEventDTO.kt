package com.thanh0x.coursedeal.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Event payload emitted over SSE for active viewers on a course coupon deal.
 */
@Schema(description = "Live viewers event payload for a course deal")
data class LiveWatchersEventDTO(
    @Schema(description = "Number of active concurrent viewers on this deal", example = "18")
    val activeViewers: Long = 0L,
)
