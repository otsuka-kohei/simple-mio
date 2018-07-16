package com.otsuka.simplemio.mio

data class CouponInfoJson(
        val returnCode: String,
        val couponInfo: List<CouponInfo>
)

data class CouponInfo(
        val hddServiceCode: String,
        val plan: String,
        val hdoInfo: List<CouponHdoInfo>?,
        val hduInfo: List<CouponHduInfo>,
        val coupon: List<Coupon>?,
        val history: List<History>?,
        val remains: Int?
)

data class CouponHdoInfo(
        val hdoServiceCode: String,
        val number: String,
        val iccid: String,
        val regulation: Boolean,
        val sms: Boolean,
        val voice: Boolean,
        val couponUse: Boolean,
        val coupon: List<Coupon>
)

data class CouponHduInfo(
        val hduServiceCode: String,
        val number: String,
        val iccid: String,
        val regulation: Boolean,
        val sms: Boolean,
        val voice: Boolean,
        val couponUse: Boolean,
        val coupon: List<Coupon>?
)

data class Coupon(
        val volume: String,
        val expire: String?,
        val type: String
)

data class History(
        val date: String,
        val event: String,
        val volume: Int,
        val type: String
)