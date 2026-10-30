package com.thanh0x.coursedeal.service

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.json.gson.GsonFactory
import com.thanh0x.coursedeal.config.IdentityProperties
import com.thanh0x.coursedeal.exception.BadRequestException
import com.thanh0x.coursedeal.model.user.AuthProvider
import com.thanh0x.coursedeal.model.user.SocialAccount
import com.thanh0x.coursedeal.model.user.UserEntity
import com.thanh0x.coursedeal.repository.SocialAccountRepository
import com.thanh0x.coursedeal.repository.UserRepository
import com.thanh0x.coursedeal.security.TokenProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class GoogleOAuthPayloadParsingTest {
    private val userRepository = mock(UserRepository::class.java)
    private val socialAccountRepository = mock(SocialAccountRepository::class.java)
    private val tokenProvider = mock(TokenProvider::class.java)
    private val properties = IdentityProperties()
    private val userSessionService = mock(UserSessionService::class.java)
    private val jsonFactory = GsonFactory.getDefaultInstance()

    private val service =
        SocialAuthService(
            userRepository = userRepository,
            socialAccountRepository = socialAccountRepository,
            tokenProvider = tokenProvider,
            properties = properties,
            userSessionService = userSessionService,
        )

    @BeforeEach
    fun setUp() {
        `when`(userRepository.save(any(UserEntity::class.java))).thenAnswer { it.arguments[0] }
        `when`(socialAccountRepository.save(any(SocialAccount::class.java))).thenAnswer { it.arguments[0] }
    }

    private fun loadPayload(fixturePath: String): GoogleIdToken.Payload {
        val stream =
            javaClass.getResourceAsStream(fixturePath)
                ?: throw IllegalArgumentException("Fixture not found at $fixturePath")
        return jsonFactory.fromInputStream(stream, GoogleIdToken.Payload::class.java)
    }

    @Test
    fun `parse valid fixture accurately maps email, name, avatarUrl, and providerId`() {
        val payload = loadPayload("/fixtures/google-oauth-payload-valid.json")

        assertEquals("alex.doe@example.com", payload.email)
        assertEquals(true, payload.emailVerified)
        assertEquals("Alex Doe", payload["name"])
        assertEquals("https://lh3.googleusercontent.com/a/ALm5wu3example-avatar", payload["picture"])
        assertEquals("109876543210987654321", payload.subject)

        val user = service.processGooglePayload(payload).user

        assertNotNull(user)
        assertEquals("alex.doe@example.com", user.email)
        assertEquals("Alex Doe", user.fullName)
        assertEquals("https://lh3.googleusercontent.com/a/ALm5wu3example-avatar", user.avatarUrl)

        val socialAccountCaptor = ArgumentCaptor.forClass(SocialAccount::class.java)
        verify(socialAccountRepository).save(socialAccountCaptor.capture())
        val savedAccount = socialAccountCaptor.value
        assertEquals(AuthProvider.GOOGLE, savedAccount.provider)
        assertEquals("109876543210987654321", savedAccount.providerId)
        assertEquals(user, savedAccount.user)
    }

    @Test
    fun `parse missing email fixture throws BadRequestException`() {
        val payload = loadPayload("/fixtures/google-oauth-payload-missing-email.json")

        val exception =
            assertThrows(BadRequestException::class.java) {
                service.processGooglePayload(payload)
            }
        assertEquals("Google ID Token missing email claim", exception.message)
    }

    @Test
    fun `parse unverified email fixture throws BadRequestException`() {
        val payload = loadPayload("/fixtures/google-oauth-payload-unverified-email.json")

        val exception =
            assertThrows(BadRequestException::class.java) {
                service.processGooglePayload(payload)
            }
        assertEquals("Google email is not verified", exception.message)
    }
}
