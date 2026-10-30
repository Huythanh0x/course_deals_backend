package com.thanh0x.coursedeal.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.thanh0x.coursedeal.config.IdentityProperties
import com.thanh0x.coursedeal.dto.EmailLoginRequestDTO
import com.thanh0x.coursedeal.dto.RefreshTokenRequestDTO
import com.thanh0x.coursedeal.dto.RegisterRequestDTO
import com.thanh0x.coursedeal.dto.SendOtpRequestDTO
import com.thanh0x.coursedeal.dto.VerifyOtpRequestDTO
import com.thanh0x.coursedeal.exception.BadRequestException
import com.thanh0x.coursedeal.exception.ConflictException
import com.thanh0x.coursedeal.exception.UnauthorizedException
import com.thanh0x.coursedeal.model.user.UserEntity
import com.thanh0x.coursedeal.repository.UserRepository
import com.thanh0x.coursedeal.security.TokenProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.Duration
import java.util.Optional

class EmailAuthServiceTest {
    private val userRepository = mock(UserRepository::class.java)
    private val passwordEncoder = BCryptPasswordEncoder()

    @Suppress("UNCHECKED_CAST")
    private val redisTemplate = mock(RedisTemplate::class.java) as RedisTemplate<String, Any>

    @Suppress("UNCHECKED_CAST")
    private val valueOperations = mock(ValueOperations::class.java) as ValueOperations<String, Any>

    private val objectMapper = ObjectMapper()
    private val tokenProvider = mock(TokenProvider::class.java)
    private val userSessionService = mock(UserSessionService::class.java)
    private val properties = IdentityProperties(jwtExpiration = 3600000L, sessionTtlSeconds = 2592000L)

    private val service =
        EmailAuthService(
            userRepository = userRepository,
            passwordEncoder = passwordEncoder,
            redisTemplate = redisTemplate,
            objectMapper = objectMapper,
            tokenProvider = tokenProvider,
            userSessionService = userSessionService,
            properties = properties,
        )

    private fun <T> nonNullAny(fallback: T): T = ArgumentMatchers.any<T>() ?: fallback

