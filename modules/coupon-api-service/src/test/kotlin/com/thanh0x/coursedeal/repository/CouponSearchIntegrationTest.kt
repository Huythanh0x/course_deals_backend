package com.thanh0x.coursedeal.repository

import com.thanh0x.coursedeal.dto.CouponSearchCriteriaDTO
import com.thanh0x.coursedeal.model.coupon.CouponCourseData
import com.thanh0x.coursedeal.model.coupon.CourseLevel
import com.thanh0x.coursedeal.repository.specification.CouponCourseSpecifications
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.context.ActiveProfiles
import java.time.Instant

@DataJpaTest
@ActiveProfiles("test")
class CouponSearchIntegrationTest {
    @Autowired
    private lateinit var repository: CouponCourseRepository

    @BeforeEach
    @Suppress("LongMethod")
    fun setup() {
        repository.deleteAll()

        repository.saveAll(
            listOf(
                CouponCourseData(
                    courseId = 1,
                    title = "Complete Python Bootcamp 2026",
                    heading = "From Zero to Hero in Python",
                    description = "Learn Python like a Pro! Basics to advanced apps.",
                    category = "Development",
                    author = "Jose Portilla",
                    rating = 4.6f,
                    reviews = 12000,
                    students = 50000,
                    language = "English",
                    level = CourseLevel.BEGINNER,
                    expiredDate = Instant.now().plusSeconds(86400),
                ),
                CouponCourseData(
                    courseId = 2,
                    title = "Machine Learning A-Z: AI, Python & R",
                    heading = "Learn to create ML Algorithms",
                    description = "Master Machine Learning on Python & R.",
                    category = "Data Science",
                    author = "Kirill Eremenko",
                    rating = 4.3f,
                    reviews = 8000,
                    students = 35000,
                    language = "English",
                    level = CourseLevel.INTERMEDIATE,
                    expiredDate = Instant.now().plusSeconds(7200),
                ),
                CouponCourseData(
                    courseId = 3,
                    title = "Aprende Desarrollo Web con JavaScript",
                    heading = "Curso Completo de Desarrollo Web",
                    description = "Domina HTML, CSS, JavaScript y crea aplicaciones.",
                    category = "Development",
                    author = "Carlos Silva",
                    rating = 4.8f,
                    reviews = 3000,
                    students = 15000,
                    language = "Spanish",
                    level = CourseLevel.ALL_LEVELS,
                    expiredDate = Instant.now().plusSeconds(43200),
                ),
                CouponCourseData(
                    courseId = 4,
                    title = "Expired Python Mastery Course",
                    heading = "Old Python Course",
                    description = "This deal has already expired.",
                    category = "Development",
                    author = "Jose Portilla",
                    rating = 4.9f,
                    reviews = 500,
                    students = 2000,
                    language = "English",
                    level = CourseLevel.EXPERT,
                    expiredDate = Instant.now().minusSeconds(3600),
                ),
            ),
        )
    }

    @Test
    fun `search filters out expired coupons`() {
        val now = Instant.now()
        val spec =
            CouponCourseSpecifications.withCriteria(
                CouponSearchCriteriaDTO(q = "Python"),
                now,
            )
        val result = repository.findAll(spec, PageRequest.of(0, 10))

        assertEquals(2, result.totalElements)
        assertTrue(result.content.none { it.courseId == 4 })
    }

    @Test
    fun `search by free-text q across title and description`() {
        val now = Instant.now()
        val spec =
            CouponCourseSpecifications.withCriteria(
                CouponSearchCriteriaDTO(q = "algorithms"),
                now,
            )
        val result = repository.findAll(spec, PageRequest.of(0, 10))

        assertEquals(1, result.totalElements)
        assertEquals(2, result.content[0].courseId)
    }

    @Test
    fun `search with multi-faceted filters category minRating language`() {
        val now = Instant.now()
        val criteria =
            CouponSearchCriteriaDTO(
                category = "Development",
                minRating = 4.5f,
                language = "Spanish",
            )
        val spec = CouponCourseSpecifications.withCriteria(criteria, now)
        val result = repository.findAll(spec, PageRequest.of(0, 10))

        assertEquals(1, result.totalElements)
        assertEquals(3, result.content[0].courseId)
        assertEquals("Carlos Silva", result.content[0].author)
    }

    @Test
    fun `search with instructor filter`() {
        val now = Instant.now()
        val criteria =
            CouponSearchCriteriaDTO(
                instructor = "Kirill",
            )
        val spec = CouponCourseSpecifications.withCriteria(criteria, now)
        val result = repository.findAll(spec, PageRequest.of(0, 10))

        assertEquals(1, result.totalElements)
        assertEquals("Kirill Eremenko", result.content[0].author)
    }

    @Test
    fun `search with level filter`() {
        val now = Instant.now()
        val criteria =
            CouponSearchCriteriaDTO(
                level = "BEGINNER",
            )
        val spec = CouponCourseSpecifications.withCriteria(criteria, now)
        val result = repository.findAll(spec, PageRequest.of(0, 10))

        assertEquals(1, result.totalElements)
        assertEquals(1, result.content[0].courseId)
    }

    @Test
    fun `search with pagination and sorting by rating desc`() {
        val now = Instant.now()
        val criteria = CouponSearchCriteriaDTO()
        val spec = CouponCourseSpecifications.withCriteria(criteria, now)
        val pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "rating"))

        val page1 = repository.findAll(spec, pageable)
        assertEquals(3, page1.totalElements)
        assertEquals(2, page1.content.size)
        assertEquals(3, page1.content[0].courseId) // 4.8 rating
        assertEquals(1, page1.content[1].courseId) // 4.6 rating

        val page2 = repository.findAll(spec, PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "rating")))
        assertEquals(1, page2.content.size)
        assertEquals(2, page2.content[0].courseId) // 4.3 rating
    }
}
