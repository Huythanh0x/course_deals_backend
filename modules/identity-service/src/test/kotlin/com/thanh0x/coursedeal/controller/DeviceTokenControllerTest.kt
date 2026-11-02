package com.thanh0x.coursedeal.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.thanh0x.coursedeal.dto.DeviceTokenRegistrationRequestDTO
import com.thanh0x.coursedeal.model.user.DeviceTokenEntity
import com.thanh0x.coursedeal.model.user.PlatformType
import com.thanh0x.coursedeal.model.user.UserEntity
import com.thanh0x.coursedeal.repository.DeviceTokenRepository
import com.thanh0x.coursedeal.repository.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.Optional

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class DeviceTokenControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var deviceTokenRepository: DeviceTokenRepository

    @MockitoBean
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `HP-01 anonymous device token registration succeeds with userId null`() {
        val request =
            DeviceTokenRegistrationRequestDTO(
                fcmToken = "anon-token-123",
                platform = PlatformType.ANDROID,
            )

        val savedEntity =
            DeviceTokenEntity(
                id = 1L,
                user = null,
                fcmToken = "anon-token-123",
                platform = PlatformType.ANDROID,
            )

        `when`(deviceTokenRepository.findByFcmToken("anon-token-123")).thenReturn(Optional.empty())
        `when`(deviceTokenRepository.save(any())).thenReturn(savedEntity)

        mockMvc.perform(
            post("/api/v1/users/device-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.fcmToken").value("anon-token-123"))
            .andExpect(jsonPath("$.platform").value("ANDROID"))
            .andExpect(jsonPath("$.userId").doesNotExist())
            .andExpect(jsonPath("$.registered").value(true))
    }

    @Test
    fun `HP-02 authenticated device token registration links to user`() {
        val authUser = UserEntity(id = 42, username = "testuser", email = "test@example.com")
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(authUser, null, emptyList())

        val request =
            DeviceTokenRegistrationRequestDTO(
                fcmToken = "auth-token-456",
                platform = PlatformType.IOS,
            )

        val savedEntity =
            DeviceTokenEntity(
                id = 2L,
                user = authUser,
                fcmToken = "auth-token-456",
                platform = PlatformType.IOS,
            )

        `when`(userRepository.findById(42)).thenReturn(Optional.of(authUser))
        `when`(deviceTokenRepository.findByFcmToken("auth-token-456")).thenReturn(Optional.empty())
        `when`(deviceTokenRepository.save(any())).thenReturn(savedEntity)

        mockMvc.perform(
            post("/api/v1/users/device-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.fcmToken").value("auth-token-456"))
            .andExpect(jsonPath("$.platform").value("IOS"))
            .andExpect(jsonPath("$.userId").value(42))
            .andExpect(jsonPath("$.registered").value(true))
    }

    @Test
    fun `HP-03 re-registration updates existing token platform and user without error`() {
        val authUser = UserEntity(id = 7, username = "existinguser")
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(authUser, null, emptyList())

        val existingEntity =
            DeviceTokenEntity(
                id = 10L,
                user = null,
                fcmToken = "existing-token-789",
                platform = PlatformType.ANDROID,
            )

        val updatedEntity =
            DeviceTokenEntity(
                id = 10L,
                user = authUser,
                fcmToken = "existing-token-789",
                platform = PlatformType.IOS,
            )

        val request =
            DeviceTokenRegistrationRequestDTO(
                fcmToken = "existing-token-789",
                platform = PlatformType.IOS,
            )

        `when`(userRepository.findById(7)).thenReturn(Optional.of(authUser))
        `when`(deviceTokenRepository.findByFcmToken("existing-token-789")).thenReturn(Optional.of(existingEntity))
        `when`(deviceTokenRepository.save(any())).thenReturn(updatedEntity)

        mockMvc.perform(
            post("/api/v1/users/device-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.fcmToken").value("existing-token-789"))
            .andExpect(jsonPath("$.platform").value("IOS"))
            .andExpect(jsonPath("$.userId").value(7))
            .andExpect(jsonPath("$.registered").value(true))
    }

    @Test
    fun `HP-04 delete device token returns 204 No Content`() {
        `when`(deviceTokenRepository.deleteByFcmToken("token-to-delete")).thenReturn(1)

        mockMvc.perform(
            delete("/api/v1/users/device-token")
                .param("fcmToken", "token-to-delete"),
        )
            .andExpect(status().isNoContent)

        verify(deviceTokenRepository).deleteByFcmToken("token-to-delete")
    }

    @Test
    fun `FP-01 blank fcmToken returns 400 Bad Request`() {
        val invalidJson = """{"fcmToken": "", "platform": "ANDROID"}"""

        mockMvc.perform(
            post("/api/v1/users/device-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `FP-02 missing or invalid platform returns 400 Bad Request`() {
        val invalidPlatformJson = """{"fcmToken": "valid-token", "platform": "UNKNOWN_OS"}"""

        mockMvc.perform(
            post("/api/v1/users/device-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidPlatformJson),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `FP-03 missing fcmToken parameter on delete returns 400 Bad Request`() {
        mockMvc.perform(delete("/api/v1/users/device-token"))
            .andExpect(status().isBadRequest)
    }
}
