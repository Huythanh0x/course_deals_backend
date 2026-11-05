package com.thanh0x.coursedeal.web

import com.thanh0x.coursedeal.dto.CouponDetailDTO
import com.thanh0x.coursedeal.dto.CouponQueryDTO
import com.thanh0x.coursedeal.dto.CouponSummaryDTO
import com.thanh0x.coursedeal.dto.PagedCouponResponseDTO
import com.thanh0x.coursedeal.web.PublicLegalViews.CMP_FAVICON_DATA_URI
import com.thanh0x.coursedeal.web.PublicLegalViews.renderCmpLogoMark
import com.thanh0x.coursedeal.web.PublicLegalViews.renderSaaSFooter
import com.thanh0x.coursedeal.web.PublicLegalViews.renderSaaSHeader
import com.thanh0x.coursedeal.web.PublicLegalViews.renderSaaSLandingSections
import kotlinx.html.ButtonType
import kotlinx.html.DIV
import kotlinx.html.FlowContent
import kotlinx.html.FormMethod
import kotlinx.html.HTML
import kotlinx.html.InputType
import kotlinx.html.a
import kotlinx.html.article
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.h3
import kotlinx.html.head
import kotlinx.html.html
import kotlinx.html.id
import kotlinx.html.img
import kotlinx.html.input
import kotlinx.html.label
import kotlinx.html.link
import kotlinx.html.main
import kotlinx.html.meta
import kotlinx.html.nav
import kotlinx.html.option
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.section
import kotlinx.html.select
import kotlinx.html.span
import kotlinx.html.stream.appendHTML
import kotlinx.html.style
import kotlinx.html.title
import kotlinx.html.unsafe
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Type-safe Kotlin HTML renderer (`kotlinx.html`) for the CourseDeals public web pages and HTMX fragments.
 *
 * Uses the exact Material 3 color palette and iconography from `course_deals_ComposeMultiplatform`:
 * - Primary: `#0B57D0` / `#A8C7FA`
 * - Primary Container: `#00458F` / `#D3E3FD`
 * - Surface Dark: `#111318`, Surface Container: `#1E2025`, Surface Container High: `#282A2F`
 * - RatingAmber: `#E8A33D`
 * - Error Container: `#93000A` / `#FFDAD6`
 */
@Suppress("TooManyFunctions", "LongMethod", "LargeClass", "MaxLineLength")
object PublicWebViews {
    private const val SECONDS_PER_MINUTE = 60L
    private const val SECONDS_PER_HOUR = 3600L
    private const val SECONDS_PER_DAY = 86400L
    private const val MILLIS_PER_SECOND = 1000L
    private const val PAGINATION_WINDOW_RADIUS = 2
    private const val MINUTES_PER_HOUR = 60
    private const val URGENT_HOURS_THRESHOLD = 6L
    private const val WARNING_HOURS_THRESHOLD = 24L
    private const val STAR_COUNT = 5

    private const val SVG_GROUP_ICON =
        "<svg viewBox='0 0 24 24' width='14' height='14' fill='currentColor' class='inline-block shrink-0'>" +
            "<path d='M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3z" +
            "m-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3z" +
            "m0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5z" +
            "m8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z'/>" +
            "</svg>"

    private const val SVG_REVIEWS_ICON =
        "<svg viewBox='0 0 24 24' width='13' height='13' fill='currentColor' class='inline-block shrink-0'>" +
            "<path d='M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H6l-2 2V4h16v12z'/>" +
            "</svg>"

    private const val SVG_TIMER_ICON =
        "<svg viewBox='0 0 24 24' width='12' height='12' fill='currentColor' class='inline-block shrink-0'>" +
            "<path d='M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2z" +
            "M12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8z" +
            "m.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z'/>" +
            "</svg>"

    private const val SVG_FIRE_ICON =
        "<svg viewBox='0 0 24 24' width='12' height='12' fill='currentColor' class='inline-block shrink-0'>" +
            "<path d='M13.5.67s.74 2.65.74 4.8c0 2.06-1.35 3.73-3.41 3.73-2.07 0-3.63-1.67-3.63-3.73l.03-.36" +
            "C5.21 7.51 4 10.62 4 14c0 4.42 3.58 8 8 8s8-3.58 8-8C20 8.61 17.41 3.8 13.5.67z" +
            "M11.71 19c-1.78 0-3.22-1.4-3.22-3.14 0-1.62 1.05-2.76 2.81-3.12 1.77-.36 3.6-1.21 4.62-2.58" +
            ".39 1.29.59 2.65.59 4.04 0 2.65-2.15 4.8-4.8 4.8z'/>" +
            "</svg>"

    val categories: List<String> =
        listOf(
            "",
            "Development",
            "IT & Software",
            "Business",
            "Design",
            "Marketing",
            "Personal Development",
            "Teaching & Academics",
        )

    val languages: List<String> =
        listOf(
            "",
            "English",
            "Spanish",
            "Portuguese",
            "German",
            "French",
            "Vietnamese",
            "Japanese",
        )

    val levels: List<Pair<String, String>> =
        listOf(
            "" to "All Levels",
            "BEGINNER" to "Beginner",
            "INTERMEDIATE" to "Intermediate",
            "EXPERT" to "Expert",
            "ALL_LEVELS" to "All Levels (Tag)",
        )

