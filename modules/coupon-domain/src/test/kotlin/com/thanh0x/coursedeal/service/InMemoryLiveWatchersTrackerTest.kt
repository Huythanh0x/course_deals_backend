package com.thanh0x.coursedeal.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

class InMemoryLiveWatchersTrackerTest {
    private lateinit var tracker: InMemoryLiveWatchersTracker

    @BeforeEach
    fun setup() {
        tracker = InMemoryLiveWatchersTracker()
    }

    @Test
    fun `records multiple heartbeats and increments active watchers count`() {
        val courseId = 101

        assertEquals(0L, tracker.getActiveWatchersCount(courseId))

        val count1 = tracker.recordHeartbeat(courseId, "user-1", Duration.ofSeconds(30))
        assertEquals(1L, count1)

        val count2 = tracker.recordHeartbeat(courseId, "user-2", Duration.ofSeconds(30))
        assertEquals(2L, count2)

        // Same user heartbeat does not increase count
        val count1Again = tracker.recordHeartbeat(courseId, "user-1", Duration.ofSeconds(30))
        assertEquals(2L, count1Again)
        assertEquals(2L, tracker.getActiveWatchersCount(courseId))
    }

    @Test
    fun `removes watcher decreases active count`() {
        val courseId = 102
        tracker.recordHeartbeat(courseId, "user-1", Duration.ofSeconds(30))
        tracker.recordHeartbeat(courseId, "user-2", Duration.ofSeconds(30))

        val remaining = tracker.removeWatcher(courseId, "user-1")
        assertEquals(1L, remaining)
        assertEquals(1L, tracker.getActiveWatchersCount(courseId))

        tracker.removeWatcher(courseId, "user-2")
        assertEquals(0L, tracker.getActiveWatchersCount(courseId))
    }

    @Test
    fun `evicts expired watchers`() {
        val courseId = 103
        // Add watcher with negative TTL so it is already expired
        tracker.recordHeartbeat(courseId, "expired-user", Duration.ofMillis(-100))

        assertEquals(0L, tracker.getActiveWatchersCount(courseId))
    }
}
