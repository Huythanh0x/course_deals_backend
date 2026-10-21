package com.thanh0x.coursedeal.dto

/**
 * Representation of a user profile with personal details and notification preferences.
 */
data class UserProfileDTO(
    val id: Int,
    val email: String?,
    val fullName: String?,
    val avatarUrl: String?,
    val fcmToken: String?,
    val preferences: PreferenceDTO?,
)
