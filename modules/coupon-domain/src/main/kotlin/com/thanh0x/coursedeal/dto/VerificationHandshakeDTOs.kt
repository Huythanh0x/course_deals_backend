package com.thanh0x.coursedeal.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Schema(description = "Response returned upon starting a client verification handshake")
data class VerificationHandshakeResponseDTO(
    @Schema(description = "Target course ID to verify", example = "12345")
    val courseId: Int,
    @Schema(
        description = "Unique single-use nonce for this handshake session",
        example = "4fa85f64-5717-4562-b3fc-2c963f66afa6",
    )
    val nonce: String,
    @Schema(
        description = "Udemy coupon validation URL or target endpoint for verification",
        example = "https://www.udemy.com/api-2.0/course-landing-components/12345/me/?couponCode=FREE100",
    )
    val verificationUrl: String,
    @Schema(description = "HMAC-SHA256 signature binding courseId, nonce, and expiration", example = "a1b2c3d4...")
    val signature: String,
    @Schema(description = "Epoch seconds when the handshake ticket expires (TTL 60s)", example = "1785810480")
    val expiresAt: Long,
)

@Schema(description = "Attestation payload sent back by the mobile client after querying provider")
data class VerificationAttestationRequestDTO(
    @Schema(
        description = "Unique single-use nonce received in handshake",
        example = "4fa85f64-5717-4562-b3fc-2c963f66afa6",
    )
    @field:NotBlank(message = "Nonce is required")
    val nonce: String,
    @Schema(description = "HMAC signature received in handshake response", example = "a1b2c3d4...")
    @field:NotBlank(message = "Signature is required")
    val signature: String,
    @Schema(description = "Whether the coupon was observed as valid and redeemable on Udemy")
    @field:NotNull(message = "isValid status is required")
    val isValid: Boolean,
    @Schema(description = "Observed remaining uses, if present in Udemy response")
    @field:Min(value = 0, message = "usesRemaining must be >= 0")
    val usesRemaining: Int? = null,
    @Schema(description = "Optional observed coupon expiration epoch timestamp in seconds")
    val expiredTime: Long? = null,
    @Schema(description = "Optional client device/carrier attestation metadata or IP hint")
    val clientMetadata: String? = null,
)

@Schema(description = "Attestation acknowledgment response")
data class VerificationAttestationResponseDTO(
    @Schema(description = "Course ID that was verified", example = "12345")
    val courseId: Int,
    @Schema(description = "Whether the attestation was accepted and applied", example = "true")
    val accepted: Boolean,
    @Schema(description = "Updated remaining redemptions on server", example = "250")
    val remainingUses: Int? = null,
    @Schema(description = "Human-readable status summary", example = "Attestation verified and applied")
    val message: String,
)
