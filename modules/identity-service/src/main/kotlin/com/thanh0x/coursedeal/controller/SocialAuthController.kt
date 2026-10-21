package com.thanh0x.coursedeal.controller

import com.thanh0x.coursedeal.dto.AuthResponseDTO
import com.thanh0x.coursedeal.dto.MockLoginRequestDTO
import com.thanh0x.coursedeal.dto.SocialLoginRequestDTO
import com.thanh0x.coursedeal.service.SocialAuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth/social")
@Tag(name = "Social Auth", description = "Password-less login via Google/Apple ID tokens")
class SocialAuthController(private val socialAuthService: SocialAuthService) {
    @PostMapping("/login")
    @Operation(
        summary = "Exchange a social ID token for a server-side JWT",
        description =
            "Verify a Google/Apple ID token (obtained client-side via that provider's SDK) and " +
                "return a server-issued JWT. Creates the user account on first login. This endpoint " +
                "is public - no Authorization header required.",
    )
    fun login(
        @Valid @RequestBody request: SocialLoginRequestDTO,
    ): ResponseEntity<AuthResponseDTO> {
        return ResponseEntity.ok(socialAuthService.login(request))
    }

    @PostMapping("/mock-login")
    @Operation(
        summary = "Local development mock authentication",
        description =
            "Directly returns a valid signed JWT for a local test user without contacting Google servers. " +
                "Only active when custom.mock-auth-enabled is true (e.g. local profile). Accepts optional " +
                "custom email, name, and fcmToken.",
    )
    fun mockLogin(
        @RequestBody(required = false) request: MockLoginRequestDTO?,
    ): ResponseEntity<AuthResponseDTO> {
        return ResponseEntity.ok(socialAuthService.loginMock(request ?: MockLoginRequestDTO()))
    }
}
