package com.thanh0x.coursedeal.exception

import com.thanh0x.coursedeal.observability.ErrorTrackingReporter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.ServletWebRequest

class GlobalExceptionHandlerTest {
    @Test
    fun `handleGlobalException reports 500 error with safe tags and returns internal server error`() {
        var capturedThrowable: Throwable? = null
        var capturedTags: Map<String, String>? = null
        val reporter =
            ErrorTrackingReporter { throwable, tags ->
                capturedThrowable = throwable
                capturedTags = tags
            }
        val handler = GlobalExceptionHandler(reporter)
        val servletRequest =
            MockHttpServletRequest("POST", "/api/v1/coupons/101/report").apply {
                queryString = "token=secret"
                addHeader("Authorization", "Bearer super-secret-jwt")
            }
        val webRequest = ServletWebRequest(servletRequest)
        val unexpectedError = IllegalStateException("Database connection pool exhausted")

        val response = handler.handleGlobalException(unexpectedError, webRequest)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals(500, response.body?.status)
        assertEquals("/api/v1/coupons/101/report", response.body?.path)
        assertSame(unexpectedError, capturedThrowable)
        assertEquals(
            mapOf(
                "service" to "coupon-api-service",
                "endpoint" to "/api/v1/coupons/101/report",
                "http_method" to "POST",
            ),
            capturedTags,
        )
    }

    @Test
    fun `domain 4xx exceptions do not report to error tracking`() {
        var capturedThrowable: Throwable? = null
        val reporter =
            ErrorTrackingReporter { throwable, _ ->
                capturedThrowable = throwable
            }
        val handler = GlobalExceptionHandler(reporter)
        val webRequest = ServletWebRequest(MockHttpServletRequest("GET", "/api/v1/coupons/999"))

        val notFoundResponse =
            handler.handleResourceNotFoundException(
                ResourceNotFoundException("Coupon not found"),
                webRequest,
            )
        val badRequestResponse =
            handler.handleBadRequestException(
                BadRequestException("Invalid parameter"),
                webRequest,
            )

        assertEquals(HttpStatus.NOT_FOUND, notFoundResponse.statusCode)
        assertEquals(HttpStatus.BAD_REQUEST, badRequestResponse.statusCode)
        assertNull(capturedThrowable)
    }

    @Test
    fun `default SentryErrorTrackingReporter operates as safe no-op when DSN is empty`() {
        val handler = GlobalExceptionHandler()
        val webRequest = ServletWebRequest(MockHttpServletRequest("GET", "/api/v1/coupons"))

        val response = handler.handleGlobalException(RuntimeException("Unexpected failure"), webRequest)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
    }
}
