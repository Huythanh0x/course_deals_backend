package com.thanh0x.coursedeal.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * Request DTO for email/password registration.
 */
data class RegisterRequestDTO(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String = "",
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 6, message = "Password must be at least 6 characters")
    val password: String = "",
    val fullName: String? = null,
)

/**
 * Response DTO for email/password registration OTP dispatch.
 */
data class RegisterResponseDTO(
    val message: String = "OTP sent to email",
    val email: String = "",
    val expiresInSeconds: Long = 86400,
)

/**
 * Request DTO for verifying email registration OTP.
 */
data class VerifyOtpRequestDTO(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String = "",
    @field:NotBlank(message = "OTP is required")
    val otp: String = "",
    val fcmToken: String? = null,
)

/**
 * Request DTO for email/password login.
 */
data class EmailLoginRequestDTO(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String = "",
    @field:NotBlank(message = "Password is required")
    val password: String = "",
    val fcmToken: String? = null,
)

/**
 * Request DTO for refreshing an access token using a refresh token.
 */
data class RefreshTokenRequestDTO(
    @field:NotBlank(message = "Refresh token is required")
    val refreshToken: String = "",
)
