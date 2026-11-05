package com.thanh0x.coursedeal.web

import kotlinx.html.FlowContent
import kotlinx.html.HTML
import kotlinx.html.a
import kotlinx.html.article
import kotlinx.html.body
import kotlinx.html.details
import kotlinx.html.div
import kotlinx.html.footer
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.h3
import kotlinx.html.head
import kotlinx.html.header
import kotlinx.html.html
import kotlinx.html.id
import kotlinx.html.li
import kotlinx.html.link
import kotlinx.html.main
import kotlinx.html.meta
import kotlinx.html.nav
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.section
import kotlinx.html.span
import kotlinx.html.stream.appendHTML
import kotlinx.html.summary
import kotlinx.html.title
import kotlinx.html.ul
import kotlinx.html.unsafe

/**
 * Renders the CMP Material 3 aligned shared Header (`ic_logo_mark`), Features/Platforms Bento Grid,
 * FAQ Accordion, 4-Column SaaS Footer, and dedicated `/terms` and `/privacy` legal pages.
 *
 * Palette matches `course_deals_ComposeMultiplatform/core/ui/theme/Theme.kt`:
 * - Surface Dark: `#111318`
 * - Surface Container Low: `#1A1C20`
 * - Surface Container: `#1E2025`
 * - Surface Container High: `#282A2F`
 * - Primary: `#0B57D0` (Light) / `#A8C7FA` (Dark)
 * - Primary Container: `#00458F` (Dark) / `#D3E3FD` (Light)
 * - RatingAmber: `#E8A33D`
 * - Error Container: `#93000A` / `#FFDAD6`
 */
@Suppress("TooManyFunctions", "LongMethod", "LargeClass", "MaxLineLength")
object PublicLegalViews {
    const val GITHUB_KMP_URL = "https://github.com/Huythanh0x/course_deals_ComposeMultiplatform"
    const val GITHUB_RELEASES_URL =
        "https://github.com/Huythanh0x/course_deals_ComposeMultiplatform/releases"
    const val GITHUB_BACKEND_URL = "https://github.com/Huythanh0x/course_deals_backend"
    const val GITHUB_PROFILE_URL = "https://github.com/Huythanh0x"
    const val CONTACT_EMAIL = "huythanh0x@gmail.com"

    const val CMP_FAVICON_DATA_URI =
        "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 108 108'%3E" +
            "%3Crect width='108' height='108' rx='24' fill='%2300458F'/%3E" +
            "%3Cpath fill='%23FFFFFF' d='M36,28L55.5,28C57.6,28 59.6,28.8 61.1,30.3" +
            "L80.7,49.9C83.8,53 83.8,58 80.7,61.1L61.1,80.7C58,83.8 53,83.8 49.9,80.7" +
            "L30.3,61.1C28.8,59.6 28,57.6 28,55.5L28,36C28,31.6 31.6,28 36,28Z'/%3E" +
            "%3Cpath fill='%230B57D0' d='M43,39A4,4 0 1,0 43,47A4,4 0 1,0 43,39Z'/%3E" +
            "%3Cpath fill='%230B57D0' d='M51,48L51,64L65,56Z'/%3E" +
            "%3C/svg%3E"

    private data class FeatureCardItem(
        val badge: String,
        val title: String,
        val description: String,
    )

    private data class PlatformCardItem(
        val iconSvg: String,
        val name: String,
        val subtitle: String,
        val description: String,
        val ctaLabel: String,
        val ctaHref: String,
    )

    private data class FaqItem(
        val question: String,
        val answer: String,
    )

