package com.thanh0x.coursedeal.crawlerrunner

import com.thanh0x.coursedeal.model.coupon.CourseJsonData
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class CourseDataExtractorTest {
    @Test
    fun `extracts coupon code when followed by query parameters`() {
        val url = "https://www.udemy.com/course/claude-ai/?couponCode=KEEPLEARNING&rand=999502"
        val extractor = CourseDataExtractor(url, 6553707)
        val field = CourseDataExtractor::class.java.getDeclaredField("couponCode")
        field.isAccessible = true
        val code = field.get(extractor) as String

        assertEquals("KEEPLEARNING", code)
    }

    @Test
    fun `extracts coupon code without preceding slash`() {
        val extractor = CourseDataExtractor("https://www.udemy.com/course/claude-ai?couponCode=FREE100", 6553707)
        val field = CourseDataExtractor::class.java.getDeclaredField("couponCode")
        field.isAccessible = true
        val code = field.get(extractor) as String

        assertEquals("FREE100", code)
    }

    @Test
    fun `extracts empty coupon code when none present`() {
        val extractor = CourseDataExtractor("https://www.udemy.com/course/claude-ai/", 6553707)
        val field = CourseDataExtractor::class.java.getDeclaredField("couponCode")
        field.isAccessible = true
        val code = field.get(extractor) as String

        assertEquals("", code)
    }

    @Test
    fun `happy path - genuine 100 percent off paid course returns valid CouponJsonData`() {
        val extractor =
            CourseDataExtractor(
                "https://www.udemy.com/course/paid-course/?couponCode=PROMO2026&utm_source=test",
                1234567,
            )
        val json =
            JSONObject(
                """
                {
                  "price_text": {
                    "data": {
                      "pricing_result": {
                        "price": { "amount": 0.0 },
                        "list_price": { "amount": 399000.0 },
                        "has_discount_saving": true,
                        "campaign": {
                          "code": "PROMO2026",
                          "uses_remaining": 49,
                          "end_time": "2026-12-06 07:42:00+00:00"
                        }
                      }
                    }
                  }
                }
                """.trimIndent(),
            )

        val result = extractor.extractDataCouponFromOfficialAPI(json)

        assertNotNull(result)
        assertEquals(0.0f, result?.price)
        assertEquals(49, result?.usesRemaining)
        assertEquals("PROMO2026", result?.couponCode)
        assertEquals(Instant.parse("2026-12-06T07:42:00Z"), result?.expiredDate)

        val courseData =
            CourseJsonData(
                category = "Development",
                subCategory = "Programming Languages",
                courseTitle = "Mastering Kotlin",
                level = "All Levels",
                author = "Jane Doe",
                contentLength = 120,
                rating = 4.8f,
                numberReviews = 250,
                students = 5000,
                language = "English",
                headline = "Learn Kotlin fast",
                description = "Full Kotlin guide",
            )
        val combined = extractor.combineCourseAndCouponData(result, courseData)
        assertNotNull(combined)
        assertEquals("PROMO2026", combined?.couponCode)
        assertEquals(49, combined?.usesRemaining)
    }

    @Test
    fun `failed path - rejects Always-Free course 4202336 with campaign null and list_price 0`() {
        val extractor =
            CourseDataExtractor(
                "https://www.udemy.com/course/object-oriented-programming-in-cpp/?couponCode=FAKECODE",
                4202336,
            )
        val alwaysFreeJson =
            JSONObject(
                """
                {
                  "price_text": {
                    "data": {
                      "pricing_result": {
                        "price": { "amount": 0.0 },
                        "list_price": { "amount": 0.0 },
                        "has_discount_saving": false,
                        "campaign": null
                      }
                    }
                  }
                }
                """.trimIndent(),
            )

        val result = extractor.extractDataCouponFromOfficialAPI(alwaysFreeJson)
        assertNull(result)
    }

    @Test
    fun `failed path - rejects paid course when coupon campaign is null or uses_remaining is 0`() {
        val extractor =
            CourseDataExtractor(
                "https://www.udemy.com/course/paid-course/?couponCode=EXPIRED100",
                998877,
            )
        val nullCampaignJson =
            JSONObject(
                """
                {
                  "price_text": {
                    "data": {
                      "pricing_result": {
                        "price": { "amount": 0.0 },
                        "list_price": { "amount": 399000.0 },
                        "has_discount_saving": true,
                        "campaign": null
                      }
                    }
                  }
                }
                """.trimIndent(),
            )
        assertNull(extractor.extractDataCouponFromOfficialAPI(nullCampaignJson))

        val zeroUsesJson =
            JSONObject(
                """
                {
                  "price_text": {
                    "data": {
                      "pricing_result": {
                        "price": { "amount": 0.0 },
                        "list_price": { "amount": 399000.0 },
                        "has_discount_saving": true,
                        "campaign": {
                          "code": "EXPIRED100",
                          "uses_remaining": 0,
                          "end_time": "2026-12-06 07:42:00+00:00"
                        }
                      }
                    }
                  }
                }
                """.trimIndent(),
            )
        assertNull(extractor.extractDataCouponFromOfficialAPI(zeroUsesJson))
    }

    @Test
    fun `failed path - rejects course when couponCode is blank`() {
        val extractor = CourseDataExtractor("https://www.udemy.com/course/bare-url-without-coupon/", 4202336)
        val validLookingJson =
            JSONObject(
                """
                {
                  "price_text": {
                    "data": {
                      "pricing_result": {
                        "price": { "amount": 0.0 },
                        "list_price": { "amount": 399000.0 },
                        "has_discount_saving": true,
                        "campaign": {
                          "code": "ANYCODE",
                          "uses_remaining": 100,
                          "end_time": "2026-12-06 07:42:00+00:00"
                        }
                      }
                    }
                  }
                }
                """.trimIndent(),
            )

        assertNull(extractor.extractDataCouponFromOfficialAPI(validLookingJson))
        assertNull(extractor.getFullCouponCodeData())
    }

    @Test
    fun `error path - unparseable end_time falls back to 4 days from now instead of 2030`() {
        val extractor =
            CourseDataExtractor(
                "https://www.udemy.com/course/paid-course/?couponCode=VALID100",
                1234567,
            )
        val malformedDateJson =
            JSONObject(
                """
                {
                  "price_text": {
                    "data": {
                      "pricing_result": {
                        "price": { "amount": 0.0 },
                        "list_price": { "amount": 199000.0 },
                        "has_discount_saving": true,
                        "campaign": {
                          "uses_remaining": 25,
                          "end_time": "not-a-valid-timestamp"
                        }
                      }
                    }
                  }
                }
                """.trimIndent(),
            )

        val before = Instant.now()
        val result = extractor.extractDataCouponFromOfficialAPI(malformedDateJson)
        val after = Instant.now()

        assertNotNull(result)
        val expiredDate = result!!.expiredDate!!
        val expectedMin = before.plus(Duration.ofDays(4)).minusSeconds(5)
        val expectedMax = after.plus(Duration.ofDays(4)).plusSeconds(5)

        assertTrue(
            !expiredDate.isBefore(expectedMin) && !expiredDate.isAfter(expectedMax),
            "Expected fallback expiry ~4 days from now, but got $expiredDate",
        )
        assertTrue(expiredDate.isBefore(Instant.parse("2029-01-01T00:00:00Z")))
        assertEquals("VALID100", result.couponCode)
    }
}
