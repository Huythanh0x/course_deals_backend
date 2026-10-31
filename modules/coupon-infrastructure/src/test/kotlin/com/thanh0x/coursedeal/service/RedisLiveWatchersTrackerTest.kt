package com.thanh0x.coursedeal.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyDouble
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ZSetOperations
import java.time.Duration

class RedisLiveWatchersTrackerTest {
    private lateinit var redisTemplate: RedisTemplate<String, Any>
    private lateinit var zSetOps: ZSetOperations<String, Any>
    private lateinit var tracker: RedisLiveWatchersTracker

    @Suppress("UNCHECKED_CAST")
    @BeforeEach
    fun setup() {
        redisTemplate = mock(RedisTemplate::class.java) as RedisTemplate<String, Any>
        zSetOps = mock(ZSetOperations::class.java) as ZSetOperations<String, Any>
        `when`(redisTemplate.opsForZSet()).thenReturn(zSetOps)
        tracker = RedisLiveWatchersTracker(redisTemplate)
    }

    @Test
    fun `recordHeartbeat adds to zset and evicts expired`() {
        val courseId = 42
        val viewerId = "session-123"
        val key = "deal:42:viewers"

        `when`(zSetOps.zCard(key)).thenReturn(5L)

        val count = tracker.recordHeartbeat(courseId, viewerId, Duration.ofSeconds(30))

        assertEquals(5L, count)
        verify(zSetOps).add(eq(key), eq(viewerId), anyDouble())
        verify(zSetOps).removeRangeByScore(eq(key), eq(0.0), anyDouble())
        verify(redisTemplate).expire(eq(key), any(Duration::class.java))
    }

    @Test
    fun `removeWatcher removes member and returns updated card`() {
        val courseId = 42
        val viewerId = "session-123"
        val key = "deal:42:viewers"

        `when`(zSetOps.zCard(key)).thenReturn(4L)

        val count = tracker.removeWatcher(courseId, viewerId)

        assertEquals(4L, count)
        verify(zSetOps).remove(key, viewerId)
        verify(zSetOps).removeRangeByScore(eq(key), eq(0.0), anyDouble())
    }

    @Test
    fun `getActiveWatchersCount purges expired and returns card`() {
        val courseId = 42
        val key = "deal:42:viewers"

        `when`(zSetOps.zCard(key)).thenReturn(18L)

        val count = tracker.getActiveWatchersCount(courseId)

        assertEquals(18L, count)
        verify(zSetOps).removeRangeByScore(eq(key), eq(0.0), anyDouble())
        verify(zSetOps).zCard(key)
    }
}
