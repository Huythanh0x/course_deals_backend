package com.thanh0x.coursedeal.service

import com.thanh0x.coursedeal.exception.BadRequestException
import com.thanh0x.coursedeal.repository.PasskeyCredentialRepository
import com.thanh0x.coursedeal.repository.UserRepository
import com.thanh0x.coursedeal.security.TokenProvider
import com.yubico.webauthn.RelyingParty
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations

/**
 * finishAuthentication/finishRegistration both wrap every failure in BadRequestException, so
 * this focuses on the error paths reachable without mocking the FIDO2 crypto builder chain:
 * an expired/missing WebAuthn handshake in Redis must fail cleanly and never touch the DB.
 */
class PasskeyServiceTest {
    private val relyingParty = mock(RelyingParty::class.java)
    private val userRepository = mock(UserRepository::class.java)
    private val passkeyRepository = mock(PasskeyCredentialRepository::class.java)
    private val redisTemplate = mock(RedisTemplate::class.java) as RedisTemplate<String, Any>
    private val valueOperations = mock(ValueOperations::class.java) as ValueOperations<String, Any>
    private val tokenProvider = mock(TokenProvider::class.java)
    private val userSessionService = mock(UserSessionService::class.java)

    private val service =
        PasskeyService(
            relyingParty,
            userRepository,
            passkeyRepository,
            redisTemplate,
            tokenProvider,
            userSessionService,
        )

    @BeforeEach
    fun setUp() {
        `when`(redisTemplate.opsForValue()).thenReturn(valueOperations)
    }

    @Test
    fun `finishAuthentication throws BadRequestException when the webauthn handshake has expired`() {
        `when`(valueOperations.get("webauthn:auth:user@example.com")).thenReturn(null)

        val exception =
            assertThrows(BadRequestException::class.java) {
                service.finishAuthentication("user@example.com", "{}")
            }

        assertTrue(exception.message!!.contains("Authentication failed"))
        verify(userRepository, never()).findByEmail("user@example.com")
    }

    @Test
    fun `finishRegistration throws BadRequestException when the webauthn handshake has expired`() {
        `when`(valueOperations.get("webauthn:reg:user@example.com")).thenReturn(null)

        val exception =
            assertThrows(BadRequestException::class.java) {
                service.finishRegistration("user@example.com", "{}")
            }

        assertTrue(exception.message!!.contains("Registration failed"))
        verify(passkeyRepository, never()).save(org.mockito.ArgumentMatchers.any())
    }
}