    val sortOptions: List<Pair<String, String>> =
        listOf(
            "createdAt" to "Newest First",
            "students" to "Most Popular",
            "rating" to "Highest Rated",
            "reviews" to "Most Reviewed",
            "expiredTime" to "Expiring Soon",
        )

    /**
     * Renders the complete SSR landing page with CMP Material 3 Hero, Hot Deals showcase, Features, Platforms, and FAQ.
     */
    @Suppress("UnusedParameter")
    fun renderHomePage(
        queryDto: CouponQueryDTO,
        hotDeals: List<CouponSummaryDTO>,
        pagedCoupons: PagedCouponResponseDTO,
    ): String =
        buildString {
            append("<!DOCTYPE html>\n")
            appendHTML().html {
                attributes["lang"] = "en"
                attributes["class"] = "dark"
                renderHead(
                    pageTitle = "CourseDeals — 100% Off Free Udemy Coupons (Updated Hourly)",
                    description =
                        "Browse verified 100% off Udemy course coupons across Development, IT & Software, " +
                            "Business, and Design. Built with Kotlin Multiplatform.",
                    canonicalPath = "/",
                    ogImage = hotDeals.firstOrNull()?.previewImage,
                )
                body(classes = "bg-[#111318] text-[#E2E2E9] min-h-screen flex flex-col antialiased") {
                    renderSaaSHeader(pagedCoupons.totalCoupon)
                    main(classes = "flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-14") {
                        renderHeroBanner(pagedCoupons)
                        if (hotDeals.isNotEmpty()) {
                            renderHotDealsSection(hotDeals, pagedCoupons.totalCoupon)
                        }
                        renderSaaSLandingSections()
                    }
                    renderSaaSFooter()
                }
            }
        }

    /**
     * Renders only the `#coupon-results` inner HTML fragment for HTMX filter/search/page swaps.
     */
    fun renderCouponResultsFragment(
        queryDto: CouponQueryDTO,
        pagedCoupons: PagedCouponResponseDTO,
    ): String =
        buildString {
            appendHTML(prettyPrint = false).div {
                renderCouponResultsInner(queryDto, pagedCoupons)
            }
        }

    /**
     * Renders the SEO & OpenGraph shareable course detail page (`/courses/{courseId}`).
     */
    fun renderCourseDetailPage(detail: CouponDetailDTO): String {
        val safeTitle = detail.title ?: "Free Udemy Course #${detail.courseId}"
        val safeDesc =
            detail.heading?.takeIf { it.isNotBlank() }
                ?: "Enroll in $safeTitle for free with a verified 100% off Udemy coupon on CourseDeals."
        val enrollUrl =
            detail.couponUrl?.takeIf { it.isNotBlank() }
                ?: "https://www.udemy.com/course/${detail.courseId}/"

        return buildString {
            append("<!DOCTYPE html>\n")
            appendHTML().html {
                attributes["lang"] = "en"
                attributes["class"] = "dark"
                renderHead(
                    pageTitle = "$safeTitle — 100% Off Udemy Coupon | CourseDeals",
                    description = safeDesc,
                    canonicalPath = "/courses/${detail.courseId}",
                    ogImage = detail.previewImage,
                )
                body(classes = "bg-[#111318] text-[#E2E2E9] min-h-screen flex flex-col antialiased") {
                    renderSaaSHeader()
                    main(classes = "flex-1 max-w-5xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8") {
                        div(classes = "mb-6 flex items-center justify-between gap-4") {
                            a(
                                href = "/",
                                classes =
                                    "inline-flex items-center gap-2 text-sm font-semibold text-[#A8C7FA] " +
                                        "hover:text-[#D3E3FD] transition",
                            ) {
                                +"← Back to All Coupons"
                            }
                            a(
                                href = "/app",
                                classes =
                                    "inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full text-xs sm:text-sm " +
                                        "font-semibold bg-[#00458F]/60 text-[#D3E3FD] border border-[#A8C7FA]/30 " +
                                        "hover:bg-[#0B57D0] hover:text-white transition",
                            ) {
                                +"Open in Web App (/app) →"
                            }
                        }
                        renderCourseDetailCard(detail, safeTitle, safeDesc, enrollUrl)
                    }
                    renderSaaSFooter()
                }
            }
        }
    }

    private fun HTML.renderHead(
        pageTitle: String,
        description: String,
        canonicalPath: String,
        ogImage: String?,
    ) {
        head {
            meta(charset = "UTF-8")
            meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
            title { +pageTitle }
            meta(name = "description", content = description)
            meta(name = "robots", content = "index, follow")
            link(rel = "icon", type = "image/svg+xml", href = CMP_FAVICON_DATA_URI)
            link(rel = "canonical", href = "https://coupons.thanh0x.com$canonicalPath")

            // OpenGraph & Social Preview tags
            meta {
                attributes["property"] = "og:type"
                attributes["content"] = "website"
            }
            meta {
                attributes["property"] = "og:site_name"
                attributes["content"] = "CourseDeals"
            }
            meta {
                attributes["property"] = "og:title"
                attributes["content"] = pageTitle
            }
            meta {
                attributes["property"] = "og:description"
                attributes["content"] = description
            }
            meta {
                attributes["property"] = "og:url"
                attributes["content"] = "https://coupons.thanh0x.com$canonicalPath"
            }
            if (!ogImage.isNullOrBlank()) {
                meta {
                    attributes["property"] = "og:image"
                    attributes["content"] = ogImage
                }
            }
            meta(name = "twitter:card", content = "summary_large_image")
            meta(name = "twitter:title", content = pageTitle)
            meta(name = "twitter:description", content = description)

            script(src = "https://cdn.tailwindcss.com") {}
            script(src = "https://unpkg.com/htmx.org@2.0.3") {}
            style {
                unsafe {
                    raw(
                        """
                        .htmx-indicator { display: none; }
                        .htmx-request .htmx-indicator { display: inline-flex; }
                        .no-scrollbar::-webkit-scrollbar { display: none; }
                        .no-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }
                        """.trimIndent(),
                    )
                }
            }
        }
    }

