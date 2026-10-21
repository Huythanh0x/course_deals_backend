package com.thanh0x.coursedeal.controller

import com.thanh0x.coursedeal.model.user.UserEntity
import com.thanh0x.coursedeal.security.TokenAuthenticationFilter
import com.thanh0x.coursedeal.service.UserSessionService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AuthControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var userSessionService: UserSessionService

    @Test
    fun `logout should invalidate current session and return 200 OK`() {
        val user = UserEntity(id = 1, email = "user@example.com")
        val auth = UsernamePasswordAuthenticationToken(user, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth

        mockMvc.perform(
            post("/api/v1/auth/logout")
                .requestAttr(TokenAuthenticationFilter.SESSION_ID_ATTRIBUTE, "test-session-id"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Logged out successfully"))

        verify(userSessionService).invalidateSession("test-session-id", 1)
    }

    @Test
    fun `logout-all should invalidate all user sessions and return 200 OK`() {
        val user = UserEntity(id = 2, email = "user2@example.com")
        val auth = UsernamePasswordAuthenticationToken(user, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth

        mockMvc.perform(
            post("/api/v1/auth/logout-all"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Logged out of all sessions successfully"))

        verify(userSessionService).invalidateAllUserSessions(2)
    }
}
