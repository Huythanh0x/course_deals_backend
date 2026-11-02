package com.thanh0x.coursedeal.crawlerrunner

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CourseDataExtractorTest {
    @Test
    fun `extracts coupon code when followed by query parameters`() {
        val url = "https://www.udemy.com/course/claude-ai/?couponCode=KEEPLEARNING&rand=999502"
        val extractor = CourseDataExtractor(url, 6553707)
        // Using reflection to inspect private couponCode
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
}
