package com.thanh0x.coursedeal.service

import java.time.Duration

private const val DEFAULT_HEARTBEAT_SECONDS = 30L

/**
 * Service contract for tracking active viewers on course deals.
 */
interface LiveWatchersTracker {
    /**
     * Records or refreshes a viewer's heartbeat for the given course deal.
     *
     * @param courseId the ID of the course deal
     * @param viewerId unique session / client identifier for the viewer
     * @param ttl heartbeat timeout before the viewer is evicted
     * @return current count of active viewers for the course
     */
    fun recordHeartbeat(
        courseId: Int,
        viewerId: String,
        ttl: Duration = Duration.ofSeconds(DEFAULT_HEARTBEAT_SECONDS),
    ): Long

    /**
     * Removes a viewer from the active watchers set.
     *
     * @param courseId the ID of the course deal
     * @param viewerId unique session / client identifier for the viewer
     * @return updated count of active viewers
     */
    fun removeWatcher(
        courseId: Int,
        viewerId: String,
    ): Long

    /**
     * Gets the count of active viewers for the course.
     *
     * @param courseId the ID of the course deal
     * @return current active viewers count
     */
    fun getActiveWatchersCount(courseId: Int): Long
}
