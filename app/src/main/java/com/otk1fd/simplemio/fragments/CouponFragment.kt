package com.otk1fd.simplemio.fragments

import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ExpandableListView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.otk1fd.simplemio.HttpErrorHandler
import com.otk1fd.simplemio.R
import com.otk1fd.simplemio.Util
import com.otk1fd.simplemio.activities.MainActivity
import com.otk1fd.simplemio.dialog.EditTextDialogFragment
import com.otk1fd.simplemio.dialog.EditTextDialogFragmentData
import com.otk1fd.simplemio.mio.CouponInfo
import com.otk1fd.simplemio.mio.CouponInfoResponse
import com.otk1fd.simplemio.mio.Mio
import com.otk1fd.simplemio.ui.CouponExpandableListAdapter
import com.otk1fd.simplemio.ui.listview_item.CouponListItemChild
import com.otk1fd.simplemio.ui.listview_item.CouponListItemParent
import kotlinx.coroutines.launch
import java.util.*


/**
 * Created by otk1fd on 2018/02/24.
 */
class CouponFragment : Fragment(), View.OnClickListener {
    private lateinit var mio: Mio

    private lateinit var applyButton: FloatingActionButton
    private lateinit var couponListView: ExpandableListView
    private lateinit var progressDialog: ProgressDialog
    private lateinit var couponSwipeRefreshLayout: SwipeRefreshLayout

    private val couponStatus: MutableMap<String, Boolean> =
        HashMap<String, Boolean>().withDefault { false }
    private var oldCouponStatus = cloneHashMapWithDefault(couponStatus)

    private var expandAllGroup = false

    private var expandState: Parcelable? = null
    private var firstVisiblePosition: Int? = 0
    private var offsetPosition: Int? = 0

    private var firstStarting: Boolean = true

    private var bulkUpdateCounter: Int = 0


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_coupon, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mio = (requireActivity() as MainActivity).mio

        applyButton = requireActivity().findViewById(R.id.applyButton)
        applyButton.setOnClickListener(this)
        applyButton.hide()

        progressDialog = ProgressDialog(requireActivity())

        couponListView = requireActivity().findViewById(R.id.couponListView)
        // ExpandableListView が展開されたときに自動スクロールするようにする
        couponListView.setOnGroupClickListener { parent, v, groupPosition, id ->
            couponListView.smoothScrollToPosition(groupPosition)
            return@setOnGroupClickListener false
        }

        couponListView.setOnItemLongClickListener { adapterView, view, i, l ->
            val groupPosition = ExpandableListView.getPackedPositionGroup(l)
            val childPosition = ExpandableListView.getPackedPositionChild(l)

            val packedPositionType = ExpandableListView.getPackedPositionType(l)
            if (packedPositionType == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
                val adapter = couponListView.expandableListAdapter
                val child = adapter.getChild(groupPosition, childPosition) as CouponListItemChild
                val serviceCode = child.serviceCode
                showEditTextDialogToSetSimName(serviceCode)
            }
            false
        }

        couponSwipeRefreshLayout = requireActivity().findViewById(R.id.couponSwipeRefreshLayout)

        couponSwipeRefreshLayout.setOnRefreshListener {
            expandState = couponListView.onSaveInstanceState()
            firstVisiblePosition = couponListView.firstVisiblePosition
            offsetPosition = couponListView.getChildAt(0)?.top ?: 0
            lifecycleScope.launch {
                setCouponInfoByHttp()
            }
        }
        couponSwipeRefreshLayout.setColorSchemeColors(
            ContextCompat.getColor(
                requireActivity(),
                R.color.colorPrimary
            )
        )

