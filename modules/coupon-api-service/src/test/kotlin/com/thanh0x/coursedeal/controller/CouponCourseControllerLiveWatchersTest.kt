package com.thanh0x.coursedeal.controller

import com.thanh0x.coursedeal.exception.ResourceNotFoundException
import com.thanh0x.coursedeal.repository.UserRepository
import com.thanh0x.coursedeal.security.TokenProvider
import com.thanh0x.coursedeal.service.CourseResponseService
import com.thanh0x.coursedeal.service.LiveWatchersService
import com.thanh0x.coursedeal.service.UserSessionService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@WebMvcTest(CouponCourseController::class)
@AutoConfigureMockMvc(addFilters = false)
class CouponCourseControllerLiveWatchersTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var liveWatchersService: LiveWatchersService

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
    private lateinit var userSessionService: UserSessionService

    @Test
    fun `streamLiveWatchers returns 200 with text-event-stream when course exists`() {
        val emitter = SseEmitter(10000L)
        `when`(liveWatchersService.subscribe(101)).thenReturn(emitter)

        mockMvc.perform(
            get("/api/v1/coupons/101/live-watchers")
                .accept(MediaType.TEXT_EVENT_STREAM),
        )
            .andExpect(status().isOk)
            .andExpect(request().asyncStarted())
    }

    @Test
    fun `streamLiveWatchers returns 404 when course is not found or expired`() {
        `when`(liveWatchersService.subscribe(999)).thenThrow(ResourceNotFoundException("Course id not found: 999"))

        mockMvc.perform(
            get("/api/v1/coupons/999/live-watchers")
                .accept(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isNotFound)
    }
}
