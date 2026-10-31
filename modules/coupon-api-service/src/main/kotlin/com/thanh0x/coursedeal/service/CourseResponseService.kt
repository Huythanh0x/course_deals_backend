package com.thanh0x.coursedeal.service

import com.thanh0x.coursedeal.config.logger
import com.thanh0x.coursedeal.crawlerrunner.CourseDataExtractor
import com.thanh0x.coursedeal.dto.CouponDetailDTO
import com.thanh0x.coursedeal.dto.CouponQueryDTO
import com.thanh0x.coursedeal.dto.CouponSearchCriteriaDTO
import com.thanh0x.coursedeal.dto.PagedCouponResponseDTO
import com.thanh0x.coursedeal.exception.BadRequestException
import com.thanh0x.coursedeal.exception.ResourceNotFoundException
import com.thanh0x.coursedeal.mapper.CouponMapper
import com.thanh0x.coursedeal.model.coupon.CouponCourseData
import com.thanh0x.coursedeal.model.coupon.CouponCourseHistory
import com.thanh0x.coursedeal.repository.CouponCourseHistoryRepository
import com.thanh0x.coursedeal.repository.CouponCourseRepository
import com.thanh0x.coursedeal.repository.ExpiredCouponRepository
import com.thanh0x.coursedeal.repository.specification.CouponCourseSpecifications
import com.thanh0x.coursedeal.utils.Constant
import com.thanh0x.coursedeal.utils.LastFetchTimeManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Service class for handling course response operations.
 */