    private val saasFeatures =
        listOf(
            FeatureCardItem(
                badge = "Hourly Sync Engine",
                title = "Automated Coupon Verification",
                description =
                    "Background JobRunr workers scrape and verify Udemy coupon codes every hour, " +
                        "automatically pruning expired or redeemed links so you only see working 100% off deals.",
            ),
            FeatureCardItem(
                badge = "Material 3 + HTMX",
                title = "Instant Search & Smart Filtering",
                description =
                    "Filter hundreds of active courses by Category, Instructional Language, Minimum Rating, " +
                        "Student Count, or Expiry Time in milliseconds.",
            ),
            FeatureCardItem(
                badge = "5 Native Targets",
                title = "True Multiplatform Ecosystem",
                description =
                    "Run the Dual-Engine Compose Web App (Kotlin/Wasm + Kotlin/JS) right in your browser at /app, " +
                        "or install native apps for Android, iOS, Apple Watch, and macOS Desktop.",
            ),
            FeatureCardItem(
                badge = "Modern Auth",
                title = "Passkeys, Google OAuth & OTP",
                description =
                    "Sign in seamlessly with WebAuthn Passkeys, Google Sign-In, or Email/Password " +
                        "to sync your favorite course categories and keyword alerts across devices.",
            ),
            FeatureCardItem(
                badge = "Push Alerts",
                title = "Real-Time Deal Notifications",
                description =
                    "Never miss a limited 1,000-redemption coupon again. Configure your preferred topics and " +
                        "receive instant Firebase Cloud Messaging (FCM) push alerts when matching courses drop.",
            ),
            FeatureCardItem(
                badge = "Open Source & API",
                title = "100% Open-Source & OpenAPI 3.0",
                description =
                    "Built transparently with Spring Boot 3.5, Kotlin Multiplatform 2.3, Redis, and MySQL. " +
                        "Explore our public Swagger UI or self-host on your own Proxmox homelab.",
            ),
        )

    private val platformItems =
        listOf(
            PlatformCardItem(
                iconSvg =
                    "<svg viewBox='0 0 24 24' width='22' height='22' fill='currentColor'>" +
                        "<path d='M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2z" +
                        "m-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93z" +
                        "m6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2" +
                        "c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z'/>" +
                        "</svg>",
                name = "Compose Web App (Wasm + JS)",
                subtitle = "Runs directly in your browser",
                description =
                    "Interactive Material 3 SPA matching our native CMP app 1:1 with HOME, COURSE, and PROFILE tabs, " +
                        "plus instant runtime switching between Kotlin/Wasm and Kotlin/JS.",
                ctaLabel = "Launch /app →",
                ctaHref = "/app",
            ),
            PlatformCardItem(
                iconSvg =
                    "<svg viewBox='0 0 24 24' width='22' height='22' fill='currentColor'>" +
                        "<path d='M17.6 9.48l1.84-3.18c.16-.31.04-.69-.26-.85-.29-.15-.65-.06-.83.22l-1.88 3.24" +
                        "c-2.86-1.21-6.08-1.21-8.94 0L5.65 5.67c-.19-.28-.54-.37-.83-.22-.3.16-.42.54-.26.85" +
                        "l1.84 3.18C4.8 10.85 3.5 12.62 3.5 15h17c0-2.38-1.3-4.15-2.9-5.52z" +
                        "M7 12.75c-.41 0-.75-.34-.75-.75s.34-.75.75-.75.75.34.75.75-.34.75-.75.75z" +
                        "m10 0c-.41 0-.75-.34-.75-.75s.34-.75.75-.75.75.34.75.75-.34.75-.75.75z'/>" +
                        "</svg>",
                name = "Android App",
                subtitle = "Android 7.0+ (SDK 24–37)",
                description =
                    "Native Jetpack Compose & Material You client with Passkey login, background sync, " +
                        "and instant FCM push notifications.",
                ctaLabel = "Download APK ↗",
                ctaHref = GITHUB_RELEASES_URL,
            ),
            PlatformCardItem(
                iconSvg =
                    "<svg viewBox='0 0 24 24' width='22' height='22' fill='currentColor'>" +
                        "<path d='M18.71 19.5c-.83 1.24-1.71 2.45-3.05 2.47-1.34.03-1.77-.79-3.29-.79" +
                        "-1.53 0-2 .77-3.27.82-1.31.05-2.3-1.32-3.14-2.53C4.25 17 2.94 12.45 4.7 9.39" +
                        "c.87-1.52 2.43-2.48 4.12-2.51 1.28-.02 2.5.87 3.29.87.78 0 2.26-1.07 3.81-.91" +
                        ".65.03 2.47.26 3.64 1.98-.09.06-2.17 1.28-2.15 3.81.03 3.02 2.65 4.03 2.68 4.04" +
                        "-.03.07-.42 1.44-1.38 2.83M15.96 6.33c.67-.81 1.12-1.94 1-3.08-.96.04-2.13.64-2.82 1.45" +
                        "-.61.7-.15 1.85-1.01 2.97 1.08.08 2.16-.53 2.83-1.34z'/>" +
                        "</svg>",
                name = "iOS & Apple Watch",
                subtitle = "iPhone, iPad & watchOS Companion",
                description =
                    "Shared Compose Multiplatform iOS app paired with a native SwiftUI Apple Watch app " +
                        "supporting Digital Crown browsing and Apple Handoff.",
                ctaLabel = "View on GitHub ↗",
                ctaHref = GITHUB_KMP_URL,
            ),
            PlatformCardItem(
                iconSvg =
                    "<svg viewBox='0 0 24 24' width='22' height='22' fill='currentColor'>" +
                        "<path d='M20 18c1.1 0 1.99-.9 1.99-2L22 6c0-1.1-.9-2-2-2H4c-1.1 0-2 .9-2 2v10" +
                        "c0 1.1.9 2 2 2H0v2h24v-2h-4zM4 6h16v10H4V6z'/>" +
                        "</svg>",
                name = "macOS Desktop",
                subtitle = "Native Apple Silicon & Intel DMG",
                description =
                    "Fast desktop companion built with Compose for Desktop (JVM 17), deep-linking, " +
                        "and native window management.",
                ctaLabel = "Download DMG ↗",
                ctaHref = GITHUB_RELEASES_URL,
            ),
        )

