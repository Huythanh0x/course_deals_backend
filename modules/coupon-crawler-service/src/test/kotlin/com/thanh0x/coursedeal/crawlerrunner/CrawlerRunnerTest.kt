package com.thanh0x.coursedeal.crawlerrunner

import com.thanh0x.coursedeal.config.CrawlerProperties
import com.thanh0x.coursedeal.crawlerrunner.crawler.EnextCrawler
import com.thanh0x.coursedeal.crawlerrunner.crawler.RealDiscountCrawler
import com.thanh0x.coursedeal.repository.ExpiredCouponRepository
import com.thanh0x.coursedeal.service.CouponCleanupService
import com.thanh0x.coursedeal.service.CourseScraperService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.LocalDateTime

/**
 * startCrawler() launches a background coroutine rather than running synchronously, so these
 * tests give it a moment to complete its first round instead of driving it directly. The
 * default intervalTime (900_000ms) keeps a second round from starting during that wait.
 */
class CrawlerRunnerTest {
    private val courseScraperService = mock(CourseScraperService::class.java)
    private val couponCleanupService = mock(CouponCleanupService::class.java)
    private val expiredCouponRepository = mock(ExpiredCouponRepository::class.java)
    private val enextCrawler = mock(EnextCrawler::class.java)
    private val realDiscountCrawler = mock(RealDiscountCrawler::class.java)
    private val properties = CrawlerProperties()

    private val runner =
        CrawlerRunner(
            courseScraperService,
            couponCleanupService,
            expiredCouponRepository,
            enextCrawler,
            realDiscountCrawler,
            properties,
        )

    private fun <T> nonNullAny(fallback: T): T = any<T>() ?: fallback

    @BeforeEach
    fun setUp() {
        `when`(couponCleanupService.archiveAndPurgeExpiredCoupons()).thenReturn(0)
        `when`(
            expiredCouponRepository.findRecentlyCheckedExpiredUrls(nonNullAny(LocalDateTime.now())),
        ).thenReturn(emptySet())
    }

    @AfterEach
    fun tearDown() {
        runner.stop()
    }

    @Test
    fun `startCrawler discovers urls from both crawlers, dedupes overlap, and enqueues each exactly once`() {
        `when`(enextCrawler.getAllCouponUrls()).thenReturn(listOf("https://a.example/1", "https://b.example/2"))
        `when`(realDiscountCrawler.getAllCouponUrls()).thenReturn(listOf("https://b.example/2", "https://c.example/3"))

        runner.startCrawler()
        Thread.sleep(500)

        verify(couponCleanupService, times(1)).archiveAndPurgeExpiredCoupons()
        verify(courseScraperService, times(1)).enqueueScrapingTask("https://a.example/1", "crawler")
        verify(courseScraperService, times(1)).enqueueScrapingTask("https://b.example/2", "crawler")
        verify(courseScraperService, times(1)).enqueueScrapingTask("https://c.example/3", "crawler")
    }

    @Test
    fun `startCrawler purges expired coupons and skips recently checked expired urls`() {
        `when`(couponCleanupService.archiveAndPurgeExpiredCoupons()).thenReturn(5)
        `when`(expiredCouponRepository.findRecentlyCheckedExpiredUrls(nonNullAny(LocalDateTime.now()))).thenReturn(
            setOf("https://expired.example/old"),
        )
        `when`(enextCrawler.getAllCouponUrls()).thenReturn(
            listOf("https://valid.example/1", "https://expired.example/old"),
        )
        `when`(realDiscountCrawler.getAllCouponUrls()).thenReturn(emptyList())

        runner.startCrawler()
        Thread.sleep(500)

        verify(couponCleanupService, times(1)).archiveAndPurgeExpiredCoupons()
        verify(courseScraperService, times(1)).enqueueScrapingTask("https://valid.example/1", "crawler")
        verify(courseScraperService, times(0)).enqueueScrapingTask("https://expired.example/old", "crawler")
    }
}
