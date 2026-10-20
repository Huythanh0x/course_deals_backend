package com.thanh0x.coursedeal.controller

import com.thanh0x.coursedeal.config.ApiProperties
import com.thanh0x.coursedeal.dto.CouponDetailDTO
import com.thanh0x.coursedeal.dto.CouponQueryDTO
import com.thanh0x.coursedeal.dto.CouponRequestDTO
import com.thanh0x.coursedeal.dto.PagedCouponResponseDTO
import com.thanh0x.coursedeal.service.CourseResponseService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Controller class for managing coupon-related endpoints.
 */
@RestController
@RequestMapping("api/v1/coupons")
@Tag(name = "Coupons", description = "Browsing, searching, and submitting Udemy coupons")
class CouponCourseController(
    private val courseResponseService: CourseResponseService,
    private val properties: ApiProperties,
) {
    @GetMapping
    @Operation(
        summary = "List/search/filter coupons (public)",
        description =
            "Paged, filterable coupon listing. All query params are optional. " +
                "Supports local filtering/sorting with fields: language, rating, students, reviews, " +
                "expiredTime, createdAt. Time fields are returned as Unix timestamps (seconds). " +
                "`rating`/`contentLength` use -1 to mean \"no filter\".",
    )
    fun listCoupons(
        @ModelAttribute queryDto: CouponQueryDTO,
    ): PagedCouponResponseDTO {
        return courseResponseService.listCoupons(queryDto)
    }

    /**
     * Creates a new coupon based on the provided Udemy coupon URL.
     * Returns 202 Accepted as validation is performed asynchronously.
     */
    @PostMapping
    @Operation(
        summary = "Submit a coupon URL for validation",
        description =
            "Returns 202 Accepted immediately - the URL is enqueued and validated asynchronously by a " +
                "background worker (JobRunr), not validated inline. Poll GET /{courseId} afterwards, or " +
                "just wait for the push notification if the user's preferences match.",
    )
    @SecurityRequirement(name = "bearerAuth")
    fun createCoupon(
        @Valid @RequestBody requestBody: CouponRequestDTO,
        request: HttpServletRequest,
    ): ResponseEntity<String> {
        courseResponseService.saveNewCouponUrlAsync(requestBody.couponUrl, request.remoteAddr)
        return ResponseEntity.accepted().body("Coupon submission received and is being processed.")
    }

    /**
     * Deletes a coupon by its course identifier.
     * <p>
     * Direct deletion is currently disabled.
     *
     * @param courseId the course identifier of the coupon to be deleted
     */
    @DeleteMapping("/{courseId}")
    @Operation(
        summary = "Delete a coupon (not implemented)",
        description = "Always throws 501 Not Implemented - direct deletion is intentionally disabled.",
    )
    @SecurityRequirement(name = "bearerAuth")
    @Suppress("UnusedParameter")
    fun deleteCoupon(
        @PathVariable("courseId") courseId: Int,
    ) {
        throw UnsupportedOperationException("Direct deletion is not allowed.")
    }

    /**
     * Triggers a refresh of the coupon data from the provider.
     * Requires a valid hash/secret to prevent unauthorized scraping load.
     */
    @PutMapping("/{courseId}/refresh")
    @Operation(
        summary = "Re-validate a coupon against the provider",
        description =
            "Requires BOTH a valid JWT AND the `secret` query param matching the server's configured " +
                "refresh secret (a separate anti-abuse gate, not something a normal client will have).",
    )
    @SecurityRequirement(name = "bearerAuth")
    fun refreshCoupon(
        @PathVariable("courseId") courseId: Int,
        @Parameter(description = "Server-side refresh secret, not the JWT") @RequestParam("secret") secret: String,
        request: HttpServletRequest,
    ): ResponseEntity<String> {
        if (properties.refreshSecret.isEmpty() || properties.refreshSecret != secret) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh secret.")
        }

        courseResponseService.refreshCouponAsync(courseId, request.remoteAddr)
        return ResponseEntity.accepted().body("Refresh request received.")
    }

    /**
     * Updates an existing coupon.
     * <p>
     * Update functionality is currently disabled. A future implementation must refresh the coupon
     * data from Udemy and include a hash check to ensure the request is valid before applying it.
     *
     * @return the updated coupon data
     */
    @PutMapping("/{courseId}")
    @Operation(
        summary = "Update a coupon (not implemented)",
        description = "Always throws 501 Not Implemented - manual updates are not yet supported.",
    )
    @SecurityRequirement(name = "bearerAuth")
    @Suppress("UnusedParameter")
    fun updateCoupon(
        @PathVariable("courseId") courseId: Int,
    ): CouponDetailDTO {
        throw UnsupportedOperationException("Manual update is not yet implemented.")
    }

    /**
     * Retrieves coupon details for a specific course identified by courseId.
     */
    @GetMapping("/{courseId}")
    @Operation(summary = "Get a single coupon by course ID (public)")
    fun getCouponDetail(
        @PathVariable("courseId") courseId: String,
    ): CouponDetailDTO {
        return courseResponseService.getCouponDetail(courseId)
    }
}
