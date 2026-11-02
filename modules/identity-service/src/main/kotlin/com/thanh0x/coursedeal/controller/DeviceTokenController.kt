package com.thanh0x.coursedeal.controller

import com.thanh0x.coursedeal.dto.DeviceTokenRegistrationRequestDTO
import com.thanh0x.coursedeal.dto.DeviceTokenResponseDTO
import com.thanh0x.coursedeal.model.user.DeviceTokenEntity
import com.thanh0x.coursedeal.model.user.UserEntity
import com.thanh0x.coursedeal.repository.DeviceTokenRepository
import com.thanh0x.coursedeal.repository.UserRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

/**
 * REST controller for registering, updating, and unregistering FCM device tokens.
 */
@RestController
@RequestMapping("/api/v1/users/device-token")
@Tag(name = "Device Token", description = "Endpoints for registering and managing FCM device tokens")
class DeviceTokenController(
    private val deviceTokenRepository: DeviceTokenRepository,
    private val userRepository: UserRepository,
) {
    @PostMapping
    @Operation(
        summary = "Register or update FCM device token",
        description = "Registers a device token. If authenticated, links to the user. If guest, user_id is null.",
    )
    @SecurityRequirement(name = "bearerAuth")
    fun registerDeviceToken(
        @AuthenticationPrincipal user: UserEntity?,
        @Valid @RequestBody request: DeviceTokenRegistrationRequestDTO,
    ): ResponseEntity<DeviceTokenResponseDTO> {
        val managedUser = user?.let { userRepository.findById(it.id).orElse(it) }

        val existing = deviceTokenRepository.findByFcmToken(request.fcmToken)
        val entity =
            if (existing.isPresent) {
                val tokenEntity = existing.get()
                tokenEntity.user = managedUser
                tokenEntity.platform = request.platform
                tokenEntity.updatedAt = LocalDateTime.now()
                tokenEntity
            } else {
                DeviceTokenEntity(
                    user = managedUser,
                    fcmToken = request.fcmToken,
                    platform = request.platform,
                )
            }

        val saved = deviceTokenRepository.save(entity)

        return ResponseEntity.ok(
            DeviceTokenResponseDTO(
                fcmToken = saved.fcmToken,
                platform = saved.platform,
                userId = saved.user?.id,
                registered = true,
            ),
        )
    }

    @DeleteMapping
    @Operation(
        summary = "Delete FCM device token",
        description = "Deletes a registered device token by FCM token value.",
    )
    fun deleteDeviceToken(
        @RequestParam fcmToken: String,
    ): ResponseEntity<Void> {
        deviceTokenRepository.deleteByFcmToken(fcmToken)
        return ResponseEntity.noContent().build()
    }
}
