package com.otk1fd.simplemio.activities

import android.app.ProgressDialog
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.otk1fd.simplemio.HttpErrorHandler
import com.otk1fd.simplemio.R
import com.otk1fd.simplemio.Util
import com.otk1fd.simplemio.mio.MioUtil
import com.otk1fd.simplemio.mio.PacketLog
import com.otk1fd.simplemio.mio.PacketLogInfoJson
import kotlinx.android.synthetic.main.activity_packet_log_chart.*


class PacketLogActivity : AppCompatActivity() {

    private lateinit var lineChart: LineChart
    private lateinit var progressDialog: ProgressDialog
    private val dateList = ArrayList<String>()

    private lateinit var hddServiceCode: String
    private lateinit var serviceCode: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hddServiceCode = intent.getStringExtra("hddServiceCode")
        serviceCode = intent.getStringExtra("serviceCode")

        setContentView(R.layout.activity_packet_log_chart)

        setSupportActionBar(packetLogToolbar)

        val simName = Util.loadSimName(this, serviceCode)

        supportActionBar?.title = """${if (simName != "") simName else serviceCode}のデータ通信量履歴"""
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        progressDialog = ProgressDialog(this)

        lineChart = findViewById(R.id.lineChartView)

        initLineChart()
    }

    override fun onResume() {
        super.onResume()

        setDataToLineChartByHttp(hddServiceCode, serviceCode)
    }

    override fun onPause() {
        super.onPause()

        stopProgressDialog()
    }

    private fun initLineChart() {
        lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        lineChart.xAxis.valueFormatter = XAxisValueFormatterForDate(dateList)
        lineChart.xAxis.granularity = 7f

        lineChart.axisLeft.valueFormatter = YAxisValueFormatterForUnitMB()
        lineChart.axisLeft.axisMinimum = 0f

        lineChart.axisRight.valueFormatter = YAxisValueFormatterForUnitMB()
        lineChart.axisRight.axisMinimum = 0f

        val description = Description()
        description.text = ""
        lineChart.description = description
    }


    private fun setDataToLineChartByHttp(hddServiceCode: String, serviceCode: String) {
        startProgressDialog()
        MioUtil.updatePacket(this, execFunc = { it ->
            val packetLogInfoJson: PacketLogInfoJson? = MioUtil.parseJsonToPacketLog(it)

            MioUtil.cacheJson(this, it, this.applicationContext.getString(R.string.preference_key_cache_packet_log))

            packetLogInfoJson?.let { setDataToLineChart(it, hddServiceCode, serviceCode) }

            stopProgressDialog()
        }, errorFunc = {
            HttpErrorHandler.handleHttpError(it) { setDataToLineChartByCache(hddServiceCode, serviceCode) }
            stopProgressDialog()
        })
    }

    private fun setDataToLineChartByCache(hddServiceCode: String, serviceCode: String) {
        val packetLogInfoJson = MioUtil.parseJsonToPacketLog(MioUtil.loadJsonCache(this, this.applicationContext.getString(R.string.preference_key_cache_packet_log)))
        packetLogInfoJson?.let { setDataToLineChart(it, hddServiceCode, serviceCode) }
    }

    private fun setDataToLineChart(packetLogInfoJson: PacketLogInfoJson, hddServiceCode: String, serviceCode: String) {

        val couponUseDataSet = getLineDataFromJson(packetLogInfoJson, hddServiceCode, serviceCode, true, R.color.packetLogChartWithCoupon, "クーポンON", true)
        val notCouponUseDataSet = getLineDataFromJson(packetLogInfoJson, hddServiceCode, serviceCode, false, R.color.packetLogChartWithoutCoupon, "クーポンOFF")

        val dataSets = ArrayList<ILineDataSet>()
        // クーポンOFF時のグラフを先に描画し，そのあとクーポンON時のグラフを表示する
        dataSets.add(notCouponUseDataSet)
        dataSets.add(couponUseDataSet)

        val lineData = LineData(dataSets)
        lineChart.data = lineData
        lineChart.invalidate()
    }

    private fun getLineDataFromJson(packetLogInfoJson: PacketLogInfoJson?, hddServiceCode: String, serviceCode: String, couponUse: Boolean, colorResourceId: Int, label: String, setDateList: Boolean = false): LineDataSet {

        val packetLogInfoList = packetLogInfoJson?.packetLogInfo.orEmpty()
        val packetLogInfo = packetLogInfoList.find { it.hddServiceCode == hddServiceCode }

        val packetLog: ArrayList<PacketLog> = if (serviceCode.contains("hdo")) {
            val hdoInfo = packetLogInfo?.hdoInfo.orEmpty()
            val hdoPacketLog = hdoInfo.find { it.hdoServiceCode == serviceCode }?.packetLog.orEmpty()
            ArrayList(hdoPacketLog)
        } else {
            val hduInfo = packetLogInfo?.hduInfo.orEmpty()
            val hduPacketLog = hduInfo.find { it.hduServiceCode == serviceCode }?.packetLog.orEmpty()
            ArrayList(hduPacketLog)
        }

        val entries = ArrayList<Entry>()

        if (setDateList) {
            dateList.clear()
        }


        for ((index, log) in packetLog.withIndex()) {
            if (couponUse) {
                entries.add(Entry(index.toFloat(), log.withCoupon.toFloat()))
            } else {
                entries.add(Entry(index.toFloat(), log.withoutCoupon.toFloat()))
            }
            if (setDateList) {
                dateList.add(log.date)
            }
        }

        val color = ContextCompat.getColor(this, R.color.colorPrimary)
        val dataSet = LineDataSet(entries, label)
        dataSet.color = color
        dataSet.setCircleColor(color)
        dataSet.setDrawCircleHole(false)
        dataSet.setDrawValues(false)
        dataSet.lineWidth = 4.0f
        dataSet.circleSize = 4.0f
        dataSet.highLightColor = color
        dataSet.highlightLineWidth = 1.0f

        return dataSet
    }

    private fun startProgressDialog() {
        progressDialog.setTitle("読み込み中")
        progressDialog.setMessage("少々お待ちください")
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        progressDialog.show()
    }

    private fun stopProgressDialog() {
        progressDialog.dismiss()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return false
    }

}

private class XAxisValueFormatterForDate(val xValueStrings: List<String>) : IAxisValueFormatter {
    override fun getFormattedValue(value: Float, axis: AxisBase): String {
        // "value" represents the position of the label on the axis (x or y)

        val dateStr = xValueStrings[value.toInt()]
        val yearStr = dateStr.substring(0, 4)
        val monthStr = dateStr.substring(4, 6)
        val dayStr = dateStr.substring(6, 8)
        return "$yearStr/$monthStr/$dayStr"
    }
}

private class YAxisValueFormatterForUnitMB() : IAxisValueFormatter {
    override fun getFormattedValue(value: Float, axis: AxisBase): String {
        // "value" represents the position of the label on the axis (x or y)
        return "${value.toInt()}MB"
    }
}