    private val faqItems =
        listOf(
            FaqItem(
                question = "Are these Udemy courses really 100% free?",
                answer =
                    "Yes! Instructors on Udemy regularly generate promotional 100% off coupon codes (valid for " +
                        "up to 1,000 redemptions or 5 days) to attract early students and reviews. CourseDeals " +
                        "verifies those coupon codes automatically and links you directly to Udemy.",
            ),
            FaqItem(
                question = "Why does a coupon sometimes show a paid price on Udemy?",
                answer =
                    "Udemy promotional coupons have a strict cap of 1,000 redemptions worldwide. Popular courses " +
                        "can hit that limit within minutes. Our background crawler re-validates coupons every hour " +
                        "and removes expired deals automatically.",
            ),
            FaqItem(
                question = "What is the difference between this website (/) and the Web App (/app)?",
                answer =
                    "The main website (/) uses Kotlin Server-Side Rendering (SSR) + HTMX for instant load times, " +
                        "full Google SEO indexing, and rich social link previews. The /app route launches our " +
                        "full Compose Multiplatform Web app (with both Kotlin/Wasm and Kotlin/JS engines) " +
                        "matching our Android/iOS/Desktop client.",
            ),
            FaqItem(
                question = "Is CourseDeals free and open-source?",
                answer =
                    "100% free and open-source under the MIT License. Both our Spring Boot backend and our " +
                        "Kotlin Multiplatform client (Android, iOS, watchOS, Desktop, Web) are publicly available " +
                        "on GitHub (@Huythanh0x).",
            ),
        )

    /**
     * Renders the original CMP `ic_logo_mark.xml` vector icon (blue price tag with play triangle).
     */
    fun FlowContent.renderCmpLogoMark(sizeClass: String = "w-9 h-9") {
        span(
            classes =
                "inline-flex items-center justify-center $sizeClass rounded-xl " +
                    "bg-[#00458F] border border-[#A8C7FA]/30 shadow-md shadow-[#0B57D0]/25 shrink-0",
        ) {
            unsafe {
                raw(
                    "<svg viewBox='22 22 66 66' class='w-6 h-6' xmlns='http://www.w3.org/2000/svg'>" +
                        "<path fill='#FFFFFF' d='M36,28L55.5,28C57.6,28 59.6,28.8 61.1,30.3" +
                        "L80.7,49.9C83.8,53 83.8,58 80.7,61.1L61.1,80.7C58,83.8 53,83.8 49.9,80.7" +
                        "L30.3,61.1C28.8,59.6 28,57.6 28,55.5L28,36C28,31.6 31.6,28 36,28Z'/>" +
                        "<path fill='#0B57D0' d='M43,39A4,4 0 1,0 43,47A4,4 0 1,0 43,39Z'/>" +
                        "<path fill='#0B57D0' d='M51,48L51,64L65,56Z'/>" +
                        "</svg>",
                )
            }
        }
    }

