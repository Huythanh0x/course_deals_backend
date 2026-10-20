package com.thanh0x.coursedeal.service

import com.thanh0x.coursedeal.model.coupon.CouponCourseData
import com.thanh0x.coursedeal.model.coupon.ExpiredCourseData
import com.thanh0x.coursedeal.repository.CouponCourseRepository
import com.thanh0x.coursedeal.repository.ExpiredCouponRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Instant

class CouponCleanupServiceTest {
    private val couponCourseRepository = mock(CouponCourseRepository::class.java)
    private val expiredCouponRepository = mock(ExpiredCouponRepository::class.java)

    private val service = CouponCleanupService(couponCourseRepository, expiredCouponRepository)

    @Test
    fun `archiveAndPurgeExpiredCoupons returns 0 when no expired coupons found`() {
        val now = Instant.now()
        `when`(couponCourseRepository.findByExpiredDateLessThanEqual(now)).thenReturn(emptyList())

        val result = service.archiveAndPurgeExpiredCoupons(now)

        assertEquals(0, result)
        verify(couponCourseRepository, never()).deleteAllCouponsByUrl(org.mockito.ArgumentMatchers.anySet())
    }

    @Test
    fun `archiveAndPurgeExpiredCoupons archives and deletes expired coupons`() {
        val now = Instant.now()
        val expired1 =
            CouponCourseData(
                courseId = 101,
                title = "Course 1",
                couponUrl = "https://example.com/c1",
                expiredDate = now.minusSeconds(3600),
            )
        val expired2 =
            CouponCourseData(
                courseId = 102,
                title = "Course 2",
                couponUrl = "https://example.com/c2",
                expiredDate = now.minusSeconds(1800),
            )

        `when`(couponCourseRepository.findByExpiredDateLessThanEqual(now)).thenReturn(listOf(expired1, expired2))
        `when`(expiredCouponRepository.findAllCouponUrls()).thenReturn(setOf("https://example.com/c2"))

        val count = service.archiveAndPurgeExpiredCoupons(now)

        assertEquals(2, count)

        // c1 should be saved as new expired entry
        @Suppress("UNCHECKED_CAST")
        val captor = ArgumentCaptor.forClass(List::class.java as Class<List<ExpiredCourseData>>)
        verify(expiredCouponRepository, times(1)).saveAll(captor.capture())
        val savedList = captor.value
        assertEquals(1, savedList.size)
        assertEquals("https://example.com/c1", savedList[0].couponUrl)
        assertEquals(101, savedList[0].courseId)

        // c2 should update its timestamp
        verify(expiredCouponRepository, times(1)).updateUpdatedAtForUrls(setOf("https://example.com/c2"))

        // Both URLs should be purged from couponCourseRepository
        verify(couponCourseRepository, times(1)).deleteAllCouponsByUrl(
            setOf("https://example.com/c1", "https://example.com/c2"),
        )
    }

    @Test
    fun `archiveAndPurgeSingleCoupon deletes from active and archives to expired repo`() {
        val coupon =
            CouponCourseData(
                courseId = 200,
                title = "Single Course",
                couponUrl = "https://example.com/single",
            )
        `when`(expiredCouponRepository.findByCouponUrl("https://example.com/single")).thenReturn(null)

        service.archiveAndPurgeSingleCoupon(coupon)

        verify(expiredCouponRepository, times(1)).save(org.mockito.ArgumentMatchers.any(ExpiredCourseData::class.java))
        verify(couponCourseRepository, times(1)).deleteByCouponUrl("https://example.com/single")
    }

    @Test
    fun `recordExpiredUrl updates timestamp if already exists`() {
        val existing = ExpiredCourseData(couponUrl = "https://example.com/existing")
        `when`(expiredCouponRepository.findByCouponUrl("https://example.com/existing")).thenReturn(existing)

        service.recordExpiredUrl("https://example.com/existing")

        verify(expiredCouponRepository, times(1)).updateUpdatedAtForUrls(setOf("https://example.com/existing"))
    }
}
