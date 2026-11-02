package com.thanh0x.coursedeal.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for remote mobile client app version compatibility and feature flags.
 */
@ConfigurationProperties(prefix = "custom.app-config")
data class AppConfigProperties(
    var minSupportedVersionCode: Int = DEFAULT_MIN_SUPPORTED_VERSION_CODE,
    var latestVersionCode: Int = DEFAULT_LATEST_VERSION_CODE,
    var forceUpdateUrl: String = DEFAULT_FORCE_UPDATE_URL,
    var featureFlags: FeatureFlagsProperties = FeatureFlagsProperties(),
) {
    /**
     * Nested feature flag toggle properties serialized into the remote app config payload.
     */
    data class FeatureFlagsProperties(
        var clientVerificationEnabled: Boolean = false,
        var communityReportsEnabled: Boolean = true,
        var preferExternalBrowserCheckout: Boolean = false,
        var enrollFlowReportThreshold: Double = DEFAULT_ENROLL_FLOW_REPORT_THRESHOLD,
    ) {
        fun toMap(): Map<String, Any> =
            mapOf(
                "clientVerificationEnabled" to clientVerificationEnabled,
                "communityReportsEnabled" to communityReportsEnabled,
                "preferExternalBrowserCheckout" to preferExternalBrowserCheckout,
                "enrollFlowReportThreshold" to enrollFlowReportThreshold,
            )
    }

    companion object {
        const val DEFAULT_MIN_SUPPORTED_VERSION_CODE = 105
        const val DEFAULT_LATEST_VERSION_CODE = 106
        const val DEFAULT_FORCE_UPDATE_URL = "https://play.google.com/store/apps/details?id=com.thanh0x.coursedeals"
        const val DEFAULT_ENROLL_FLOW_REPORT_THRESHOLD = 0.3
    }
}
