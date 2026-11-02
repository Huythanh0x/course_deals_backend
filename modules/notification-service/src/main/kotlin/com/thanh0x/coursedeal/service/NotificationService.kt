package com.thanh0x.coursedeal.service

import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import com.google.firebase.messaging.TopicManagementResponse
import com.thanh0x.coursedeal.config.logger
import com.thanh0x.coursedeal.model.coupon.CouponCourseData
import com.thanh0x.coursedeal.model.user.UserEntity
import com.thanh0x.coursedeal.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class NotificationService(private val userRepository: UserRepository) {
    private val log = logger()

    /**
     * Sends personalized notifications to users interested in the new coupon.
     */
    fun notifyInterestedUsers(coupon: CouponCourseData) {
        val allUsers = userRepository.findAll()

        for (user in allUsers) {
            val token = user.fcmToken
            if (token.isNullOrEmpty()) {
                continue
            }

            if (isUserInterested(user, coupon)) {
                sendPush(
                    token,
                    "Course Deal: " + coupon.category,
                    "Free: " + coupon.title,
                )
            }
        }
    }

    private fun isUserInterested(
        user: UserEntity,
        coupon: CouponCourseData,
    ): Boolean {
        val pref = user.preference ?: return false

        val categoryMatch =
            pref.notificationsEnabled &&
                pref.categories.any { cat ->
                    cat.equals(coupon.category, ignoreCase = true)
                }

        val title = coupon.title?.lowercase() ?: ""
        val keywordMatch =
            pref.notificationsEnabled &&
                pref.keywords.any { keyword ->
                    title.contains(keyword.lowercase())
                }

        return categoryMatch || keywordMatch
    }

    fun sendPush(
        token: String,
        title: String,
        body: String,
    ) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("FirebaseApp is not initialized. Skipping push notification.")
            return
        }

        try {
            val message =
                Message.builder()
                    .setNotification(
                        Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build(),
                    )
                    .setToken(token)
                    .build()

            val response = FirebaseMessaging.getInstance().send(message)
            log.info("Successfully sent message: $response")
        } catch (e: FirebaseMessagingException) {
            log.error("Error sending FCM message", e)
        }
    }

    fun subscribeToTopic(
        tokens: List<String>,
        topic: String,
    ): TopicManagementResponse? {
        if (FirebaseApp.getApps().isEmpty() || tokens.isEmpty()) {
            if (FirebaseApp.getApps().isEmpty()) {
                log.warn("FirebaseApp is not initialized. Skipping topic subscription for topic: {}", topic)
            } else {
                log.debug("No tokens provided for topic subscription to {}", topic)
            }
            return null
        }
        return try {
            val response = FirebaseMessaging.getInstance().subscribeToTopic(tokens, topic)
            log.info(
                "Subscribed {} tokens to topic {}: {} successes, {} failures",
                tokens.size,
                topic,
                response.successCount,
                response.failureCount,
            )
            response
        } catch (e: FirebaseMessagingException) {
            log.error("Error subscribing to topic: {}", topic, e)
            null
        }
    }

    fun unsubscribeFromTopic(
        tokens: List<String>,
        topic: String,
    ): TopicManagementResponse? {
        if (FirebaseApp.getApps().isEmpty() || tokens.isEmpty()) {
            if (FirebaseApp.getApps().isEmpty()) {
                log.warn("FirebaseApp is not initialized. Skipping topic unsubscription for topic: {}", topic)
            } else {
                log.debug("No tokens provided for topic unsubscription from {}", topic)
            }
            return null
        }
        return try {
            val response = FirebaseMessaging.getInstance().unsubscribeFromTopic(tokens, topic)
            log.info(
                "Unsubscribed {} tokens from topic {}: {} successes, {} failures",
                tokens.size,
                topic,
                response.successCount,
                response.failureCount,
            )
            response
        } catch (e: FirebaseMessagingException) {
            log.error("Error unsubscribing from topic: {}", topic, e)
            null
        }
    }

    fun sendToTopic(
        topic: String,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap(),
    ): String? {
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("FirebaseApp is not initialized. Skipping sendToTopic for topic: {}", topic)
            return null
        }
        return try {
            val builder =
                Message.builder()
                    .setTopic(topic)
                    .setNotification(
                        Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build(),
                    )
            if (data.isNotEmpty()) {
                builder.putAllData(data)
            }
            val response = FirebaseMessaging.getInstance().send(builder.build())
            log.info("Successfully sent topic message to {}: {}", topic, response)
            response
        } catch (e: FirebaseMessagingException) {
            log.error("Error sending FCM message to topic: {}", topic, e)
            null
        }
    }
}
