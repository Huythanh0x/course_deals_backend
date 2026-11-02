package com.thanh0x.coursedeal.observability

import com.thanh0x.coursedeal.config.logger
import io.sentry.Sentry
import org.springframework.stereotype.Component

/**
 * Abstraction for reporting unexpected application and background worker exceptions
 * to the configured error tracking provider (Sentry).
 */
fun interface ErrorTrackingReporter {
    fun reportException(
        throwable: Throwable,
        tags: Map<String, String>,
    )
}

/**
 * Default Sentry-backed implementation of [ErrorTrackingReporter].
 *
 * Operates as a safe no-op when Sentry is disabled or configured with a blank DSN.
 * Only attaches explicitly provided safe metadata tags and strips any sensitive keys.
 */
@Component
class SentryErrorTrackingReporter : ErrorTrackingReporter {
    private val log = logger()

    companion object {
        private val SENSITIVE_TAG_KEYS =
            setOf(
                "authorization",
                "token",
                "access_token",
                "refresh_token",
                "cookie",
                "password",
                "secret",
                "ip",
                "client_ip",
                "remote_addr",
                "email",
            )

        fun sanitizeTags(tags: Map<String, String>): Map<String, String> =
            tags.filter { (key, value) ->
                val normalizedKey = key.trim().lowercase()
                normalizedKey.isNotEmpty() &&
                    value.isNotBlank() &&
                    normalizedKey !in SENSITIVE_TAG_KEYS
            }
    }

    @Suppress("TooGenericExceptionCaught")
    override fun reportException(
        throwable: Throwable,
        tags: Map<String, String>,
    ) {
        try {
            val safeTags = sanitizeTags(tags)
            Sentry.captureException(throwable) { scope ->
                safeTags.forEach { (key, value) ->
                    scope.setTag(key, value)
                }
            }
        } catch (e: Exception) {
            log.debug("Failed to report exception to Sentry: {}", e.message)
        }
    }
}
