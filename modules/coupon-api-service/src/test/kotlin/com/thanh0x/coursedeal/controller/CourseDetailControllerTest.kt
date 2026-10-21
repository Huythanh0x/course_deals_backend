package com.thanh0x.coursedeal.controller

import com.thanh0x.coursedeal.dto.CourseDetailDTO
import com.thanh0x.coursedeal.repository.UserRepository
import com.thanh0x.coursedeal.security.TokenProvider
import com.thanh0x.coursedeal.service.CourseDetailService
import com.thanh0x.coursedeal.service.CourseResponseService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * A web-layer slice test: only CourseDetailController + MVC infra load, CourseDetailService is
 * mocked - the point here is purely the controller's request/response mapping (SecurityConfig's
 * own test covers the actual auth rules).
 *
 * @WebMvcTest still instantiates two things beyond CourseDetailController itself, even with
 * addFilters=false (that flag only skips applying filters to requests, not constructing their
 * beans):
 * - SecurityConfig's Filter bean chain: TokenAuthenticationFilter needs TokenProvider + UserRepository.
 * - CouponCourseController: it carries @EnableConfigurationProperties directly on the class, and
 *   that @Import-based registration bypasses @WebMvcTest's normal controller-scanning filter, so
 *   its CourseResponseService dependency (which needs a real CouponCourseRepository/DataSource)
 *   gets pulled in too.
 * All four are mocked below purely to satisfy the bean graph - none are exercised by these tests.
 */
@WebMvcTest(CourseDetailController::class)
@AutoConfigureMockMvc(addFilters = false)
class CourseDetailControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var courseDetailService: CourseDetailService

    @Suppress("UnusedPrivateProperty")
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
    private lateinit var userSessionService: com.thanh0x.coursedeal.service.UserSessionService

    @Test
    fun `getCourseDetails returns 200 with the course when the service finds it`() {
        `when`(courseDetailService.getCourseDetails(123, null)).thenReturn(
            CourseDetailDTO(courseId = 123, title = "Kotlin Coroutines Masterclass"),
        )

        mockMvc.perform(get("/api/v1/coupons/123/details"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.courseId").value(123))
            .andExpect(jsonPath("$.title").value("Kotlin Coroutines Masterclass"))
    }

    @Test
    fun `getCourseDetails returns 404 when the service finds nothing`() {
        `when`(courseDetailService.getCourseDetails(999, null)).thenReturn(null)

        mockMvc.perform(get("/api/v1/coupons/999/details"))
            .andExpect(status().isNotFound)
    }
}
