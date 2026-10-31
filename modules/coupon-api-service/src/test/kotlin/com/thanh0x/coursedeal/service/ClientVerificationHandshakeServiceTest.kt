package com.thanh0x.coursedeal.service

import com.thanh0x.coursedeal.config.ApiProperties
import com.thanh0x.coursedeal.dto.VerificationAttestationRequestDTO
import com.thanh0x.coursedeal.exception.BadRequestException
import com.thanh0x.coursedeal.exception.ResourceNotFoundException
import com.thanh0x.coursedeal.model.coupon.CouponCourseData
import com.thanh0x.coursedeal.repository.CouponCourseRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Instant

class ClientVerificationHandshakeServiceTest {
    private val couponCourseRepository = mock(CouponCourseRepository::class.java)
    private val couponCleanupService = mock(CouponCleanupService::class.java)
    private val properties =
        ApiProperties(
            refreshSecret = "test-secret-key-at-least-32-chars-long",
            handshakeSecret = "test-handshake-secret-32-chars-long",
            handshakeTtlSeconds = 60L,
        )

    private lateinit var service: ClientVerificationHandshakeService

    @BeforeEach
    fun setUp() {
        // tests in-memory fallback & logic
        service =
            ClientVerificationHandshakeService(
                couponCourseRepository = couponCourseRepository,
                properties = properties,
                couponCleanupService = couponCleanupService,
                redisTemplate = null,
            )
    }

    @Test
    fun `initiateHandshake throws ResourceNotFoundException when course does not exist`() {
        `when`(couponCourseRepository.findByCourseId(999)).thenReturn(null)

        assertThrows(ResourceNotFoundException::class.java) {
            service.initiateHandshake(999)
        }
    }

    @Test
    fun `initiateHandshake throws BadRequestException when course already expired`() {
        val expiredCourse =
            CouponCourseData(
                courseId = 100,
                couponUrl = "https://udemy.com/test",
                expiredDate = Instant.now().minusSeconds(3600),
            )
        `when`(couponCourseRepository.findByCourseId(100)).thenReturn(expiredCourse)

        val ex =
            assertThrows(BadRequestException::class.java) {
                service.initiateHandshake(100)
            }
        assertTrue(ex.message!!.contains("already expired"))
    }

    @Test
    fun `initiateHandshake generates valid ticket with HMAC signature and nonce`() {
        val activeCourse =
            CouponCourseData(
                courseId = 200,
                couponUrl = "https://udemy.com/course/test/?couponCode=FREE100",
                couponCode = "FREE100",
                expiredDate = Instant.now().plusSeconds(86400),
                usesRemaining = 500,
            )
        `when`(couponCourseRepository.findByCourseId(200)).thenReturn(activeCourse)

        val handshake = service.initiateHandshake(200)

        assertEquals(200, handshake.courseId)
        assertNotNull(handshake.nonce)
        assertFalse(handshake.nonce.isBlank())
        assertNotNull(handshake.signature)
        assertEquals("https://udemy.com/course/test/?couponCode=FREE100", handshake.verificationUrl)
        assertTrue(handshake.expiresAt > Instant.now().epochSecond)
    }

    @Test
    fun `verifyAttestation accepts valid attestation and updates remaining uses`() {
        val course =
            CouponCourseData(
                courseId = 300,
                couponUrl = "https://udemy.com/course/test300",
                couponCode = "DISC100",
                usesRemaining = 500,
                expiredDate = Instant.now().plusSeconds(86400),
            )
        `when`(couponCourseRepository.findByCourseId(300)).thenReturn(course)

        val handshake = service.initiateHandshake(300)

        val attestationReq =
            VerificationAttestationRequestDTO(
                nonce = handshake.nonce,
                signature = handshake.signature,
                isValid = true,
                usesRemaining = 420,
            )

        val response = service.verifyAttestation(300, attestationReq)

        assertTrue(response.accepted)
        assertEquals(300, response.courseId)
        assertEquals(420, response.remainingUses)
        assertEquals(420, course.usesRemaining)
        verify(couponCourseRepository).save(course)
    }

    @Test
    fun `verifyAttestation with dead coupon archives and purges coupon`() {
        val course =
            CouponCourseData(
                courseId = 400,
                couponUrl = "https://udemy.com/course/test400",
                couponCode = "DEAD100",
                usesRemaining = 10,
                expiredDate = Instant.now().plusSeconds(86400),
            )
        `when`(couponCourseRepository.findByCourseId(400)).thenReturn(course)

        val handshake = service.initiateHandshake(400)

        val attestationReq =
            VerificationAttestationRequestDTO(
                nonce = handshake.nonce,
                signature = handshake.signature,
                isValid = false,
            )

        val response = service.verifyAttestation(400, attestationReq)

        assertTrue(response.accepted)
        assertEquals(400, response.courseId)
        assertEquals(0, response.remainingUses)
        verify(couponCleanupService).archiveAndPurgeSingleCoupon(course)
    }

    @Test
    fun `verifyAttestation rejects replayed nonce`() {
        val course =
            CouponCourseData(
                courseId = 500,
                couponUrl = "https://udemy.com/course/test500",
                usesRemaining = 100,
                expiredDate = Instant.now().plusSeconds(86400),
            )
        `when`(couponCourseRepository.findByCourseId(500)).thenReturn(course)

        val handshake = service.initiateHandshake(500)

        val attestationReq =
            VerificationAttestationRequestDTO(
                nonce = handshake.nonce,
                signature = handshake.signature,
                isValid = true,
                usesRemaining = 90,
            )

        // First verification should succeed
        val response1 = service.verifyAttestation(500, attestationReq)
        assertTrue(response1.accepted)

        // Replay attempt with same nonce must be rejected
        val ex =
            assertThrows(BadRequestException::class.java) {
                service.verifyAttestation(500, attestationReq)
            }
        assertTrue(ex.message!!.contains("already-used verification nonce"))
    }

    @Test
    fun `verifyAttestation rejects tampered signature`() {
        val course =
            CouponCourseData(
                courseId = 600,
                couponUrl = "https://udemy.com/course/test600",
                usesRemaining = 100,
                expiredDate = Instant.now().plusSeconds(86400),
            )
        `when`(couponCourseRepository.findByCourseId(600)).thenReturn(course)

        val handshake = service.initiateHandshake(600)

        val attestationReq =
            VerificationAttestationRequestDTO(
                nonce = handshake.nonce,
                signature = "tampered_signature_abc123",
                isValid = true,
                usesRemaining = 50,
            )

        val ex =
            assertThrows(BadRequestException::class.java) {
                service.verifyAttestation(600, attestationReq)
            }
        assertTrue(ex.message!!.contains("Invalid or expired verification ticket or signature"))
    }
}
