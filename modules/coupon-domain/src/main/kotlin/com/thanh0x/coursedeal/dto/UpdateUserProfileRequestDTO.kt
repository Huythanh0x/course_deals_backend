package com.thanh0x.coursedeal.dto

import jakarta.validation.constraints.Size

/**
 * Request DTO for updating user profile fields.
 */
data class UpdateUserProfileRequestDTO(
    @field:Size(max = 255, message = "Full name must not exceed 255 characters")
    val fullName: String? = null,
    @field:Size(max = 1024, message = "Avatar URL must not exceed 1024 characters")
    val avatarUrl: String? = null,
    @field:Size(max = 512, message = "FCM token must not exceed 512 characters")
    val fcmToken: String? = null,
)
