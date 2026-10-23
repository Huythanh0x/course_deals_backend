package com.thanh0x.coursedeal.service

import com.thanh0x.coursedeal.config.IdentityProperties
import com.thanh0x.coursedeal.dto.MockLoginRequestDTO
import com.thanh0x.coursedeal.dto.SocialLoginRequestDTO
import com.thanh0x.coursedeal.exception.BadRequestException
import com.thanh0x.coursedeal.model.user.AuthProvider
import com.thanh0x.coursedeal.model.user.UserEntity
import com.thanh0x.coursedeal.repository.SocialAccountRepository
import com.thanh0x.coursedeal.repository.UserRepository
import com.thanh0x.coursedeal.security.TokenProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class SocialAuthServiceTest {
    private val userRepository = mock(UserRepository::class.java)
    private val socialAccountRepository = mock(SocialAccountRepository::class.java)
    private val tokenProvider = mock(TokenProvider::class.java)
    private val properties = IdentityProperties()
    private val userSessionService = mock(UserSessionService::class.java)

    private val service =
        SocialAuthService(
            userRepository = userRepository,
            socialAccountRepository = socialAccountRepository,
            tokenProvider = tokenProvider,
            properties = properties,
            userSessionService = userSessionService,
        )

    private fun <T> nonNullAny(fallback: T): T = ArgumentMatchers.any<T>() ?: fallback

    @BeforeEach
    fun setUp() {
        val dummyUser = UserEntity()
        `when`(userSessionService.createSession(nonNullAny(dummyUser))).thenReturn("mock-session-id")
        `when`(userSessionService.createRefreshToken("mock-session-id")).thenReturn("mock-refresh-token")
        `when`(tokenProvider.createToken(nonNullAny(dummyUser), ArgumentMatchers.any())).thenReturn("signed-jwt-token")
        `when`(userRepository.save(nonNullAny(dummyUser))).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `login with mock token when mockAuthEnabled is true returns valid JWT and creates user`() {
        properties.mockAuthEnabled = true

        val request =
            SocialLoginRequestDTO(
                provider = AuthProvider.GOOGLE,
                idToken = "mock-google-token",
            )

        val response = service.login(request)

        assertNotNull(response)
        assertEquals("signed-jwt-token", response.accessToken)
        assertEquals("Bearer", response.tokenType)
        verify(userRepository).save(nonNullAny(UserEntity()))
    }

    @Test
    fun `login with mock token when mockAuthEnabled is false attempts verification and fails`() {
        properties.mockAuthEnabled = false

        val request =
            SocialLoginRequestDTO(
                provider = AuthProvider.GOOGLE,
                idToken = "mock-google-token",
            )

        assertThrows(BadRequestException::class.java) {
            service.login(request)
        }
    }

    @Test
    fun `loginMock when mockAuthEnabled is true creates user and returns JWT`() {
        properties.mockAuthEnabled = true

        val request =
            MockLoginRequestDTO(
                email = "custom-dev@example.com",
                name = "Custom Dev",
            )

        val response = service.loginMock(request)

        assertNotNull(response)
        assertEquals("signed-jwt-token", response.accessToken)
        assertEquals("Bearer", response.tokenType)
        verify(userRepository).save(nonNullAny(UserEntity()))
    }

    @Test
    fun `loginMock populates default avatarUrl on user`() {
        properties.mockAuthEnabled = true

        val request =
            MockLoginRequestDTO(
                email = "custom-dev-avatar@example.com",
                name = "Custom Dev",
            )

        val captor = org.mockito.ArgumentCaptor.forClass(UserEntity::class.java)
        service.loginMock(request)

        verify(userRepository).save(captor.capture())
        assertEquals("https://lh3.googleusercontent.com/a/default-user", captor.value.avatarUrl)
    }

    @Test
    fun `loginMock when mockAuthEnabled is false throws BadRequestException`() {
        properties.mockAuthEnabled = false

        assertThrows(BadRequestException::class.java) {
            service.loginMock(MockLoginRequestDTO())
        }
    }

    @Test
    fun `login with unsupported provider throws BadRequestException`() {
        val request =
            SocialLoginRequestDTO(
                provider = AuthProvider.PASSKEY,
                idToken = "some-token-long-enough",
            )

        assertThrows(BadRequestException::class.java) {
            service.login(request)
        }
    }
}
