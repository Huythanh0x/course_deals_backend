package com.thanh0x.coursedeal.controller

import com.thanh0x.coursedeal.model.user.UserEntity
import com.thanh0x.coursedeal.security.TokenAuthenticationFilter
import com.thanh0x.coursedeal.service.UserSessionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Session management and logout operations")
@SecurityRequirement(name = "bearerAuth")
class AuthController(private val userSessionService: UserSessionService) {
    @PostMapping("/logout", "/social/logout")
    @Operation(
        summary = "Log out of the current session",
        description = "Invalidates the active session in Redis associated with the current bearer token.",
    )
    fun logout(
        @AuthenticationPrincipal user: UserEntity,
        request: HttpServletRequest,
    ): ResponseEntity<Map<String, String>> {
        val sessionId = request.getAttribute(TokenAuthenticationFilter.SESSION_ID_ATTRIBUTE) as? String
        if (sessionId != null) {
            userSessionService.invalidateSession(sessionId, user.id)
        }
        return ResponseEntity.ok(mapOf("message" to "Logged out successfully"))
    }

    @PostMapping("/logout-all", "/social/logout-all")
    @Operation(
        summary = "Log out of all sessions",
        description = "Invalidates all active sessions in Redis for the authenticated user across all devices.",
    )
    fun logoutAll(
        @AuthenticationPrincipal user: UserEntity,
    ): ResponseEntity<Map<String, String>> {
        userSessionService.invalidateAllUserSessions(user.id)
        return ResponseEntity.ok(mapOf("message" to "Logged out of all sessions successfully"))
    }
}
