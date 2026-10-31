package com.thanh0x.coursedeal.service

import com.thanh0x.coursedeal.exception.ResourceNotFoundException
import com.thanh0x.coursedeal.model.coupon.CouponCourseData
import com.thanh0x.coursedeal.repository.CouponCourseRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.Instant

class LiveWatchersServiceTest {
    private lateinit var tracker: InMemoryLiveWatchersTracker
    private lateinit var repository: CouponCourseRepository
    private lateinit var service: LiveWatchersService

    @BeforeEach
    fun setup() {
        tracker = InMemoryLiveWatchersTracker()
        repository = mock(CouponCourseRepository::class.java)
        service = LiveWatchersService(tracker, repository)
    }

    @AfterEach
    fun tearDown() {
        service.destroy()
    }

    @Test
    fun `subscribe throws ResourceNotFoundException if course not found`() {
        `when`(repository.findByCourseId(999)).thenReturn(null)

        assertThrows(ResourceNotFoundException::class.java) {
            service.subscribe(999)
        }
    }

    @Test
    fun `subscribe throws ResourceNotFoundException if course is expired`() {
        val expiredCourse =
            CouponCourseData(
                courseId = 100,
                expiredDate = Instant.now().minusSeconds(3600),
            )
        `when`(repository.findByCourseId(100)).thenReturn(expiredCourse)

        assertThrows(ResourceNotFoundException::class.java) {
            service.subscribe(100)
        }
    }

    @Test
    fun `subscribe successfully creates emitter and increments watcher count`() {
        val activeCourse =
            CouponCourseData(
                courseId = 100,
                expiredDate = Instant.now().plusSeconds(3600),
            )
        `when`(repository.findByCourseId(100)).thenReturn(activeCourse)

        val emitter1 = service.subscribe(100)
        assertNotNull(emitter1)
        assertEquals(1L, tracker.getActiveWatchersCount(100))

        val emitter2 = service.subscribe(100)
        assertNotNull(emitter2)
        assertEquals(2L, tracker.getActiveWatchersCount(100))
    }

    @Test
    fun `broadcastAllDeals maintains watchers count`() {
        val activeCourse =
            CouponCourseData(
                courseId = 101,
                expiredDate = Instant.now().plusSeconds(3600),
            )
        `when`(repository.findByCourseId(101)).thenReturn(activeCourse)

        service.subscribe(101)
        service.broadcastAllDeals()

        assertEquals(1L, tracker.getActiveWatchersCount(101))
    }
}
