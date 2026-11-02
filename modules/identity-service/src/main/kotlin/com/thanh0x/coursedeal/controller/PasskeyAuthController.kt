package com.thanh0x.coursedeal.controller

import com.thanh0x.coursedeal.dto.AuthResponseDTO
import com.thanh0x.coursedeal.service.PasskeyService
import com.yubico.webauthn.AssertionRequest
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Two independent two-step handshakes (WebAuthn/FIDO2), driven by a platform passkey API
 * (e.g. Android's Credential Manager, iOS's ASAuthorizationController):
 * Registration: GET options -> platform passkey prompt -> POST finish with the attestation.
 * Authentication: GET options -> platform passkey prompt -> POST finish with the assertion,
 * returning a JWT exactly like SocialAuthController.login does.
 * All four endpoints are public: they ARE the auth flow, so no Authorization header exists yet.
 */
@RestController
@RequestMapping("/api/v1/auth/passkey")
@Tag(name = "Passkey Auth", description = "WebAuthn/FIDO2 passwordless registration and login")
class PasskeyAuthController(private val passkeyService: PasskeyService) {
    @GetMapping("/registration/options")
    @Operation(
        operationId = "getPasskeyRegistrationOptions",
        summary = "Step 1 of 2: get passkey registration options",
        description =
            "Returns a PublicKeyCredentialCreationOptions challenge to pass to the platform's " +
                "passkey creation API. The result is cached server-side (Redis, 5 min TTL) keyed " +
                "by email, matched by /registration/finish.",
    )
    @ApiResponse(
        responseCode = "200",
        description = "WebAuthn PublicKeyCredentialCreationOptions JSON payload",
        content = [Content(schema = Schema(type = "string"))],
    )
    fun getRegistrationOptions(
        @Parameter(description = "Email identifying the user creating a passkey") @RequestParam email: String,
        @Parameter(description = "Display name shown by the platform's passkey UI")
        @RequestParam(defaultValue = "Course Deal User") name: String,
    ): ResponseEntity<PublicKeyCredentialCreationOptions> {
        return ResponseEntity.ok(passkeyService.startRegistration(email, name))
    }

    @PostMapping("/registration/finish")
    @Operation(
        operationId = "finishPasskeyRegistration",
        summary = "Step 2 of 2: complete passkey registration",
        description =
            "Submit the platform's attestation response (as JSON) for the challenge obtained from " +
                "/registration/options. Fails with 400 if the options have expired (5 min TTL) or " +
                "the email doesn't match. Returns a JWT on success, exactly like a login.",
    )
    fun finishRegistration(
        @Parameter(description = "Must match the email used in /registration/options") @RequestParam email: String,
        @RequestBody responseJson: String,
    ): ResponseEntity<AuthResponseDTO> {
        return ResponseEntity.ok(passkeyService.finishRegistration(email, responseJson))
    }

    @GetMapping("/authentication/options")
    @Operation(
        operationId = "getPasskeyAuthenticationOptions",
        summary = "Step 1 of 2: get passkey authentication (login) options",
        description = "Returns an AssertionRequest challenge to pass to the platform's passkey get() API.",
    )
    @ApiResponse(
        responseCode = "200",
        description = "WebAuthn AssertionRequest JSON payload",
        content = [Content(schema = Schema(type = "string"))],
    )
    fun getAuthenticationOptions(
        @Parameter(description = "Email of the user logging in") @RequestParam email: String,
    ): ResponseEntity<AssertionRequest> {
        return ResponseEntity.ok(passkeyService.startAuthentication(email))
    }

    @PostMapping("/authentication/finish")
    @Operation(
        operationId = "finishPasskeyAuthentication",
        summary = "Step 2 of 2: complete passkey authentication and get a JWT",
        description =
            "Submit the platform's assertion response (as JSON) for the challenge obtained from " +
                "/authentication/options. Returns the same AuthResponseDTO shape as social login - " +
                "use the accessToken as a Bearer token on subsequent requests.",
    )
    fun finishAuthentication(
        @Parameter(description = "Must match the email used in /authentication/options") @RequestParam email: String,
        @RequestBody responseJson: String,
    ): ResponseEntity<AuthResponseDTO> {
        return ResponseEntity.ok(passkeyService.finishAuthentication(email, responseJson))
    }
}
