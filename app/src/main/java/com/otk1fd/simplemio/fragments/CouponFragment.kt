package com.otk1fd.simplemio.fragments

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
import com.otk1fd.simplemio.dialog.ProgressDialogFragment
import com.otk1fd.simplemio.dialog.ProgressDialogFragmentData
import com.otk1fd.simplemio.mio.Mio
import com.otk1fd.simplemio.mio.json.CouponInfo
import com.otk1fd.simplemio.mio.json.CouponInfoResponse
import com.otk1fd.simplemio.mio.json.PacketLogInfoResponse
import com.otk1fd.simplemio.ui.CouponExpandableListAdapter
import com.otk1fd.simplemio.ui.listview_item.CouponListItemChild
import com.otk1fd.simplemio.ui.listview_item.CouponListItemParent
import kotlinx.coroutines.launch
import java.util.*


/**
 * Created by otk1fd on 2018/02/24.
 */
class CouponFragment : Fragment() {
    private lateinit var mio: Mio

    private lateinit var applyButton: FloatingActionButton
    private lateinit var couponExpandableListView: ExpandableListView
    private lateinit var couponSwipeRefreshLayout: SwipeRefreshLayout

    private val currentCouponStatusMap: MutableMap<String, Boolean> =
        HashMap<String, Boolean>().withDefault { false }
    private var previousCouponStatusMap = cloneHashMapWithDefault(currentCouponStatusMap)

    private var expandAllGroup = false

    private var expandStateParcelable: Parcelable? = null
    private var firstVisiblePosition: Int? = 0
    private var firstVisiblePositionOffsetForChileElement: Int? = 0

    private var isFirstTime: Boolean = true

    private var progressDialogFragment: ProgressDialogFragment? = null

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
        applyButton.hide()
        applyButton.setOnClickListener {
            requireActivity().lifecycleScope.launch {
                startProgressDialog()
                val couponInfoResponseWithHttpResponseCode = mio.getCouponInfo()
                val httpResponseCode: Int = mio.applyCouponSetting(currentCouponStatusMap)

                if (httpResponseCode == HttpErrorHandler.HTTP_OK) {
                    setCouponInfoByHttp()
                } else {
                    HttpErrorHandler.handleHttpError(
                        couponInfoResponseWithHttpResponseCode.httpStatusCode,
                        errorByHttpGetRequest = false
                    )
                }

                stopProgressDialog()
            }
        }

        couponExpandableListView = requireActivity().findViewById(R.id.couponListView)
        // ExpandableListView が展開されたときに自動スクロールするようにする
        couponExpandableListView.setOnGroupClickListener { _, _, groupPosition, _ ->
            couponExpandableListView.smoothScrollToPosition(groupPosition)
            return@setOnGroupClickListener false
        }

        couponExpandableListView.setOnItemLongClickListener { _, _, _, id ->
            val groupPosition = ExpandableListView.getPackedPositionGroup(id)
            val childPosition = ExpandableListView.getPackedPositionChild(id)

            val packedPositionType = ExpandableListView.getPackedPositionType(id)
            if (packedPositionType == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
                val adapter = couponExpandableListView.expandableListAdapter
                val child = adapter.getChild(groupPosition, childPosition) as CouponListItemChild
                val serviceCode = child.serviceCode
                showEditTextDialogToSetSimName(serviceCode)
            }
            false
        }

        couponSwipeRefreshLayout = requireActivity().findViewById(R.id.couponSwipeRefreshLayout)

