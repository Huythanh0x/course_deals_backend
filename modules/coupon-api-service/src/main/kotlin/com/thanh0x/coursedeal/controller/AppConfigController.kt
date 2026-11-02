package com.thanh0x.coursedeal.controller

import com.thanh0x.coursedeal.config.AppConfigProperties
import com.thanh0x.coursedeal.dto.AppConfigResponseDTO
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * REST controller exposing public remote application configuration and version compatibility settings.
 */
@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/api/v1/app")
@Tag(name = "App Config", description = "Remote application configuration and version compatibility gate")
class AppConfigController(
    private val appConfigProperties: AppConfigProperties,
) {
    /**
     * Returns remote app configuration including minimum supported version code, latest version code,
     * force update store URL, and remote feature flags.
     */
    @GetMapping("/config")
    @Operation(
        operationId = "getAppConfig",
        summary = "Get remote app configuration",
        description =
            "Returns minimum supported version code, latest version code, " +
                "force update store URL, and remote feature flags.",
    )
    fun getAppConfig(): ResponseEntity<AppConfigResponseDTO> {
        val response =
            AppConfigResponseDTO(
                minSupportedVersionCode = appConfigProperties.minSupportedVersionCode,
                latestVersionCode = appConfigProperties.latestVersionCode,
                forceUpdateUrl = appConfigProperties.forceUpdateUrl,
                featureFlags = appConfigProperties.featureFlags.toMap(),
            )
        return ResponseEntity.ok(response)
    }
}
