package com.otk1fd.simplemio.activity

import android.app.ProgressDialog
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.utils.ViewPortHandler
import com.otk1fd.simplemio.R
import com.otk1fd.simplemio.mio.MioUtil
import com.otk1fd.simplemio.mio.PacketLog
import com.otk1fd.simplemio.mio.PacketLogInfoJson
import kotlinx.android.synthetic.main.activity_history.*


class HistoryActivity : AppCompatActivity() {

    private lateinit var lineChart: LineChart
    private lateinit var progressDialog: ProgressDialog
    private var dateList = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val hddServiceCode = intent.getStringExtra("hddServiceCode")
        val serviceCode = intent.getStringExtra("serviceCode")

        setContentView(R.layout.activity_history)

        setSupportActionBar(historyToolbar)

        supportActionBar?.title = serviceCode
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        progressDialog = ProgressDialog(this)

        lineChart = findViewById(R.id.lineChartView)

        initLineChart()

        setDataToLineChart(hddServiceCode, serviceCode)
    }

    private fun initLineChart() {
        val xAxis = lineChart.xAxis
        xAxis.setValueFormatter() { original: String, index: Int, viewPortHandler: ViewPortHandler -> dateList[index] }
    }
    

    private fun setDataToLineChart(hddServiceCode: String, serviceCode: String) {
        startProgressDialog()
        MioUtil.updateCoupon(this, execFunc = { it ->
            val packetLogInfoJson: PacketLogInfoJson? = MioUtil.parseJsonToHistory(it)

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

            for ((index, log) in packetLog.withIndex()) {
                entries.add(Entry(index.toFloat(), log.withCoupon))
            }



            stopProgressDialog()
        }, errorFunc = {
            stopProgressDialog()
        })

    }

    private fun startProgressDialog(): Unit {
        progressDialog.setTitle("読み込み中")
        progressDialog.setMessage("少々お待ちください")
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        progressDialog.show()
    }

    private fun stopProgressDialog(): Unit {
        progressDialog.dismiss()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return false
    }

}