    private fun FlowContent.renderHeroBanner(pagedCoupons: PagedCouponResponseDTO) {
        val lastFetchLabel = formatLastFetchAgo(pagedCoupons.lastFetchTime)
        val totalActive = pagedCoupons.totalCoupon ?: 0L
        section(
            classes =
                "relative overflow-hidden rounded-3xl bg-gradient-to-br from-[#1A1C20] via-[#00458F]/40 " +
                    "to-[#1E2025] border border-[#A8C7FA]/30 p-6 sm:p-10 lg:p-12 shadow-2xl",
        ) {
            div(classes = "max-w-4xl mx-auto text-center space-y-6") {
                div(
                    classes =
                        "inline-flex flex-wrap items-center justify-center gap-2 px-4 py-1.5 " +
                            "rounded-full text-xs font-semibold bg-[#00458F]/70 text-[#D3E3FD] " +
                            "border border-[#A8C7FA]/35",
                ) {
                    renderCmpLogoMark("w-5 h-5")
                    +"Open-Source 100% Off Course Deals • Synced $lastFetchLabel"
                }
                h1(
                    classes =
                        "text-3xl sm:text-5xl lg:text-6xl font-extrabold tracking-tight text-[#E2E2E9] leading-tight",
                ) {
                    +"Never Pay Full Price for "
                    span(
                        classes =
                            "bg-gradient-to-r from-[#A8C7FA] via-[#D3E3FD] to-[#E8A33D] " +
                                "bg-clip-text text-transparent",
                    ) {
                        +"Online Courses Again"
                    }
                }
                p(classes = "text-sm sm:text-lg text-[#C4C6D0] max-w-2xl mx-auto leading-relaxed") {
                    +"CourseDeals automatically crawls and verifies 100% off Udemy coupons every hour. "
                    +"Browse instantly on the web, or launch our full-featured Compose Multiplatform app "
                    +"across Web (Wasm + JS), Android, iOS, watchOS, and macOS."
                }
                div(classes = "flex flex-wrap items-center justify-center gap-3 pt-2") {
                    a(
                        href = "/app",
                        classes =
                            "inline-flex items-center gap-2 px-6 py-3.5 rounded-full text-sm sm:text-base " +
                                "font-bold bg-[#0B57D0] hover:bg-[#1967D2] text-white " +
                                "shadow-lg shadow-[#0B57D0]/35 transition",
                    ) {
                        +"Launch Web App (/app) →"
                    }
                    a(
                        href = "#hot-deals",
                        classes =
                            "inline-flex items-center gap-2 px-6 py-3.5 rounded-full text-sm sm:text-base " +
                                "font-semibold bg-[#1E2025] hover:bg-[#282A2F] text-[#E2E2E9] " +
                                "border border-[#44474F] transition",
                    ) {
                        +"Explore Hot Deals"
                    }
                    a(
                        href = PublicLegalViews.GITHUB_KMP_URL,
                        target = "_blank",
                        classes =
                            "inline-flex items-center gap-2 px-5 py-3.5 rounded-full text-sm sm:text-base " +
                                "font-semibold bg-[#1A1C20] hover:bg-[#282A2F] text-[#C4C6D0] " +
                                "hover:text-[#E2E2E9] border border-[#44474F] transition",
                    ) {
                        attributes["rel"] = "noopener noreferrer"
                        +"★ Star on GitHub"
                    }
                }

                // 4-Card Live Metrics Strip
                div(
                    classes =
                        "grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4 pt-6 border-t border-[#44474F]/60 text-left",
                ) {
                    renderHeroMetricCard(
                        value = if (totalActive > 0L) "$totalActive+" else "Live",
                        label = "Active 100% Off Coupons",
                        subtext = "Verified & ready to claim",
                    )
                    renderHeroMetricCard(
                        value = "Hourly",
                        label = "Automated Sync Engine",
                        subtext = "Last updated $lastFetchLabel",
                    )
                    renderHeroMetricCard(
                        value = "5 Targets",
                        label = "Kotlin Multiplatform",
                        subtext = "Web, Android, iOS, Watch, Mac",
                    )
                    renderHeroMetricCard(
                        value = "100% Free",
                        label = "Open-Source Material 3",
                        subtext = "No paywalls or hidden fees",
                    )
                }
            }
        }
    }

