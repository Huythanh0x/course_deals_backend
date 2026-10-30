package com.thanh0x.coursedeal.service

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.thanh0x.coursedeal.config.IdentityProperties
import com.thanh0x.coursedeal.dto.AuthResponseDTO
import com.thanh0x.coursedeal.dto.MockLoginRequestDTO
import com.thanh0x.coursedeal.dto.SocialLoginRequestDTO
import com.thanh0x.coursedeal.exception.BadRequestException
import com.thanh0x.coursedeal.model.user.AuthProvider
import com.thanh0x.coursedeal.model.user.SocialAccount
import com.thanh0x.coursedeal.model.user.UserEntity
import com.thanh0x.coursedeal.repository.SocialAccountRepository
import com.thanh0x.coursedeal.repository.UserRepository
import com.thanh0x.coursedeal.security.TokenProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.IOException
import java.security.GeneralSecurityException
import java.util.Collections

@Service
class SocialAuthService(
    private val userRepository: UserRepository,
    private val socialAccountRepository: SocialAccountRepository,
    private val tokenProvider: TokenProvider,
    private val properties: IdentityProperties,
    private val userSessionService: UserSessionService,
) {
    @Transactional
    fun login(request: SocialLoginRequestDTO): AuthResponseDTO {
        val user: UserEntity =
            when (request.provider) {
                AuthProvider.GOOGLE -> verifyGoogleToken(request.idToken)
                AuthProvider.APPLE -> verifyAppleToken()
                else -> throw BadRequestException("Unsupported social provider")
            }

        if (request.fcmToken != null) {
            user.fcmToken = request.fcmToken
            userRepository.save(user)
        }

        val sessionId = userSessionService.createSession(user)
        val refreshToken = userSessionService.createRefreshToken(sessionId)
        val token = tokenProvider.createToken(user, sessionId)
        return AuthResponseDTO(
            accessToken = token,
            refreshToken = refreshToken,
            tokenType = "Bearer",
        )
    }

    @Transactional
    fun loginMock(request: MockLoginRequestDTO = MockLoginRequestDTO()): AuthResponseDTO {
        if (!properties.mockAuthEnabled) {
            throw BadRequestException("Mock authentication is disabled in this environment")
        }
        val defaultAvatar = "https://lh3.googleusercontent.com/a/default-user"
        val user =
            findOrCreateUser(
                request.email,
                request.name,
                AuthProvider.GOOGLE,
                "mock-google-${request.email}",
                defaultAvatar,
            )
        if (request.fcmToken != null) {
            user.fcmToken = request.fcmToken
            userRepository.save(user)
        }
        val sessionId = userSessionService.createSession(user)
        val refreshToken = userSessionService.createRefreshToken(sessionId)
        val token = tokenProvider.createToken(user, sessionId)
        return AuthResponseDTO(
            accessToken = token,
            refreshToken = refreshToken,
            tokenType = "Bearer",
        )
    }

    private fun verifyGoogleToken(idTokenString: String): UserEntity {
        if (properties.mockAuthEnabled && idTokenString.startsWith("mock-")) {
            return findOrCreateMockUser(idTokenString)
        }

        try {
            val verifier =
                GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory())
                    .setAudience(Collections.singletonList(properties.googleClientId))
                    .build()

            val idToken = verifier.verify(idTokenString) ?: throw BadRequestException("Invalid Google ID Token")

            val payload = idToken.payload
            return processGooglePayload(payload)
        } catch (e: GeneralSecurityException) {
            failGoogleVerification(e)
        } catch (e: IOException) {
            failGoogleVerification(e)
        } catch (e: IllegalArgumentException) {
            failGoogleVerification(e)
        }
    }

    fun processGooglePayload(payload: GoogleIdToken.Payload): UserEntity {
        val email = payload.email
        if (email.isNullOrBlank()) {
            throw BadRequestException("Google ID Token missing email claim")
        }
        if (payload.emailVerified != true) {
            throw BadRequestException("Google email is not verified")
        }

        val providerId = payload.subject
        val name = payload["name"] as String?
        val picture = payload["picture"] as String?

        return findOrCreateUser(email, name, AuthProvider.GOOGLE, providerId, picture)
    }

    private fun findOrCreateMockUser(idTokenString: String): UserEntity {
        val email =
            if (idTokenString.contains("@")) {
                idTokenString.removePrefix("mock-")
            } else {
                "dev-user@coursedeals.com"
            }
        val name = "Local Dev User"
        val providerId = "mock-google-$email"
        val avatar = "https://lh3.googleusercontent.com/a/default-user"
        return findOrCreateUser(email, name, AuthProvider.GOOGLE, providerId, avatar)
    }

    private fun failGoogleVerification(e: Exception): Nothing {
        throw BadRequestException("Failed to verify Google Token: " + e.message, e)
    }

    private fun verifyAppleToken(): UserEntity {
        // Mocking Apple for now - requires JWT validation with Apple's public keys
        throw UnsupportedOperationException("Apple login not yet implemented")
    }

    private fun findOrCreateUser(
        email: String,
        name: String?,
        provider: AuthProvider,
        providerId: String,
        avatarUrl: String? = null,
    ): UserEntity {
        val socialAccount = socialAccountRepository.findByProviderAndProviderId(provider, providerId)
        if (socialAccount != null) {
            val existingUser = socialAccount.user!!
            if (avatarUrl != null && existingUser.avatarUrl == null) {
                existingUser.avatarUrl = avatarUrl
                userRepository.save(existingUser)
            }
            return existingUser
        }

        val user =
            userRepository.findByEmail(email) ?: run {
                val newUser =
                    UserEntity(
                        email = email,
                        fullName = name,
                        avatarUrl = avatarUrl,
                        // fallback username
                        username = email,
                    )
                userRepository.save(newUser)
            }

        if (avatarUrl != null && user.avatarUrl == null) {
            user.avatarUrl = avatarUrl
            userRepository.save(user)
        }

        val newSocialAccount =
            SocialAccount(
                provider = provider,
                providerId = providerId,
                user = user,
            )
        socialAccountRepository.save(newSocialAccount)

        return user
    }
}
