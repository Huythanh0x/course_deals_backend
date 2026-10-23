package com.thanh0x.coursedeal.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.thanh0x.coursedeal.config.IdentityProperties
import com.thanh0x.coursedeal.config.logger
import com.thanh0x.coursedeal.model.user.UserEntity
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.UUID

@Service
@Suppress("TooGenericExceptionCaught")
class UserSessionService(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val properties: IdentityProperties,
    private val objectMapper: ObjectMapper,
) {
    private val log = logger()

    companion object {
        private const val SESSION_PREFIX = "auth:session:"
        private const val USER_SESSIONS_PREFIX = "auth:user_sessions:"
        private const val REFRESH_PREFIX = "auth:refresh:"
        private const val SESSION_REFRESH_PREFIX = "auth:session_refresh:"
    }

    data class SessionData(
        val sessionId: String = "",
        val userId: Int = 0,
        val email: String? = null,
        val createdAt: Long = System.currentTimeMillis(),
    )

    fun createSession(user: UserEntity): String {
        val sessionId = UUID.randomUUID().toString()
        val sessionKey = SESSION_PREFIX + sessionId
        val userSessionsKey = USER_SESSIONS_PREFIX + user.id
        val ttl = Duration.ofSeconds(properties.sessionTtlSeconds)

        val sessionData =
            SessionData(
                sessionId = sessionId,
                userId = user.id,
                email = user.email,
            )

        try {
            val json = objectMapper.writeValueAsString(sessionData)
            redisTemplate.opsForValue().set(sessionKey, json, ttl)
            redisTemplate.opsForSet().add(userSessionsKey, sessionId)
            redisTemplate.expire(userSessionsKey, ttl)
        } catch (e: Exception) {
            log.error("Failed to store user session in Redis: {}", e.message)
        }

        return sessionId
    }

    fun createRefreshToken(sessionId: String): String {
        val refreshToken = UUID.randomUUID().toString()
        val ttl = Duration.ofSeconds(properties.sessionTtlSeconds)

        try {
            val oldRefreshToken = redisTemplate.opsForValue().get(SESSION_REFRESH_PREFIX + sessionId) as? String
            if (oldRefreshToken != null) {
                redisTemplate.delete(REFRESH_PREFIX + oldRefreshToken)
            }
            redisTemplate.opsForValue().set(REFRESH_PREFIX + refreshToken, sessionId, ttl)
            redisTemplate.opsForValue().set(SESSION_REFRESH_PREFIX + sessionId, refreshToken, ttl)
        } catch (e: Exception) {
            log.error("Failed to store refresh token in Redis: {}", e.message)
        }

        return refreshToken
    }

    fun getSession(sessionId: String): SessionData? {
        return try {
            val json = redisTemplate.opsForValue().get(SESSION_PREFIX + sessionId) as? String
            if (json != null) {
                objectMapper.readValue(json, SessionData::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            log.error("Failed to read session from Redis: {}", e.message)
            null
        }
    }

    fun rotateRefreshToken(refreshToken: String): Pair<SessionData, String>? {
        return try {
            val refreshKey = REFRESH_PREFIX + refreshToken
            val sessionId =
                if (refreshToken.isNotBlank()) {
                    redisTemplate.opsForValue().get(refreshKey) as? String
                } else {
                    null
                }
            if (sessionId == null) {
                null
            } else {
                redisTemplate.delete(refreshKey)
                val sessionData = if (isSessionActive(sessionId)) getSession(sessionId) else null
                if (sessionData == null) {
                    redisTemplate.delete(SESSION_REFRESH_PREFIX + sessionId)
                    null
                } else {
                    val newRefreshToken = createRefreshToken(sessionId)
                    sessionData to newRefreshToken
                }
            }
        } catch (e: Exception) {
            log.error("Failed to rotate refresh token: {}", e.message)
            null
        }
    }

    fun isSessionActive(sessionId: String): Boolean {
        return try {
            redisTemplate.hasKey(SESSION_PREFIX + sessionId) == true
        } catch (e: Exception) {
            log.error("Failed to check session active status in Redis: {}", e.message)
            false
        }
    }

    fun invalidateSession(
        sessionId: String,
        userId: Int? = null,
    ) {
        try {
            deleteRefreshTokenForSession(sessionId)
            redisTemplate.delete(SESSION_PREFIX + sessionId)
            if (userId != null) {
                redisTemplate.opsForSet().remove(USER_SESSIONS_PREFIX + userId, sessionId)
            }
        } catch (e: Exception) {
            log.error("Failed to invalidate session: {}", e.message)
        }
    }

    fun invalidateAllUserSessions(userId: Int) {
        try {
            val userSessionsKey = USER_SESSIONS_PREFIX + userId
            val sessionIds = redisTemplate.opsForSet().members(userSessionsKey)
            if (!sessionIds.isNullOrEmpty()) {
                sessionIds.forEach { deleteRefreshTokenForSession(it.toString()) }
                val keysToDelete = sessionIds.map { SESSION_PREFIX + it.toString() }
                redisTemplate.delete(keysToDelete)
            }
            redisTemplate.delete(userSessionsKey)
        } catch (e: Exception) {
            log.error("Failed to invalidate all user sessions: {}", e.message)
        }
    }

    private fun deleteRefreshTokenForSession(sessionId: String) {
        val refreshToken = redisTemplate.opsForValue().get(SESSION_REFRESH_PREFIX + sessionId) as? String
        if (refreshToken != null) {
            redisTemplate.delete(REFRESH_PREFIX + refreshToken)
        }
        redisTemplate.delete(SESSION_REFRESH_PREFIX + sessionId)
    }
}
