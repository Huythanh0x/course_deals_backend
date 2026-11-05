package com.thanh0x.coursedeal.controller

import com.thanh0x.coursedeal.dto.CouponDetailDTO
import com.thanh0x.coursedeal.dto.CouponQueryDTO
import com.thanh0x.coursedeal.dto.CouponSummaryDTO
import com.thanh0x.coursedeal.dto.PagedCouponResponseDTO
import com.thanh0x.coursedeal.model.coupon.CourseLevel
import com.thanh0x.coursedeal.repository.UserRepository
import com.thanh0x.coursedeal.security.TokenProvider
import com.thanh0x.coursedeal.service.CourseResponseService
import com.thanh0x.coursedeal.service.UserSessionService
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(PublicWebController::class)
@AutoConfigureMockMvc(addFilters = false)
class PublicWebControllerTest {
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

    private val sampleSummary =
        CouponSummaryDTO(
            courseId = 7404801,
            title = "Mastering Kotlin Multiplatform & Spring Boot",
            category = "Development",
            level = CourseLevel.ALL_LEVELS,
            language = "English",
            author = "Vo Huy Thanh",
            rating = 4.8f,
            reviews = 320,
            students = 15400,
            previewImage = "https://img-c.udemycdn.com/course/750x422/7404801.jpg",
            expiredTime = 1900000000L,
            createdAt = 1800000000L,
            isNew = true,
        )

    private val samplePagedResponse =
        PagedCouponResponseDTO(
            lastFetchTime = System.currentTimeMillis(),
            totalCoupon = 1,
            totalPage = 2,
            currentPage = 0,
            courses = listOf(sampleSummary),
        )

    @Test
    fun `GET root renders SSR home page with hot deals and coupon grid`() {
        val defaultWebQuery = CouponQueryDTO(numberPerPage = "24")
        `when`(courseResponseService.listCoupons(defaultWebQuery)).thenReturn(samplePagedResponse)
        `when`(courseResponseService.getHotDeals(null, 4.5f, 0, 8))
            .thenReturn(samplePagedResponse)

        mockMvc.perform(get("/"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("CourseDeals")))
            .andExpect(content().string(containsString("Hot Deals")))
            .andExpect(content().string(containsString("Mastering Kotlin Multiplatform &amp; Spring Boot")))
            .andExpect(content().string(containsString("/app")))
            .andExpect(content().string(containsString("#features")))
            .andExpect(content().string(containsString("#platforms")))
            .andExpect(content().string(containsString("#faq")))
            .andExpect(content().string(containsString("github.com/Huythanh0x")))
            .andExpect(content().string(containsString("huythanh0x@gmail.com")))
    }

    @Test
    fun `GET terms and privacy render legal SaaS pages`() {
        mockMvc.perform(get("/terms"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("Terms of Service")))
            .andExpect(content().string(containsString("huythanh0x@gmail.com")))

        mockMvc.perform(get("/privacy"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("Privacy Policy")))
            .andExpect(content().string(containsString("huythanh0x@gmail.com")))
    }

    @Test
    fun `GET partials coupons returns HTMX partial fragment`() {
        val filterQuery =
            CouponQueryDTO(
                category = "Development",
                query = "Kotlin",
                numberPerPage = "24",
            )
        `when`(courseResponseService.listCoupons(filterQuery)).thenReturn(samplePagedResponse)

        mockMvc.perform(
            get("/partials/coupons")
                .param("category", "Development")
                .param("query", "Kotlin"),
        )
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("Mastering Kotlin Multiplatform &amp; Spring Boot")))
            .andExpect(content().string(containsString("Development")))
    }

    @Test
    fun `GET course detail renders OpenGraph metadata and Udemy redeem link`() {
        val sampleDetail =
            CouponDetailDTO(
                courseId = 7404801,
                category = "Development",
                subCategory = "Mobile Development",
                title = "Mastering Kotlin Multiplatform & Spring Boot",
                contentLength = 420,
                level = CourseLevel.ALL_LEVELS,
                author = "Vo Huy Thanh",
                rating = 4.8f,
                reviews = 320,
                students = 15400,
                couponCode = "KOTLIN2026FREE",
                previewImage = "https://img-c.udemycdn.com/course/750x422/7404801.jpg",
                couponUrl = "https://www.udemy.com/course/kmp/?couponCode=KOTLIN2026FREE",
                expiredTime = 1900000000L,
                usesRemaining = 850,
                heading = "Build production apps with Kotlin",
                description = "<p>Learn full-stack Kotlin engineering.</p>",
                language = "English",
                isNew = true,
            )
        `when`(courseResponseService.getCouponDetail("7404801")).thenReturn(sampleDetail)

        mockMvc.perform(get("/courses/7404801"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("og:title")))
            .andExpect(content().string(containsString("KOTLIN2026FREE")))
            .andExpect(content().string(containsString("Redeem 100% Off on Udemy")))
    }
}
