package com.otk1fd.simplemio.activities

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
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
import com.otk1fd.simplemio.databinding.ActivityPacketLogChartBinding
import com.otk1fd.simplemio.dialog.ProgressDialogFragment
import com.otk1fd.simplemio.dialog.ProgressDialogFragmentData
import com.otk1fd.simplemio.mio.Mio
import com.otk1fd.simplemio.mio.json.PacketLog
import com.otk1fd.simplemio.mio.json.PacketLogInfoResponse
import kotlinx.coroutines.launch


/**
 * IIJmioの利用履歴を折れ線グラフにして表示するActivity．
 * [PacketLogFragment][com.otk1fd.simplemio.fragments.PacketLogFragment]から呼び出される．
 */
class PacketLogChartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPacketLogChartBinding

    private lateinit var lineChartView: LineChart
    private lateinit var nodataMessageLayout: ConstraintLayout

    private lateinit var mio: Mio
    private lateinit var httpErrorHandler: HttpErrorHandler

    private val dateList = ArrayList<String>()

    private lateinit var hddServiceCode: String
    private lateinit var serviceCode: String

    private var progressDialogFragment: ProgressDialogFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPacketLogChartBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        lineChartView = binding.contentPacketLogChart.lineChartView

        nodataMessageLayout = binding.contentPacketLogChart.nodataMessageLayout

        mio = Mio(this)
        httpErrorHandler = HttpErrorHandler(this, mio)

        // 呼び出し元から渡された，表示する利用履歴のSIMのサービスコードを取得する
        hddServiceCode = intent.getStringExtra("hddServiceCode") ?: ""
        serviceCode = intent.getStringExtra("serviceCode") ?: ""

        setSupportActionBar(binding.packetLogToolbar)

        // CouponFragmentでユーザが設定したSIMの表示名を取得する
        val simName = Util.loadSimName(this, serviceCode)

        // Toolbarにタイトルを設定
        supportActionBar?.title = "${if (simName != "") simName else serviceCode}のデータ通信量履歴"
        // Toolbarに×ボタンを左上に設定
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close)
        // Toolbarの×ボタンを表示する
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initLineChart()
    }

    override fun onResume() {
        super.onResume()

        setDataToLineChartByCache(hddServiceCode, serviceCode)
    }

    override fun onPause() {
        super.onPause()

        stopProgressDialog()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(com.otk1fd.simplemio.R.menu.packet_log_chart_activity, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item?.itemId) {
            R.id.action_reload -> {
                setDataToLineChartByHttp(hddServiceCode, serviceCode)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * 折れ線表示域を初期化する
     */
    private fun initLineChart() {
        // X軸を下に表示する．
        lineChartView.xAxis.position = XAxis.XAxisPosition.BOTTOM

        // X軸の値の表示を変えるカスタムフォーマッタを指定する．
        lineChartView.xAxis.valueFormatter = XAxisValueFormatterForDate(dateList)

        // X軸の値を表示する最小インターバルを設定する．
        // 1個ずつに指定したので，実質的にX軸の値をもれなく表示する．
        lineChartView.xAxis.isGranularityEnabled = true
        lineChartView.xAxis.granularity = 1f

        // 画面内に表示するX軸の値の数（非強制）を設定する．
        lineChartView.xAxis.labelCount = 3

        // Y軸の値の表示を変えるカスタムフォーマッタを指定する．
        lineChartView.axisLeft.valueFormatter = YAxisValueFormatterForUnitMB()
        lineChartView.axisRight.valueFormatter = YAxisValueFormatterForUnitMB()

        // Y軸の最小値を設定する．
        lineChartView.axisLeft.axisMinimum = 0f
        lineChartView.axisRight.axisMinimum = 0f

        // Y軸の値を表示する最小インターバルを設定する．
        // 1個ずつに指定したので，実質的にY軸の値をもれなく表示する．
        lineChartView.axisLeft.isGranularityEnabled = true
        lineChartView.axisLeft.granularity = 1f
        lineChartView.axisRight.isGranularityEnabled = true
        lineChartView.axisRight.granularity = 1f

        // X軸に沿ってアニメーションしながらプロットする。
        // ProGuardで難読化するとアニメーションしなくなるので注意
        lineChartView.animateX(1500)

        // Descriptionを非表示にする．
        val description = Description()
        description.text = ""
        lineChartView.description = description
    }


    /**
     * キャッシュから利用履歴データを読み出してグラフにセットする．
     *
     * @param hddServiceCode セットする利用履歴データのhddサービスコード
     * @param serviceCode セットする利用履歴データのhdxサービスコード
     */
    private fun setDataToLineChartByCache(hddServiceCode: String, serviceCode: String) {
        // キャッシュから利用履歴データを読み込む．
        val jsonString =
            mio.loadCachedJsonString(this.applicationContext.getString(R.string.preference_key_cache_packet_log))

        // 読み込んだ利用履歴データが空でなかったら（一度でもキャッシュしたことがあれば）それをグラフにセットする．
        if (jsonString != "{}") {
            Log.d("Set Packet Log (Cache)", "JSON: $jsonString")

            nodataMessageLayout.visibility = View.GONE
            lineChartView.visibility = View.VISIBLE

            val packetLogInfoJson = Mio.parseJsonToPacketLog(jsonString)
            packetLogInfoJson?.let { setDataToLineChart(it, hddServiceCode, serviceCode) }
        } else {
            Log.d("Set Packet Log (Cache)", "Caches JSON is empty")

            nodataMessageLayout.visibility = View.VISIBLE
            lineChartView.visibility = View.GONE
        }
    }

    private fun setDataToLineChartByHttp(hddServiceCode: String, serviceCode: String) {
        lifecycleScope.launch {
            startProgressDialog()

            val packetLogInfoResponseWithHttpResponseCode = mio.getUsageInfo()
            packetLogInfoResponseWithHttpResponseCode.packetLogInfoResponse?.let {
                mio.cacheJsonString(
                    Mio.parsePacketLogToJson(it),
                    getString(R.string.preference_key_cache_packet_log)
                )
                setDataToLineChartByCache(hddServiceCode, serviceCode)
            }?.let {
                httpErrorHandler.handleHttpError(packetLogInfoResponseWithHttpResponseCode.httpStatusCode)
                setDataToLineChartByCache(hddServiceCode, serviceCode)
            }

            stopProgressDialog()
        }
    }

    /**
     * グラフに利用履歴データをセットし，プロットする．
     *
     * @param packetLogInfoResponse IIJmioのAPIから取得できる利用履歴JSONデータ全体
     * @param hddServiceCode セットする利用履歴データのhddサービスコード
     * @param serviceCode セットする利用履歴データのhdxサービスコード
     */
    private fun setDataToLineChart(
        packetLogInfoResponse: PacketLogInfoResponse,
        hddServiceCode: String,
        serviceCode: String
    ) {
        // クーポンON時のグラフプロットデータ
        val withCouponLineDataSet = getLineDataSet(
            packetLogInfoResponse,
            hddServiceCode,
            serviceCode,
            true,
            R.color.packetLogChartWithCoupon,
            "クーポンON",
            true
        )

        // クーポンOFF時のグラフプロットデータ
        val withoutCouponLineDataSet = getLineDataSet(
            packetLogInfoResponse,
            hddServiceCode,
            serviceCode,
            false,
            R.color.packetLogChartWithoutCoupon,
            "クーポンOFF",
            false
        )

        // クーポンOFF時のグラフを先に描画し，そのあとクーポンON時のグラフを表示する
        val dataSets = ArrayList<ILineDataSet>()
        dataSets.add(withoutCouponLineDataSet)
        dataSets.add(withCouponLineDataSet)

        // グラフをデータをセットし，プロットする．
        lineChartView.data = LineData(dataSets)
    }

    /**
     * APIから取得した利用履歴データから，グラフにプロットするデータを作成する．
     *
     * @param packetLogInfoResponse IIJmioのAPIから取得できる利用履歴JSONデータ全体
     * @param hddServiceCode セットする利用履歴データのhddサービスコード
     * @param serviceCode セットする利用履歴データのhdxサービスコード
     * @param couponUse クーポンON時のデータかどうか
     * @param colorResourceId プロットするグラフの色のカラーリソースID
     * @param label データのラベル　「クーポンON時」などとする
     * @param setDateList X軸用の日付データリストを初期化するかどうか
     *
     * @return グラフプロットデータセット
     */
    private fun getLineDataSet(
        packetLogInfoResponse: PacketLogInfoResponse?,
        hddServiceCode: String,
        serviceCode: String,
        couponUse: Boolean,
        colorResourceId: Int,
        label: String,
        setDateList: Boolean
    ): LineDataSet {

        // JSONデータから指定したhddサービスコードの項目を取り出す．
        val packetLogInfoList = packetLogInfoResponse?.packetLogInfo.orEmpty()
        val packetLogInfo = packetLogInfoList.find { it.hddServiceCode == hddServiceCode }

        // 指定したhdxサービスコードの利用履歴データを取り出す．
        val packetLog: List<PacketLog> = with(serviceCode) {
            when {
                contains("hdo") -> packetLogInfo?.hdoInfo?.find { it.hdoServiceCode == serviceCode }?.packetLog.orEmpty()
                contains("hdu") -> packetLogInfo?.hduInfo?.find { it.hduServiceCode == serviceCode }?.packetLog.orEmpty()
                contains("hdx") -> packetLogInfo?.hdxInfo?.find { it.hdxServiceCode == serviceCode }?.packetLog.orEmpty()
                else -> arrayListOf()
            }
        }

        val entries = arrayListOf<Entry>()

        if (setDateList) {
            dateList.clear()
        }

        for ((index, log) in packetLog.withIndex()) {
            // 利用履歴データを，インデックスとペアにしてエントリーとし，リストに加える．
            if (couponUse) {
                entries.add(Entry(index.toFloat(), log.withCoupon.toFloat()))
            } else {
                entries.add(Entry(index.toFloat(), log.withoutCoupon.toFloat()))
            }

            // 利用履歴データの日付部分を，X軸用のデータリストに加える．
            if (setDateList) {
                dateList.add(log.date)
            }
        }

        val color = ContextCompat.getColor(this, colorResourceId)
        val dataSet = LineDataSet(entries, label)

        // 折れ線グラフの線の色
        dataSet.color = color

        // 折れ線グラフの点（折れるところ）の色
        dataSet.setCircleColor(color)

        // 折れ線グラフの点を塗りつぶしにする
        dataSet.setDrawCircleHole(false)

        // 折れ線グラフの点に値を表示しない
        dataSet.setDrawValues(false)

        // 折れ線グラフの線と点のサイズ
        dataSet.lineWidth = 4.0f
        dataSet.circleRadius = 4.0f

        // グラフをタップした時に表示されるハイライトの見た目の設定
        dataSet.highLightColor = color
        dataSet.highlightLineWidth = 1.0f

        return dataSet
    }

    /**
     * ぐるぐる回るダイアログを表示する．
     */
    private fun startProgressDialog() {
        val progressDialogFragmentData = ProgressDialogFragmentData(
            title = "読み込み中",
            message = "少々お待ちください"
        )
        progressDialogFragment =
            ProgressDialogFragment.show(this, progressDialogFragmentData)
    }

    /**
     * ぐるぐる回るダイアログを閉じる．
     */
    private fun stopProgressDialog() {
        progressDialogFragment?.dismiss()
    }

    override fun onSupportNavigateUp(): Boolean {
        // ×ボタンを押したときの動作を，戻るキーを押したときと同じ動作にする．
        onBackPressed()
        return false
    }


    /**
     * X軸に表示する値のフォーマッタ
     *
     * @param xValueStrings X軸の値（日付）の文字列のリスト
     */
    private class XAxisValueFormatterForDate(val xValueStrings: List<String>) :
        IAxisValueFormatter /*ValueFormatter()*/ {
        override fun getFormattedValue(value: Float, axis: AxisBase): String {
            // "value" represents the position of the label on the axis (x or y)

            // 日付データの文字列 2018/01/01 の場合，20180101 となる．
            val dateStr = xValueStrings[value.toInt()]

            // 年月日ごとに部分文字列を抽出
            val yearStr = dateStr.substring(0, 4)
            val monthStr = dateStr.substring(4, 6)
            val dayStr = dateStr.substring(6, 8)

            // 2018/01/01 の形で返す．
            return "$yearStr/$monthStr/$dayStr"
        }
    }

    /**
     * Y軸に表示する値のフォーマッタ
     */
    private class YAxisValueFormatterForUnitMB : IAxisValueFormatter /*ValueFormatter()*/ {
        override fun getFormattedValue(value: Float, axis: AxisBase): String {
            // "value" represents the position of the label on the axis (x or y)

            // 使用したデータ量（MB）に単位の文字列を付加して返す．
            Log.d("hoge", "${value.toInt()}MB")
            return "${value.toInt()}MB"
        }
    }
}