        val preference = requireActivity().getSharedPreferences(
            requireActivity().getString(R.string.preference_file_name),
            Context.MODE_PRIVATE
        )
        expandAllGroup = preference.getBoolean(
            requireActivity().getString(R.string.preference_key_expand_all_group),
            false
        )
    }

    override fun onResume() {
        super.onResume()

        if (firstStarting) {
            firstCachingAndSetByHTTP()
            firstStarting = false
        } else {
            lifecycleScope.launch {
                setCouponInfoByCache()
            }
        }
    }

    override fun onPause() {
        super.onPause()

        couponSwipeRefreshLayout.isRefreshing = false
        stopProgressDialog()

        expandState = couponListView.onSaveInstanceState()
        firstVisiblePosition = couponListView.firstVisiblePosition
        offsetPosition = couponListView.getChildAt(0)?.top ?: 0
    }

    private fun showEditTextDialogToSetSimName(serviceCode: String) {
        val editTextDialogFragmentData = EditTextDialogFragmentData(
            title = "SIMの名前を入力してください",
            message = "",
            positiveButtonText = "完了",
            positiveButtonFunc = { fragmentActivity, text ->
                val simnName = text.replace("\n", " ")
                Util.saveSimName(fragmentActivity, serviceCode, simnName)
                fragmentActivity.lifecycleScope.launch {
                    setCouponInfoByCache()
                }
            },
            negativeButtonText = "キャンセル",
            defaultText = Util.loadSimName(requireActivity(), serviceCode),
            hint = "SIMの名前"
        )

        EditTextDialogFragment.show(requireActivity(), editTextDialogFragmentData)
    }


    override fun onClick(v: View?) {
        if (v == applyButton) {
            requireActivity().lifecycleScope.launch {
                startProgressDialog()
                val couponInfoResponseWithHttpResponseCode = mio.getCouponInfo()
                val httpResponseCode: Int = mio.applyCouponSetting(couponStatus)

                if (httpResponseCode == 200) {
                    setCouponInfoByHttp()
                } else {
                    HttpErrorHandler.handleHttpError(
                        couponInfoResponseWithHttpResponseCode.httpStatusCode,
                        getError = false
                    )
                }

                stopProgressDialog()
            }
        }
    }

    private suspend fun setCouponInfoByHttp() {
        val couponInfoResponseWithHttpResponseCode = mio.getCouponInfo()

        couponInfoResponseWithHttpResponseCode.couponInfoResponse?.let {
            mio.cacheJsonString(
                Mio.parseCouponToJson(it),
                requireActivity().applicationContext.getString(R.string.preference_key_cache_coupon)
            )
            setCouponInfoByCache()
        }?:let {
            HttpErrorHandler.handleHttpError(couponInfoResponseWithHttpResponseCode.httpStatusCode) {
                lifecycleScope.launch {
                    setCouponInfoByCache()
                }
            }
        }

        couponSwipeRefreshLayout.isRefreshing = false
    }

    private suspend fun setCouponInfoByCache() {
        val jsonString =
            mio.loadCachedJsonString(requireActivity().applicationContext.getString(R.string.preference_key_cache_coupon))
        if (jsonString != "{}") {
            val couponInfoJson = Mio.parseJsonToCoupon(jsonString)
            couponInfoJson?.let { setCouponInfo(it) }
        } else {
            couponSwipeRefreshLayout.isRefreshing = true
            setCouponInfoByHttp()
        }
    }

    private fun setCouponInfo(couponInfoResponse: CouponInfoResponse) {
        // ExpandableListView のそれぞれの Group 要素の展開状況を控えておく
        val oldAdapter = couponListView.expandableListAdapter
        expandState = couponListView.onSaveInstanceState()

        // 親要素のリスト
        val parents = ArrayList<CouponListItemParent>()
        // 子要素のリスト（親ごとに分類するため，リストのリストになる）
        val childrenList = ArrayList<List<CouponListItemChild>>()

        val couponInfoList = couponInfoResponse.couponInfo
        for (couponInfo in couponInfoList) {
            val parent = CouponListItemParent(
                couponInfo.hddServiceCode,
                getJapanesePlanName(couponInfo.plan),
                getVolume(couponInfo)
            )
            parents.add(parent)

            val children = ArrayList<CouponListItemChild>()

            val hdoInfoList = couponInfo.hdoInfo.orEmpty()
            for (hdoInfo in hdoInfoList) {
                val type: String = if (hdoInfo.voice) "音声" else if (hdoInfo.sms) "SMS" else "データ"
                val child = CouponListItemChild(
                    hdoInfo.number,
                    hdoInfo.hdoServiceCode,
                    type,
                    hdoInfo.couponUse
                )
                children.add(child)
            }

            val hduInfoList = couponInfo.hduInfo.orEmpty()
            for (hduInfo in hduInfoList) {
                val type: String = if (hduInfo.voice) "音声" else if (hduInfo.sms) "SMS" else "データ"
                val child = CouponListItemChild(
                    hduInfo.number,
                    hduInfo.hduServiceCode,
                    type,
                    hduInfo.couponUse
                )
                children.add(child)
            }

            childrenList.add(children)
        }

        val couponExpandableListAdapter = CouponExpandableListAdapter(requireActivity(),
            parents,
            childrenList,
            setCouponStatus = { serviceCode, status ->
                couponStatus[serviceCode] = status
                updateApplyButtonShow()
            },
            getCouponStatus = { serviceCode -> couponStatus.getValue(serviceCode) })

        couponListView.setAdapter(couponExpandableListAdapter)

        couponInfoResponse.let { setCouponStatus(it) }

        // すべて展開するように設定されている場合はすべて展開する
        // そうでなければ，控えておいた ExpandableListView の展開状況を復元する
        val groupNum: Int = couponExpandableListAdapter.groupCount
        if (expandAllGroup) {
            for (i in 0 until groupNum) {
                couponListView.expandGroup(i)
            }
        }

        expandState?.let { couponListView.onRestoreInstanceState(it) }

        val firstVisiblePositionSet = firstVisiblePosition ?: 0
        val childPositionSet = offsetPosition ?: 0
        couponListView.setSelectionFromTop(firstVisiblePositionSet, childPositionSet)

        oldCouponStatus = cloneHashMapWithDefault(couponStatus)
        updateApplyButtonShow()
    }

    private fun updateApplyButtonShow() {
        if (couponStatus != oldCouponStatus) {
            applyButton.show()
        } else {
            applyButton.hide()
        }
    }

    private fun getVolume(couponInfo: CouponInfo): String {
        val preference = requireActivity().getSharedPreferences(
            requireActivity().getString(R.string.preference_file_name),
            Context.MODE_PRIVATE
        )
        val useOnlyMB = preference.getBoolean(
            requireActivity().getString(R.string.preference_key_use_MB_only),
            false
        )

        val plan: String = couponInfo.plan

        if (plan.contains("Eco")) {
            val mb = couponInfo.remains ?: 0
            if (!useOnlyMB && mb > 1000) {
                val gb = mb / 1000
                return gb.toString() + "GB"
            }

            return mb.toString() + "MB"

        } else {
            val couponList = couponInfo.coupon.orEmpty()

            val now: Calendar = Calendar.getInstance()
            val nowYear: Int = now.get(Calendar.YEAR)
            val nowMonth: Int = now.get(Calendar.MONTH) + 1

            var volume = 0

            for (coupon in couponList) {
                val expire: String = coupon.expire ?: "197001"
                val expireYear: Int = expire.substring(0, 4).toInt()
                val expireMonth: Int = expire.substring(4, 6).toInt()

                if (expireYear < nowYear && expireMonth < nowMonth) {
                    break
                }

                volume += coupon.volume
            }

            if (!useOnlyMB && volume > 1000) {
                volume /= 1000
                return volume.toString() + "GB"
            }

            return volume.toString() + "MB"
        }
    }

    private fun getJapanesePlanName(plan: String): String {
        if (plan == "Family Share") return "ファミリーシェアプラン"
        if (plan == "Minimum Start") return "ミニマムスタートプラン"
        if (plan == "Light Start") return "ライトスタートプラン"
        if (plan == "Eco Minimum") return "エコプランミニマム"
        if (plan == "Eco Standard") return "エコプランスタンダード"

        return ""
    }

    private fun setCouponStatus(couponInfoResponse: CouponInfoResponse): Unit {
        for (couponInfo in couponInfoResponse.couponInfo) {

            val hdoInfoList = couponInfo.hdoInfo.orEmpty()
            for (hdoInfo in hdoInfoList) {
                couponStatus[hdoInfo.hdoServiceCode] = hdoInfo.couponUse
            }

            val hduInfoList = couponInfo.hduInfo.orEmpty()
            for (hduInfo in hduInfoList) {
                couponStatus[hduInfo.hduServiceCode] = hduInfo.couponUse
            }

        }
    }

    private fun startProgressDialog() {
        progressDialog.setTitle("クーポン設定適用中")
        progressDialog.setMessage("少々お待ちください")
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        progressDialog.show()
    }

    private fun stopProgressDialog() {
        progressDialog.dismiss()
    }

    private fun <K, V> cloneHashMapWithDefault(srcMap: MutableMap<K, V>): MutableMap<K, V> {
        val map = HashMap<K, V>()
        val keys = srcMap.keys
        for (key in keys) {
            map[key] = srcMap.getValue(key)
        }
        return map
    }

    private fun countBulkUpdateFinished() {
        bulkUpdateCounter++
        if (bulkUpdateCounter == 2) {
            couponSwipeRefreshLayout.isRefreshing = false
        }
    }

    private fun firstCachingAndSetByHTTP() {
        couponSwipeRefreshLayout.isRefreshing = true
        bulkUpdateCounter = 0

        lifecycleScope.launch {
            val couponInfoResponseWithHttpResponseCode = mio.getCouponInfo()
            couponInfoResponseWithHttpResponseCode.couponInfoResponse?.let {
                mio.cacheJsonString(
                    Mio.parseCouponToJson(it),
                    requireActivity().getString(R.string.preference_key_cache_coupon)
                )
                setCouponInfoByCache()
            }?:let {
                HttpErrorHandler.handleHttpError(couponInfoResponseWithHttpResponseCode.httpStatusCode) {
                    requireActivity().lifecycleScope.launch {
                        setCouponInfoByCache()
                    }
                }
            }

            countBulkUpdateFinished()
        }

        lifecycleScope.launch {
            val packetLogInfoResponseWithHttpResponseCode = mio.getUsageInfo()
            packetLogInfoResponseWithHttpResponseCode.packetLogInfoResponse?.let {
                mio.cacheJsonString(
                    Mio.parsePacketLogToJson(it),
                    requireActivity().getString(R.string.preference_key_cache_packet_log)
                )
            }?:let {
                HttpErrorHandler.handleHttpError(
                    packetLogInfoResponseWithHttpResponseCode.httpStatusCode,
                    suggestLogin = false
                )
            }

            countBulkUpdateFinished()
        }
    }
}