        couponSwipeRefreshLayout.setOnRefreshListener {
            expandStateParcelable = couponExpandableListView.onSaveInstanceState()
            firstVisiblePosition = couponExpandableListView.firstVisiblePosition
            firstVisiblePositionOffsetForChileElement =
                couponExpandableListView.getChildAt(0)?.top ?: 0
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

        if (isFirstTime) {
            firstCachingAndSetByHTTP()
            isFirstTime = false
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

        expandStateParcelable = couponExpandableListView.onSaveInstanceState()
        firstVisiblePosition = couponExpandableListView.firstVisiblePosition
        firstVisiblePositionOffsetForChileElement = couponExpandableListView.getChildAt(0)?.top ?: 0
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

    private suspend fun setCouponInfoByHttp() {
        Log.d("Set Coupon Info (HTTP)", "Start")
        couponSwipeRefreshLayout.isRefreshing = true

        val couponInfoResponseWithHttpResponseCode = mio.getCouponInfo()

        couponInfoResponseWithHttpResponseCode.couponInfoResponse?.let {
            Log.d("Set Coupon Info (HTTP)", "Finish successfully")
            mio.cacheJsonString(
                Mio.parseCouponToJson(it),
                requireActivity().applicationContext.getString(R.string.preference_key_cache_coupon)
            )
            setCouponInfoByCache()
        } ?: let {
            Log.d("Set Coupon Info (HTTP)", "Finish failed")
            HttpErrorHandler.handleHttpError(couponInfoResponseWithHttpResponseCode.httpStatusCode) {
                lifecycleScope.launch {
                    setCouponInfoByCache()
                }
            }
        }

        couponSwipeRefreshLayout.isRefreshing = false
    }

    fun refreshCouponInfo() {
        setCouponInfoByCache()
    }

    private fun setCouponInfoByCache() {
        val jsonString =
            mio.loadCachedJsonString(requireActivity().applicationContext.getString(R.string.preference_key_cache_coupon))

        if (jsonString != "{}") {
            Log.d("Set Coupon (Cache)", "JSON: $jsonString")
            val couponInfoJson = Mio.parseJsonToCoupon(jsonString)
            couponInfoJson?.let { setCouponInfo(it) }
        } else {
            Log.d("Set Coupon (Cache)", "Caches JSON is empty")
        }
    }

    private fun setCouponInfo(couponInfoResponse: CouponInfoResponse) {
        // ExpandableListView のそれぞれの Group 要素の展開状況を控えておく
        val oldAdapter = couponExpandableListView.expandableListAdapter
        expandStateParcelable = couponExpandableListView.onSaveInstanceState()

        // 親要素のリスト
        val parents = ArrayList<CouponListItemParent>()
        // 子要素のリスト（親ごとに分類するため，リストのリストになる）
        val childrenList = ArrayList<List<CouponListItemChild>>()

        val couponInfoList = couponInfoResponse.couponInfo
        for (couponInfo in couponInfoList) {
            val parent = CouponListItemParent(
                couponInfo.hddServiceCode,
                Mio.getJapanesePlanName(couponInfo.plan),
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

        val couponExpandableListAdapter = CouponExpandableListAdapter(
            this,
            parents,
            childrenList,
            setCouponStatusFunc = { serviceCode, status ->
                currentCouponStatusMap[serviceCode] = status
                updateApplyButtonShow()
            },
            getCouponStatusFunc = { serviceCode -> currentCouponStatusMap.getValue(serviceCode) })

        couponExpandableListView.setAdapter(couponExpandableListAdapter)

        couponInfoResponse.let { setCouponStatus(it) }

        // すべて展開するように設定されている場合はすべて展開する
        // そうでなければ，控えておいた ExpandableListView の展開状況を復元する
        val groupNum: Int = couponExpandableListAdapter.groupCount
        if (expandAllGroup) {
            for (i in 0 until groupNum) {
                couponExpandableListView.expandGroup(i)
            }
        }

        expandStateParcelable?.let { couponExpandableListView.onRestoreInstanceState(it) }

        val firstVisiblePositionSet = firstVisiblePosition ?: 0
        val childPositionSet = firstVisiblePositionOffsetForChileElement ?: 0
        couponExpandableListView.setSelectionFromTop(firstVisiblePositionSet, childPositionSet)

        previousCouponStatusMap = cloneHashMapWithDefault(currentCouponStatusMap)
        updateApplyButtonShow()
    }

    private fun updateApplyButtonShow() {
        if (currentCouponStatusMap != previousCouponStatusMap) {
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

    private fun setCouponStatus(couponInfoResponse: CouponInfoResponse): Unit {
        for (couponInfo in couponInfoResponse.couponInfo) {

            val hdoInfoList = couponInfo.hdoInfo.orEmpty()
            for (hdoInfo in hdoInfoList) {
                currentCouponStatusMap[hdoInfo.hdoServiceCode] = hdoInfo.couponUse
            }

            val hduInfoList = couponInfo.hduInfo.orEmpty()
            for (hduInfo in hduInfoList) {
                currentCouponStatusMap[hduInfo.hduServiceCode] = hduInfo.couponUse
            }

        }
    }

    private fun startProgressDialog() {
        val progressDialogFragmentData = ProgressDialogFragmentData(
            title = "クーポン設定適用中",
            message = "少々お待ちください"
        )
        progressDialogFragment =
            ProgressDialogFragment.show(requireActivity(), progressDialogFragmentData)
    }

    private fun stopProgressDialog() {
        progressDialogFragment?.dismiss()
    }

    private fun <K, V> cloneHashMapWithDefault(srcMap: MutableMap<K, V>): MutableMap<K, V> {
        val map = HashMap<K, V>()
        val keys = srcMap.keys
        for (key in keys) {
            map[key] = srcMap.getValue(key)
        }
        return map
    }

    private fun firstCachingAndSetByHTTP() {
        lifecycleScope.launch {
            couponSwipeRefreshLayout.isRefreshing = true

            val couponInfoResponseWithHttpResponseCode = mio.getCouponInfo()
            couponInfoResponseWithHttpResponseCode.couponInfoResponse?.let { couponInfoResponse: CouponInfoResponse ->
                mio.cacheJsonString(
                    Mio.parseCouponToJson(couponInfoResponse),
                    requireActivity().getString(R.string.preference_key_cache_coupon)
                )
                setCouponInfoByCache()

                val packetLogInfoResponseWithHttpResponseCode = mio.getUsageInfo()
                packetLogInfoResponseWithHttpResponseCode.packetLogInfoResponse?.let { packetLogInfoResponse: PacketLogInfoResponse ->
                    mio.cacheJsonString(
                        Mio.parsePacketLogToJson(packetLogInfoResponse),
                        requireActivity().getString(R.string.preference_key_cache_packet_log)
                    )
                } ?: let {
                    HttpErrorHandler.handleHttpError(
                        packetLogInfoResponseWithHttpResponseCode.httpStatusCode
                    )
                }
            } ?: let {
                HttpErrorHandler.handleHttpError(couponInfoResponseWithHttpResponseCode.httpStatusCode) {
                    requireActivity().lifecycleScope.launch {
                        setCouponInfoByCache()
                    }
                }
            }

            couponSwipeRefreshLayout.isRefreshing = false
        }
    }
}
