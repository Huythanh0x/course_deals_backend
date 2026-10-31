package com.thanh0x.coursedeal.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ZSetOperations
import java.time.Duration
import java.time.Instant

private const val KEY_GRACE_PERIOD_SECONDS = 30L

/**
 * Redis-backed implementation of LiveWatchersTracker using Redis Sorted Sets (ZSET).
 * Key format: deal:{id}:viewers where values are viewer IDs and scores are epoch millisecond expiry times.
 */
@Suppress("TooGenericExceptionCaught")
class RedisLiveWatchersTracker(
    private val redisTemplate: RedisTemplate<String, Any>,
) : LiveWatchersTracker {
    private val log: Logger = LoggerFactory.getLogger(RedisLiveWatchersTracker::class.java)

    private fun key(courseId: Int): String = "deal:$courseId:viewers"

    override fun recordHeartbeat(
        courseId: Int,
        viewerId: String,
        ttl: Duration,
    ): Long {
        val dealKey = key(courseId)
        val now = Instant.now().toEpochMilli()
        val expiresAt = now + ttl.toMillis()

        return try {
            val zSetOps: ZSetOperations<String, Any> = redisTemplate.opsForZSet()
            // Add or update the viewer's expiration score
            zSetOps.add(dealKey, viewerId, expiresAt.toDouble())
            // Remove viewers whose heartbeat expired (score < now)
            zSetOps.removeRangeByScore(dealKey, 0.0, now.toDouble())
            // Refresh overall key TTL so abandoned keys don't leak
            redisTemplate.expire(dealKey, ttl.plusSeconds(KEY_GRACE_PERIOD_SECONDS))

            val count = zSetOps.zCard(dealKey) ?: 0L
            count
        } catch (e: Exception) {
            log.error(
                "Failed to record heartbeat in Redis for courseId={} viewerId={}: {}",
                courseId,
                viewerId,
                e.message,
            )
            0L
        }
    }

    override fun removeWatcher(
        courseId: Int,
        viewerId: String,
    ): Long {
        val dealKey = key(courseId)
        val now = Instant.now().toEpochMilli()

        return try {
            val zSetOps: ZSetOperations<String, Any> = redisTemplate.opsForZSet()
            zSetOps.remove(dealKey, viewerId)
            zSetOps.removeRangeByScore(dealKey, 0.0, now.toDouble())
            val count = zSetOps.zCard(dealKey) ?: 0L
            count
        } catch (e: Exception) {
            log.error(
                "Failed to remove watcher from Redis for courseId={} viewerId={}: {}",
                courseId,
                viewerId,
                e.message,
            )
            0L
        }
    }

    override fun getActiveWatchersCount(courseId: Int): Long {
        val dealKey = key(courseId)
        val now = Instant.now().toEpochMilli()

        return try {
            val zSetOps: ZSetOperations<String, Any> = redisTemplate.opsForZSet()
            // Evict expired
            zSetOps.removeRangeByScore(dealKey, 0.0, now.toDouble())
            val count = zSetOps.zCard(dealKey) ?: 0L
            count
        } catch (e: Exception) {
            log.error(
                "Failed to get active watchers count from Redis for courseId={}: {}",
                courseId,
                e.message,
            )
            0L
        }
    }
}
