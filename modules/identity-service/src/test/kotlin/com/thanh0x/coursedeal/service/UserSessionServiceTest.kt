package com.thanh0x.coursedeal.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.thanh0x.coursedeal.config.IdentityProperties
import com.thanh0x.coursedeal.model.user.UserEntity
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.SetOperations
import org.springframework.data.redis.core.ValueOperations

class UserSessionServiceTest {
    @Suppress("UNCHECKED_CAST")
    private val redisTemplate = mock(RedisTemplate::class.java) as RedisTemplate<String, Any>

    @Suppress("UNCHECKED_CAST")
    private val valueOperations = mock(ValueOperations::class.java) as ValueOperations<String, Any>

    @Suppress("UNCHECKED_CAST")
    private val setOperations = mock(SetOperations::class.java) as SetOperations<String, Any>

    private val properties = IdentityProperties(sessionTtlSeconds = 3600)
    private val objectMapper = ObjectMapper()

    private val service = UserSessionService(redisTemplate, properties, objectMapper)

    @BeforeEach
    fun setUp() {
        `when`(redisTemplate.opsForValue()).thenReturn(valueOperations)
        `when`(redisTemplate.opsForSet()).thenReturn(setOperations)
    }

    @Test
    fun `createSession saves session data and returns non-empty sessionId`() {
        val user = UserEntity(id = 1, email = "user@example.com")

        val sessionId = service.createSession(user)

        assertNotNull(sessionId)
        assertTrue(sessionId.isNotBlank())
        verify(valueOperations).set(eq("auth:session:$sessionId"), any(), any(java.time.Duration::class.java))
        verify(setOperations).add(eq("auth:user_sessions:1"), eq(sessionId))
    }

    @Test
    fun `isSessionActive returns true when key exists in redis`() {
        `when`(redisTemplate.hasKey("auth:session:active-sid")).thenReturn(true)

        assertTrue(service.isSessionActive("active-sid"))
    }

    @Test
    fun `isSessionActive returns false when key is absent in redis`() {
        `when`(redisTemplate.hasKey("auth:session:dead-sid")).thenReturn(false)

        assertFalse(service.isSessionActive("dead-sid"))
    }

    @Test
    fun `invalidateSession deletes session key and removes from set`() {
        service.invalidateSession("sid-123", userId = 1)

        verify(redisTemplate).delete("auth:session:sid-123")
        verify(setOperations).remove("auth:user_sessions:1", "sid-123")
    }

    @Test
    fun `invalidateAllUserSessions deletes all user session keys and the set`() {
        `when`(setOperations.members("auth:user_sessions:1")).thenReturn(setOf("sid-1", "sid-2"))

        service.invalidateAllUserSessions(1)

        verify(redisTemplate).delete(listOf("auth:session:sid-1", "auth:session:sid-2"))
        verify(redisTemplate).delete("auth:user_sessions:1")
    }
}
