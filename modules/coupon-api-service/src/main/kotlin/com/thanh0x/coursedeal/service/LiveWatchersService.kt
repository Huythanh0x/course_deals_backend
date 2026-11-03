package com.thanh0x.coursedeal.service

import com.thanh0x.coursedeal.config.logger
import com.thanh0x.coursedeal.dto.LiveWatchersEventDTO
import com.thanh0x.coursedeal.exception.ResourceNotFoundException
import com.thanh0x.coursedeal.repository.CouponCourseRepository
import org.springframework.beans.factory.DisposableBean
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Service managing real-time Server-Sent Events (SSE) connections for live coupon deal watchers.
 * Streams deal watcher count every 10 seconds and on initial connection.
 */
@Suppress("TooGenericExceptionCaught")
@Service
class LiveWatchersService(
    private val liveWatchersTracker: LiveWatchersTracker,
    private val couponCourseRepository: CouponCourseRepository,
) : DisposableBean {
    private val log = logger()

    private val scheduledExecutor: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "live-watchers-broadcast").apply { isDaemon = true }
        }

    // Map: courseId -> (viewerId -> SseEmitter)
    private val dealEmitters = ConcurrentHashMap<Int, ConcurrentHashMap<String, SseEmitter>>()

    init {
        // Broadcast every 10 seconds
        scheduledExecutor.scheduleAtFixedRate(
            { broadcastAllDeals() },
            BROADCAST_INTERVAL_SECONDS,
            BROADCAST_INTERVAL_SECONDS,
            TimeUnit.SECONDS,
        )
    }

    /**
     * Subscribes a client to the SSE live watcher stream for a given course ID.
     */
    fun subscribe(courseId: Int): SseEmitter {
        // Validate course existence and validity
        val course = couponCourseRepository.findByCourseId(courseId)
        val isExpired = course?.expiredDate?.let { it <= Instant.now() } ?: false
        if (course == null || isExpired) {
            throw ResourceNotFoundException("Course id not found: $courseId")
        }

        val viewerId = UUID.randomUUID().toString()
        val emitter = SseEmitter(SSE_TIMEOUT_MS)

        val emittersForDeal = dealEmitters.computeIfAbsent(courseId) { ConcurrentHashMap() }
        emittersForDeal[viewerId] = emitter

        // Record heartbeat in tracker
        val initialCount = liveWatchersTracker.recordHeartbeat(courseId, viewerId, HEARTBEAT_TTL)

        val cleanup = {
            emittersForDeal.remove(viewerId)
            if (emittersForDeal.isEmpty()) {
                dealEmitters.remove(courseId, emittersForDeal)
            }
            liveWatchersTracker.removeWatcher(courseId, viewerId)
        }

        emitter.onCompletion {
            log.debug("SSE completed for courseId={} viewerId={}", courseId, viewerId)
            cleanup()
        }
        emitter.onTimeout {
            log.debug("SSE timed out for courseId={} viewerId={}", courseId, viewerId)
            emitter.complete()
            cleanup()
        }
        emitter.onError { ex ->
            log.debug("SSE error for courseId={} viewerId={}: {}", courseId, viewerId, ex.message)
            cleanup()
        }

        // Send initial event immediately
        try {
            sendEvent(emitter, initialCount)
        } catch (e: Exception) {
            log.debug("Failed sending initial SSE event for courseId={}: {}", courseId, e.message)
            emitter.completeWithError(e)
            cleanup()
        }

        return emitter
    }

    /**
     * Broadcasts current watcher counts to all active connections across all deals.
     */
    fun broadcastAllDeals() {
        if (dealEmitters.isEmpty()) return

        for ((courseId, emitters) in dealEmitters) {
            if (emitters.isEmpty()) continue

            // Touch/heartbeat each active viewer so their Redis entry doesn't expire while connected
            for (viewerId in emitters.keys) {
                liveWatchersTracker.recordHeartbeat(courseId, viewerId, HEARTBEAT_TTL)
            }

            val currentCount = liveWatchersTracker.getActiveWatchersCount(courseId)
            val deadViewers = mutableListOf<String>()

            for ((viewerId, emitter) in emitters) {
                try {
                    sendEvent(emitter, currentCount)
                } catch (e: Exception) {
                    log.debug("Error streaming SSE to viewerId={} for courseId={}: {}", viewerId, courseId, e.message)
                    deadViewers.add(viewerId)
                }
            }

            for (deadId in deadViewers) {
                emitters.remove(deadId)
                liveWatchersTracker.removeWatcher(courseId, deadId)
            }

            if (emitters.isEmpty()) {
                dealEmitters.remove(courseId, emitters)
            }
        }
    }

    private fun sendEvent(
        emitter: SseEmitter,
        count: Long,
    ) {
        val event =
            SseEmitter.event()
                .name("watchers")
                .data(LiveWatchersEventDTO(activeViewers = count), MediaType.APPLICATION_JSON)
        emitter.send(event)
    }

    override fun destroy() {
        scheduledExecutor.shutdownNow()
        for ((_, emitters) in dealEmitters) {
            for ((_, emitter) in emitters) {
                try {
                    emitter.complete()
                } catch (_: Exception) {
                }
            }
        }
        dealEmitters.clear()
    }

    companion object {
        const val SSE_TIMEOUT_MS = 60_000L // 60 seconds connection lifetime before reconnecting
        val HEARTBEAT_TTL: Duration = Duration.ofSeconds(30)
        const val BROADCAST_INTERVAL_SECONDS = 10L
    }
}
