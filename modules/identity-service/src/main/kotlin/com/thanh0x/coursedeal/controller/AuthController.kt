package com.thanh0x.coursedeal.controller

import com.thanh0x.coursedeal.dto.AuthResponseDTO
import com.thanh0x.coursedeal.dto.EmailLoginRequestDTO
import com.thanh0x.coursedeal.dto.RefreshTokenRequestDTO
import com.thanh0x.coursedeal.dto.RegisterRequestDTO
import com.thanh0x.coursedeal.dto.RegisterResponseDTO
import com.thanh0x.coursedeal.dto.SendOtpRequestDTO
import com.thanh0x.coursedeal.dto.VerifyOtpRequestDTO
import com.thanh0x.coursedeal.model.user.UserEntity
import com.thanh0x.coursedeal.security.TokenAuthenticationFilter
import com.thanh0x.coursedeal.service.EmailAuthService
import com.thanh0x.coursedeal.service.UserSessionService
import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Email/password authentication, OTP verification, refresh tokens, and session logout")
class AuthController(
    private val userSessionService: UserSessionService,
    private val emailAuthService: EmailAuthService,
) {
    @PostMapping("/register")
    @Operation(
        operationId = "register",
        summary = "Register with email and password",
        description = "Stores pending registration and 24-hour OTP (000000) in Redis for email verification.",
    )
    fun register(
        @Valid @RequestBody request: RegisterRequestDTO,
    ): ResponseEntity<RegisterResponseDTO> {
        return ResponseEntity.ok(emailAuthService.register(request))
    }

    @PostMapping("/send-otp")
    @Operation(
        operationId = "sendOtp",
        summary = "Send passwordless OTP to email",
        description =
            "Stores pending passwordless verification and 24-hour OTP (000000) in Redis for email verification.",
    )
    fun sendOtp(
        @Valid @RequestBody request: SendOtpRequestDTO,
    ): ResponseEntity<RegisterResponseDTO> {
        return ResponseEntity.ok(emailAuthService.sendOtp(request))
    }

    @Hidden
    @PostMapping("/email/send-otp")
    fun sendEmailOtpAlias(
        @Valid @RequestBody request: SendOtpRequestDTO,
    ): ResponseEntity<RegisterResponseDTO> = sendOtp(request)

    @PostMapping("/verify-otp")
    @Operation(
        operationId = "verifyOtp",
        summary = "Verify registration OTP",
        description = "Verifies the 24-hour Redis OTP, creates the user account, and issues access + refresh tokens.",
    )
    fun verifyOtp(
        @Valid @RequestBody request: VerifyOtpRequestDTO,
    ): ResponseEntity<AuthResponseDTO> {
        return ResponseEntity.ok(emailAuthService.verifyOtp(request))
    }

    @PostMapping("/login")
    @Operation(
        operationId = "login",
        summary = "Log in with email and password",
        description = "Authenticates a registered user by email and password and issues access + refresh tokens.",
    )
    fun login(
        @Valid @RequestBody request: EmailLoginRequestDTO,
    ): ResponseEntity<AuthResponseDTO> {
        return ResponseEntity.ok(emailAuthService.login(request))
    }

    @PostMapping("/refresh")
    @Operation(
        operationId = "refresh",
        summary = "Refresh access token",
        description = "Rotates a valid refresh token and issues a new JWT access token and refresh token.",
    )
    fun refresh(
        @Valid @RequestBody request: RefreshTokenRequestDTO,
    ): ResponseEntity<AuthResponseDTO> {
        return ResponseEntity.ok(emailAuthService.refreshToken(request))
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        operationId = "logout",
        summary = "Log out of the current session",
        description = "Invalidates the active session in Redis associated with the current bearer token.",
    )
    fun logout(
        @Parameter(hidden = true) @AuthenticationPrincipal user: UserEntity,
        @Parameter(hidden = true) request: HttpServletRequest,
    ): ResponseEntity<Map<String, String>> {
        val sessionId = request.getAttribute(TokenAuthenticationFilter.SESSION_ID_ATTRIBUTE) as? String
        if (sessionId != null) {
            userSessionService.invalidateSession(sessionId, user.id)
        }
        return ResponseEntity.ok(mapOf("message" to "Logged out successfully"))
    }

    @Hidden
    @PostMapping("/social/logout")
    fun socialLogoutAlias(
        @Parameter(hidden = true) @AuthenticationPrincipal user: UserEntity,
        @Parameter(hidden = true) request: HttpServletRequest,
    ): ResponseEntity<Map<String, String>> = logout(user, request)

    @PostMapping("/logout-all")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        operationId = "logoutAll",
        summary = "Log out of all sessions",
        description = "Invalidates all active sessions in Redis for the authenticated user across all devices.",
    )
    fun logoutAll(
        @Parameter(hidden = true) @AuthenticationPrincipal user: UserEntity,
    ): ResponseEntity<Map<String, String>> {
        userSessionService.invalidateAllUserSessions(user.id)
        return ResponseEntity.ok(mapOf("message" to "Logged out of all sessions successfully"))
    }

    @Hidden
    @PostMapping("/social/logout-all")
    fun socialLogoutAllAlias(
        @Parameter(hidden = true) @AuthenticationPrincipal user: UserEntity,
    ): ResponseEntity<Map<String, String>> = logoutAll(user)
}
