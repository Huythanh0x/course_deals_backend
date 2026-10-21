package com.thanh0x.coursedeal.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.thanh0x.coursedeal.dto.UpdateUserProfileRequestDTO
import com.thanh0x.coursedeal.model.user.UserEntity
import com.thanh0x.coursedeal.model.user.UserPreference
import com.thanh0x.coursedeal.repository.UserPreferenceRepository
import com.thanh0x.coursedeal.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.Optional

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class UserProfileControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var userRepository: UserRepository

    @MockitoBean
    private lateinit var userPreferenceRepository: UserPreferenceRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var testUser: UserEntity

    @BeforeEach
    fun setUp() {
        testUser =
            UserEntity(
                id = 1,
                email = "user@example.com",
                fullName = "John Doe",
                avatarUrl = "https://example.com/avatar.jpg",
                fcmToken = "test-fcm-token",
            )
        val auth = UsernamePasswordAuthenticationToken(testUser, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth
    }

    @Test
    fun `getProfile returns user profile with existing preferences`() {
        val pref =
            UserPreference(
                userId = 1,
                user = testUser,
                categories = mutableSetOf("Development"),
                keywords = mutableSetOf("Kotlin"),
                notificationsEnabled = true,
            )

        `when`(userRepository.findById(1)).thenReturn(Optional.of(testUser))
        `when`(userPreferenceRepository.findById(1)).thenReturn(Optional.of(pref))

        mockMvc.perform(get("/api/v1/user/profile"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.email").value("user@example.com"))
            .andExpect(jsonPath("$.fullName").value("John Doe"))
            .andExpect(jsonPath("$.avatarUrl").value("https://example.com/avatar.jpg"))
            .andExpect(jsonPath("$.fcmToken").value("test-fcm-token"))
            .andExpect(jsonPath("$.preferences.notificationsEnabled").value(true))
            .andExpect(jsonPath("$.preferences.categories[0]").value("Development"))
            .andExpect(jsonPath("$.preferences.keywords[0]").value("Kotlin"))
    }

    @Test
    fun `getProfile creates default preferences when none exist`() {
        val defaultPref =
            UserPreference(
                userId = 1,
                user = testUser,
                categories = mutableSetOf(),
                keywords = mutableSetOf(),
                notificationsEnabled = true,
            )

        `when`(userRepository.findById(1)).thenReturn(Optional.of(testUser))
        `when`(userPreferenceRepository.findById(1)).thenReturn(Optional.empty())
        `when`(userPreferenceRepository.save(any())).thenReturn(defaultPref)

        mockMvc.perform(get("/api/v1/user/profile"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.preferences.notificationsEnabled").value(true))
            .andExpect(jsonPath("$.preferences.categories").isEmpty)
    }

    @Test
    fun `updateProfile updates non-null fields and returns updated profile`() {
        val request =
            UpdateUserProfileRequestDTO(
                fullName = "Jane Doe",
                avatarUrl = "https://example.com/new-avatar.jpg",
                fcmToken = "new-fcm-token",
            )

        val updatedUser =
            UserEntity(
                id = 1,
                email = "user@example.com",
                fullName = "Jane Doe",
                avatarUrl = "https://example.com/new-avatar.jpg",
                fcmToken = "new-fcm-token",
            )

        val pref =
            UserPreference(
                userId = 1,
                user = updatedUser,
                categories = mutableSetOf("Design"),
                keywords = mutableSetOf("Figma"),
                notificationsEnabled = false,
            )

        `when`(userRepository.findById(1)).thenReturn(Optional.of(testUser))
        `when`(userRepository.save(any())).thenReturn(updatedUser)
        `when`(userPreferenceRepository.findById(1)).thenReturn(Optional.of(pref))

        mockMvc.perform(
            put("/api/v1/user/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.fullName").value("Jane Doe"))
            .andExpect(jsonPath("$.avatarUrl").value("https://example.com/new-avatar.jpg"))
            .andExpect(jsonPath("$.fcmToken").value("new-fcm-token"))
    }
}
