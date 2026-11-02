package com.thanh0x.coursedeal.controller

import com.thanh0x.coursedeal.config.AppConfigProperties
import com.thanh0x.coursedeal.repository.UserRepository
import com.thanh0x.coursedeal.security.TokenProvider
import com.thanh0x.coursedeal.service.CourseResponseService
import com.thanh0x.coursedeal.service.UserSessionService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

@WebMvcTest(AppConfigController::class)
@EnableConfigurationProperties(AppConfigProperties::class)
@AutoConfigureMockMvc(addFilters = false)
class AppConfigControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

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
    fun `getAppConfig returns 200 OK with default remote configuration and feature flags`() {
        mockMvc.perform(get("/api/v1/app/config"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.minSupportedVersionCode").value(105))
            .andExpect(jsonPath("$.latestVersionCode").value(106))
            .andExpect(
                jsonPath("$.forceUpdateUrl")
                    .value("https://play.google.com/store/apps/details?id=com.thanh0x.coursedeals"),
            )
            .andExpect(jsonPath("$.featureFlags.clientVerificationEnabled").value(false))
            .andExpect(jsonPath("$.featureFlags.communityReportsEnabled").value(true))
            .andExpect(jsonPath("$.featureFlags.preferExternalBrowserCheckout").value(false))
            .andExpect(jsonPath("$.featureFlags.enrollFlowReportThreshold").value(0.3))
    }

    @Test
    fun `getAppConfig reflects custom property overrides`() {
        val customProperties =
            AppConfigProperties(
                minSupportedVersionCode = 110,
                latestVersionCode = 112,
                forceUpdateUrl = "https://apps.apple.com/app/course-deals/id123456789",
                featureFlags =
                    AppConfigProperties.FeatureFlagsProperties(
                        clientVerificationEnabled = true,
                        communityReportsEnabled = false,
                        preferExternalBrowserCheckout = true,
                        enrollFlowReportThreshold = 0.5,
                    ),
            )
        val standaloneMockMvc =
            MockMvcBuilders.standaloneSetup(AppConfigController(customProperties)).build()

        standaloneMockMvc.perform(get("/api/v1/app/config"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.minSupportedVersionCode").value(110))
            .andExpect(jsonPath("$.latestVersionCode").value(112))
            .andExpect(
                jsonPath("$.forceUpdateUrl")
                    .value("https://apps.apple.com/app/course-deals/id123456789"),
            )
            .andExpect(jsonPath("$.featureFlags.clientVerificationEnabled").value(true))
            .andExpect(jsonPath("$.featureFlags.communityReportsEnabled").value(false))
            .andExpect(jsonPath("$.featureFlags.preferExternalBrowserCheckout").value(true))
            .andExpect(jsonPath("$.featureFlags.enrollFlowReportThreshold").value(0.5))
    }
}
