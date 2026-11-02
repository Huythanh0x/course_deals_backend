package com.thanh0x.coursedeal.controller

import com.thanh0x.coursedeal.model.user.UserEntity
import com.thanh0x.coursedeal.repository.UserRepository
import com.thanh0x.coursedeal.service.UserSessionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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
    @DeleteMapping("/me")
    @Transactional
    @Operation(
        summary = "Delete the current authenticated user's account",
        description =
            "Permanently deletes user account, cascade relations, and invalidates all active sessions.",
    )
    fun deleteMyAccount(
        @AuthenticationPrincipal user: UserEntity,
    ): ResponseEntity<Map<String, String>> {
        userSessionService.invalidateAllUserSessions(user.id)
        userRepository.deleteById(user.id)
        return ResponseEntity.ok(mapOf("message" to "Account deleted successfully"))
    }
}