    private fun FlowContent.renderHeroMetricCard(
        value: String,
        label: String,
        subtext: String,
    ) {
        div(
            classes =
                "rounded-2xl bg-[#1E2025]/90 border border-[#44474F]/70 p-4 space-y-1",
        ) {
            div(classes = "text-lg sm:text-2xl font-extrabold text-[#E2E2E9]") { +value }
            div(classes = "text-xs sm:text-sm font-semibold text-[#A8C7FA]") { +label }
            div(classes = "text-[11px] text-[#C4C6D0]") { +subtext }
        }
    }

    private fun FlowContent.renderHotDealsSection(
        hotDeals: List<CouponSummaryDTO>,
        totalCoupons: Long?,
    ) {
        val countLabel = if (totalCoupons != null && totalCoupons > 0L) "$totalCoupons+" else "All"
        section(classes = "space-y-5 scroll-mt-20") {
            id = "hot-deals"
            div(classes = "flex flex-col sm:flex-row sm:items-end justify-between gap-2") {
                div {
                    h2(classes = "text-xl sm:text-2xl font-extrabold text-[#E2E2E9] flex items-center gap-2") {
                        span(classes = "text-[#E8A33D]") { +"★" }
                        +"Hot Deals Showcase (4.5★+)"
                    }
                    p(classes = "text-xs sm:text-sm text-[#C4C6D0]") {
                        +"Highest-rated Udemy courses sorted by student enrollment — styled identically to our CMP CouponCourseCard"
                    }
                }
                a(
                    href = "/app",
                    classes =
                        "inline-flex items-center gap-1.5 text-xs sm:text-sm font-semibold " +
                            "text-[#A8C7FA] hover:text-[#D3E3FD] transition",
                ) {
                    +"Browse All $countLabel Courses in Web App (/app) →"
                }
            }
            div(classes = "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5") {
                for (course in hotDeals) {
                    renderCmpStyleCourseCard(course)
                }
            }
            div(classes = "pt-2 text-center") {
                a(
                    href = "/app",
                    classes =
                        "inline-flex items-center justify-center gap-2 px-6 py-3.5 rounded-full " +
                            "text-sm sm:text-base font-bold bg-[#0B57D0] hover:bg-[#1967D2] " +
                            "text-white shadow-lg shadow-[#0B57D0]/30 transition",
                ) {
                    +"Explore All $countLabel Free Courses in Web App (/app) →"
                }
            }
        }
    }

    /**
     * Renders a course card matching `CouponCourseCard.kt` from `course_deals_ComposeMultiplatform`:
     * - 16:9 image with top-right urgency countdown ticker (`Schedule` / `LocalFireDepartment`)
     * - 2-line bold title (`#E2E2E9`)
     * - 5-star `#E8A33D` (`RatingAmber`) rating bar + numeric score
     * - `Group` students count + `ChatBubbleOutline` reviews count
     */
    private fun FlowContent.renderCmpStyleCourseCard(course: CouponSummaryDTO) {
        val courseUrl = "/courses/${course.courseId}"
        val titleText = course.title ?: "Course #${course.courseId}"
        val (expiryLabel, isUrgent, isWarning) = computeExpiryBadge(course.expiredTime)
        val badgeClasses =
            when {
                isUrgent -> "bg-[#93000A] text-[#FFDAD6] border border-[#FFB4AB]/40"
                isWarning -> "bg-[#00458F] text-[#D3E3FD] border border-[#A8C7FA]/40"
                else -> "bg-[#111318]/85 text-[#E2E2E9] border border-[#44474F]/70 backdrop-blur-sm"
            }

        article(
            classes =
                "bg-[#1E2025] border border-[#44474F]/70 hover:border-[#A8C7FA] " +
                    "rounded-2xl overflow-hidden flex flex-col justify-between transition group shadow-md",
        ) {
            a(href = courseUrl, classes = "relative aspect-video bg-[#282A2F] block overflow-hidden") {
                if (!course.previewImage.isNullOrBlank()) {
                    img(
                        src = course.previewImage,
                        alt = titleText,
                        classes = "w-full h-full object-cover group-hover:scale-105 transition duration-300",
                    ) {
                        attributes["loading"] = "lazy"
                    }
                }
                val courseCategory = course.category
                if (!courseCategory.isNullOrBlank()) {
                    span(
                        classes =
                            "absolute top-2.5 left-2.5 px-2.5 py-0.5 rounded-md text-[10px] font-semibold " +
                                "bg-[#111318]/85 text-[#D3E3FD] border border-[#44474F]/60 backdrop-blur-sm",
                    ) {
                        +courseCategory
                    }
                }
                span(
                    classes =
                        "absolute top-2.5 right-2.5 inline-flex items-center gap-1 px-2.5 py-0.5 " +
                            "rounded-lg text-[11px] font-bold shadow $badgeClasses",
                ) {
                    unsafe { raw(if (isUrgent) SVG_FIRE_ICON else SVG_TIMER_ICON) }
                    +expiryLabel
                }
            }
            div(classes = "p-4 flex-1 flex flex-col justify-between space-y-3") {
                h3(
                    classes =
                        "text-sm font-bold text-[#E2E2E9] line-clamp-2 min-h-[2.5rem] " +
                            "group-hover:text-[#A8C7FA] transition leading-snug",
                ) {
                    a(href = courseUrl) { +titleText }
                }

                div(classes = "space-y-2") {
                    // 5-star RatingAmber (#E8A33D) row
                    div(classes = "flex items-center gap-1.5") {
                        renderFiveStarIcons(course.rating)
                        span(classes = "text-xs font-bold text-[#E8A33D]") {
                            +formatRating(course.rating)
                        }
                    }

                    // Students (Group icon) + Reviews (ChatBubbleOutline icon) + FREE chip
                    div(
                        classes =
                            "flex items-center justify-between text-xs text-[#C4C6D0] " +
                                "pt-2 border-t border-[#44474F]/50",
                    ) {
                        div(classes = "flex items-center gap-3") {
                            span(classes = "inline-flex items-center gap-1") {
                                unsafe { raw(SVG_GROUP_ICON) }
                                +formatNumber(course.students)
                            }
                            if (course.reviews > 0) {
                                span(classes = "inline-flex items-center gap-1") {
                                    unsafe { raw(SVG_REVIEWS_ICON) }
                                    +formatNumber(course.reviews)
                                }
                            }
                        }
                        a(
                            href = courseUrl,
                            classes =
                                "px-2.5 py-0.5 rounded-md text-[11px] font-bold " +
                                    "bg-[#00458F] text-[#D3E3FD] group-hover:bg-[#0B57D0] group-hover:text-white transition",
                        ) {
                            +"100% OFF"
                        }
                    }
                }
            }
        }
    }

