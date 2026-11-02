package com.thanh0x.coursedeal.repository

import com.thanh0x.coursedeal.model.user.DeviceTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

@Repository
interface DeviceTokenRepository : JpaRepository<DeviceTokenEntity, Long> {
    fun findByFcmToken(fcmToken: String): Optional<DeviceTokenEntity>

    fun findByUserId(userId: Int): List<DeviceTokenEntity>

    @Transactional
    @Modifying
    fun deleteByFcmToken(fcmToken: String): Int
}
