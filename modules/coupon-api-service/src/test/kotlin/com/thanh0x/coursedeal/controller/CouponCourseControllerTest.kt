package com.thanh0x.coursedeal.controller

import com.thanh0x.coursedeal.dto.CouponSummaryDTO
import com.thanh0x.coursedeal.dto.PagedCouponResponseDTO
import com.thanh0x.coursedeal.repository.UserRepository
import com.thanh0x.coursedeal.security.TokenProvider
import com.thanh0x.coursedeal.service.CourseDetailService
import com.thanh0x.coursedeal.service.CourseResponseService
import com.thanh0x.coursedeal.service.UserSessionService
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(CouponCourseController::class)
@AutoConfigureMockMvc(addFilters = false)
class CouponCourseControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var courseResponseService: CourseResponseService

    @Suppress("UnusedPrivateProperty")
    @MockitoBean
    private lateinit var courseDetailService: CourseDetailService

    @Suppress("UnusedPrivateProperty")
    @MockitoBean
    private lateinit var userRepository: UserRepository

    @Suppress("UnusedPrivateProperty")
    @MockitoBean
    private lateinit var tokenProvider: TokenProvider

    @Suppress("UnusedPrivateProperty")
    @MockitoBean
    private lateinit var userSessionService: UserSessionService

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyObject(): T {
        any<T>()
        return null as T
    }

    @Test
    fun `searchCoupons returns 200 with paged search results`() {
        val response =
            PagedCouponResponseDTO(
                lastFetchTime = 123456789L,
                totalCoupon = 1,
                totalPage = 1,
                currentPage = 0,
                courses =
                    listOf(
                        CouponSummaryDTO(
                            courseId = 101,
                            title = "Complete Python Bootcamp",
                            category = "Development",
                            author = "Jose Portilla",
                            rating = 4.6f,
                            language = "English",
                        ),
                    ),
            )

        `when`(courseResponseService.searchCoupons(anyObject())).thenReturn(response)

        mockMvc.perform(
            get("/api/v1/coupons/search")
                .param("q", "bootcamp")
                .param("category", "Development")
                .param("instructor", "Jose")
                .param("minRating", "4.5")
                .param("language", "English")
                .param("sort", "rating")
                .param("page", "0")
                .param("size", "20"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalCoupon").value(1))
            .andExpect(jsonPath("$.courses[0].courseId").value(101))
            .andExpect(jsonPath("$.courses[0].title").value("Complete Python Bootcamp"))
            .andExpect(jsonPath("$.courses[0].author").value("Jose Portilla"))
    }
}
