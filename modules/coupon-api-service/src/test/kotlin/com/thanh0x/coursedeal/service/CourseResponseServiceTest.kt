package com.thanh0x.coursedeal.service

import com.thanh0x.coursedeal.dto.CouponDetailDTO
import com.thanh0x.coursedeal.dto.CouponQueryDTO
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
}