    /**
     * Renders the sticky CMP Material 3 navigation header.
     */
    fun FlowContent.renderSaaSHeader(activeCouponsCount: Long? = null) {
        header(
            classes =
                "sticky top-0 z-40 backdrop-blur-xl bg-[#111318]/90 border-b border-[#44474F]/60",
        ) {
            div(
                classes =
                    "max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between gap-4",
            ) {
                div(classes = "flex items-center gap-6") {
                    a(href = "/", classes = "flex items-center gap-2.5 group") {
                        renderCmpLogoMark("w-9 h-9")
                        div(classes = "flex flex-col") {
                            span(classes = "text-lg sm:text-xl font-extrabold tracking-tight text-[#E2E2E9] leading-none") {
                                +"CourseDeals"
                            }
                            span(classes = "text-[10px] font-semibold text-[#A8C7FA] tracking-wide") {
                                +"Course Deal CMP"
                            }
                        }
                    }

                    nav(classes = "hidden lg:flex items-center gap-6 text-sm font-medium text-[#C4C6D0]") {
                        a(href = "/#hot-deals", classes = "hover:text-[#A8C7FA] transition") { +"Hot Deals" }
                        a(href = "/#features", classes = "hover:text-[#A8C7FA] transition") { +"Features" }
                        a(href = "/#platforms", classes = "hover:text-[#A8C7FA] transition") { +"Platforms" }
                        a(href = "/#faq", classes = "hover:text-[#A8C7FA] transition") { +"FAQ" }
                        a(
                            href = "/swagger-ui/index.html",
                            target = "_blank",
                            classes = "hover:text-[#A8C7FA] transition",
                        ) {
                            +"API"
                        }
                    }
                }

                div(classes = "flex items-center gap-2.5 sm:gap-3") {
                    if (activeCouponsCount != null && activeCouponsCount > 0L) {
                        span(
                            classes =
                                "hidden xl:inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs " +
                                    "font-semibold bg-[#00458F]/60 text-[#D3E3FD] border border-[#A8C7FA]/30",
                        ) {
                            span(classes = "w-2 h-2 rounded-full bg-[#A8C7FA] animate-pulse") {}
                            +"$activeCouponsCount Active Deals"
                        }
                    }
                    a(
                        href = GITHUB_KMP_URL,
                        target = "_blank",
                        classes =
                            "hidden sm:inline-flex items-center gap-1.5 px-3.5 py-2 rounded-full text-xs sm:text-sm " +
                                "font-semibold bg-[#1E2025] hover:bg-[#282A2F] text-[#E2E2E9] " +
                                "border border-[#44474F] transition",
                    ) {
                        attributes["rel"] = "noopener noreferrer"
                        +"★ GitHub"
                    }
                    a(
                        href = "/app",
                        classes =
                            "inline-flex items-center gap-1.5 px-4 py-2 rounded-full text-xs sm:text-sm " +
                                "font-bold bg-[#0B57D0] hover:bg-[#1967D2] text-white " +
                                "shadow-md shadow-[#0B57D0]/30 transition",
                    ) {
                        +"Open Web App →"
                    }
                }
            }
        }
    }

