package com.otk1fd.simplemio.fragments

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ExpandableListView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.otk1fd.simplemio.HttpErrorHandler
import com.otk1fd.simplemio.R
import com.otk1fd.simplemio.Util
import com.otk1fd.simplemio.activities.MainActivity
import com.otk1fd.simplemio.databinding.FragmentCouponBinding
import com.otk1fd.simplemio.dialog.EditTextDialogFragment
import com.otk1fd.simplemio.dialog.EditTextDialogFragmentData
import com.otk1fd.simplemio.dialog.ProgressDialogFragment
import com.otk1fd.simplemio.dialog.ProgressDialogFragmentData
import com.otk1fd.simplemio.mio.Mio
import com.otk1fd.simplemio.mio.json.CouponInfo
import com.otk1fd.simplemio.mio.json.CouponInfoResponse
import com.otk1fd.simplemio.ui.CouponExpandableListAdapter
import com.otk1fd.simplemio.ui.listview_item.CouponListItemChild
import com.otk1fd.simplemio.ui.listview_item.CouponListItemParent
import kotlinx.coroutines.launch
import java.util.*


/**
 * Created by otk1fd on 2018/02/24.
 */
class CouponFragment : Fragment() {

    private lateinit var binding: FragmentCouponBinding

    private lateinit var mio: Mio
    private lateinit var httpErrorHandler: HttpErrorHandler

    private lateinit var applyButton: FloatingActionButton
    private lateinit var couponExpandableListView: ExpandableListView
    private lateinit var couponSwipeRefreshLayout: SwipeRefreshLayout
    private lateinit var nodataMessageLayout: ConstraintLayout

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
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCouponBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mio = (requireActivity() as MainActivity).mio
        httpErrorHandler = (requireActivity() as MainActivity).httpErrorHandler

        nodataMessageLayout = binding.nodataMessageLayout

        applyButton = binding.applyButton
        applyButton.hide()
        applyButton.setOnClickListener {
            requireActivity().lifecycleScope.launch {
                startProgressDialog()

                val httpResponseCode: Int = mio.applyCouponSetting(currentCouponStatusMap)
                if (httpResponseCode == HttpErrorHandler.HTTP_OK) {
                    setCouponInfoByHttp()
                } else {
                    httpErrorHandler.handleHttpError(
                        httpResponseCode,
                        errorByHttpGetRequest = false
                    )
                }

                stopProgressDialog()
            }
        }

        couponExpandableListView = binding.couponListView
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

        couponSwipeRefreshLayout = binding.couponSwipeRefreshLayout

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
            lifecycleScope.launch {
                setCouponInfoByHttp()
            }
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
                val simName = text.replace("\n", " ")
                Util.saveSimName(requireActivity(), serviceCode, simName)
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
        nodataMessageLayout.visibility = View.GONE

        val couponInfoResponseWithHttpResponseCode = mio.getCouponInfo()
        couponInfoResponseWithHttpResponseCode.couponInfoResponse?.let {
            Log.d("Set Coupon Info (HTTP)", "Finish successfully")
            mio.cacheJsonString(
                Mio.parseCouponToJson(it),
                getString(R.string.preference_key_cache_coupon)
            )
        } ?: let {
            Log.d("Set Coupon Info (HTTP)", "Finish failed")
            httpErrorHandler.handleHttpError(couponInfoResponseWithHttpResponseCode.httpStatusCode)
        }
        setCouponInfoByCache()

        couponSwipeRefreshLayout.isRefreshing = false
    }

    fun refreshCouponInfo() {
        setCouponInfoByCache()
    }

    private fun setCouponInfoByCache() {
        val jsonString =
            mio.loadCachedJsonString(getString(R.string.preference_key_cache_coupon))

        if (jsonString != "{}") {
            Log.d("Set Coupon (Cache)", "JSON: $jsonString")

            nodataMessageLayout.visibility = View.GONE

            val couponInfoJson = Mio.parseJsonToCoupon(jsonString)
            couponInfoJson?.let { setCouponInfo(it) }
        } else {
            Log.d("Set Coupon (Cache)", "Caches JSON is empty")

            nodataMessageLayout.visibility = View.VISIBLE
        }
    }

    private fun setCouponInfo(couponInfoResponse: CouponInfoResponse) {
        // ExpandableListView のそれぞれの Group 要素の展開状況を控えておく
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

        setCouponStatus(couponInfoResponse)

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
                val expire: String = coupon.expire ?: ""
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

    private fun setCouponStatus(couponInfoResponse: CouponInfoResponse) {
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
}
