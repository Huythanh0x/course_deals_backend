package com.thanh0x.coursedeal.dto

/**
 * Response DTO for successful authentication.
 */
data class AuthResponseDTO(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val tokenType: String? = null,
    val expiresIn: String = "3600",
    val isNewUser: Boolean = false,
)
