package com.otk1fd.simplemio.fragments

import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ExpandableListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.otk1fd.simplemio.HttpErrorHandler
import com.otk1fd.simplemio.R
import com.otk1fd.simplemio.Util
import com.otk1fd.simplemio.mio.ApplyCouponStatusResultJson
import com.otk1fd.simplemio.mio.CouponInfo
import com.otk1fd.simplemio.mio.CouponInfoJson
import com.otk1fd.simplemio.mio.MioUtil
import com.otk1fd.simplemio.ui.CouponExpandableListAdapter
import com.otk1fd.simplemio.ui.listview_item.CouponListItemChild
import com.otk1fd.simplemio.ui.listview_item.CouponListItemParent
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList


/**
 * Created by otk1fd on 2018/02/24.
 */
class CouponFragment : Fragment(), View.OnClickListener {

    private lateinit var applyButton: FloatingActionButton
    private lateinit var couponListView: ExpandableListView
    private lateinit var progressDialog: ProgressDialog
    private lateinit var couponSwipeRefreshLayout: SwipeRefreshLayout

    private val couponStatus = HashMap<String, Boolean>().withDefault { false }
    private var oldCouponStatus = cloneHashMapWithDefault(couponStatus)

    private var expandAllGroup = false

    private var expandState: Parcelable? = null
    private var firstVisiblePosition: Int? = 0
    private var offsetPosition: Int? = 0


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_coupon, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        applyButton = activity!!.findViewById(R.id.applyButton)
        applyButton.setOnClickListener(this)
        applyButton.hide()

        progressDialog = ProgressDialog(activity!!)

        couponListView = activity!!.findViewById(R.id.couponListView)
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

        couponSwipeRefreshLayout = activity!!.findViewById(R.id.couponSwipeRefreshLayout)

