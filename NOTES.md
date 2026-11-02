# Ticket #161: Distributed mobile client verification handshake protocol

## Overview
Datacenter crawlers are heavily rate-limited by Cloudflare / Udemy bot mitigation. This feature establishes a secure handshake protocol allowing mobile clients on residential IPs to verify remaining coupon quantities upon deal views.

## Scope & Implementation Details
1. **Backend Endpoints (`coupon-api-service`)**:
   - `POST /api/v1/coupons/{courseId}/verification-handshake`:
     - Checks whether coupon exists and is not expired.
     - Generates cryptographically secure single-use `nonce` (`UUID.randomUUID()`) and HMAC-SHA256 signature binding `courseId:nonce:expiresAt` with 60-second TTL.
     - Stores ticket state in Redis (`cd:handshake:<nonce>`) with fallback to thread-safe in-memory cache.
     - Returns `VerificationHandshakeResponseDTO` containing `courseId`, `nonce`, `verificationUrl`, `signature`, and `expiresAt`.
   - `POST /api/v1/coupons/{courseId}/attestation`:
     - Consumes nonce atomically to prevent replay attacks.
     - Validates expiration and HMAC-SHA256 signature using constant-time digest comparison (`MessageDigest.isEqual`).
     - If mobile client attests `isValid == false` (e.g. 404 / expired), calls `CouponCleanupService.archiveAndPurgeSingleCoupon`.
     - If client reports updated `usesRemaining` or `expiredTime`, updates and persists `CouponCourseData`.
     - Returns `VerificationAttestationResponseDTO` with `accepted = true` and current `remainingUses`.

2. **Security & Configuration**:
   - Configured `custom.handshake-secret` and `custom.handshake-ttl-seconds` across `application.yml`, `application-local.yml`, `application-mobile-local.yml`, and `application-test.properties`.
   - Updated `SecurityConfig` to permit all public requests to `POST /api/v1/coupons/*/verification-handshake` and `POST /api/v1/coupons/*/attestation` while ensuring replay & signature checks protect endpoints.

3. **Domain & DTOs (`coupon-domain`)**:
   - `VerificationHandshakeResponseDTO`
   - `VerificationAttestationRequestDTO`
   - `VerificationAttestationResponseDTO`

4. **Architecture Documentation (`docs/business-logic.md`)**:
   - Added section and Mermaid sequence diagram detailing the distributed mobile client verification handshake lifecycle.

## Test Results
- `./gradlew check` (ktlintCheck + detekt + tests across all modules): **BUILD SUCCESSFUL** (108 actionable tasks, all passed)
- Comprehensive test coverage added in `ClientVerificationHandshakeServiceTest`:
  - `initiateHandshake throws ResourceNotFoundException when course does not exist`
  - `initiateHandshake throws BadRequestException when course already expired`
  - `initiateHandshake generates valid ticket with HMAC signature and nonce`
  - `verifyAttestation accepts valid attestation and updates remaining uses`
  - `verifyAttestation with dead coupon archives and purges coupon`
  - `verifyAttestation rejects replayed nonce`
  - `verifyAttestation rejects tampered signature`
- Security filter test in `SecurityConfigTest`:
  - `coupon verification handshake endpoints are public`