@Service
class CourseResponseService(
    private val couponCourseRepository: CouponCourseRepository,
    private val expiredCouponRepository: ExpiredCouponRepository,
    private val couponCourseHistoryRepository: CouponCourseHistoryRepository,
    private val couponMapper: CouponMapper,
    private val courseScraperService: CourseScraperService,
) {
    private val log = logger()

    /**
     * Unified listing endpoint that supports basic pagination, structured filters, free-text search, and sorting.
     */
    fun listCoupons(queryDto: CouponQueryDTO): PagedCouponResponseDTO {
        handlePagingParameters(queryDto.pageIndex, queryDto.numberPerPage)

        val sort = createSort(queryDto.sortBy, queryDto.sortOrder)
        val pageable =
            PageRequest.of(
                queryDto.pageIndex.toInt(),
                Math.min(queryDto.numberPerPage.toInt(), Constant.MAX_PAGE_SIZE),
                sort,
            )

        val hasQuery = !queryDto.query.isBlank()
        val hasStructuredFilters =
            (queryDto.rating != "-1") ||
                (queryDto.contentLength != "-1") ||
                !queryDto.level.isBlank() ||
                !queryDto.category.isBlank() ||
                !queryDto.language.isBlank()

        val now = Instant.now()
        val page: Page<CouponCourseData> =
            when {
                hasQuery -> {
                    couponCourseRepository.findByQueryAndActive(
                        queryDto.query,
                        now,
                        pageable,
                    )
                }
                hasStructuredFilters -> {
                    couponCourseRepository.findWithStructuredFilters(
                        queryDto.rating.toFloatOrNull() ?: -1f,
                        queryDto.contentLength.toIntOrNull() ?: -1,
                        queryDto.level,
                        queryDto.category,
                        queryDto.language,
                        now,
                        pageable,
                    )
                }
                else -> {
                    couponCourseRepository.findAllActive(now, pageable)
                }
            }

        val dtos = page.content.map { couponMapper.toSummaryDto(it) }

        return PagedCouponResponseDTO(
            LastFetchTimeManager.loadLasFetchedTimeInMilliSecond(),
            page.totalElements,
            page.totalPages,
            page.pageable.pageNumber,
            dtos,
        )
    }

    /**
     * Multi-faceted full-text search endpoint implementation with Specification.
     */
    fun searchCoupons(criteria: CouponSearchCriteriaDTO): PagedCouponResponseDTO {
        if (criteria.page < 0 || criteria.size < 0) {
            throw BadRequestException("Page index and size cannot be negative")
        }

        val sort = createSearchSort(criteria.sort, criteria.sortDirection)
        val pageSize = Math.min(criteria.size, Constant.MAX_PAGE_SIZE)
        val pageable = PageRequest.of(criteria.page, pageSize, sort)

        val now = Instant.now()
        val spec = CouponCourseSpecifications.withCriteria(criteria, now)
        val page = couponCourseRepository.findAll(spec, pageable)

        val dtos = page.content.map { couponMapper.toSummaryDto(it) }

        return PagedCouponResponseDTO(
            LastFetchTimeManager.loadLasFetchedTimeInMilliSecond(),
            page.totalElements,
            page.totalPages,
            page.pageable.pageNumber,
            dtos,
        )
    }

    /**
     * Creates a Sort object for faceted search queries.
     */
    private fun createSearchSort(
        sort: String?,
        sortDirection: String?,
    ): Sort {
        val direction =
            if (sortDirection?.equals("asc", ignoreCase = true) == true) {
                Sort.Direction.ASC
            } else {
                Sort.Direction.DESC
            }

        return when (sort?.lowercase()) {
            "relevance" -> Sort.by(Sort.Direction.DESC, "rating").and(Sort.by(Sort.Direction.DESC, "reviews"))
            "newest" -> Sort.by(direction, "createdAt")
            "expiring_soon" -> Sort.by(Sort.Direction.ASC, "expiredDate")
            "rating" -> Sort.by(direction, "rating")
            "students" -> Sort.by(direction, "students")
            "reviews" -> Sort.by(direction, "reviews")
            else -> createSort(sort, sortDirection)
        }
    }

    /**
     * Creates a Sort object based on the provided sort field and order.
     */
    private fun createSort(
        sortBy: String?,
        sortOrder: String?,
    ): Sort {
        val actualSortBy = if (sortBy.isNullOrBlank()) "createdAt" else sortBy

        val direction =
            if (sortOrder?.equals("asc", ignoreCase = true) == true) {
                Sort.Direction.ASC
            } else {
                Sort.Direction.DESC
            }

        val sortField =
            when (actualSortBy.lowercase()) {
                "students" -> "students"
                "rating" -> "rating"
                "reviews" -> "reviews"
                "expiredtime", "expired_time", "expireddate", "expired_date" -> "expiredDate"
                "createdat", "created_at", "newest" -> "createdAt"
                "contentlength", "content_length" -> "contentLength"
                "usesremaining", "uses_remaining" -> "usesRemaining"
                else -> {
                    log.warn("Unknown sort field: {}, defaulting to createdAt", actualSortBy)
                    "createdAt"
                }
            }

        return Sort.by(direction, sortField)
    }

    /**
     * Saves a new coupon URL asynchronously.
     *
     * @param couponUrl  the URL of the coupon to save
     * @param remoteAddr the remote address of the user saving the coupon
     */
    fun saveNewCouponUrlAsync(
        couponUrl: String,
        remoteAddr: String?,
    ) {
        courseScraperService.enqueueScrapingTask(couponUrl, remoteAddr ?: "unknown")
    }

    /**
     * Refreshes a coupon URL asynchronously.
     */
    fun refreshCouponAsync(
        courseId: Int,
        remoteAddr: String?,
    ) {
        couponCourseRepository.findById(courseId).ifPresent { coupon ->
            courseScraperService.enqueueScrapingTask(coupon.couponUrl ?: "", remoteAddr ?: "unknown")
        }
    }

    /**
     * Saves a new coupon URL to the database.
     */
    fun saveNewCouponUrl(couponUrl: String): CouponDetailDTO {
        val extractor = CourseDataExtractor(couponUrl)
        val couponData =
            extractor.getFullCouponCodeData()
                ?: throw BadRequestException("Coupon is invalid or expired")

        val existedBefore =
            couponCourseRepository.findByCouponUrl(couponUrl) != null ||
                expiredCouponRepository.findByCouponUrl(couponUrl) != null
        couponData.isNew = !existedBefore

        val saved = couponCourseRepository.save(couponData)
        couponCourseHistoryRepository.save(
            CouponCourseHistory(
                courseId = saved.courseId,
                title = saved.title,
                couponUrl = saved.couponUrl ?: "",
                status = if (existedBefore) "reactivated" else "new",
            ),
        )
        return couponMapper.toDetailDto(saved)
    }

    /**
     * Validates and handles paging parameters for pagination.
     */
    fun handlePagingParameters(
        pageIndex: String,
        numberPerPage: String,
    ) {
        try {
            pageIndex.toInt()
            numberPerPage.toInt()
        } catch (e: NumberFormatException) {
            log.warn("Invalid paging parameters pageIndex={}, numberPerPage={}", pageIndex, numberPerPage, e)
            throw BadRequestException(e.toString())
        }
        if (pageIndex.toInt() < 0 || numberPerPage.toInt() < 0) {
            throw BadRequestException("Page index and number of course per page cannot be negative")
        }
    }

    /**
     * Retrieves the coupon course data by course ID.
     */
    fun getCouponDetail(courseId: String): CouponDetailDTO {
        val couponCourseData = couponCourseRepository.findByCourseId(courseId.toInt())
        val isExpired = couponCourseData?.expiredDate?.let { it <= Instant.now() } ?: false
        return if (couponCourseData != null && !isExpired) {
            couponMapper.toDetailDto(couponCourseData)
        } else {
            throw ResourceNotFoundException("Course id not found: $courseId")
        }
    }
}
