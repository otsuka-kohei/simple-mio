package com.otsuka.simplemio.mio

data class CouponInfoJson(
        val returnCode: String,
        val couponInfo: ArrayList<CouponInfo>
)

data class CouponInfo(
        val hddServiceCode: String,
        val plan: String,
        val hdoInfo: ArrayList<HdoInfo>?,
        val hduInfo: ArrayList<HduInfo>,
        val coupon: ArrayList<Coupon>?,
        val history: ArrayList<History>?,
        val remains: Int?
)

data class HdoInfo(
        val hdoServiceCode: String,
        val number: String,
        val iccid: String,
        val regulation: String,
        val sms: Boolean,
        val voice: Boolean,
        val couponUse: Boolean,
        val coupon: ArrayList<Coupon>
)

data class HduInfo(
        val hduServiceCode: String,
        val number: String,
        val iccid: String,
        val regulation: String,
        val sms: Boolean,
        val voice: Boolean,
        val couponUse: Boolean,
        val coupon: ArrayList<Coupon>?
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