package com.thanh0x.coursedeal.service

import com.thanh0x.coursedeal.dto.CouponDetailDTO
import com.thanh0x.coursedeal.dto.CouponQueryDTO
import com.thanh0x.coursedeal.dto.CouponSummaryDTO
import com.thanh0x.coursedeal.exception.BadRequestException
import com.thanh0x.coursedeal.exception.ResourceNotFoundException
import com.thanh0x.coursedeal.mapper.CouponMapper
import com.thanh0x.coursedeal.model.coupon.CouponCourseData
import com.thanh0x.coursedeal.repository.CouponCourseHistoryRepository
import com.thanh0x.coursedeal.repository.CouponCourseRepository
import com.thanh0x.coursedeal.repository.ExpiredCouponRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyFloat
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.Instant

class CourseResponseServiceTest {
    private val couponCourseRepository = mock(CouponCourseRepository::class.java)
    private val expiredCouponRepository = mock(ExpiredCouponRepository::class.java)
    private val couponCourseHistoryRepository = mock(CouponCourseHistoryRepository::class.java)
    private val couponMapper = mock(CouponMapper::class.java)
    private val courseScraperService = mock(CourseScraperService::class.java)

    private val service =
        CourseResponseService(
            couponCourseRepository,
            expiredCouponRepository,
            couponCourseHistoryRepository,
            couponMapper,
            courseScraperService,
        )

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyObject(): T {
        any<T>()
        return null as T
    }

    private fun eqFloat(value: Float): Float {
        org.mockito.ArgumentMatchers.eq(value)
        return value
    }

    private fun eqString(value: String): String {
        org.mockito.ArgumentMatchers.eq(value)
        return value
    }

    @Test
    fun `listCoupons calls findAllActive when no filters or query provided`() {
        val page = PageImpl<CouponCourseData>(emptyList(), PageRequest.of(0, 10), 0)
        `when`(couponCourseRepository.findAllActive(anyObject(), anyObject())).thenReturn(page)

        val queryDto = CouponQueryDTO(pageIndex = "0", numberPerPage = "10")
        val result = service.listCoupons(queryDto)

        assertNotNull(result)
        verify(couponCourseRepository).findAllActive(anyObject(), anyObject())
    }

    @Test
    fun `listCoupons calls findByQueryAndActive when query is present`() {
        val page = PageImpl<CouponCourseData>(emptyList(), PageRequest.of(0, 10), 0)
        `when`(
            couponCourseRepository.findByQueryAndActive(
                anyString(),
                anyObject(),
                anyObject(),
            ),
        ).thenReturn(page)

        val queryDto = CouponQueryDTO(query = "kotlin", pageIndex = "0", numberPerPage = "10")
        val result = service.listCoupons(queryDto)

        assertNotNull(result)
        verify(couponCourseRepository).findByQueryAndActive(
            anyString(),
            anyObject(),
            anyObject(),
        )
    }

    @Test
    fun `listCoupons calls findWithStructuredFilters when filters are provided`() {
        val page = PageImpl<CouponCourseData>(emptyList(), PageRequest.of(0, 10), 0)
        `when`(
            couponCourseRepository.findWithStructuredFilters(
                anyFloat(),
                anyInt(),
                anyString(),
                anyString(),
                anyString(),
                anyObject(),
                anyObject(),
            ),
        ).thenReturn(page)

        val queryDto = CouponQueryDTO(category = "Development", rating = "4.0", pageIndex = "0", numberPerPage = "10")
        val result = service.listCoupons(queryDto)

        assertNotNull(result)
        verify(couponCourseRepository).findWithStructuredFilters(
            anyFloat(),
            anyInt(),
            anyString(),
            anyString(),
            anyString(),
            anyObject(),
            anyObject(),
        )
    }

    @Test
    fun `getCouponDetail throws ResourceNotFoundException if course is expired`() {
        val courseData =
            CouponCourseData(
                courseId = 123,
                title = "Expired Course",
                expiredDate = Instant.now().minusSeconds(3600),
            )
        `when`(couponCourseRepository.findByCourseId(123)).thenReturn(courseData)

        assertThrows(ResourceNotFoundException::class.java) {
            service.getCouponDetail("123")
        }
    }

