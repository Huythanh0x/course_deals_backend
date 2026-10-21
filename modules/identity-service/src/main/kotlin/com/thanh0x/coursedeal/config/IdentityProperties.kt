package com.thanh0x.coursedeal.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "custom")
data class IdentityProperties(
    var jwtSecret: String = "",
    var jwtExpiration: Long = 3600000,
    var googleClientId: String = "",
    var mockAuthEnabled: Boolean = false,
    var sessionTtlSeconds: Long = 2592000L,
    var webauthn: WebAuthnProperties = WebAuthnProperties(),
)

data class WebAuthnProperties(
    var rpId: String = "localhost",
    var rpName: String = "Course Deal",
    var origins: Set<String> = setOf("http://localhost:8080"),
)