        couponSwipeRefreshLayout.setOnRefreshListener {
            expandState = couponListView.onSaveInstanceState()
            firstVisiblePosition = couponListView.firstVisiblePosition
            offsetPosition = couponListView.getChildAt(0).top

            setCouponInfoByHttp()
        }
        couponSwipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(activity!!, R.color.colorPrimary))

        val preference = activity!!.getSharedPreferences(activity!!.getString(R.string.preference_file_name), Context.MODE_PRIVATE)
        expandAllGroup = preference.getBoolean(activity!!.getString(R.string.preference_key_expand_all_group), false)
    }

    override fun onResume() {
        super.onResume()

        // 自動で更新を開始
        // ログイン済みか確認
        if (MioUtil.loadToken(activity!!) != "") {
            couponSwipeRefreshLayout.isRefreshing = true
            setCouponInfoByHttp()
        }
    }

    override fun onPause() {
        super.onPause()

        couponSwipeRefreshLayout.isRefreshing = false
        stopProgressDialog()

        expandState = couponListView.onSaveInstanceState()
        firstVisiblePosition = couponListView.firstVisiblePosition
        offsetPosition = couponListView.getChildAt(0).top
    }

    private fun showEditTextDialogToSetSimName(serviceCode: String) {
        val simNameEditText = EditText(activity!!)

        val dialog = AlertDialog.Builder(activity!!)

        dialog.setTitle("SIMの名前を入力してください")
        dialog.setView(simNameEditText)

        simNameEditText.setSingleLine()
        val defaultSimName = Util.loadSimName(activity!!, serviceCode)
        simNameEditText.setText(defaultSimName, TextView.BufferType.NORMAL)
        simNameEditText.setSelection(simNameEditText.text.length)

        dialog.setPositiveButton("完了") { dialog, whichButton ->
            var simnName = simNameEditText.text.toString()
            simnName = simnName.replace("\n", " ")
            Util.saveSimName(activity!!, serviceCode, simnName)
            setCouponInfoByCache()
        }

        dialog.setNegativeButton("キャンセル") { dialog, whichButton ->
        }

        dialog.show()
    }


    override fun onClick(v: View?) {
        if (v == applyButton) {
            startProgressDialog()
            MioUtil.applyCouponStatus(activity!!, couponStatus, execFunc = { it ->
                val applyCouponStatusResultJson: ApplyCouponStatusResultJson? = MioUtil.parseJsonToApplyCouponResponse(it)
                if (applyCouponStatusResultJson?.returnCode == "OK") {
                    setCouponInfoByHttp()
                }
                stopProgressDialog()
            }, errorFunc = {
                HttpErrorHandler.handleHttpError(it, getError = false)
                stopProgressDialog()
            })
        }
    }


    private fun setCouponInfoByHttp() {
        MioUtil.updateCoupon(activity!!, execFunc = { it ->
            val couponInfoJson: CouponInfoJson? = MioUtil.parseJsonToCoupon(it)

            MioUtil.cacheJson(activity!!, it, activity!!.applicationContext.getString(R.string.preference_key_cache_coupon))

            couponInfoJson?.let { setCouponInfo(it) }
            couponSwipeRefreshLayout.isRefreshing = false
        }, errorFunc = {
            // クーポン情報の取得に失敗した場合はキャッシュしているJSONデータをリストに適用する
            HttpErrorHandler.handleHttpError(it) { setCouponInfoByCache() }
            couponSwipeRefreshLayout.isRefreshing = false
        })
    }

    private fun setCouponInfoByCache() {
        val jsonString = MioUtil.loadJsonStringFromCache(activity!!, activity!!.applicationContext.getString(R.string.preference_key_cache_coupon))
        if (jsonString != "{}") {
            val couponInfoJson = MioUtil.parseJsonToCoupon(JSONObject(jsonString))
            couponInfoJson?.let { setCouponInfo(it) }
        }
    }

    private fun setCouponInfo(couponInfoJson: CouponInfoJson) {
        // ExpandableListView のそれぞれの Group 要素の展開状況を控えておく
        val oldAdapter = couponListView.expandableListAdapter
        expandState = couponListView.onSaveInstanceState()

        // 親要素のリスト
        val parents = ArrayList<CouponListItemParent>()
        // 子要素のリスト（親ごとに分類するため，リストのリストになる）
        val childrenList = ArrayList<List<CouponListItemChild>>()

        val couponInfoList = couponInfoJson.couponInfo
        for (couponInfo in couponInfoList) {
            val parent = CouponListItemParent(couponInfo.hddServiceCode, getJapanesePlanName(couponInfo.plan), getVolume(couponInfo))
            parents.add(parent)

            val children = ArrayList<CouponListItemChild>()

            val hdoInfoList = couponInfo.hdoInfo.orEmpty()
            for (hdoInfo in hdoInfoList) {
                val type: String = if (hdoInfo.voice) "音声" else if (hdoInfo.sms) "SMS" else "データ"
                val child = CouponListItemChild(hdoInfo.number, hdoInfo.hdoServiceCode, type, hdoInfo.couponUse)
                children.add(child)
            }

            val hduInfoList = couponInfo.hduInfo.orEmpty()
            for (hduInfo in hduInfoList) {
                val type: String = if (hduInfo.voice) "音声" else if (hduInfo.sms) "SMS" else "データ"
                val child = CouponListItemChild(hduInfo.number, hduInfo.hduServiceCode, type, hduInfo.couponUse)
                children.add(child)
            }

            childrenList.add(children)
        }

        val couponExpandableListAdapter = CouponExpandableListAdapter(activity!!, parents, childrenList, setCouponStatus = { serviceCode, status ->
            couponStatus[serviceCode] = status
            updateApplyButtonShow()
        },
                getCouponStatus = { serviceCode -> couponStatus.getValue(serviceCode) })

        couponListView.setAdapter(couponExpandableListAdapter)

        couponInfoJson.let { setCouponStatus(it) }

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
        val preference = activity!!.getSharedPreferences(activity!!.getString(R.string.preference_file_name), Context.MODE_PRIVATE)
        val useOnlyMB = preference.getBoolean(activity!!.getString(R.string.preference_key_use_MB_only), false)

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

    private fun setCouponStatus(couponInfoJson: CouponInfoJson): Unit {
        for (couponInfo in couponInfoJson.couponInfo) {

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

    private fun <K, V> cloneHashMapWithDefault(srcMap: MutableMap<K, V>): HashMap<K, V> {
        val map = HashMap<K, V>()
        val keys = srcMap.keys
        for (key in keys) {
            map[key] = srcMap.getValue(key)
        }
        return map
    }
}
