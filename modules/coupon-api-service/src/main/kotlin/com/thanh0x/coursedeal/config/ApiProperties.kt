package com.thanh0x.coursedeal.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "custom")
data class ApiProperties(
    var refreshSecret: String = "",
    var handshakeSecret: String = "",
    var handshakeTtlSeconds: Long = 60L,
)
