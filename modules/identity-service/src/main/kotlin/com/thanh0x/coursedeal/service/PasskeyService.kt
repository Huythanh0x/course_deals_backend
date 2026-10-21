package com.thanh0x.coursedeal.service

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.thanh0x.coursedeal.config.logger
import com.thanh0x.coursedeal.dto.AuthResponseDTO
import com.thanh0x.coursedeal.exception.BadRequestException
import com.thanh0x.coursedeal.model.user.PasskeyCredential
import com.thanh0x.coursedeal.model.user.UserEntity
import com.thanh0x.coursedeal.repository.PasskeyCredentialRepository
import com.thanh0x.coursedeal.repository.UserRepository
import com.thanh0x.coursedeal.security.TokenProvider
import com.yubico.webauthn.AssertionRequest
import com.yubico.webauthn.FinishAssertionOptions
import com.yubico.webauthn.FinishRegistrationOptions
import com.yubico.webauthn.RelyingParty
import com.yubico.webauthn.StartAssertionOptions
import com.yubico.webauthn.StartRegistrationOptions
import com.yubico.webauthn.data.ByteArray
import com.yubico.webauthn.data.PublicKeyCredential
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions
import com.yubico.webauthn.data.UserIdentity
import com.yubico.webauthn.exception.AssertionFailedException
import com.yubico.webauthn.exception.RegistrationFailedException
import org.springframework.dao.DataAccessException
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.IOException
import java.time.Duration

/**
 * Service for handling WebAuthn (Passkey) registration and authentication.
 */
@Service
class PasskeyService(
    private val relyingParty: RelyingParty,
    private val userRepository: UserRepository,
    private val passkeyRepository: PasskeyCredentialRepository,
    private val redisTemplate: RedisTemplate<String, Any>,
    private val tokenProvider: TokenProvider,
    private val userSessionService: UserSessionService,
) {
    private val log = logger()
    private val objectMapper = ObjectMapper()

    companion object {
        private const val REG_OPTIONS_PREFIX = "webauthn:reg:"
        private const val AUTH_REQUEST_PREFIX = "webauthn:auth:"
        private const val HANDSHAKE_TTL_MINUTES = 5L
    }

    fun startRegistration(
        email: String,
        fullName: String,
    ): PublicKeyCredentialCreationOptions {
        val user =
            userRepository.findByEmail(email) ?: userRepository.save(
                UserEntity(
                    email = email,
                    username = email,
                    fullName = fullName,
                ),
            )

        val options =
            relyingParty.startRegistration(
                StartRegistrationOptions.builder()
                    .user(
                        UserIdentity.builder()
                            .name(user.email!!)
                            .displayName(user.fullName!!)
                            .id(ByteArray(user.id.toString().toByteArray()))
                            .build(),
                    )
                    .build(),
            )

        storeInRedis(REG_OPTIONS_PREFIX + email, options)
        return options
    }

    @Transactional
    fun finishRegistration(
        email: String,
        responseJson: String,
    ): AuthResponseDTO {
        try {
            val options = getFromRedis(REG_OPTIONS_PREFIX + email, PublicKeyCredentialCreationOptions::class.java)

            val pkc = PublicKeyCredential.parseRegistrationResponseJson(responseJson)

            val result =
                relyingParty.finishRegistration(
                    FinishRegistrationOptions.builder()
                        .request(options)
                        .response(pkc)
                        .build(),
                )

            val user = userRepository.findByEmail(email) ?: throw BadRequestException("User not found")

            val credential =
                PasskeyCredential(
                    credentialId = result.keyId.id.bytes,
                    publicKey = result.publicKeyCose.bytes,
                    signatureCount = result.signatureCount,
                    user = user,
                )

            passkeyRepository.save(credential)

            val sessionId = userSessionService.createSession(user)
            return AuthResponseDTO(
                accessToken = tokenProvider.createToken(user, sessionId),
                tokenType = "Bearer",
            )
        } catch (e: BadRequestException) {
            failRegistration(email, e)
        } catch (e: RegistrationFailedException) {
            failRegistration(email, e)
        } catch (e: IOException) {
            failRegistration(email, e)
        }
    }

    private fun failRegistration(
        email: String,
        e: Exception,
    ): Nothing {
        log.error("Registration failed for {}: {}", email, e.message)
        throw BadRequestException("Registration failed: " + e.message, e)
    }

    fun startAuthentication(email: String): AssertionRequest {
        val request =
            relyingParty.startAssertion(
                StartAssertionOptions.builder()
                    .username(java.util.Optional.of(email))
                    .build(),
            )

        storeInRedis(AUTH_REQUEST_PREFIX + email, request)
        return request
    }

    @Transactional
    fun finishAuthentication(
        email: String,
        responseJson: String,
    ): AuthResponseDTO {
        return try {
            val request = getFromRedis(AUTH_REQUEST_PREFIX + email, AssertionRequest::class.java)
            val pkc = PublicKeyCredential.parseAssertionResponseJson(responseJson)

            val result =
                relyingParty.finishAssertion(
                    FinishAssertionOptions.builder()
                        .request(request)
                        .response(pkc)
                        .build(),
                )

            if (!result.isSuccess) {
                log.error("WebAuthn assertion failed for {}", email)
                throw BadRequestException("Authentication failed")
            }

            val user = userRepository.findByEmail(email) ?: throw BadRequestException("User not found")

            passkeyRepository.findByCredentialId(result.credentialId.bytes)?.apply {
                signatureCount = result.signatureCount
                passkeyRepository.save(this)
            }

            val sessionId = userSessionService.createSession(user)
            AuthResponseDTO(
                accessToken = tokenProvider.createToken(user, sessionId),
                tokenType = "Bearer",
            )
        } catch (e: BadRequestException) {
            failAuthentication(email, e)
        } catch (e: AssertionFailedException) {
            failAuthentication(email, e)
        } catch (e: IOException) {
            failAuthentication(email, e)
        }
    }

    private fun failAuthentication(
        email: String,
        e: Exception,
    ): Nothing {
        log.error("Authentication failed for {}: {}", email, e.message)
        throw BadRequestException("Authentication failed: " + e.message, e)
    }

    private fun storeInRedis(
        key: String,
        value: Any,
    ) {
        try {
            val json = objectMapper.writeValueAsString(value)
            redisTemplate.opsForValue().set(key, json, Duration.ofMinutes(HANDSHAKE_TTL_MINUTES))
        } catch (e: JsonProcessingException) {
            throw IllegalStateException("Failed to store WebAuthn state", e)
        } catch (e: DataAccessException) {
            throw IllegalStateException("Failed to store WebAuthn state", e)
        }
    }

    private fun <T> getFromRedis(
        key: String,
        clazz: Class<T>,
    ): T {
        val json =
            redisTemplate.opsForValue().get(key) as String?
                ?: throw BadRequestException("Handshake expired or not found")
        return try {
            objectMapper.readValue(json, clazz)
        } catch (e: JsonProcessingException) {
            throw BadRequestException("Failed to retrieve WebAuthn state: " + e.message, e)
        }
    }
}