    private fun FlowContent.renderFiveStarIcons(rating: Float) {
        val filledCount = rating.roundToInt().coerceIn(0, STAR_COUNT)
        span(classes = "inline-flex items-center text-xs leading-none tracking-tight") {
            for (idx in 1..STAR_COUNT) {
                span(classes = if (idx <= filledCount) "text-[#E8A33D]" else "text-[#44474F]") {
                    +"★"
                }
            }
        }
    }

    internal fun FlowContent.renderFilterForm(queryDto: CouponQueryDTO) {
        form(
            action = "/",
            method = FormMethod.get,
            classes = "bg-[#1E2025] border border-[#44474F]/70 rounded-2xl p-4 sm:p-5 space-y-4",
        ) {
            id = "coupon-filter-form"
            attributes["hx-get"] = "/partials/coupons"
            attributes["hx-target"] = "#coupon-results"
            attributes["hx-push-url"] = "true"
            attributes["hx-trigger"] = "change, submit"

            input(type = InputType.hidden, name = "category") {
                id = "category-input"
                value = queryDto.category
            }
            input(type = InputType.hidden, name = "pageIndex") {
                id = "page-index-input"
                value = "0"
            }
            input(type = InputType.hidden, name = "numberPerPage") {
                value = queryDto.numberPerPage
            }

            div(classes = "grid grid-cols-1 md:grid-cols-12 gap-3") {
                div(classes = "md:col-span-5 relative") {
                    label(classes = "sr-only") {
                        attributes["for"] = "search-query"
                        +"Search courses"
                    }
                    input(
                        type = InputType.search,
                        name = "query",
                        classes =
                            "w-full bg-[#282A2F] border border-[#44474F] focus:border-[#A8C7FA] rounded-full " +
                                "px-4 py-2.5 text-sm text-[#E2E2E9] placeholder-[#8E9099] outline-none transition",
                    ) {
                        id = "search-query"
                        placeholder = "Search Python, Kotlin, AWS, React..."
                        value = queryDto.query
                        attributes["hx-get"] = "/partials/coupons"
                        attributes["hx-target"] = "#coupon-results"
                        attributes["hx-include"] = "#coupon-filter-form"
                        attributes["hx-trigger"] = "input changed delay:250ms, search"
                        attributes["hx-push-url"] = "true"
                    }
                }
                div(classes = "md:col-span-3") {
                    select(
                        classes =
                            "w-full bg-[#282A2F] border border-[#44474F] focus:border-[#A8C7FA] rounded-xl " +
                                "px-3 py-2.5 text-sm text-[#E2E2E9] outline-none",
                    ) {
                        name = "language"
                        for (lang in languages) {
                            option {
                                value = lang
                                selected = queryDto.language.equals(lang, ignoreCase = true)
                                +(if (lang.isEmpty()) "All Languages" else lang)
                            }
                        }
                    }
                }
                div(classes = "md:col-span-2") {
                    select(
                        classes =
                            "w-full bg-[#282A2F] border border-[#44474F] focus:border-[#A8C7FA] rounded-xl " +
                                "px-3 py-2.5 text-sm text-[#E2E2E9] outline-none",
                    ) {
                        name = "level"
                        for ((lvlKey, lvlLabel) in levels) {
                            option {
                                value = lvlKey
                                selected = queryDto.level.equals(lvlKey, ignoreCase = true)
                                +lvlLabel
                            }
                        }
                    }
                }
                div(classes = "md:col-span-2") {
                    select(
                        classes =
                            "w-full bg-[#282A2F] border border-[#44474F] focus:border-[#A8C7FA] rounded-xl " +
                                "px-3 py-2.5 text-sm text-[#E2E2E9] outline-none",
                    ) {
                        name = "sortBy"
                        for ((sortKey, sortLabel) in sortOptions) {
                            option {
                                value = sortKey
                                selected = queryDto.sortBy.equals(sortKey, ignoreCase = true)
                                +sortLabel
                            }
                        }
                    }
                }
            }

            div(classes = "flex items-center gap-2 overflow-x-auto no-scrollbar pt-1") {
                for (cat in categories) {
                    val isSelected = queryDto.category.equals(cat, ignoreCase = true)
                    val labelText = if (cat.isEmpty()) "All Categories" else cat
                    val pillClasses =
                        if (isSelected) {
                            "px-3.5 py-1.5 rounded-full text-xs font-semibold " +
                                "bg-[#00458F] text-[#D3E3FD] border border-[#A8C7FA]/50 shrink-0 transition"
                        } else {
                            "px-3.5 py-1.5 rounded-full text-xs font-medium bg-[#282A2F] hover:bg-[#33353A] " +
                                "text-[#C4C6D0] border border-[#44474F] shrink-0 transition"
                        }
                    button(type = ButtonType.button, classes = pillClasses) {
                        attributes["onclick"] =
                            "document.getElementById('category-input').value='$cat';" +
                            "document.getElementById('page-index-input').value='0';" +
                            "document.querySelectorAll('[data-cat-pill]').forEach(el => {" +
                            "  el.className = 'px-3.5 py-1.5 rounded-full text-xs font-medium bg-[#282A2F] " +
                            "hover:bg-[#33353A] text-[#C4C6D0] border border-[#44474F] shrink-0 transition';" +
                            "});" +
                            "this.className = 'px-3.5 py-1.5 rounded-full text-xs font-semibold " +
                            "bg-[#00458F] text-[#D3E3FD] border border-[#A8C7FA]/50 shrink-0 transition';" +
                            "htmx.trigger('#coupon-filter-form', 'change');"
                        attributes["data-cat-pill"] = cat
                        +labelText
                    }
                }
            }
        }
    }

