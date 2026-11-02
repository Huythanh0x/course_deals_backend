package com.thanh0x.coursedeal.repository

import com.thanh0x.coursedeal.model.user.DeviceTokenEntity
import com.thanh0x.coursedeal.model.user.PlatformType
import com.thanh0x.coursedeal.model.user.UserEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DeviceTokenRepositoryTest {
    @Autowired
    private lateinit var deviceTokenRepository: DeviceTokenRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun `HP-01 save and find anonymous device token`() {
        val entity =
            DeviceTokenEntity(
                user = null,
                fcmToken = "anon-fcm-tok-1",
                platform = PlatformType.ANDROID,
            )
        val saved = deviceTokenRepository.save(entity)
        assertThat(saved.id).isNotNull

        val found = deviceTokenRepository.findByFcmToken("anon-fcm-tok-1")
        assertThat(found).isPresent
        assertThat(found.get().user).isNull()
        assertThat(found.get().platform).isEqualTo(PlatformType.ANDROID)
    }

    @Test
    fun `HP-02 save and find by userId`() {
        val user = userRepository.save(UserEntity(username = "tokenuser", email = "tokenuser@test.com"))
        val token =
            DeviceTokenEntity(
                user = user,
                fcmToken = "user-fcm-tok-1",
                platform = PlatformType.IOS,
            )
        deviceTokenRepository.save(token)

        val tokens = deviceTokenRepository.findByUserId(user.id)
        assertThat(tokens).hasSize(1)
        assertThat(tokens[0].fcmToken).isEqualTo("user-fcm-tok-1")
        assertThat(tokens[0].platform).isEqualTo(PlatformType.IOS)
    }

    @Test
    fun `HP-04 deleteByFcmToken removes token`() {
        val entity =
            DeviceTokenEntity(
                user = null,
                fcmToken = "to-delete-tok",
                platform = PlatformType.ANDROID,
            )
        deviceTokenRepository.save(entity)

        val deletedCount = deviceTokenRepository.deleteByFcmToken("to-delete-tok")
        assertThat(deletedCount).isEqualTo(1)

        val found = deviceTokenRepository.findByFcmToken("to-delete-tok")
        assertThat(found).isEmpty
    }

    @Test
    fun `RP-01 cascade deletes device tokens when user is deleted`() {
        val user = userRepository.save(UserEntity(username = "cascadetarget", email = "cascade@test.com"))
        val token =
            DeviceTokenEntity(
                user = user,
                fcmToken = "cascade-fcm-tok",
                platform = PlatformType.ANDROID,
            )
        user.deviceTokens.add(token)
        deviceTokenRepository.save(token)

        assertThat(deviceTokenRepository.findByFcmToken("cascade-fcm-tok")).isPresent

        userRepository.delete(user)
        userRepository.flush()
        deviceTokenRepository.flush()

        // After deleting user, token should no longer exist (due to cascade)
        val found = deviceTokenRepository.findByFcmToken("cascade-fcm-tok")
        assertThat(found).isEmpty
    }
}
