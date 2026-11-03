package com.thanh0x.coursedeal.config

import com.thanh0x.coursedeal.service.InMemoryLiveWatchersTracker
import com.thanh0x.coursedeal.service.LiveWatchersTracker
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class LiveWatchersConfig {
    @Bean
    @ConditionalOnMissingBean(LiveWatchersTracker::class)
    fun inMemoryLiveWatchersTracker(): LiveWatchersTracker {
        return InMemoryLiveWatchersTracker()
    }
}
