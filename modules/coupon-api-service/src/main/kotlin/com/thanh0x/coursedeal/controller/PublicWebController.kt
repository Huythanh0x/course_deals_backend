package com.thanh0x.coursedeal.controller

import com.thanh0x.coursedeal.dto.CouponQueryDTO
import com.thanh0x.coursedeal.service.CourseResponseService
import com.thanh0x.coursedeal.web.PublicLegalViews
import com.thanh0x.coursedeal.web.PublicWebViews
import io.swagger.v3.oas.annotations.Hidden
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseBody

/**
 * Controller serving the Kotlin SSR + HTMX public website (`/`, `/coupons`, `/partials/coupons`, `/courses/{id}`)
 * and forwarding `/app` to the bundled Compose Multiplatform Web (`wasmJs`) application.
 */
@Hidden
@Controller
class PublicWebController(
    private val courseResponseService: CourseResponseService,
) {
    companion object {
        private const val DEFAULT_WEB_PAGE_SIZE = "24"
        private const val DEFAULT_HOT_DEALS_COUNT = 8
        private const val DEFAULT_HOT_DEALS_MIN_RATING = 4.5f
    }

    /**
     * Renders the public SSR home page (or just the `#coupon-results` fragment when triggered via HTMX).
     */
    @GetMapping(value = ["/", "/coupons"], produces = [MediaType.TEXT_HTML_VALUE])
    @ResponseBody
    fun homePage(
        @ModelAttribute queryDto: CouponQueryDTO,
        request: HttpServletRequest,
    ): ResponseEntity<String> {
        val normalizedQuery = normalizeWebQuery(queryDto)
        val pagedCoupons = courseResponseService.listCoupons(normalizedQuery)
        val isHtmxRequest = request.getHeader("HX-Request")?.equals("true", ignoreCase = true) == true

        if (isHtmxRequest) {
            val fragment = PublicWebViews.renderCouponResultsFragment(normalizedQuery, pagedCoupons)
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
                .body(fragment)
        }

        val hotDeals =
            runCatching {
                courseResponseService.getHotDeals(
                    preference = normalizedQuery.category.takeIf { it.isNotBlank() },
                    minRating = DEFAULT_HOT_DEALS_MIN_RATING,
                    pageIndex = 0,
                    numberPerPage = DEFAULT_HOT_DEALS_COUNT,
                ).courses.orEmpty()
            }.getOrDefault(emptyList())

        val html = PublicWebViews.renderHomePage(normalizedQuery, hotDeals, pagedCoupons)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
            .body(html)
    }

    /**
     * Dedicated HTMX partial endpoint for live search, category/language filters, sorting, and pagination.
     */
    @GetMapping(value = ["/partials/coupons"], produces = [MediaType.TEXT_HTML_VALUE])
    @ResponseBody
    fun couponResultsPartial(
        @ModelAttribute queryDto: CouponQueryDTO,
    ): ResponseEntity<String> {
        val normalizedQuery = normalizeWebQuery(queryDto)
        val pagedCoupons = courseResponseService.listCoupons(normalizedQuery)
        val fragment = PublicWebViews.renderCouponResultsFragment(normalizedQuery, pagedCoupons)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
            .body(fragment)
    }

    /**
     * Renders the shareable SEO + OpenGraph course detail page (`/courses/{courseId}`).
     */
    @GetMapping(value = ["/courses/{courseId}"], produces = [MediaType.TEXT_HTML_VALUE])
    @ResponseBody
    fun courseDetailPage(
        @PathVariable("courseId") courseId: String,
    ): ResponseEntity<String> {
        val detail = courseResponseService.getCouponDetail(courseId)
        val html = PublicWebViews.renderCourseDetailPage(detail)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
            .body(html)
    }

    /**
     * Renders the public Terms of Service page (`/terms`).
     */
    @GetMapping(value = ["/terms"], produces = [MediaType.TEXT_HTML_VALUE])
    @ResponseBody
    fun termsOfServicePage(): ResponseEntity<String> =
        ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
            .body(PublicLegalViews.renderTermsPage())

    /**
     * Renders the public Privacy Policy page (`/privacy`).
     */
    @GetMapping(value = ["/privacy"], produces = [MediaType.TEXT_HTML_VALUE])
    @ResponseBody
    fun privacyPolicyPage(): ResponseEntity<String> =
        ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
            .body(PublicLegalViews.renderPrivacyPage())

    /**
     * Forwards `/app` and `/app/` to the static Compose Multiplatform Web (`wasmJs`) entry point.
     */
    @Suppress("FunctionOnlyReturningConstant")
    @GetMapping(value = ["/app", "/app/"])
    fun composeWebAppEntry(): String = "forward:/app/index.html"

    private fun normalizeWebQuery(queryDto: CouponQueryDTO): CouponQueryDTO {
        val requestedSize = queryDto.numberPerPage.toIntOrNull()
        val effectiveSize =
            if (queryDto.numberPerPage == "1000" || requestedSize == null || requestedSize <= 0) {
                DEFAULT_WEB_PAGE_SIZE
            } else {
                queryDto.numberPerPage
            }
        return queryDto.copy(numberPerPage = effectiveSize)
    }
}
