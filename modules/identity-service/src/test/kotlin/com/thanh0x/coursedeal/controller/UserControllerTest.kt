package com.thanh0x.coursedeal.controller

import com.thanh0x.coursedeal.model.user.UserEntity
import com.thanh0x.coursedeal.repository.UserRepository
import com.thanh0x.coursedeal.service.UserSessionService
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.Optional

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class UserControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var userRepository: UserRepository

    @MockitoBean
    private lateinit var userSessionService: UserSessionService

    private lateinit var testUser: UserEntity

    @BeforeEach
    fun setUp() {
        testUser =
            UserEntity(
                id = 42,
                username = "deleteme",
                email = "deleteme@example.com",
            )
        val auth = UsernamePasswordAuthenticationToken(testUser, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth
    }

    @Test
    fun `acceptTerms sets termsAcceptedAt and saves user`() {
        `when`(userRepository.findById(42)).thenReturn(Optional.of(testUser))
        `when`(userRepository.save(testUser)).thenReturn(testUser)

        mockMvc.perform(post("/api/v1/users/me/accept-terms"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Terms of Service and Privacy Policy accepted"))
            .andExpect(jsonPath("$.termsAcceptedAt").isNotEmpty)

        assertNotNull(testUser.termsAcceptedAt)
        verify(userRepository).save(testUser)
    }

    @Test
    fun `deleteMyAccount deletes user and invalidates all active sessions`() {
        mockMvc.perform(delete("/api/v1/users/me"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Account deleted successfully"))

        verify(userSessionService).invalidateAllUserSessions(42)
        verify(userRepository).deleteById(42)
    }
}
