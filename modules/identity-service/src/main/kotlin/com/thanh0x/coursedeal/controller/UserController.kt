package com.thanh0x.coursedeal.controller

import com.thanh0x.coursedeal.model.user.UserEntity
import com.thanh0x.coursedeal.repository.UserRepository
import com.thanh0x.coursedeal.service.UserSessionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * REST controller for user account lifecycle management (e.g. account deletion / GDPR).
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "Endpoints for managing user accounts")
@SecurityRequirement(name = "bearerAuth")
class UserController(
    private val userRepository: UserRepository,
    private val userSessionService: UserSessionService,
) {
    @PostMapping("/me/accept-terms")
    @Transactional
    @Operation(
        operationId = "acceptTerms",
        summary = "Accept Terms of Service and Privacy Policy",
        description =
            "Records the UTC timestamp when the authenticated user accepted Terms of Service and Privacy Policy.",
    )
    fun acceptTerms(
        @Parameter(hidden = true) @AuthenticationPrincipal user: UserEntity,
    ): ResponseEntity<Map<String, String>> {
        val managedUser = userRepository.findById(user.id).orElse(user)
        if (managedUser.termsAcceptedAt == null) {
            managedUser.termsAcceptedAt = LocalDateTime.now(ZoneOffset.UTC)
            userRepository.save(managedUser)
        }
        return ResponseEntity.ok(
            mapOf(
                "message" to "Terms of Service and Privacy Policy accepted",
                "termsAcceptedAt" to managedUser.termsAcceptedAt.toString(),
            ),
        )
    }

    @DeleteMapping("/me")
    @Transactional
    @Operation(
        operationId = "deleteMyAccount",
        summary = "Delete the current authenticated user's account",
        description =
            "Permanently deletes user account, cascade relations, and invalidates all active sessions.",
    )
    fun deleteMyAccount(
        @Parameter(hidden = true) @AuthenticationPrincipal user: UserEntity,
    ): ResponseEntity<Map<String, String>> {
        userSessionService.invalidateAllUserSessions(user.id)
        userRepository.deleteById(user.id)
        return ResponseEntity.ok(mapOf("message" to "Account deleted successfully"))
    }
}
