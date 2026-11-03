package com.thanh0x.coursedeal.service

import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory fallback implementation of LiveWatchersTracker with heartbeat expiration.
 * Used when Redis is not available or in standalone/test profiles.
 */
class InMemoryLiveWatchersTracker : LiveWatchersTracker {
    // Map of courseId -> Map of viewerId -> expiration epoch millis
    private val courseWatchers = ConcurrentHashMap<Int, ConcurrentHashMap<String, Long>>()

    override fun recordHeartbeat(
        courseId: Int,
        viewerId: String,
        ttl: Duration,
    ): Long {
        val now = Instant.now().toEpochMilli()
        val expiresAt = now + ttl.toMillis()
        val watchers = courseWatchers.computeIfAbsent(courseId) { ConcurrentHashMap() }
        watchers[viewerId] = expiresAt
        evictExpired(watchers, now)
        return watchers.size.toLong()
    }

    override fun removeWatcher(
        courseId: Int,
        viewerId: String,
    ): Long {
        val now = Instant.now().toEpochMilli()
        val watchers = courseWatchers[courseId] ?: return 0L
        watchers.remove(viewerId)
        evictExpired(watchers, now)
        if (watchers.isEmpty()) {
            courseWatchers.remove(courseId, watchers)
        }
        return watchers.size.toLong()
    }

    override fun getActiveWatchersCount(courseId: Int): Long {
        val now = Instant.now().toEpochMilli()
        val watchers = courseWatchers[courseId] ?: return 0L
        evictExpired(watchers, now)
        if (watchers.isEmpty()) {
            courseWatchers.remove(courseId, watchers)
        }
        return watchers.size.toLong()
    }

    private fun evictExpired(
        watchers: ConcurrentHashMap<String, Long>,
        now: Long,
    ) {
        watchers.entries.removeIf { it.value <= now }
    }
}
