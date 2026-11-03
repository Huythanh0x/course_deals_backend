package com.thanh0x.coursedeal.config

import com.thanh0x.coursedeal.service.LiveWatchersTracker
import com.thanh0x.coursedeal.service.RedisLiveWatchersTracker
import com.thanh0x.coursedeal.service.RedisService
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

/**
 * Redis configuration for the application.
 * Only enabled when spring.data.redis.host is configured.
 */
@Configuration
@ConditionalOnProperty(name = ["spring.data.redis.host"])
class RedisConfig {
    @Value("\${spring.data.redis.host:localhost}")
    private lateinit var redisHost: String

    @Value("\${spring.data.redis.port:6379}")
    private var redisPort: Int = 0

    @Value("\${spring.data.redis.password:}")
    private lateinit var redisPassword: String

    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory {
        val config = RedisStandaloneConfiguration()
        config.hostName = redisHost
        config.port = redisPort
        if (redisPassword.isNotEmpty()) {
            config.setPassword(redisPassword)
        }
        return LettuceConnectionFactory(config)
    }

    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.connectionFactory = connectionFactory
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = GenericJackson2JsonRedisSerializer()
        template.hashKeySerializer = StringRedisSerializer()
        template.hashValueSerializer = GenericJackson2JsonRedisSerializer()
        template.afterPropertiesSet()
        return template
    }

    @Bean
    fun redisService(redisTemplate: RedisTemplate<String, Any>): RedisService {
        val service = RedisService(redisTemplate)
        // Set singleton instance for static access
        RedisService.instance = service
        return service
    }

    @Bean
    @Primary
    fun redisLiveWatchersTracker(redisTemplate: RedisTemplate<String, Any>): LiveWatchersTracker {
        return RedisLiveWatchersTracker(redisTemplate)
    }
}
