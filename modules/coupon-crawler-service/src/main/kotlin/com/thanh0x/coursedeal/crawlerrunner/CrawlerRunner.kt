package com.thanh0x.coursedeal.crawlerrunner

import com.thanh0x.coursedeal.config.CrawlerProperties
import com.thanh0x.coursedeal.config.logger
import com.thanh0x.coursedeal.crawlerrunner.crawler.EnextCrawler
import com.thanh0x.coursedeal.crawlerrunner.crawler.RealDiscountCrawler
import com.thanh0x.coursedeal.repository.ExpiredCouponRepository
import com.thanh0x.coursedeal.service.CouponCleanupService
import com.thanh0x.coursedeal.service.CourseScraperService
import com.thanh0x.coursedeal.utils.LastFetchTimeManager
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicLong

/**
 * CrawlerRunner class is responsible for running the web crawlers to fetch coupon URLs
 * and handing them off to the CourseScraperService for async processing.
 */
@Component
@EnableConfigurationProperties(CrawlerProperties::class)
class CrawlerRunner(
    private val courseScraperService: CourseScraperService,
    private val couponCleanupService: CouponCleanupService,
    private val expiredCouponRepository: ExpiredCouponRepository,
    private val enextCrawler: EnextCrawler,
    private val realDiscountCrawler: RealDiscountCrawler,
    private val properties: CrawlerProperties,
) : ApplicationRunner {
    private val log = logger()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun run(args: ApplicationArguments) {
        startCrawler()
    }

    /**
     * Starts the crawler process that continuously fetches coupon URLs.
     *
     * This is the top-level loop for a long-running background daemon: it must survive any
     * single round's failure (network, DB, etc.) and keep running for the next round.
     */
    @Suppress("TooGenericExceptionCaught")
    fun startCrawler() {
        val lastFetchedTime = LastFetchTimeManager.loadLasFetchedTimeInMilliSecond()
        val startTime = AtomicLong(lastFetchedTime)

        scope.launch {
            try {
                delayUntilTheNextRound(startTime.get())
                while (isActive) {
                    startTime.set(System.currentTimeMillis())

                    log.info("Starting new crawl round...")
                    try {
                        val purged = couponCleanupService.archiveAndPurgeExpiredCoupons()
                        if (purged > 0) {
                            log.info("Purged and archived {} expired coupons", purged)
                        }
                    } catch (e: Exception) {
                        log.error("Failed to run expired coupon cleanup", e)
                    }

                    val discoveredUrls = mutableListOf<String>()
                    discoveredUrls.addAll(enextCrawler.getAllCouponUrls())
                    discoveredUrls.addAll(realDiscountCrawler.getAllCouponUrls())

                    val uniqueUrls = discoveredUrls.toSet()

                    val recentlyExpiredUrls =
                        if (properties.skipRecentlyCheckedExpiredHours > 0) {
                            val cutoff =
                                LocalDateTime.now().minusHours(properties.skipRecentlyCheckedExpiredHours.toLong())
                            expiredCouponRepository.findRecentlyCheckedExpiredUrls(cutoff)
                        } else {
                            emptySet()
                        }

                    val urlsToEnqueue = uniqueUrls.filterNot { recentlyExpiredUrls.contains(it) }
                    log.info(
                        "Discovered {} unique URLs (skipped {} recently checked expired). Handing off {} tasks...",
                        uniqueUrls.size,
                        uniqueUrls.size - urlsToEnqueue.size,
                        urlsToEnqueue.size,
                    )

                    for (url in urlsToEnqueue) {
                        courseScraperService.enqueueScrapingTask(url, "crawler")
                    }

                    log.info("Crawl round finished. Handed off {} tasks.", urlsToEnqueue.size)
                    LastFetchTimeManager.updateLastBulkRefreshCoupon()

                    delayUntilTheNextRound(startTime.get())
                }
            } catch (e: CancellationException) {
                log.info("Crawler coroutine cancelled", e)
                throw e
            } catch (e: Exception) {
                log.error("Error in crawler loop", e)
            }
        }
    }

    private suspend fun delayUntilTheNextRound(startTime: Long) {
        val runTime = System.currentTimeMillis() - startTime
        val delayTime = (properties.intervalTime - runTime).coerceAtLeast(0)
        log.info("Waiting {} ms until the next run", delayTime)
        delay(delayTime)
    }

    @PreDestroy
    fun stop() {
        scope.cancel()
    }
}
