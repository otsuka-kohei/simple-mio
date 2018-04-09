package com.otsuka.simplemio.mio

data class DataUsageInfoJson(
        val returnCode: String,
        val packetLogInfo: ArrayList<PacketLogInfo>
)

data class PacketLogInfo(
        val hddServiceCode: String,
        val plan: String,
        val hdoInfo: ArrayList<UsageHdoInfo>,
        val hduInfo: ArrayList<UsageHduInfo>
)

data class UsageHdoInfo(
        val hdoServiceCode: String,
        val packetLog: ArrayList<PacketLog>
)

data class UsageHduInfo(
        val hduServiceCode: String,
        val packetLog: ArrayList<PacketLog>
)

data class PacketLog(
        val date: String,
        val withCoupon: Int,
        val withoutCoupon: Int
)