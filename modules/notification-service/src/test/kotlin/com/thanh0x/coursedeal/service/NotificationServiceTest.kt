package com.thanh0x.coursedeal.service

import com.thanh0x.coursedeal.model.coupon.CouponCourseData
import com.thanh0x.coursedeal.model.user.UserEntity
import com.thanh0x.coursedeal.model.user.UserPreference
import com.thanh0x.coursedeal.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

/**
 * sendPush() calls the real FirebaseMessaging singleton, so these tests spy on the service and
 * stub sendPush() out - the point is verifying WHICH users get notified (category/keyword/
 * notificationsEnabled matching), not the FCM call itself.
 */
class NotificationServiceTest {
    private val userRepository = mock(UserRepository::class.java)
    private val service = spy(NotificationService(userRepository))

    @Test
    fun `notifies only users with a matching category and notifications enabled`() {
        doNothing().`when`(service).sendPush(anyString(), anyString(), anyString())

        val interested =
            userWithPreference(
                fcmToken = "token-interested",
                notificationsEnabled = true,
                categories = setOf("Kotlin"),
            )
        val wrongCategory =
            userWithPreference(
                fcmToken = "token-wrong-category",
                notificationsEnabled = true,
                categories = setOf("Java"),
            )
        val notificationsDisabled =
            userWithPreference(
                fcmToken = "token-disabled",
                notificationsEnabled = false,
                categories = setOf("Kotlin"),
            )
        val noToken =
            userWithPreference(
                fcmToken = null,
                notificationsEnabled = true,
                categories = setOf("Kotlin"),
            )

        `when`(userRepository.findAll()).thenReturn(
            listOf(interested, wrongCategory, notificationsDisabled, noToken),
        )

        val coupon = CouponCourseData(category = "Kotlin", title = "Kotlin Coroutines Masterclass")
        service.notifyInterestedUsers(coupon)

        verify(service, times(1)).sendPush(eqKt("token-interested"), anyString(), anyString())
        verify(service, never()).sendPush(eqKt("token-wrong-category"), anyString(), anyString())
        verify(service, never()).sendPush(eqKt("token-disabled"), anyString(), anyString())
    }

    @Test
    fun `matches by keyword in the coupon title when the category does not match`() {
        doNothing().`when`(service).sendPush(anyString(), anyString(), anyString())

        val keywordUser =
            userWithPreference(
                fcmToken = "token-keyword",
                notificationsEnabled = true,
                categories = setOf("Design"),
                keywords = setOf("coroutines"),
            )
        `when`(userRepository.findAll()).thenReturn(listOf(keywordUser))

        val coupon = CouponCourseData(category = "Programming", title = "Kotlin Coroutines Masterclass")
        service.notifyInterestedUsers(coupon)

        verify(service, times(1)).sendPush(eqKt("token-keyword"), anyString(), anyString())
    }

    @Test
    fun `EP-01 subscribeToTopic when FirebaseApp uninitialized returns null safely`() {
        val result = service.subscribeToTopic(listOf("token1", "token2"), "deals_all")
        assertThat(result).isNull()
    }

    @Test
    fun `EP-01 unsubscribeFromTopic when FirebaseApp uninitialized returns null safely`() {
        val result = service.unsubscribeFromTopic(listOf("token1"), "deals_all")
        assertThat(result).isNull()
    }

    @Test
    fun `EP-01 sendToTopic when FirebaseApp uninitialized returns null safely`() {
        val result = service.sendToTopic("deals_all", "New Deal", "Free Course!", mapOf("id" to "123"))
        assertThat(result).isNull()
    }

    @Test
    fun `subscribeToTopic with empty tokens returns null immediately`() {
        val result = service.subscribeToTopic(emptyList(), "deals_all")
        assertThat(result).isNull()
    }

    @Test
    fun `unsubscribeFromTopic with empty tokens returns null immediately`() {
        val result = service.unsubscribeFromTopic(emptyList(), "deals_all")
        assertThat(result).isNull()
    }

    @Test
    fun `sendPush when FirebaseApp uninitialized does not crash or throw`() {
        // Calling un-stubbed sendPush directly with uninitialized FirebaseApp
        val rawService = NotificationService(userRepository)
        rawService.sendPush("dummy-token", "Title", "Body")
    }

    // Mockito's eq() returns null as an internal placeholder, which crashes against sendPush's
    // non-null String parameters (Kotlin inserts a null-check on them). This wrapper substitutes
    // the real value when that happens, while still registering the matcher with Mockito.
    private fun <T> eqKt(value: T): T = org.mockito.Mockito.eq(value) ?: value

    private fun userWithPreference(
        fcmToken: String?,
        notificationsEnabled: Boolean,
        categories: Set<String> = emptySet(),
        keywords: Set<String> = emptySet(),
    ): UserEntity {
        val user = UserEntity(fcmToken = fcmToken)
        user.preference =
            UserPreference(
                user = user,
                categories = categories.toMutableSet(),
                keywords = keywords.toMutableSet(),
                notificationsEnabled = notificationsEnabled,
            )
        return user
    }
}
