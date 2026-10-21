package com.thanh0x.coursedeal.dto

/**
 * Request payload for local development mock authentication.
 */
data class MockLoginRequestDTO(
    val email: String = "dev-user@coursedeals.com",
    val name: String = "Local Dev User",
    val fcmToken: String? = null,
)
