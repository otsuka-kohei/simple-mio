package com.otk1fd.simplemio.mio

data class UsageInfoResponseWithHttpResponseCode(
    val usageInfoResponse: UsageInfoResponse?,
    val httpStatusCode: Int
)

data class UsageInfoResponse(
    val returnCode: String,
    val usageInfo: List<UsageInfo>
)

data class UsageInfo(
    val hddServiceCode: String,
    val plan: String,
    val usageHdoInfoList: List<UsageHdoInfo>,
    val usageHduInfoList: List<UsageHduInfo>
)

data class UsageHdoInfo(
    val hdoServiceCode: String,
    val usageList: List<Usage>
)

data class UsageHduInfo(
    val hduServiceCode: String,
    val usageList: List<Usage>
)

data class Usage(
    val date: String,
    val withCoupon: Int,
    val withoutCoupon: Int
)