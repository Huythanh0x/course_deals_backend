package com.thanh0x.coursedeal.crawlerrunner

import com.thanh0x.coursedeal.config.logger
import com.thanh0x.coursedeal.crawlerrunner.fetcher.WebContentFetcher
import com.thanh0x.coursedeal.model.coupon.CouponCourseData
import com.thanh0x.coursedeal.model.coupon.CouponJsonData
import com.thanh0x.coursedeal.model.coupon.CourseJsonData
import com.thanh0x.coursedeal.model.coupon.CourseLevel
import com.thanh0x.coursedeal.utils.UrlUtils
import org.json.JSONObject
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * A class that extracts course coupon data from the provided coupon URL.
 */
@Suppress("TooManyFunctions")
class CourseDataExtractor {
    private val log = logger()
    private val couponUrl: String
    var courseId: Int = 0
        private set
    private var couponCode: String = ""

    companion object {
        private const val FALLBACK_EXPIRY_SECONDS = 4 * 24 * 60 * 60L
        private val DISCOVER_DEEPLINK_ID_REGEX = Regex("""udemy://discover\?courseId=(\d+)""")
        private const val MIN_TIMESTAMP_LENGTH = 19
        private const val TIMEZONE_OFFSET_LENGTH = 5
    }

    /**
     * Creates a new CourseDataExtractor with the given coupon URL.
     * Will attempt to extract courseId from the URL's HTML page.
     *
     * @param couponUrl The coupon URL to extract data from
     */
    constructor(couponUrl: String) {
        this.couponUrl = couponUrl
        this.courseId = extractCourseId()
        this.couponCode = extractCouponCode()
    }

    /**
     * Creates a new CourseDataExtractor with the given coupon URL and courseId.
     * Skips the expensive HTTP request to extract courseId from HTML.
     *
     * @param couponUrl The coupon URL to extract data from
     * @param courseId The course ID (if already known from database)
     */
    constructor(couponUrl: String, courseId: Int) {
        this.couponUrl = couponUrl
        this.courseId = courseId
        this.couponCode = extractCouponCode()
    }

    /**
     * Extracts the course ID from the HTML document retrieved from a given coupon URL.
     */
    private fun extractCourseId(): Int {
        val document = WebContentFetcher().getHtmlDocumentFrom(couponUrl)
        if (document == null) {
            log.warn("Unable to load document for coupon URL {}", couponUrl)
            return -1
        }

        val html = document.html()
        val resolvedId =
            document.body().attr("data-clp-course-id").toIntOrNull()
                ?: extractCourseIdFromDiscoverDeeplink(html).takeIf { it > 0 }
                ?: extractCourseIdFromNextData(html).takeIf { it > 0 }
                ?: extractCourseIdFromImage(html).takeIf { it > 0 }

        if (resolvedId == null) {
            log.warn("Course id not found in HTML for {}", couponUrl)
        }
        return resolvedId ?: -1
    }

    private fun extractCourseIdFromDiscoverDeeplink(html: String?): Int {
        if (html.isNullOrEmpty()) return -1
        return DISCOVER_DEEPLINK_ID_REGEX.find(html)?.groupValues?.get(1)?.toIntOrNull() ?: -1
    }

    private fun extractCourseIdFromNextData(html: String?): Int {
        if (html.isNullOrEmpty()) return -1
        val regex = Regex(""""course"\s*:\s*\{\s*"id"\s*:\s*"?(\d+)"?""")
        return regex.find(html)?.groupValues?.get(1)?.toIntOrNull() ?: -1
    }

    private fun extractCourseIdFromImage(html: String?): Int {
        if (html.isNullOrEmpty()) return -1
        val regex = Regex("""udemycdn\.com/course/\d+x\d+/(\d+)_""")
        return regex.find(html)?.groupValues?.get(1)?.toIntOrNull() ?: -1
    }

    /**
     * Extracts the coupon code from a given coupon URL by parsing the query parameter.
     *
     * @return the extracted coupon code
     */
    private fun extractCouponCode(): String {
        val query = couponUrl.substringAfter('?', "")
        if (query.isEmpty()) return ""
        return query.split('&')
            .firstOrNull { it.startsWith("couponCode=", ignoreCase = true) }
            ?.substringAfter('=')
            ?.substringBefore('&')
            ?.substringBefore('#')
            ?: ""
    }