    private fun DIV.renderCouponResultsInner(
        queryDto: CouponQueryDTO,
        pagedCoupons: PagedCouponResponseDTO,
    ) {
        val courses = pagedCoupons.courses.orEmpty()
        val totalCoupons = pagedCoupons.totalCoupon ?: courses.size.toLong()
        val currentPage = pagedCoupons.currentPage ?: (queryDto.pageIndex.toIntOrNull() ?: 0)
        val totalPages = pagedCoupons.totalPage ?: 1

        div(classes = "space-y-6") {
            div(classes = "flex items-center justify-between text-xs sm:text-sm text-[#C4C6D0]") {
                span {
                    +"Showing "
                    span(classes = "font-semibold text-[#E2E2E9]") { +"${courses.size}" }
                    +" of "
                    span(classes = "font-semibold text-[#E2E2E9]") { +"$totalCoupons" }
                    +" free courses"
                    if (queryDto.category.isNotBlank()) {
                        +" in "
                        span(classes = "text-[#A8C7FA] font-medium") { +queryDto.category }
                    }
                    if (queryDto.query.isNotBlank()) {
                        +" matching \""
                        span(classes = "text-[#A8C7FA] font-medium") { +queryDto.query }
                        +"\""
                    }
                }
                span {
                    +"Page ${currentPage + 1} of ${max(1, totalPages)}"
                }
            }

            if (courses.isEmpty()) {
                div(
                    classes =
                        "rounded-2xl border border-[#44474F] bg-[#1E2025] p-12 text-center space-y-3",
                ) {
                    p(classes = "text-lg font-semibold text-[#E2E2E9]") {
                        +"No matching coupons found"
                    }
                    p(classes = "text-sm text-[#C4C6D0]") {
                        +"Try clearing your search query or switching to All Categories."
                    }
                    a(
                        href = "/",
                        classes =
                            "inline-flex items-center px-4 py-2 rounded-full text-sm font-semibold " +
                                "bg-[#0B57D0] hover:bg-[#1967D2] text-white transition",
                    ) {
                        +"Reset Filters"
                    }
                }
            } else {
                div(classes = "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5") {
                    for (course in courses) {
                        renderCmpStyleCourseCard(course)
                    }
                }
            }

            if (totalPages > 1) {
                renderPaginationBar(queryDto, currentPage, totalPages)
            }
        }
    }

    private fun FlowContent.renderPaginationBar(
        queryDto: CouponQueryDTO,
        currentPage: Int,
        totalPages: Int,
    ) {
        val startPage = max(0, currentPage - PAGINATION_WINDOW_RADIUS)
        val endPage = min(totalPages - 1, currentPage + PAGINATION_WINDOW_RADIUS)

        nav(classes = "flex items-center justify-center gap-1.5 pt-4 flex-wrap") {
            attributes["aria-label"] = "Pagination"
            if (currentPage > 0) {
                renderPageLink(queryDto, currentPage - 1, "← Prev", isCurrent = false)
            }
            if (startPage > 0) {
                renderPageLink(queryDto, 0, "1", isCurrent = false)
                if (startPage > 1) {
                    span(classes = "px-2 text-[#8E9099] text-sm") { +"…" }
                }
            }
            for (page in startPage..endPage) {
                renderPageLink(queryDto, page, "${page + 1}", isCurrent = page == currentPage)
            }
            if (endPage < totalPages - 1) {
                if (endPage < totalPages - 2) {
                    span(classes = "px-2 text-[#8E9099] text-sm") { +"…" }
                }
                renderPageLink(queryDto, totalPages - 1, "$totalPages", isCurrent = false)
            }
            if (currentPage < totalPages - 1) {
                renderPageLink(queryDto, currentPage + 1, "Next →", isCurrent = false)
            }
        }
    }