    /**
     * Renders the Features Bento Grid (`#features`), Multiplatform Apps Showcase (`#platforms`),
     * FAQ Accordion (`#faq`), and bottom CMP CTA card.
     */
    fun FlowContent.renderSaaSLandingSections() {
        // Features Bento Grid
        section(classes = "pt-8 space-y-8") {
            id = "features"
            div(classes = "text-center max-w-3xl mx-auto space-y-3") {
                span(
                    classes =
                        "inline-block px-3.5 py-1 rounded-full text-xs font-bold uppercase tracking-wider " +
                            "bg-[#00458F]/60 text-[#D3E3FD] border border-[#A8C7FA]/30",
                ) {
                    +"Why CourseDeals"
                }
                h2(classes = "text-2xl sm:text-4xl font-extrabold text-[#E2E2E9] tracking-tight") {
                    +"Everything You Need to Learn for Free"
                }
                p(classes = "text-sm sm:text-base text-[#C4C6D0]") {
                    +"Designed with Material 3 tokens — powered 100% by Kotlin across server, "
                    +"web, mobile, and watch."
                }
            }

            div(classes = "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5") {
                for (feature in saasFeatures) {
                    div(
                        classes =
                            "bg-[#1E2025] border border-[#44474F]/70 hover:border-[#A8C7FA]/60 " +
                                "rounded-2xl p-6 space-y-3 transition",
                    ) {
                        span(
                            classes =
                                "inline-block px-3 py-1 rounded-lg text-xs font-semibold " +
                                    "bg-[#00458F]/70 text-[#D3E3FD]",
                        ) {
                            +feature.badge
                        }
                        h3(classes = "text-lg font-bold text-[#E2E2E9]") {
                            +feature.title
                        }
                        p(classes = "text-sm text-[#C4C6D0] leading-relaxed") {
                            +feature.description
                        }
                    }
                }
            }
        }

        // Multiplatform Ecosystem Showcase
        section(classes = "pt-8 space-y-8") {
            id = "platforms"
            div(classes = "text-center max-w-3xl mx-auto space-y-3") {
                span(
                    classes =
                        "inline-block px-3.5 py-1 rounded-full text-xs font-bold uppercase tracking-wider " +
                            "bg-[#3E4759] text-[#DAE2F9] border border-[#BDC7DC]/30",
                ) {
                    +"Kotlin Multiplatform Ecosystem"
                }
                h2(classes = "text-2xl sm:text-4xl font-extrabold text-[#E2E2E9] tracking-tight") {
                    +"Same Material 3 Experience on Every Device"
                }
                p(classes = "text-sm sm:text-base text-[#C4C6D0]") {
                    +"Launch the Dual-Engine Compose Web App in your browser or download our native clients for "
                    +"Android, iOS, watchOS, and macOS."
                }
            }

            div(classes = "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5") {
                for (platform in platformItems) {
                    div(
                        classes =
                            "bg-[#1E2025] border border-[#44474F]/70 hover:border-[#A8C7FA]/60 " +
                                "rounded-2xl p-6 flex flex-col justify-between space-y-5 transition",
                    ) {
                        div(classes = "space-y-3") {
                            div(
                                classes =
                                    "w-11 h-11 rounded-xl bg-[#00458F]/70 text-[#D3E3FD] " +
                                        "flex items-center justify-center",
                            ) {
                                unsafe { raw(platform.iconSvg) }
                            }
                            div {
                                h3(classes = "text-base font-bold text-[#E2E2E9]") { +platform.name }
                                p(classes = "text-xs font-semibold text-[#A8C7FA]") { +platform.subtitle }
                            }
                            p(classes = "text-xs sm:text-sm text-[#C4C6D0] leading-relaxed") {
                                +platform.description
                            }
                        }
                        a(
                            href = platform.ctaHref,
                            target = if (platform.ctaHref.startsWith("http")) "_blank" else "_self",
                            classes =
                                "w-full inline-flex items-center justify-center px-4 py-2.5 rounded-full " +
                                    "text-xs font-bold bg-[#282A2F] hover:bg-[#0B57D0] text-[#E2E2E9] " +
                                    "hover:text-white transition",
                        ) {
                            +platform.ctaLabel
                        }
                    }
                }
            }
        }

        // FAQ Accordion
        section(classes = "pt-8 max-w-4xl mx-auto space-y-8") {
            id = "faq"
            div(classes = "text-center space-y-3") {
                span(
                    classes =
                        "inline-block px-3.5 py-1 rounded-full text-xs font-bold uppercase tracking-wider " +
                            "bg-[#00458F]/60 text-[#D3E3FD] border border-[#A8C7FA]/30",
                ) {
                    +"FAQ"
                }
                h2(classes = "text-2xl sm:text-3xl font-extrabold text-[#E2E2E9]") {
                    +"Frequently Asked Questions"
                }
            }
            div(classes = "space-y-3") {
                for (item in faqItems) {
                    details(
                        classes =
                            "group bg-[#1E2025] border border-[#44474F]/70 rounded-2xl px-6 py-4 " +
                                "open:border-[#A8C7FA]/60 transition",
                    ) {
                        summary(
                            classes =
                                "cursor-pointer list-none flex items-center justify-between " +
                                    "text-sm sm:text-base font-semibold text-[#E2E2E9]",
                        ) {
                            span { +item.question }
                            span(classes = "text-[#A8C7FA] group-open:rotate-45 transition-transform text-lg") {
                                +"+"
                            }
                        }
                        p(classes = "mt-3 text-sm text-[#C4C6D0] leading-relaxed border-t border-[#44474F]/50 pt-3") {
                            +item.answer
                        }
                    }
                }
            }
        }

        // Bottom CMP CTA Banner
        section(
            classes =
                "mt-8 rounded-3xl bg-gradient-to-r from-[#00458F]/80 via-[#1E2025] to-[#1A1C20] " +
                    "border border-[#A8C7FA]/35 p-8 sm:p-12 text-center space-y-6",
        ) {
            h2(classes = "text-2xl sm:text-4xl font-extrabold text-[#E2E2E9] tracking-tight") {
                +"Ready to Claim Your Next Free Course?"
            }
            p(classes = "max-w-2xl mx-auto text-sm sm:text-base text-[#C4C6D0]") {
                +"Open the full Compose Multiplatform Web App right in your browser, "
                +"or star the project on GitHub to support open-source Kotlin education tools."
            }
            div(classes = "flex flex-wrap items-center justify-center gap-4 pt-2") {
                a(
                    href = "/app",
                    classes =
                        "inline-flex items-center gap-2 px-6 py-3.5 rounded-full text-sm font-bold " +
                            "bg-[#0B57D0] hover:bg-[#1967D2] text-white shadow-lg shadow-[#0B57D0]/35 transition",
                ) {
                    +"Launch Compose Web App (/app)"
                }
                a(
                    href = GITHUB_KMP_URL,
                    target = "_blank",
                    classes =
                        "inline-flex items-center gap-2 px-6 py-3.5 rounded-full text-sm font-semibold " +
                            "bg-[#1E2025] hover:bg-[#282A2F] text-[#E2E2E9] border border-[#44474F] transition",
                ) {
                    attributes["rel"] = "noopener noreferrer"
                    +"★ Star on GitHub"
                }
                a(
                    href = "mailto:$CONTACT_EMAIL",
                    classes =
                        "inline-flex items-center gap-2 px-6 py-3.5 rounded-full text-sm font-semibold " +
                            "bg-[#1E2025] hover:bg-[#282A2F] text-[#E2E2E9] border border-[#44474F] transition",
                ) {
                    +"Contact Us"
                }
            }
        }
    }