    /**
     * Retrieves full coupon code data for a specific course.
     * Makes API calls to extract coupon data and course data from official APIs.
     * Combines the extracted data to create a CouponCourseData object.
     *
     * @return CouponCourseData object containing both coupon data and course data
     */
    fun getFullCouponCodeData(): CouponCourseData? {
        if (couponCode.isBlank()) {
            log.debug("Skipping extraction for {}: couponCode is blank", couponUrl)
            return null
        }
        val couponJson = WebContentFetcher.getJsonObjectFrom(UrlUtils.getCouponAPI(courseId, couponCode))
        val courseJson = WebContentFetcher.getJsonObjectFrom(UrlUtils.getCourseAPI(courseId))

        val couponDataResult = extractDataCouponFromOfficialAPI(couponJson)
        val courseDataResult = extractCourseDataFromOfficialAPI(courseJson)
        return combineCourseAndCouponData(couponDataResult, courseDataResult)
    }

    /**
     * Extracts course data from the provided JSONObject representing a course object in an official API response.
     *
     * @param courseObjectJson the JSONObject containing course data
     * @return CourseJsonData object with extracted course information
     */
    internal fun extractCourseDataFromOfficialAPI(courseObjectJson: JSONObject?): CourseJsonData? {
        if (courseObjectJson == null) return null

        val title = courseObjectJson.optString("title", "")
        val headline = courseObjectJson.optString("headline", "")
        val description = courseObjectJson.optString("description", "").trim().replace("\n", "")
        val author =
            courseObjectJson.optJSONArray("visible_instructors")
                ?.takeIf { !it.isEmpty }
                ?.optJSONObject(0)
                ?.optString("title", "Unknown") ?: "Unknown"
        val category = courseObjectJson.optJSONObject("primary_category")?.optString("title", "Unknown") ?: "Unknown"
        val subCategory =
            courseObjectJson.optJSONObject("primary_sub_category")?.optString("title", "Unknown") ?: "Unknown"
        val language = courseObjectJson.optJSONObject("locale")?.optString("simple_english_title", "") ?: ""
        val instructionalLevel = courseObjectJson.optString("instructional_level", "")
        val level =
            if (instructionalLevel.contains("Levels")) {
                instructionalLevel
            } else {
                instructionalLevel.replace(" Level", "")
            }
        val students = courseObjectJson.optInt("num_subscribers", 0)
        val rating = courseObjectJson.optFloat("avg_rating_recent", 0.0f)
        val numberReviews = courseObjectJson.optInt("num_reviews", 0)
        val contentLength = courseObjectJson.optInt("estimated_content_length", 0)

        return CourseJsonData(
            category, subCategory, title, level, author, contentLength, rating, numberReviews, students,
            language, headline, description,
        )
    }

    /**
     * Extracts data from the given coupon JSON object retrieved from the official API.
     *
     * @param couponJsonObject JSON object containing coupon data
     * @return CouponJsonData object with extracted data
     */
    @Suppress("ReturnCount")
    internal fun extractDataCouponFromOfficialAPI(couponJsonObject: JSONObject?): CouponJsonData? {
        if (couponCode.isBlank() || couponJsonObject == null || couponJsonObject.has("detail")) {
            return null
        }

        val pricingResultObj =
            couponJsonObject.optJSONObject("price_text")
                ?.optJSONObject("data")
                ?.optJSONObject("pricing_result")
                ?: return null

        if (!isValidPromotionalPricing(pricingResultObj)) {
            return null
        }

        val campaignObj = pricingResultObj.optJSONObject("campaign") ?: return null
        val usesRemaining = campaignObj.optInt("uses_remaining", 0)
        val resolvedCouponCode =
            campaignObj.optString("code", "").takeIf { it.isNotBlank() }
                ?: couponCode.substringBefore('&').trim()
        this.couponCode = resolvedCouponCode

        val expiredDate = parseExpiredDate(campaignObj.optString("end_time", null))
        val (previewImage, previewVideo) = extractPreviewAssets(couponJsonObject)
        return CouponJsonData(
            price = 0f,
            expiredDate = expiredDate,
            previewImage = previewImage,
            previewVideo = previewVideo,
            usesRemaining = usesRemaining,
            couponCode = resolvedCouponCode,
        )
    }