    private fun FlowContent.renderPageLink(
        queryDto: CouponQueryDTO,
        targetPage: Int,
        labelText: String,
        isCurrent: Boolean,
    ) {
        val queryString = buildQueryString(queryDto, targetPage)
        val hrefUrl = "/?$queryString"
        val hxUrl = "/partials/coupons?$queryString"
        val classes =
            if (isCurrent) {
                "px-3.5 py-2 rounded-xl text-xs sm:text-sm font-bold bg-[#0B57D0] text-white"
            } else {
                "px-3.5 py-2 rounded-xl text-xs sm:text-sm font-medium bg-[#1E2025] hover:bg-[#282A2F] " +
                    "text-[#C4C6D0] border border-[#44474F] transition"
            }
        a(href = hrefUrl, classes = classes) {
            attributes["hx-get"] = hxUrl
            attributes["hx-target"] = "#coupon-results"
            attributes["hx-push-url"] = hrefUrl
            attributes["onclick"] =
                "const p = document.getElementById('page-index-input'); if (p) p.value='$targetPage';"
            +labelText
        }
    }

    private fun FlowContent.renderCourseDetailCard(
        detail: CouponDetailDTO,
        safeTitle: String,
        safeDesc: String,
        enrollUrl: String,
    ) {
        article(
            classes =
                "bg-[#1E2025] border border-[#44474F]/70 rounded-3xl overflow-hidden shadow-xl",
        ) {
            div(classes = "grid grid-cols-1 lg:grid-cols-12 gap-6 p-6 sm:p-8") {
                div(classes = "lg:col-span-5 space-y-4") {
                    div(classes = "aspect-video rounded-2xl overflow-hidden bg-[#282A2F] border border-[#44474F]/60") {
                        if (!detail.previewImage.isNullOrBlank()) {
                            img(
                                src = detail.previewImage,
                                alt = safeTitle,
                                classes = "w-full h-full object-cover",
                            )
                        }
                    }
                    div(classes = "bg-[#1A1C20] border border-[#44474F]/70 rounded-2xl p-4 space-y-3") {
                        div(classes = "flex items-center justify-between text-sm") {
                            span(classes = "text-[#C4C6D0]") { +"Price" }
                            span(
                                classes =
                                    "px-2.5 py-0.5 rounded-lg bg-[#00458F] text-[#D3E3FD] font-extrabold text-sm",
                            ) {
                                +"100% OFF (FREE)"
                            }
                        }
                        val code = detail.couponCode
                        if (!code.isNullOrBlank()) {
                            div(classes = "flex items-center justify-between text-xs") {
                                span(classes = "text-[#C4C6D0]") { +"Coupon Code" }
                                span(
                                    classes =
                                        "font-mono font-semibold px-2.5 py-1 rounded bg-[#00458F]/60 " +
                                            "text-[#D3E3FD] border border-[#A8C7FA]/40",
                                ) {
                                    +code
                                }
                            }
                        }
                        if (detail.usesRemaining > 0) {
                            div(classes = "flex items-center justify-between text-xs") {
                                span(classes = "text-[#C4C6D0]") { +"Redemptions Left" }
                                span(classes = "text-[#E8A33D] font-semibold") {
                                    +"${detail.usesRemaining} remaining"
                                }
                            }
                        }
                        div(classes = "flex items-center justify-between text-xs") {
                            span(classes = "text-[#C4C6D0]") { +"Expires" }
                            span(classes = "text-[#E2E2E9] font-medium") {
                                +formatExpiryRemaining(detail.expiredTime)
                            }
                        }
                        a(
                            href = enrollUrl,
                            target = "_blank",
                            classes =
                                "w-full inline-flex items-center justify-center gap-2 px-5 py-3.5 rounded-full " +
                                    "text-sm sm:text-base font-bold bg-[#0B57D0] hover:bg-[#1967D2] " +
                                    "text-white shadow-lg shadow-[#0B57D0]/30 transition",
                        ) {
                            attributes["rel"] = "noopener noreferrer"
                            +"Redeem 100% Off on Udemy ↗"
                        }
                    }
                }

                div(classes = "lg:col-span-7 space-y-5") {
                    div(classes = "flex flex-wrap items-center gap-2 text-xs") {
                        val cat = detail.category
                        if (!cat.isNullOrBlank()) {
                            span(
                                classes =
                                    "px-3 py-1 rounded-full font-semibold bg-[#00458F]/70 " +
                                        "text-[#D3E3FD] border border-[#A8C7FA]/30",
                            ) {
                                +cat
                            }
                        }
                        val subCat = detail.subCategory
                        if (!subCat.isNullOrBlank()) {
                            span(
                                classes =
                                    "px-3 py-1 rounded-full font-medium bg-[#282A2F] text-[#C4C6D0]",
                            ) {
                                +subCat
                            }
                        }
                        val detailLang = detail.language
                        if (!detailLang.isNullOrBlank()) {
                            span(classes = "px-3 py-1 rounded-full bg-[#282A2F] text-[#C4C6D0]") {
                                +detailLang
                            }
                        }
                        if (detail.contentLength > 0) {
                            span(classes = "px-3 py-1 rounded-full bg-[#282A2F] text-[#C4C6D0]") {
                                +formatDurationMinutes(detail.contentLength)
                            }
                        }
                    }

                    h1(classes = "text-xl sm:text-3xl font-extrabold text-[#E2E2E9] leading-tight") {
                        +safeTitle
                    }
                    p(classes = "text-sm sm:text-base text-[#C4C6D0]") {
                        +safeDesc
                    }

                    div(
                        classes =
                            "flex flex-wrap items-center gap-6 text-sm text-[#C4C6D0] " +
                                "py-3 border-y border-[#44474F]/60",
                    ) {
                        span(classes = "font-semibold text-[#E8A33D]") {
                            +"★ ${formatRating(detail.rating)} (${formatNumber(detail.reviews)} reviews)"
                        }
                        span(classes = "inline-flex items-center gap-1.5") {
                            unsafe { raw(SVG_GROUP_ICON) }
                            +"${formatNumber(detail.students)} students"
                        }
                        val author = detail.author
                        if (!author.isNullOrBlank()) {
                            span { +"By $author" }
                        }
                    }

                    val descHtml = detail.description
                    if (!descHtml.isNullOrBlank()) {
                        div(classes = "space-y-2") {
                            h2(classes = "text-base font-bold text-[#E2E2E9]") { +"Course Description" }
                            div(
                                classes =
                                    "text-sm text-[#C4C6D0] leading-relaxed space-y-2 max-h-96 " +
                                        "overflow-y-auto pr-2",
                            ) {
                                +stripHtmlTags(descHtml)
                            }
                        }
                    }
                }
            }
        }
    }

