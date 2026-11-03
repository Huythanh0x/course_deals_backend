package com.thanh0x.coursedeal.controller

import com.thanh0x.coursedeal.config.ApiProperties
import com.thanh0x.coursedeal.dto.CouponSummaryDTO
import com.thanh0x.coursedeal.dto.PagedCouponResponseDTO
import com.thanh0x.coursedeal.repository.UserRepository
import com.thanh0x.coursedeal.security.TokenProvider
import com.thanh0x.coursedeal.service.CourseResponseService
import com.thanh0x.coursedeal.service.UserSessionService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(CouponCourseController::class)
@EnableConfigurationProperties(ApiProperties::class)
@AutoConfigureMockMvc(addFilters = false)
class CouponCourseControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var courseResponseService: CourseResponseService

    @Suppress("UnusedPrivateProperty")
    @MockitoBean
    private lateinit var userRepository: UserRepository

    @Suppress("UnusedPrivateProperty")
    @MockitoBean
    private lateinit var tokenProvider: TokenProvider

    @Suppress("UnusedPrivateProperty")
    @MockitoBean
    private lateinit var userSessionService: UserSessionService

    @Test
    fun `getHotDeals returns 200 OK with default parameters`() {
        val course =
            CouponSummaryDTO(
                courseId = 1,
                title = "Spring Boot 3 Masterclass",
                students = 25000,
                rating = 4.8f,
            )
        val pagedResponse =
            PagedCouponResponseDTO(
                lastFetchTime = 1700000000L,
                totalCoupon = 1L,
                totalPage = 1,
                currentPage = 0,
                courses = listOf(course),
            )

        `when`(courseResponseService.getHotDeals("", 4.7f, 0, 20)).thenReturn(pagedResponse)

        mockMvc.perform(get("/api/v1/coupons/hot-deals"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalCoupon").value(1))
            .andExpect(jsonPath("$.currentPage").value(0))
            .andExpect(jsonPath("$.courses[0].courseId").value(1))
            .andExpect(jsonPath("$.courses[0].title").value("Spring Boot 3 Masterclass"))

        verify(courseResponseService).getHotDeals("", 4.7f, 0, 20)
    }

    @Test
    fun `getHotDeals returns 200 OK when preference matches`() {
        val course =
            CouponSummaryDTO(
                courseId = 42,
                title = "Kotlin Multiplatform in Action",
                students = 12000,
                rating = 4.9f,
            )
        val pagedResponse =
            PagedCouponResponseDTO(
                lastFetchTime = 1700000000L,
                totalCoupon = 1L,
                totalPage = 1,
                currentPage = 1,
                courses = listOf(course),
            )

        `when`(courseResponseService.getHotDeals("kotlin", 4.8f, 1, 10)).thenReturn(pagedResponse)

        mockMvc.perform(
            get("/api/v1/coupons/hot-deals")
                .param("preference", "kotlin")
                .param("minRating", "4.8")
                .param("pageIndex", "1")
                .param("numberPerPage", "10"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalCoupon").value(1))
            .andExpect(jsonPath("$.currentPage").value(1))
            .andExpect(jsonPath("$.courses[0].courseId").value(42))
            .andExpect(jsonPath("$.courses[0].title").value("Kotlin Multiplatform in Action"))

        verify(courseResponseService).getHotDeals("kotlin", 4.8f, 1, 10)
    }

    @Test
    fun `getHotDeals returns 200 OK with fallback results when preference has no direct matches`() {
        val fallbackCourse =
            CouponSummaryDTO(
                courseId = 99,
                title = "Popular Web Development",
                students = 80000,
                rating = 4.8f,
            )
        val fallbackResponse =
            PagedCouponResponseDTO(
                lastFetchTime = 1700000000L,
                totalCoupon = 1L,
                totalPage = 1,
                currentPage = 0,
                courses = listOf(fallbackCourse),
            )

        `when`(courseResponseService.getHotDeals("non-existent-topic", 4.7f, 0, 20))
            .thenReturn(fallbackResponse)

        mockMvc.perform(
            get("/api/v1/coupons/hot-deals")
                .param("preference", "non-existent-topic"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalCoupon").value(1))
            .andExpect(jsonPath("$.courses[0].courseId").value(99))
            .andExpect(jsonPath("$.courses[0].title").value("Popular Web Development"))

        verify(courseResponseService).getHotDeals("non-existent-topic", 4.7f, 0, 20)
    }

    @Test
    fun `getHotDeals returns 400 Bad Request when pageIndex is negative`() {
        mockMvc.perform(
            get("/api/v1/coupons/hot-deals")
                .param("pageIndex", "-1"),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `getHotDeals returns 400 Bad Request when numberPerPage is zero or negative`() {
        mockMvc.perform(
            get("/api/v1/coupons/hot-deals")
                .param("numberPerPage", "0"),
        )
            .andExpect(status().isBadRequest)

        mockMvc.perform(
            get("/api/v1/coupons/hot-deals")
                .param("numberPerPage", "-5"),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `getHotDeals returns 400 Bad Request when minRating is negative`() {
        mockMvc.perform(
            get("/api/v1/coupons/hot-deals")
                .param("minRating", "-0.5"),
        )
            .andExpect(status().isBadRequest)
    }
}