    @Suppress("ReturnCount")
    private fun isValidPromotionalPricing(pricingResultObj: JSONObject): Boolean {
        val price = pricingResultObj.optJSONObject("price")?.optFloat("amount", Float.NaN)
        if (price == null || java.lang.Float.isNaN(price) || price != 0f) {
            return false
        }

        val campaignObj = pricingResultObj.optJSONObject("campaign")
        val usesRemaining = campaignObj?.optInt("uses_remaining", 0) ?: 0
        if (campaignObj == null || usesRemaining <= 0) {
            log.info(
                "Coupon code {} on course {} has no active campaign or 0 uses remaining",
                couponCode,
                courseId,
            )
            return false
        }

        val listPrice = pricingResultObj.optJSONObject("list_price")?.optFloat("amount", 0f) ?: 0f
        val hasDiscountSaving = pricingResultObj.optBoolean("has_discount_saving", false)
        if (!hasDiscountSaving || listPrice <= 0f) {
            log.info(
                "Course {} is Always-Free or has no discount saving (listPrice={}, hasDiscountSaving={})",
                courseId,
                listPrice,
                hasDiscountSaving,
            )
            return false
        }
        return true
    }

    private fun extractPreviewAssets(couponJsonObject: JSONObject): Pair<String, String> {
        val introductionAsset =
            couponJsonObject.optJSONObject("sidebar_container")
                ?.optJSONObject("componentProps")
                ?.optJSONObject("introductionAsset")

        val previewImage = introductionAsset?.optJSONObject("images")?.optString("image_750x422", "") ?: ""
        val previewVideo = introductionAsset?.optString("course_preview_path", "") ?: ""
        return previewImage to previewVideo
    }

    /**
     * Parses an ISO 8601 date string to Instant (UTC).
     * Handles formats like:
     * - "2030-05-19 17:24:00+00:00" (Udemy format with space and timezone)
     * - "2030-05-19T17:24:00Z" (ISO 8601 standard)
     * - "2030-05-19T17:24:00+00:00" (ISO 8601 with timezone)
     *
     * @param dateStr The date string to parse
     * @return Instant representing the date in UTC
     */
    internal fun parseExpiredDate(dateStr: String?): Instant {
        if (dateStr.isNullOrBlank()) return Instant.now().plusSeconds(FALLBACK_EXPIRY_SECONDS)

        return try {
            val trimmed = dateStr.trim()
            when {
                dateStr.contains("T") -> Instant.parse(dateStr)
                trimmed.contains(" ") -> Instant.parse(normalizeSpaceSeparatedTimestamp(trimmed))
                else -> OffsetDateTime.parse(dateStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant()
            }
        } catch (e: DateTimeParseException) {
            log.debug("Failed to parse expired date string '{}', defaulting to 4-day fallback", dateStr, e)
            Instant.now().plusSeconds(FALLBACK_EXPIRY_SECONDS)
        }
    }

    /**
     * Converts Udemy's "yyyy-MM-dd HH:mm:ss[+/-HH:mm|Z]" format to ISO 8601, assuming UTC if no
     * timezone is present.
     */
    private fun normalizeSpaceSeparatedTimestamp(trimmed: String): String {
        var normalized = trimmed.replaceFirst(" ", "T")
        val timezoneStart = normalized.indexOf("-", MIN_TIMESTAMP_LENGTH)
        val hasNegativeOffset = timezoneStart > 0 && normalized.length > timezoneStart + TIMEZONE_OFFSET_LENGTH
        val hasTimezone = normalized.contains("+") || normalized.contains("Z") || hasNegativeOffset
        if (!hasTimezone) normalized += "Z"
        return normalized
    }

    /**
     * Combines course and coupon data to create a new CouponCourseData object.
     * If the coupon price is not 0, null is returned.
     *
     * @param couponData The CouponJsonData object containing coupon information
     * @param courseData The CourseJsonData object containing course information
     * @return A new CouponCourseData object with combined data from course and coupon
     */
    @Suppress("ReturnCount")
    internal fun combineCourseAndCouponData(
        couponData: CouponJsonData?,
        courseData: CourseJsonData?,
    ): CouponCourseData? {
        if (couponData == null || courseData == null || couponData.price != 0f) return null
        val effectiveCouponCode = couponData.couponCode?.takeIf { it.isNotBlank() } ?: couponCode
        if (effectiveCouponCode.isBlank() || couponData.usesRemaining <= 0) return null
        return CouponCourseData(
            courseId = courseId,
            category = courseData.category,
            subCategory = courseData.subCategory,
            title = courseData.courseTitle,
            contentLength = courseData.contentLength,
            level = CourseLevel.fromString(courseData.level),
            author = courseData.author,
            rating = courseData.rating,
            reviews = courseData.numberReviews,
            students = courseData.students,
            couponCode = effectiveCouponCode,
            previewImage = couponData.previewImage,
            couponUrl = couponUrl,
            expiredDate = couponData.expiredDate,
            usesRemaining = couponData.usesRemaining,
            heading = courseData.headline,
            description = courseData.description,
            previewVideo = couponData.previewVideo,
            language = courseData.language,
        )
    }
}
