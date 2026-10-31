package com.thanh0x.coursedeal.service

import com.thanh0x.coursedeal.config.ApiProperties
import com.thanh0x.coursedeal.config.logger
import com.thanh0x.coursedeal.dto.VerificationAttestationRequestDTO
import com.thanh0x.coursedeal.dto.VerificationAttestationResponseDTO
import com.thanh0x.coursedeal.dto.VerificationHandshakeResponseDTO
import com.thanh0x.coursedeal.exception.BadRequestException
import com.thanh0x.coursedeal.exception.ResourceNotFoundException
import com.thanh0x.coursedeal.model.coupon.CouponCourseData
import com.thanh0x.coursedeal.repository.CouponCourseRepository
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
@Suppress("TooGenericExceptionCaught", "TooManyFunctions")
class ClientVerificationHandshakeService(
    private val couponCourseRepository: CouponCourseRepository,
    private val properties: ApiProperties,
    private val couponCleanupService: CouponCleanupService,
    private val redisTemplate: RedisTemplate<String, Any>?,
) {
    private val log = logger()

    // In-memory fallback if Redis is not configured in environment or tests
    private val inMemoryTickets = ConcurrentHashMap<String, HandshakeTicket>()

    companion object {
        private const val HMAC_ALGORITHM = "HmacSHA256"
        private const val REDIS_KEY_PREFIX = "cd:handshake:"
        private const val DEFAULT_TTL_SECONDS = 60L
        private const val SERIALIZED_PARTS_COUNT = 3
    }

    private data class HandshakeTicket(
        val nonce: String,
        val courseId: Int,
        val expiresAt: Long,
        val signature: String,
    )

    /**
     * Issues a short-lived HMAC-signed ticket allowing mobile clients to probe course availability.
     */
    fun initiateHandshake(courseId: Int): VerificationHandshakeResponseDTO {
        val coupon =
            couponCourseRepository.findByCourseId(courseId)
                ?: throw ResourceNotFoundException("Course coupon not found for id: $courseId")

        val now = Instant.now()
        if (coupon.expiredDate != null && coupon.expiredDate!!.isBefore(now)) {
            throw BadRequestException("Coupon for course $courseId has already expired")
        }

        val nonce = UUID.randomUUID().toString()
        val ttl = if (properties.handshakeTtlSeconds > 0) properties.handshakeTtlSeconds else DEFAULT_TTL_SECONDS
        val expiresAt = now.plusSeconds(ttl).epochSecond
        val signature = computeHmac(courseId, nonce, expiresAt)

        // Store ticket for single-use replay protection
        storeTicket(nonce, courseId, expiresAt, signature, ttl)

        val verificationUrl = buildVerificationUrl(coupon)

        return VerificationHandshakeResponseDTO(
            courseId = courseId,
            nonce = nonce,
            verificationUrl = verificationUrl,
            signature = signature,
            expiresAt = expiresAt,
        )
    }

    /**
     * Verifies attestation callback from mobile client, checks signature, validates single-use nonce,
     * and updates course remaining counts or archives if dead.
     */
    @Transactional
    fun verifyAttestation(
        courseId: Int,
        request: VerificationAttestationRequestDTO,
    ): VerificationAttestationResponseDTO {
        val coupon =
            couponCourseRepository.findByCourseId(courseId)
                ?: throw ResourceNotFoundException("Course coupon not found for id: $courseId")

        validateTicketAndSignature(courseId, request)

        return processAttestationOutcome(coupon, request)
    }

    private fun validateTicketAndSignature(
        courseId: Int,
        request: VerificationAttestationRequestDTO,
    ) {
        val ticket =
            retrieveAndConsumeTicket(request.nonce)
                ?: throw BadRequestException("Invalid, expired, or already-used verification nonce")

        val now = Instant.now().epochSecond
        val expectedSignature = computeHmac(courseId, request.nonce, ticket.expiresAt)
        val validSignatures =
            MessageDigest.isEqual(
                ticket.signature.toByteArray(StandardCharsets.UTF_8),
                request.signature.toByteArray(StandardCharsets.UTF_8),
            ) &&
                MessageDigest.isEqual(
                    expectedSignature.toByteArray(StandardCharsets.UTF_8),
                    request.signature.toByteArray(StandardCharsets.UTF_8),
                )

        if (ticket.courseId != courseId || ticket.expiresAt < now || !validSignatures) {
            throw BadRequestException("Invalid or expired verification ticket or signature")
        }
    }

    private fun processAttestationOutcome(
        coupon: CouponCourseData,
        request: VerificationAttestationRequestDTO,
    ): VerificationAttestationResponseDTO {
        if (!request.isValid) {
            log.info("Client reported dead coupon for courseId: {}. Archiving and purging.", coupon.courseId)
            couponCleanupService.archiveAndPurgeSingleCoupon(coupon)
            return VerificationAttestationResponseDTO(
                courseId = coupon.courseId,
                accepted = true,
                remainingUses = 0,
                message = "Course coupon marked expired based on client attestation",
            )
        }

        var updated = false
        val usesRemaining = request.usesRemaining
        if (usesRemaining != null && usesRemaining != coupon.usesRemaining) {
            coupon.usesRemaining = usesRemaining
            updated = true
        }

        val expiredTime = request.expiredTime
        if (expiredTime != null) {
            val clientExpiredInstant = Instant.ofEpochSecond(expiredTime)
            if (coupon.expiredDate == null || clientExpiredInstant.isBefore(coupon.expiredDate)) {
                coupon.expiredDate = clientExpiredInstant
                updated = true
            }
        }

        if (updated) {
            couponCourseRepository.save(coupon)
            log.info(
                "Course coupon {} updated via client attestation: remainingUses={}, expiredDate={}",
                coupon.courseId,
                coupon.usesRemaining,
                coupon.expiredDate,
            )
        }

        return VerificationAttestationResponseDTO(
            courseId = coupon.courseId,
            accepted = true,
            remainingUses = coupon.usesRemaining,
            message = "Client attestation successfully verified and applied",
        )
    }

    private fun storeTicket(
        nonce: String,
        courseId: Int,
        expiresAt: Long,
        signature: String,
        ttlSeconds: Long,
    ) {
        val key = REDIS_KEY_PREFIX + nonce
        val ticket = HandshakeTicket(nonce, courseId, expiresAt, signature)
        val storedInRedis =
            try {
                if (redisTemplate != null) {
                    val serialized = "$courseId:$expiresAt:$signature"
                    redisTemplate.opsForValue().set(key, serialized, Duration.ofSeconds(ttlSeconds))
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                log.warn("Redis unavailable for handshake storage, falling back to in-memory: {}", e.message)
                false
            }

        if (!storedInRedis) {
            cleanExpiredInMemoryTickets()
            inMemoryTickets[nonce] = ticket
        }
    }

    private fun retrieveAndConsumeTicket(nonce: String): HandshakeTicket? {
        val key = REDIS_KEY_PREFIX + nonce
        try {
            val ticketFromRedis = consumeTicketFromRedis(key, nonce)
            if (ticketFromRedis != null) {
                return ticketFromRedis
            }
        } catch (e: Exception) {
            log.warn("Redis unavailable during ticket consumption, checking in-memory: {}", e.message)
        }

        return inMemoryTickets.remove(nonce)
    }

    private fun consumeTicketFromRedis(
        key: String,
        nonce: String,
    ): HandshakeTicket? {
        val template = redisTemplate ?: return null
        val value = template.opsForValue().get(key) as? String
        val parts = value?.split(":", limit = SERIALIZED_PARTS_COUNT)
        return if (parts?.size == SERIALIZED_PARTS_COUNT) {
            template.delete(key) // Single-use consumption
            HandshakeTicket(
                nonce = nonce,
                courseId = parts[0].toInt(),
                expiresAt = parts[1].toLong(),
                signature = parts[2],
            )
        } else {
            null
        }
    }

    private fun cleanExpiredInMemoryTickets() {
        val now = Instant.now().epochSecond
        inMemoryTickets.entries.removeIf { it.value.expiresAt < now }
    }

    private fun computeHmac(
        courseId: Int,
        nonce: String,
        expiresAt: Long,
    ): String {
        val secret = getEffectiveSecret()
        val data = "$courseId:$nonce:$expiresAt"
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        val secretKey = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), HMAC_ALGORITHM)
        mac.init(secretKey)
        val bytes = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun getEffectiveSecret(): String {
        return when {
            properties.handshakeSecret.isNotBlank() -> properties.handshakeSecret
            properties.refreshSecret.isNotBlank() -> properties.refreshSecret
            else -> "cd_default_mobile_handshake_hmac_secret_key_32_chars"
        }
    }

    private fun buildVerificationUrl(coupon: CouponCourseData): String {
        val rawUrl = coupon.couponUrl ?: ""
        val couponCode = coupon.couponCode ?: ""
        return if (rawUrl.isNotBlank()) {
            rawUrl
        } else {
            "https://www.udemy.com/api-2.0/course-landing-components/${coupon.courseId}/me/?couponCode=$couponCode"
        }
    }
}
