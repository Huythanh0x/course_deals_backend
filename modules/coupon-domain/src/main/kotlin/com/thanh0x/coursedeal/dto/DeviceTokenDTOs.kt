package com.thanh0x.coursedeal.dto

import com.thanh0x.coursedeal.model.user.PlatformType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Schema(description = "Request body for registering or updating an FCM device token")
data class DeviceTokenRegistrationRequestDTO(
    @field:NotBlank(message = "FCM token must not be blank")
    @Schema(
        description = "FCM device registration token",
        example = "fcm_token_sample_abc123",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val fcmToken: String,
    @field:NotNull(message = "Platform must not be null")
    @Schema(
        description = "Client device platform",
        example = "ANDROID",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val platform: PlatformType,
)

@Schema(description = "Response returned after device token registration")
data class DeviceTokenResponseDTO(
    @Schema(description = "FCM device registration token", example = "fcm_token_sample_abc123")
    val fcmToken: String,
    @Schema(description = "Client device platform", example = "ANDROID")
    val platform: PlatformType,
    @Schema(description = "User ID if registered to an authenticated user, or null if guest", example = "42")
    val userId: Int?,
    @Schema(description = "Indicates whether the device token is registered", example = "true")
    val registered: Boolean,
)

@Schema(description = "Request body for topic subscription operations")
data class TopicSubscriptionRequestDTO(
    @field:NotBlank(message = "FCM token must not be blank")
    @Schema(
        description = "FCM device registration token",
        example = "fcm_token_sample_abc123",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val fcmToken: String,
    @field:NotBlank(message = "Topic name must not be blank")
    @Schema(
        description = "Topic name to subscribe or unsubscribe",
        example = "deals_all",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val topic: String,
)
