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
    }

    data class SessionData(
        val sessionId: String,
        val userId: Int,
        val email: String?,
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
                val keysToDelete = sessionIds.map { SESSION_PREFIX + it.toString() }
                redisTemplate.delete(keysToDelete)
            }
            redisTemplate.delete(userSessionsKey)
        } catch (e: Exception) {
            log.error("Failed to invalidate all user sessions: {}", e.message)
        }
    }
}
