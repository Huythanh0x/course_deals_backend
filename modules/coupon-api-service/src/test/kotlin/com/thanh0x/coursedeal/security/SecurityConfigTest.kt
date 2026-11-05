package com.thanh0x.coursedeal.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Unlike the other MockMvc tests in this codebase, this one leaves the real security filter
 * chain enabled - the point is to verify the permitAll/authenticated split in SecurityConfig
 * actually matches intent: public coupon browsing and auth endpoints, everything else gated.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `GET coupons list is public`() {
        mockMvc.perform(get("/api/v1/coupons"))
            .andExpect(status().isOk)
    }

    @Test
    fun `GET public web home and partials are public`() {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk)
        mockMvc.perform(get("/coupons"))
            .andExpect(status().isOk)
        mockMvc.perform(get("/partials/coupons"))
            .andExpect(status().isOk)
        mockMvc.perform(get("/terms"))
            .andExpect(status().isOk)
        mockMvc.perform(get("/privacy"))
            .andExpect(status().isOk)
    }

    @Test
    fun `GET app config is public`() {
        mockMvc.perform(get("/api/v1/app/config"))
            .andExpect(status().isOk)
    }

    @Test
    fun `auth endpoints are reachable without authentication`() {
        // A malformed body may still get rejected (400) by request validation - the point of
        // this test is only that it isn't blocked by the security layer itself (401/403).
        val result =
            mockMvc.perform(
                post("/api/v1/auth/social/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"),
            ).andReturn()

        assertThat(result.response.status).isNotIn(401, 403)
    }

    @Test
    fun `preferences requires authentication`() {
        mockMvc.perform(get("/api/v1/preferences"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `device token registration endpoint is reachable without authentication`() {
        val result =
            mockMvc.perform(
                post("/api/v1/users/device-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"),
            ).andReturn()

        assertThat(result.response.status).isNotIn(401, 403)
    }

    @Test
    fun `device token deletion endpoint is reachable without authentication`() {
        val result =
            mockMvc.perform(
                delete("/api/v1/users/device-token")
                    .param("fcmToken", "sample-token"),
            ).andReturn()

        assertThat(result.response.status).isNotIn(401, 403)
    }
}
