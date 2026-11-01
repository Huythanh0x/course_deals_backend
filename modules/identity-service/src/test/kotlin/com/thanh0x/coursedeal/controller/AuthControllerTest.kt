package com.thanh0x.coursedeal.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.thanh0x.coursedeal.dto.AuthResponseDTO
import com.thanh0x.coursedeal.dto.EmailLoginRequestDTO
import com.thanh0x.coursedeal.dto.RefreshTokenRequestDTO
import com.thanh0x.coursedeal.dto.RegisterRequestDTO
import com.thanh0x.coursedeal.dto.RegisterResponseDTO
import com.thanh0x.coursedeal.dto.SendOtpRequestDTO
import com.thanh0x.coursedeal.dto.VerifyOtpRequestDTO
import com.thanh0x.coursedeal.exception.BadRequestException
import com.thanh0x.coursedeal.exception.ConflictException
import com.thanh0x.coursedeal.exception.UnauthorizedException
import com.thanh0x.coursedeal.model.user.UserEntity
import com.thanh0x.coursedeal.security.TokenAuthenticationFilter
import com.thanh0x.coursedeal.service.EmailAuthService
import com.thanh0x.coursedeal.service.UserSessionService
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
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

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var userSessionService: UserSessionService

    @MockitoBean
    private lateinit var emailAuthService: EmailAuthService

    private fun <T> nonNullAny(fallback: T): T = ArgumentMatchers.any<T>() ?: fallback

    @Test
    fun `register should return 200 OK with OTP response`() {
        val request =
            RegisterRequestDTO(
                email = "newuser@example.com",
                password = "password123",
                fullName = "New User",
            )
        val response =
            RegisterResponseDTO(
                message = "OTP sent to email",
                email = "newuser@example.com",
                expiresInSeconds = 86400,
            )
        `when`(emailAuthService.register(nonNullAny(request))).thenReturn(response)

        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("OTP sent to email"))
            .andExpect(jsonPath("$.email").value("newuser@example.com"))
            .andExpect(jsonPath("$.expiresInSeconds").value(86400))
    }

    @Test
    fun `sendOtp should return 200 OK with OTP response`() {
        val request = SendOtpRequestDTO(email = "otpuser@example.com")
        val response =
            RegisterResponseDTO(
                message = "OTP sent to email",
                email = "otpuser@example.com",
                expiresInSeconds = 86400,
            )
        `when`(emailAuthService.sendOtp(nonNullAny(request))).thenReturn(response)

        mockMvc.perform(
            post("/api/v1/auth/send-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("OTP sent to email"))
            .andExpect(jsonPath("$.email").value("otpuser@example.com"))
            .andExpect(jsonPath("$.expiresInSeconds").value(86400))
    }

    @Test
    fun `register with duplicate email should return 409 Conflict`() {
        val request =
            RegisterRequestDTO(
                email = "dup@example.com",
                password = "password123",
            )
        `when`(emailAuthService.register(nonNullAny(request)))
            .thenThrow(ConflictException("Email is already registered"))

        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `register with short password should return 400 Bad Request`() {
        val request =
            RegisterRequestDTO(
                email = "user@example.com",
                password = "123",
            )

        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `verify-otp should return 200 OK with access and refresh tokens`() {
        val request =
            VerifyOtpRequestDTO(
                email = "newuser@example.com",
                otp = "000000",
            )
        val response =
            AuthResponseDTO(
                accessToken = "access-jwt",
                refreshToken = "refresh-token-1",
                tokenType = "Bearer",
                expiresIn = "3600",
            )
        `when`(emailAuthService.verifyOtp(nonNullAny(request))).thenReturn(response)

        mockMvc.perform(
            post("/api/v1/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value("access-jwt"))
            .andExpect(jsonPath("$.refreshToken").value("refresh-token-1"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
    }

    @Test
    fun `verify-otp with invalid otp should return 400 Bad Request`() {
        val request =
            VerifyOtpRequestDTO(
                email = "newuser@example.com",
                otp = "111111",
            )
        `when`(emailAuthService.verifyOtp(nonNullAny(request)))
            .thenThrow(BadRequestException("Invalid or expired OTP"))

        mockMvc.perform(
            post("/api/v1/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `login should return 200 OK with access and refresh tokens`() {
        val request =
            EmailLoginRequestDTO(
                email = "user@example.com",
                password = "password123",
            )
        val response =
            AuthResponseDTO(
                accessToken = "access-jwt",
                refreshToken = "refresh-token-2",
                tokenType = "Bearer",
                expiresIn = "3600",
            )
        `when`(emailAuthService.login(nonNullAny(request))).thenReturn(response)

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value("access-jwt"))
            .andExpect(jsonPath("$.refreshToken").value("refresh-token-2"))
    }

    @Test
    fun `login with invalid credentials should return 401 Unauthorized`() {
        val request =
            EmailLoginRequestDTO(
                email = "user@example.com",
                password = "wrongpassword",
            )
        `when`(emailAuthService.login(nonNullAny(request)))
            .thenThrow(UnauthorizedException("Invalid email or password"))

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `refresh should return 200 OK with rotated tokens`() {
        val request = RefreshTokenRequestDTO(refreshToken = "valid-refresh-token")
        val response =
            AuthResponseDTO(
                accessToken = "new-access-jwt",
                refreshToken = "new-refresh-token",
                tokenType = "Bearer",
                expiresIn = "3600",
            )
        `when`(emailAuthService.refreshToken(nonNullAny(request))).thenReturn(response)

        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value("new-access-jwt"))
            .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
    }

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
