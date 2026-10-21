package com.thanh0x.coursedeal.controller

import com.thanh0x.coursedeal.dto.PreferenceDTO
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
 * Drives which newly discovered coupons trigger a push notification for this user
 * (see NotificationService.notifyInterestedUsers): matched by category OR by keyword
 * substring in the course title, only when notificationsEnabled is true.
 */
@RestController
@RequestMapping("/api/v1/preferences")
@Tag(name = "Preferences", description = "Per-user notification preferences (categories, keywords)")
@SecurityRequirement(name = "bearerAuth")
class PreferenceController(
    private val userPreferenceRepository: UserPreferenceRepository,
    private val userRepository: UserRepository,
) {
    @GetMapping
    @Operation(
        summary = "Get the current user's notification preferences",
        description = "Creates and returns a default preference (notifications enabled, no filters) if none exists.",
    )
    fun getPreferences(
        @AuthenticationPrincipal user: UserEntity,
    ): ResponseEntity<PreferenceDTO> {
        val preference =
            userPreferenceRepository.findById(user.id)
                .orElseGet { createDefaultPreference(user) }

        return ResponseEntity.ok(mapToDto(preference))
    }

    @PutMapping
    @Operation(
        summary = "Replace the current user's notification preferences",
        description = "Fully replaces categories, keywords, and notificationsEnabled (not a partial merge).",
    )
    fun updatePreferences(
        @AuthenticationPrincipal user: UserEntity,
        @Valid @RequestBody dto: PreferenceDTO,
    ): ResponseEntity<PreferenceDTO> {
        val preference =
            userPreferenceRepository.findById(user.id)
                .orElseGet { createDefaultPreference(user) }

        preference.categories = dto.categories.toMutableSet()
        preference.keywords = dto.keywords.toMutableSet()
        preference.notificationsEnabled = dto.notificationsEnabled

        val saved = userPreferenceRepository.save(preference)
        return ResponseEntity.ok(mapToDto(saved))
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

    private fun mapToDto(pref: UserPreference): PreferenceDTO {
        return PreferenceDTO(
            categories = pref.categories,
            keywords = pref.keywords,
            notificationsEnabled = pref.notificationsEnabled,
        )
    }
}