    internal fun buildQueryString(
        queryDto: CouponQueryDTO,
        pageIndex: Int,
    ): String {
        val params = mutableListOf<Pair<String, String>>()
        if (queryDto.category.isNotBlank()) params.add("category" to queryDto.category)
        if (queryDto.query.isNotBlank()) params.add("query" to queryDto.query)
        if (queryDto.language.isNotBlank()) params.add("language" to queryDto.language)
        if (queryDto.level.isNotBlank()) params.add("level" to queryDto.level)
        if (queryDto.sortBy.isNotBlank() && queryDto.sortBy != "createdAt") {
            params.add("sortBy" to queryDto.sortBy)
        }
        if (queryDto.sortOrder.isNotBlank() && queryDto.sortOrder != "desc") {
            params.add("sortOrder" to queryDto.sortOrder)
        }
        params.add("pageIndex" to pageIndex.toString())
        params.add("numberPerPage" to queryDto.numberPerPage)
        return params.joinToString("&") { (k, v) ->
            "$k=${URLEncoder.encode(v, StandardCharsets.UTF_8)}"
        }
    }

    private fun formatRating(rating: Float): String {
        return if (rating <= 0f) "New" else String.format(Locale.US, "%.1f", rating)
    }

    private fun formatNumber(value: Int): String = String.format(Locale.US, "%,d", value)

    private fun formatDurationMinutes(minutes: Int): String {
        val hrs = minutes / MINUTES_PER_HOUR
        val mins = minutes % MINUTES_PER_HOUR
        return if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
    }

    private fun formatLastFetchAgo(lastFetchMillis: Long?): String {
        if (lastFetchMillis == null || lastFetchMillis <= 0L) return "recently"
        val diffSeconds = max(0L, (System.currentTimeMillis() - lastFetchMillis) / MILLIS_PER_SECOND)
        return when {
            diffSeconds < SECONDS_PER_MINUTE -> "just now"
            diffSeconds < SECONDS_PER_HOUR -> "${diffSeconds / SECONDS_PER_MINUTE}m ago"
            diffSeconds < SECONDS_PER_DAY -> "${diffSeconds / SECONDS_PER_HOUR}h ago"
            else -> "${diffSeconds / SECONDS_PER_DAY}d ago"
        }
    }

    private fun computeExpiryBadge(expiredEpochSeconds: Long?): Triple<String, Boolean, Boolean> {
        val nowSeconds = System.currentTimeMillis() / MILLIS_PER_SECOND
        val diff = if (expiredEpochSeconds != null && expiredEpochSeconds > 0L) expiredEpochSeconds - nowSeconds else null
        return when {
            diff == null -> Triple("100% OFF", false, false)
            diff <= 0L -> Triple("Expiring", true, false)
            else -> {
                val totalHours = diff / SECONDS_PER_HOUR
                val days = diff / SECONDS_PER_DAY
                val remHours = (diff % SECONDS_PER_DAY) / SECONDS_PER_HOUR
                val remMinutes = (diff % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
                val label =
                    when {
                        days > 0L -> "${days}d ${remHours}h left"
                        remHours > 0L -> "${remHours}h ${remMinutes}m left"
                        else -> "${max(1L, remMinutes)}m left"
                    }
                Triple(
                    label,
                    totalHours < URGENT_HOURS_THRESHOLD,
                    totalHours in URGENT_HOURS_THRESHOLD..<WARNING_HOURS_THRESHOLD,
                )
            }
        }
    }

    private fun formatExpiryRemaining(expiredEpochSeconds: Long?): String {
        return computeExpiryBadge(expiredEpochSeconds).first
    }

    private fun stripHtmlTags(rawHtml: String): String {
        return rawHtml
            .replace(Regex("<[^>]*>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
