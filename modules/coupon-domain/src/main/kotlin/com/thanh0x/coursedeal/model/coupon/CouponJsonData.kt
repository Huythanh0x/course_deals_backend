package com.thanh0x.coursedeal.model.coupon

import java.time.Instant

data class CouponJsonData(
    var price: Float? = null,
    var expiredDate: Instant? = null,
    var previewImage: String? = null,
    var previewVideo: String? = null,
    var usesRemaining: Int = 0,
    var couponCode: String? = null,
)
