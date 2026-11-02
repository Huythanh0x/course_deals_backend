package com.thanh0x.coursedeal.observability

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class SentryErrorTrackingReporterTest {
    private val reporter = SentryErrorTrackingReporter()

    @Test
    fun `reportException operates as safe no-op when Sentry DSN is not configured`() {
        assertDoesNotThrow {
            reporter.reportException(
                throwable = IllegalStateException("Simulated failure"),
                tags =
                    mapOf(
                        "service" to "coupon-api-service",
                        "endpoint" to "/api/v1/coupons",
                        "http_method" to "GET",
                    ),
            )
        }
    }

    @Test
    fun `sanitizeTags retains safe operational tags and strips sensitive keys and blank values`() {
        val rawTags =
            mapOf(
                "service" to "coupon-api-service",
                "endpoint" to "/api/v1/coupons",
                "http_method" to "POST",
                "job_type" to "validate_and_save_coupon",
                "source" to "crawler",
                "Authorization" to "Bearer secret-jwt",
                "token" to "raw-token",
                "client_ip" to "192.168.1.10",
                "remote_addr" to "10.0.0.1",
                "email" to "user@example.com",
                "empty_value" to "   ",
                "   " to "ignored",
            )

        val sanitized = SentryErrorTrackingReporter.sanitizeTags(rawTags)

        assertEquals(
            mapOf(
                "service" to "coupon-api-service",
                "endpoint" to "/api/v1/coupons",
                "http_method" to "POST",
                "job_type" to "validate_and_save_coupon",
                "source" to "crawler",
            ),
            sanitized,
        )
        assertFalse(sanitized.containsKey("Authorization"))
        assertFalse(sanitized.containsKey("token"))
        assertFalse(sanitized.containsKey("client_ip"))
        assertFalse(sanitized.containsKey("remote_addr"))
        assertFalse(sanitized.containsKey("email"))
    }
}
