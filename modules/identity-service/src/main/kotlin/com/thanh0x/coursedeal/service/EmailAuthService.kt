package com.thanh0x.coursedeal.service

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.thanh0x.coursedeal.config.IdentityProperties
import com.thanh0x.coursedeal.dto.AuthResponseDTO
import com.thanh0x.coursedeal.dto.EmailLoginRequestDTO
import com.thanh0x.coursedeal.dto.RefreshTokenRequestDTO
import com.thanh0x.coursedeal.dto.RegisterRequestDTO
import com.thanh0x.coursedeal.dto.RegisterResponseDTO
import com.thanh0x.coursedeal.dto.SendOtpRequestDTO
import com.thanh0x.coursedeal.dto.VerifyOtpRequestDTO
import com.thanh0x.coursedeal.exception.BadRequestException
import com.thanh0x.coursedeal.exception.ConflictException
import com.thanh0x.coursedeal.exception.UnauthorizedException
import com.thanh0x.coursedeal.model.user.UserEntity
import com.thanh0x.coursedeal.repository.UserRepository
import com.thanh0x.coursedeal.security.TokenProvider
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

/**
 * Service handling email/password registration with 24h Redis OTP verification,
 * email/password login, and refresh token rotation.
 */
@Service
@Suppress("LongParameterList")
class EmailAuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val redisTemplate: RedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper,
    private val tokenProvider: TokenProvider,
    private val userSessionService: UserSessionService,
    private val properties: IdentityProperties,
) {
    companion object {
        const val HARDCODED_OTP = "000000"
        const val OTP_KEY_PREFIX = "auth:otp:"
        const val OTP_TTL_HOURS = 24L
        const val OTP_TTL_SECONDS = 86400L
        private const val MILLIS_PER_SECOND = 1000L
    }

    data class PendingRegistration(
        val email: String = "",
        val passwordHash: String = "",
        val fullName: String? = null,
        val otp: String = HARDCODED_OTP,
    )

    fun register(request: RegisterRequestDTO): RegisterResponseDTO {
        val email = request.email.trim().lowercase()
        if (userRepository.existsByEmail(email)) {
            throw ConflictException("Email is already registered")
        }

        val passwordHash = passwordEncoder.encode(request.password)
        val pending =
            PendingRegistration(
                email = email,
                passwordHash = passwordHash,
                fullName = request.fullName?.trim()?.takeIf { it.isNotBlank() },
                otp = HARDCODED_OTP,
            )

        val json = objectMapper.writeValueAsString(pending)
        redisTemplate.opsForValue().set(
            OTP_KEY_PREFIX + email,
            json,
            Duration.ofHours(OTP_TTL_HOURS),
        )

        return RegisterResponseDTO(
            message = "OTP sent to email",
            email = email,
            expiresInSeconds = OTP_TTL_SECONDS,
        )
    }

    fun sendOtp(request: SendOtpRequestDTO): RegisterResponseDTO {
        val email = request.email.trim().lowercase()
        val pending =
            PendingRegistration(
                email = email,
                otp = HARDCODED_OTP,
            )

        val json = objectMapper.writeValueAsString(pending)
        redisTemplate.opsForValue().set(
            OTP_KEY_PREFIX + email,
            json,
            Duration.ofHours(OTP_TTL_HOURS),
        )

        return RegisterResponseDTO(
            message = "OTP sent to email",
            email = email,
            expiresInSeconds = OTP_TTL_SECONDS,
        )
    }

    @Transactional
    fun verifyOtp(request: VerifyOtpRequestDTO): AuthResponseDTO {
        val email = request.email.trim().lowercase()
        val key = OTP_KEY_PREFIX + email
        val pending = loadPendingRegistration(key)

        if (pending.otp != request.otp.trim()) {
            throw BadRequestException("Invalid or expired OTP")
        }

        val existingUser = userRepository.findByEmail(email)
        val isNewUser = (existingUser == null)
        val user =
            if (existingUser != null) {
                if (pending.passwordHash.isNotBlank()) {
                    existingUser.password = pending.passwordHash
                }
                if (!pending.fullName.isNullOrBlank()) {
                    existingUser.fullName = pending.fullName
                }
                if (!request.fcmToken.isNullOrBlank()) {
                    existingUser.fcmToken = request.fcmToken
                }
                userRepository.save(existingUser)
            } else {
                userRepository.save(
                    UserEntity(
                        username = email,
                        email = email,
                        password = pending.passwordHash.takeIf { it.isNotBlank() },
                        fullName = pending.fullName?.takeIf { it.isNotBlank() } ?: email.substringBefore("@"),
                        fcmToken = request.fcmToken?.takeIf { it.isNotBlank() },
                    ),
                )
            }

        redisTemplate.delete(key)
        return issueAuthResponse(user, isNewUser)
    }

    @Transactional
    fun login(request: EmailLoginRequestDTO): AuthResponseDTO {
        val email = request.email.trim().lowercase()
        val user = userRepository.findByEmail(email)
        val isPasswordValid =
            user != null &&
                !user.password.isNullOrBlank() &&
                passwordEncoder.matches(request.password, user.password)
        if (!isPasswordValid || user == null) {
            throw UnauthorizedException("Invalid email or password")
        }

        if (!request.fcmToken.isNullOrBlank()) {
            user.fcmToken = request.fcmToken
            userRepository.save(user)
        }

        return issueAuthResponse(user)
    }

    private fun loadPendingRegistration(key: String): PendingRegistration {
        val json =
            redisTemplate.opsForValue().get(key) as? String
                ?: throw BadRequestException("Invalid or expired OTP")
        return try {
            objectMapper.readValue(json, PendingRegistration::class.java)
        } catch (e: JsonProcessingException) {
            throw BadRequestException("Invalid or expired OTP", e)
        }
    }

    fun refreshToken(request: RefreshTokenRequestDTO): AuthResponseDTO {
        val rotated =
            userSessionService.rotateRefreshToken(request.refreshToken.trim())
                ?: throw UnauthorizedException("Invalid or expired refresh token")
        val (sessionData, newRefreshToken) = rotated
        val user =
            userRepository.findById(sessionData.userId)
                .orElseThrow { UnauthorizedException("Invalid or expired refresh token") }

        val accessToken = tokenProvider.createToken(user, sessionData.sessionId)
        return AuthResponseDTO(
            accessToken = accessToken,
            refreshToken = newRefreshToken,
            tokenType = "Bearer",
            expiresIn = (properties.jwtExpiration / MILLIS_PER_SECOND).toString(),
        )
    }

    private fun issueAuthResponse(
        user: UserEntity,
        isNewUser: Boolean = false,
    ): AuthResponseDTO {
        val sessionId = userSessionService.createSession(user)
        val refreshToken = userSessionService.createRefreshToken(sessionId)
        val accessToken = tokenProvider.createToken(user, sessionId)
        return AuthResponseDTO(
            accessToken = accessToken,
            refreshToken = refreshToken,
            tokenType = "Bearer",
            expiresIn = (properties.jwtExpiration / MILLIS_PER_SECOND).toString(),
            isNewUser = isNewUser,
        )
    }
}