    @BeforeEach
    fun setUp() {
        val dummyUser = UserEntity()
        `when`(redisTemplate.opsForValue()).thenReturn(valueOperations)
        `when`(userSessionService.createSession(nonNullAny(dummyUser))).thenReturn("session-123")
        `when`(userSessionService.createRefreshToken("session-123")).thenReturn("refresh-123")
        `when`(tokenProvider.createToken(nonNullAny(dummyUser), eq("session-123"))).thenReturn("jwt-access-123")
        `when`(userRepository.save(nonNullAny(dummyUser))).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `register stores pending registration with 000000 OTP and 24h TTL in Redis`() {
        `when`(userRepository.existsByEmail("student@example.com")).thenReturn(false)

        val response =
            service.register(
                RegisterRequestDTO(
                    email = "  Student@Example.com ",
                    password = "password123",
                    fullName = "Alice Student",
                ),
            )

        assertEquals("OTP sent to email", response.message)
        assertEquals("student@example.com", response.email)
        assertEquals(86400L, response.expiresInSeconds)

        val jsonCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(valueOperations).set(
            eq("auth:otp:student@example.com"),
            jsonCaptor.capture(),
            eq(Duration.ofHours(24)),
        )
        val stored = objectMapper.readValue(jsonCaptor.value, EmailAuthService.PendingRegistration::class.java)
        assertEquals("000000", stored.otp)
        assertEquals("student@example.com", stored.email)
        assertEquals("Alice Student", stored.fullName)
    }

    @Test
    fun `sendOtp stores pending registration with 000000 OTP regardless of user existence`() {
        val response =
            service.sendOtp(
                SendOtpRequestDTO(
                    email = "  passwordless@example.com ",
                ),
            )

        assertEquals("OTP sent to email", response.message)
        assertEquals("passwordless@example.com", response.email)
        assertEquals(86400L, response.expiresInSeconds)

        val jsonCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(valueOperations).set(
            eq("auth:otp:passwordless@example.com"),
            jsonCaptor.capture(),
            eq(Duration.ofHours(24)),
        )
        val stored = objectMapper.readValue(jsonCaptor.value, EmailAuthService.PendingRegistration::class.java)
        assertEquals("000000", stored.otp)
        assertEquals("passwordless@example.com", stored.email)
    }

    @Test
    fun `register throws ConflictException when email is already registered`() {
        `when`(userRepository.existsByEmail("existing@example.com")).thenReturn(true)

        assertThrows(ConflictException::class.java) {
            service.register(
                RegisterRequestDTO(
                    email = "existing@example.com",
                    password = "password123",
                ),
            )
        }
    }

    @Test
    fun `verifyOtp creates user deletes OTP from Redis and returns access and refresh tokens`() {
        val pending =
            EmailAuthService.PendingRegistration(
                email = "student@example.com",
                passwordHash = passwordEncoder.encode("password123"),
                fullName = "Alice Student",
                otp = "000000",
            )
        `when`(valueOperations.get("auth:otp:student@example.com"))
            .thenReturn(objectMapper.writeValueAsString(pending))
        `when`(userRepository.findByEmail("student@example.com")).thenReturn(null)

        val response =
            service.verifyOtp(
                VerifyOtpRequestDTO(
                    email = "student@example.com",
                    otp = "000000",
                    fcmToken = "fcm-xyz",
                ),
            )

        assertEquals("jwt-access-123", response.accessToken)
        assertEquals("refresh-123", response.refreshToken)
        assertEquals("Bearer", response.tokenType)
        assertEquals("3600", response.expiresIn)
        assertEquals(true, response.isNewUser)
        verify(redisTemplate).delete("auth:otp:student@example.com")
    }

    @Test
    fun `verifyOtp with existing user returns isNewUser false and preserves user`() {
        val pending =
            EmailAuthService.PendingRegistration(
                email = "existing@example.com",
                otp = "000000",
            )
        val existing =
            UserEntity(
                id = 99,
                username = "existing@example.com",
                email = "existing@example.com",
                fullName = "Existing User",
            )
        `when`(valueOperations.get("auth:otp:existing@example.com"))
            .thenReturn(objectMapper.writeValueAsString(pending))
        `when`(userRepository.findByEmail("existing@example.com")).thenReturn(existing)

        val response =
            service.verifyOtp(
                VerifyOtpRequestDTO(
                    email = "existing@example.com",
                    otp = "000000",
                    fcmToken = "updated-fcm",
                ),
            )

        assertEquals("jwt-access-123", response.accessToken)
        assertEquals("refresh-123", response.refreshToken)
        assertEquals(false, response.isNewUser)
        assertEquals("updated-fcm", existing.fcmToken)
        verify(userRepository).save(existing)
        verify(redisTemplate).delete("auth:otp:existing@example.com")
    }

    @Test
    fun `verifyOtp with wrong OTP throws BadRequestException`() {
        val pending =
            EmailAuthService.PendingRegistration(
                email = "student@example.com",
                passwordHash = "hashed",
                fullName = "Alice",
                otp = "000000",
            )
        `when`(valueOperations.get("auth:otp:student@example.com"))
            .thenReturn(objectMapper.writeValueAsString(pending))

        assertThrows(BadRequestException::class.java) {
            service.verifyOtp(
                VerifyOtpRequestDTO(
                    email = "student@example.com",
                    otp = "999999",
                ),
            )
        }
    }

    @Test
    fun `verifyOtp with expired or missing OTP throws BadRequestException`() {
        `when`(valueOperations.get("auth:otp:student@example.com")).thenReturn(null)

        assertThrows(BadRequestException::class.java) {
            service.verifyOtp(
                VerifyOtpRequestDTO(
                    email = "student@example.com",
                    otp = "000000",
                ),
            )
        }
    }

    @Test
    fun `login with valid credentials returns access and refresh tokens`() {
        val encodedPassword = passwordEncoder.encode("secret123")
        val user =
            UserEntity(
                id = 7,
                username = "student@example.com",
                email = "student@example.com",
                password = encodedPassword,
            )
        `when`(userRepository.findByEmail("student@example.com")).thenReturn(user)

        val response =
            service.login(
                EmailLoginRequestDTO(
                    email = "student@example.com",
                    password = "secret123",
                    fcmToken = "new-fcm-token",
                ),
            )

        assertEquals("jwt-access-123", response.accessToken)
        assertEquals("refresh-123", response.refreshToken)
        assertEquals("new-fcm-token", user.fcmToken)
        verify(userRepository).save(user)
    }

    @Test
    fun `login with wrong password throws UnauthorizedException`() {
        val user =
            UserEntity(
                id = 7,
                username = "student@example.com",
                email = "student@example.com",
                password = passwordEncoder.encode("secret123"),
            )
        `when`(userRepository.findByEmail("student@example.com")).thenReturn(user)

        assertThrows(UnauthorizedException::class.java) {
            service.login(
                EmailLoginRequestDTO(
                    email = "student@example.com",
                    password = "wrong-password",
                ),
            )
        }
    }

    @Test
    fun `login with social-only user without password throws UnauthorizedException`() {
        val user =
            UserEntity(
                id = 7,
                username = "social@example.com",
                email = "social@example.com",
                password = null,
            )
        `when`(userRepository.findByEmail("social@example.com")).thenReturn(user)

        assertThrows(UnauthorizedException::class.java) {
            service.login(
                EmailLoginRequestDTO(
                    email = "social@example.com",
                    password = "any-password",
                ),
            )
        }
    }

    @Test
    fun `refreshToken rotates token and returns new access and refresh tokens`() {
        val sessionData =
            UserSessionService.SessionData(
                sessionId = "session-123",
                userId = 7,
                email = "student@example.com",
            )
        val user = UserEntity(id = 7, username = "student@example.com", email = "student@example.com")
        `when`(userSessionService.rotateRefreshToken("old-refresh-token"))
            .thenReturn(sessionData to "rotated-refresh-token")
        `when`(userRepository.findById(7)).thenReturn(Optional.of(user))

        val response = service.refreshToken(RefreshTokenRequestDTO(refreshToken = "old-refresh-token"))

        assertEquals("jwt-access-123", response.accessToken)
        assertEquals("rotated-refresh-token", response.refreshToken)
        assertEquals("Bearer", response.tokenType)
    }

    @Test
    fun `refreshToken with invalid token throws UnauthorizedException`() {
        `when`(userSessionService.rotateRefreshToken("invalid-token")).thenReturn(null)

        assertThrows(UnauthorizedException::class.java) {
            service.refreshToken(RefreshTokenRequestDTO(refreshToken = "invalid-token"))
        }
    }
}