    /**
     * Renders the 4-column CMP Material 3 footer with Product, Open Source, Legal, and Contact links.
     */
    fun FlowContent.renderSaaSFooter() {
        footer(classes = "border-t border-[#44474F]/60 bg-[#111318] pt-14 pb-10 mt-16") {
            div(classes = "max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-10") {
                div(classes = "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-12 gap-8") {
                    // Column 1: Brand & Status (4 cols)
                    div(classes = "lg:col-span-4 space-y-4") {
                        a(href = "/", classes = "flex items-center gap-2.5") {
                            renderCmpLogoMark("w-8 h-8")
                            span(classes = "text-lg font-extrabold text-[#E2E2E9]") {
                                +"CourseDeals"
                            }
                        }
                        p(classes = "text-xs sm:text-sm text-[#C4C6D0] leading-relaxed max-w-sm") {
                            +"Open-source 100% off course coupon aggregator & multiplatform client "
                            +"built with Kotlin, Spring Boot, HTMX, and Compose Multiplatform."
                        }
                        div(
                            classes =
                                "inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs " +
                                    "bg-[#00458F]/50 text-[#D3E3FD] border border-[#A8C7FA]/30",
                        ) {
                            span(classes = "w-2 h-2 rounded-full bg-[#A8C7FA] animate-pulse") {}
                            +"All Systems Operational"
                        }
                    }

                    // Column 2: Product (2 cols)
                    div(classes = "lg:col-span-2 space-y-3") {
                        h3(classes = "text-xs font-bold uppercase tracking-wider text-[#E2E2E9]") {
                            +"Product"
                        }
                        ul(classes = "space-y-2 text-xs sm:text-sm text-[#C4C6D0]") {
                            li {
                                a(href = "/app", classes = "hover:text-[#A8C7FA] transition") {
                                    +"Compose Web App (/app)"
                                }
                            }
                            li {
                                a(href = "/#hot-deals", classes = "hover:text-[#A8C7FA] transition") {
                                    +"Hot Deals Showcase"
                                }
                            }
                            li {
                                a(href = "/#features", classes = "hover:text-[#A8C7FA] transition") {
                                    +"Features"
                                }
                            }
                            li {
                                a(href = "/#platforms", classes = "hover:text-[#A8C7FA] transition") {
                                    +"Mobile & Desktop Apps"
                                }
                            }
                            li {
                                a(href = "/#faq", classes = "hover:text-[#A8C7FA] transition") {
                                    +"FAQ"
                                }
                            }
                        }
                    }

                    // Column 3: Open Source & Developers (3 cols)
                    div(classes = "lg:col-span-3 space-y-3") {
                        h3(classes = "text-xs font-bold uppercase tracking-wider text-[#E2E2E9]") {
                            +"Open Source & API"
                        }
                        ul(classes = "space-y-2 text-xs sm:text-sm text-[#C4C6D0]") {
                            li {
                                a(
                                    href = GITHUB_KMP_URL,
                                    target = "_blank",
                                    classes = "hover:text-[#A8C7FA] transition",
                                ) {
                                    +"KMP Multiplatform Repo ↗"
                                }
                            }
                            li {
                                a(
                                    href = GITHUB_BACKEND_URL,
                                    target = "_blank",
                                    classes = "hover:text-[#A8C7FA] transition",
                                ) {
                                    +"Spring Boot Backend Repo ↗"
                                }
                            }
                            li {
                                a(
                                    href = GITHUB_RELEASES_URL,
                                    target = "_blank",
                                    classes = "hover:text-[#A8C7FA] transition",
                                ) {
                                    +"GitHub Releases (APK / DMG) ↗"
                                }
                            }
                            li {
                                a(
                                    href = "/swagger-ui/index.html",
                                    target = "_blank",
                                    classes = "hover:text-[#A8C7FA] transition",
                                ) {
                                    +"OpenAPI / Swagger UI"
                                }
                            }
                            li {
                                a(
                                    href = "/actuator/health",
                                    target = "_blank",
                                    classes = "hover:text-[#A8C7FA] transition",
                                ) {
                                    +"API Health Status"
                                }
                            }
                        }
                    }

                    // Column 4: Legal & Contact (3 cols)
                    div(classes = "lg:col-span-3 space-y-3") {
                        h3(classes = "text-xs font-bold uppercase tracking-wider text-[#E2E2E9]") {
                            +"Legal & Contact"
                        }
                        ul(classes = "space-y-2 text-xs sm:text-sm text-[#C4C6D0]") {
                            li { a(href = "/terms", classes = "hover:text-[#A8C7FA] transition") { +"Terms of Service" } }
                            li { a(href = "/privacy", classes = "hover:text-[#A8C7FA] transition") { +"Privacy Policy" } }
                            li {
                                a(
                                    href = "mailto:$CONTACT_EMAIL",
                                    classes = "hover:text-[#A8C7FA] transition",
                                ) {
                                    +CONTACT_EMAIL
                                }
                            }
                            li {
                                a(
                                    href = GITHUB_PROFILE_URL,
                                    target = "_blank",
                                    classes = "hover:text-[#A8C7FA] transition",
                                ) {
                                    +"GitHub @Huythanh0x"
                                }
                            }
                        }
                    }
                }

                div(
                    classes =
                        "pt-8 border-t border-[#44474F]/60 flex flex-col sm:flex-row items-center " +
                            "justify-between gap-4 text-xs text-[#8E9099]",
                ) {
                    p {
                        +"© 2026 CourseDeals by Vo Huy Thanh (@Huythanh0x). Open-source MIT License."
                    }
                    div(classes = "flex items-center gap-5") {
                        a(href = "/terms", classes = "hover:text-[#E2E2E9] transition") { +"Terms" }
                        a(href = "/privacy", classes = "hover:text-[#E2E2E9] transition") { +"Privacy" }
                        a(href = GITHUB_KMP_URL, target = "_blank", classes = "hover:text-[#E2E2E9] transition") {
                            +"GitHub"
                        }
                        a(href = "mailto:$CONTACT_EMAIL", classes = "hover:text-[#E2E2E9] transition") {
                            +"Email"
                        }
                    }
                }
            }
        }
    }

