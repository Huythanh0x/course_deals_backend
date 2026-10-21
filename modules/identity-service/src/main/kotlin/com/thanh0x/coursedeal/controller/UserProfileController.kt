package com.thanh0x.coursedeal.controller

import com.thanh0x.coursedeal.dto.PreferenceDTO
import com.thanh0x.coursedeal.dto.UpdateUserProfileRequestDTO
import com.thanh0x.coursedeal.dto.UserProfileDTO
import com.thanh0x.coursedeal.model.user.UserEntity
import com.thanh0x.coursedeal.model.user.UserPreference
import com.thanh0x.coursedeal.repository.UserPreferenceRepository
import com.thanh0x.coursedeal.repository.UserRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * REST controller for retrieving and updating the authenticated user's profile.
 */
@RestController
@RequestMapping("/api/v1/user/profile")
@Tag(name = "User Profile", description = "Endpoints for retrieving and updating the current user's profile")
@SecurityRequirement(name = "bearerAuth")
class UserProfileController(
    private val userRepository: UserRepository,
    private val userPreferenceRepository: UserPreferenceRepository,
) {
    @GetMapping
    @Operation(
        summary = "Get the current authenticated user's profile",
        description = "Returns user identity details (name, email, avatar, fcmToken) and preferences.",
    )
    fun getProfile(
        @AuthenticationPrincipal user: UserEntity,
    ): ResponseEntity<UserProfileDTO> {
        val managedUser = userRepository.findById(user.id).orElse(user)
        val preference =
            userPreferenceRepository.findById(managedUser.id)
                .orElseGet { createDefaultPreference(managedUser) }

        return ResponseEntity.ok(mapToProfileDto(managedUser, preference))
    }

    @PutMapping
    @Operation(
        summary = "Update the current user's profile",
        description = "Updates user identity details. Non-null fields overwrite existing values.",
    )
    fun updateProfile(
        @AuthenticationPrincipal user: UserEntity,
        @Valid @RequestBody request: UpdateUserProfileRequestDTO,
    ): ResponseEntity<UserProfileDTO> {
        val managedUser = userRepository.findById(user.id).orElse(user)

        request.fullName?.let { managedUser.fullName = it }
        request.avatarUrl?.let { managedUser.avatarUrl = it }
        request.fcmToken?.let { managedUser.fcmToken = it }

        val savedUser = userRepository.save(managedUser)
        val preference =
            userPreferenceRepository.findById(savedUser.id)
                .orElseGet { createDefaultPreference(savedUser) }

        return ResponseEntity.ok(mapToProfileDto(savedUser, preference))
    }

    private fun createDefaultPreference(user: UserEntity): UserPreference {
        val managedUser = userRepository.findById(user.id).orElse(user)
        val pref =
            UserPreference(
                user = managedUser,
                notificationsEnabled = true,
            )
        return userPreferenceRepository.save(pref)
    }

    private fun mapToProfileDto(
        user: UserEntity,
        pref: UserPreference,
    ): UserProfileDTO {
        return UserProfileDTO(
            id = user.id,
            email = user.email,
            fullName = user.fullName,
            avatarUrl = user.avatarUrl,
            fcmToken = user.fcmToken,
            preferences =
                PreferenceDTO(
                    categories = pref.categories,
                    keywords = pref.keywords,
                    notificationsEnabled = pref.notificationsEnabled,
                ),
        )
    }
}
