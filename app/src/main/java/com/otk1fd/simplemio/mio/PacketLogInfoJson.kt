package com.otk1fd.simplemio.mio

data class PacketLogInfoJson(
        val returnCode: String,
        val packetLogInfo: List<PacketLogInfo>
)

data class PacketLogInfo(
        val hddServiceCode: String,
        val plan: String,
        val hdoInfo: List<UsageHdoInfo>,
        val hduInfo: List<UsageHduInfo>
)

data class UsageHdoInfo(
        val hdoServiceCode: String,
        val packetLog: List<PacketLog>
)

data class UsageHduInfo(
        val hduServiceCode: String,
        val packetLog: List<PacketLog>
)

data class PacketLog(
        val date: String,
        val withCoupon: Int,
        val withoutCoupon: Int
)