    /**
     * Renders the dedicated `/terms` (Terms of Service) page.
     */
    fun renderTermsPage(): String =
        renderLegalDocumentPage(
            pageTitle = "Terms of Service — CourseDeals",
            canonicalPath = "/terms",
            heading = "Terms of Service",
            lastUpdated = "November 5, 2026",
            sections =
                listOf(
                    "1. Acceptance of Terms" to
                        "By accessing or using CourseDeals (https://coupons.thanh0x.com), our Compose " +
                        "Multiplatform Web App (/app), or our mobile/desktop applications, you agree to " +
                        "be bound by these Terms of Service.",
                    "2. Description of Service" to
                        "CourseDeals is a free, open-source aggregator that indexes publicly shared " +
                        "promotional coupons for online learning platforms (such as Udemy). We are not " +
                        "affiliated with, endorsed by, or sponsored by Udemy, Inc. or any course provider.",
                    "3. Coupon Availability & Pricing Disclaimer" to
                        "Third-party course instructors control coupon expiration dates and maximum " +
                        "redemption limits (typically 1,000 redemptions per coupon). While our background " +
                        "crawler verifies coupons hourly, CourseDeals cannot guarantee that a coupon will " +
                        "remain active at the exact moment you click through to the external provider.",
                    "4. User Accounts & Community Submissions" to
                        "Optional user accounts (via Email OTP, Google Sign-In, or Passkeys) allow you to " +
                        "save topic preferences and receive push notifications. When submitting coupon URLs " +
                        "or reporting expired deals, you agree not to submit spam or malicious links.",
                    "5. Open-Source License & Intellectual Property" to
                        "The CourseDeals software source code is licensed under the MIT License and hosted " +
                        "publicly on GitHub ($GITHUB_KMP_URL and $GITHUB_BACKEND_URL). Course titles, " +
                        "preview images, and descriptions belong to their respective instructors.",
                    "6. Contact Information" to
                        "If you have any questions about these Terms of Service, please contact us at " +
                        "$CONTACT_EMAIL or open an issue on GitHub.",
                ),
        )

