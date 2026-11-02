package com.thanh0x.coursedeal.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Response payload containing remote client configuration, minimum supported version code,
 * store update link, and feature flags.
 */
@Schema(description = "Remote application configuration and version compatibility payload")
data class AppConfigResponseDTO(
    @Schema(description = "Minimum client version code allowed to run without force update", example = "105")
    val minSupportedVersionCode: Int,
    @Schema(description = "Latest available client version code in the store", example = "106")
    val latestVersionCode: Int,
    @Schema(
        description = "Store URL where users can update the application",
        example = "https://play.google.com/store/apps/details?id=com.thanh0x.coursedeals",
    )
    val forceUpdateUrl: String,
    @Schema(
        description = "Key-value map of remote feature flags and dynamic thresholds",
        example =
            "{\"clientVerificationEnabled\":false,\"communityReportsEnabled\":true," +
                "\"preferExternalBrowserCheckout\":false,\"enrollFlowReportThreshold\":0.3}",
    )
    val featureFlags: Map<String, Any>,
)