    @Test
    fun `getCouponDetail returns dto if course is not expired`() {
        val courseData =
            CouponCourseData(
                courseId = 123,
                title = "Active Course",
                expiredDate = Instant.now().plusSeconds(3600),
            )
        val expectedDto = CouponDetailDTO(courseId = 123, title = "Active Course")
        `when`(couponCourseRepository.findByCourseId(123)).thenReturn(courseData)
        `when`(couponMapper.toDetailDto(courseData)).thenReturn(expectedDto)

        val result = service.getCouponDetail("123")

        assertEquals(123, result.courseId)
        assertEquals("Active Course", result.title)
    }

    @Test
    fun `getHotDeals queries global hot deals when preference is null or blank`() {
        val page = PageImpl<CouponCourseData>(emptyList(), PageRequest.of(0, 20), 0)
        `when`(couponCourseRepository.findHotDeals(eqFloat(4.7f), anyObject(), anyObject())).thenReturn(page)

        val resultNullPref = service.getHotDeals(null, 4.7f, 0, 20)
        assertNotNull(resultNullPref)
        val resultBlankPref = service.getHotDeals("   ", 4.7f, 0, 20)
        assertNotNull(resultBlankPref)

        verify(couponCourseRepository, never()).findHotDealsByPreference(
            anyString(),
            anyFloat(),
            anyObject(),
            anyObject(),
        )
    }

    @Test
    fun `getHotDeals returns preference hot deals when preference matches`() {
        val course =
            CouponCourseData(
                courseId = 101,
                title = "Kotlin Mastery",
                category = "Development",
                rating = 4.9f,
                students = 15000,
            )
        val page = PageImpl(listOf(course), PageRequest.of(0, 20), 1)
        val summaryDto = CouponSummaryDTO(courseId = 101, title = "Kotlin Mastery")

        `when`(
            couponCourseRepository.findHotDealsByPreference(
                eqString("development"),
                eqFloat(4.7f),
                anyObject(),
                anyObject(),
            ),
        ).thenReturn(page)
        `when`(couponMapper.toSummaryDto(course)).thenReturn(summaryDto)

        val result = service.getHotDeals("development", 4.7f, 0, 20)

        assertNotNull(result)
        assertEquals(1, result.totalCoupon)
        assertEquals(1, result.courses?.size)
        assertEquals(101, result.courses?.first()?.courseId)

        verify(couponCourseRepository).findHotDealsByPreference(
            eqString("development"),
            eqFloat(4.7f),
            anyObject(),
            anyObject(),
        )
        verify(couponCourseRepository, never()).findHotDeals(
            anyFloat(),
            anyObject(),
            anyObject(),
        )
    }

    @Test
    fun `getHotDeals falls back to global hot deals when preference yields empty results`() {
        val emptyPage = PageImpl<CouponCourseData>(emptyList(), PageRequest.of(0, 20), 0)
        val globalCourse =
            CouponCourseData(
                courseId = 202,
                title = "Global Top Deal",
                rating = 4.8f,
                students = 50000,
            )
        val globalPage = PageImpl(listOf(globalCourse), PageRequest.of(0, 20), 1)
        val summaryDto = CouponSummaryDTO(courseId = 202, title = "Global Top Deal")

        `when`(
            couponCourseRepository.findHotDealsByPreference(
                eqString("non-existent-topic"),
                eqFloat(4.7f),
                anyObject(),
                anyObject(),
            ),
        ).thenReturn(emptyPage)
        `when`(couponCourseRepository.findHotDeals(eqFloat(4.7f), anyObject(), anyObject())).thenReturn(globalPage)
        `when`(couponMapper.toSummaryDto(globalCourse)).thenReturn(summaryDto)

        val result = service.getHotDeals("non-existent-topic", 4.7f, 0, 20)

        assertNotNull(result)
        assertEquals(1, result.totalCoupon)
        assertEquals(202, result.courses?.first()?.courseId)

        verify(couponCourseRepository).findHotDealsByPreference(
            eqString("non-existent-topic"),
            eqFloat(4.7f),
            anyObject(),
            anyObject(),
        )
        verify(couponCourseRepository).findHotDeals(
            eqFloat(4.7f),
            anyObject(),
            anyObject(),
        )
    }

    @Test
    fun `getHotDeals throws BadRequestException on invalid parameters`() {
        assertThrows(BadRequestException::class.java) {
            service.getHotDeals(null, 4.7f, -1, 20)
        }
        assertThrows(BadRequestException::class.java) {
            service.getHotDeals(null, 4.7f, 0, 0)
        }
        assertThrows(BadRequestException::class.java) {
            service.getHotDeals(null, 4.7f, 0, -10)
        }
        assertThrows(BadRequestException::class.java) {
            service.getHotDeals(null, -0.1f, 0, 20)
        }
    }
}