    /**
     * Renders the dedicated `/privacy` (Privacy Policy) page.
     */
    fun renderPrivacyPage(): String =
        renderLegalDocumentPage(
            pageTitle = "Privacy Policy — CourseDeals",
            canonicalPath = "/privacy",
            heading = "Privacy Policy",
            lastUpdated = "November 5, 2026",
            sections =
                listOf(
                    "1. Information We Collect" to
                        "You can browse CourseDeals and redeem coupons without creating an account. If you " +
                        "choose to sign in or enable notifications, we collect only the minimum data " +
                        "necessary: (a) your email address and basic profile name, (b) your selected course " +
                        "category/keyword preferences, and (c) your Firebase Cloud Messaging (FCM) device " +
                        "token to deliver deal alerts.",
                    "2. Crash Reporting & Diagnostics" to
                        "We use Sentry to collect anonymized crash reports and performance traces so we can " +
                        "diagnose bugs across Android, iOS, watchOS, Desktop, Web, and Backend services. No " +
                        "passwords or raw authentication tokens are ever logged.",
                    "3. How We Use Your Information" to
                        "We use your information solely to authenticate your session, synchronize your " +
                        "saved course preferences, and send relevant coupon notifications. We never sell, " +
                        "rent, or trade your personal data to third parties or advertisers.",
                    "4. Third-Party Links" to
                        "Clicking a course redemption link redirects you to an external provider (such as " +
                        "Udemy.com). Once you leave CourseDeals, the external platform's own Privacy Policy " +
                        "and cookie practices apply.",
                    "5. Data Retention & Account Deletion" to
                        "You may delete your saved preferences, unregister your device token, or request " +
                        "full account deletion at any time by emailing $CONTACT_EMAIL.",
                    "6. Contact Us" to
                        "For privacy inquiries or data requests, contact Vo Huy Thanh at $CONTACT_EMAIL.",
                ),
        )

    private fun renderLegalDocumentPage(
        pageTitle: String,
        canonicalPath: String,
        heading: String,
        lastUpdated: String,
        sections: List<Pair<String, String>>,
    ): String =
        buildString {
            append("<!DOCTYPE html>\n")
            appendHTML().html {
                attributes["lang"] = "en"
                attributes["class"] = "dark"
                renderLegalHead(pageTitle, canonicalPath)
                body(classes = "bg-[#111318] text-[#E2E2E9] min-h-screen flex flex-col antialiased") {
                    renderSaaSHeader()
                    main(classes = "flex-1 max-w-4xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-12 space-y-8") {
                        div(classes = "space-y-2 border-b border-[#44474F]/60 pb-6") {
                            a(
                                href = "/",
                                classes = "text-xs font-semibold text-[#A8C7FA] hover:text-[#D3E3FD]",
                            ) {
                                +"← Back to CourseDeals Home"
                            }
                            h1(classes = "text-3xl sm:text-4xl font-extrabold text-[#E2E2E9] tracking-tight pt-2") {
                                +heading
                            }
                            p(classes = "text-xs sm:text-sm text-[#C4C6D0]") {
                                +"Last updated: $lastUpdated"
                            }
                        }

                        article(classes = "space-y-6") {
                            for ((secTitle, secBody) in sections) {
                                div(
                                    classes =
                                        "bg-[#1E2025] border border-[#44474F]/70 rounded-2xl p-6 space-y-2",
                                ) {
                                    h2(classes = "text-base sm:text-lg font-bold text-[#E2E2E9]") {
                                        +secTitle
                                    }
                                    p(classes = "text-sm text-[#C4C6D0] leading-relaxed") {
                                        +secBody
                                    }
                                }
                            }
                        }
                    }
                    renderSaaSFooter()
                }
            }
        }

    private fun HTML.renderLegalHead(
        pageTitle: String,
        canonicalPath: String,
    ) {
        head {
            meta(charset = "UTF-8")
            meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
            title { +pageTitle }
            link(rel = "icon", type = "image/svg+xml", href = CMP_FAVICON_DATA_URI)
            link(rel = "canonical", href = "https://coupons.thanh0x.com$canonicalPath")
            script(src = "https://cdn.tailwindcss.com") {}
        }
    }
}
