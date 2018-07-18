package com.otsuka.simplemio.fragments

import android.app.Fragment
import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ExpandableListView
import android.widget.Toast
import com.otsuka.simplemio.R
import com.otsuka.simplemio.Util
import com.otsuka.simplemio.mio.ApplyCouponStatusResultJson
import com.otsuka.simplemio.mio.CouponInfo
import com.otsuka.simplemio.mio.CouponInfoJson
import com.otsuka.simplemio.mio.MioManager
import com.otsuka.simplemio.ui.CouponExpandableListAdapter
import com.otsuka.simplemio.ui.listview_item.CouponListItemChild
import com.otsuka.simplemio.ui.listview_item.CouponListItemParent
import java.util.*
import kotlin.collections.ArrayList


/**
 * Created by otsuka on 2018/02/24.
 */
class CouponFragment : Fragment(), View.OnClickListener {

    //フラグメント上で発生するイベント（OnClickListenerとか）は極力フラグメントの中で済ませた方がいいと思う

    private lateinit var updateButton: Button
    private lateinit var applyButton: Button
    private lateinit var couponListView: ExpandableListView
    private lateinit var progressDialog: ProgressDialog

    private val couponStatus = HashMap<String, Boolean>()

    lateinit var startOAuthWithDialog: () -> Unit


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        Log.d("onCreateView", "before return")
        return inflater.inflate(R.layout.fragment_coupon, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        updateButton = activity.findViewById(R.id.updateButton)
        updateButton.setOnClickListener(this)

        applyButton = activity.findViewById(R.id.applyButton)
        applyButton.setOnClickListener(this)

        couponListView = activity.findViewById(R.id.couponListView)
        // ExpandableListView が展開されたときに自動スクロールするようにする
        couponListView.setOnGroupClickListener() { parent, v, groupPosition, id ->
            couponListView.smoothScrollToPosition(groupPosition)
            false
        }

        progressDialog = ProgressDialog(activity)

        setCouponInfoToListView()
    }

    override fun onClick(v: View?) {
        if (v == updateButton) {
            setCouponInfoToListView()
        } else if (v == applyButton) {
            MioManager.applyCouponStatus(activity, couponStatus, execFunc = { it ->
                val applyCouponStatusResultJson: ApplyCouponStatusResultJson? = MioManager.parseJsonToApplyCouponResponse(it)
                if (applyCouponStatusResultJson?.returnCode == "OK") {
                    setCouponInfoToListView()
                }
            }, errorFunc = {
                val errorCode = it.networkResponse.statusCode

                if (errorCode == 403) {
                    startOAuthWithDialog()
                } else if (errorCode == 429) {
                    Toast.makeText(activity, "1分以上時間を空けてからもう一度お試しください", Toast.LENGTH_LONG).show()
                } else {
                    Util.showAlertDialog(activity, "エラー", "予期しないエラーが発生しました。",
                            "了解")
                }
            })
        }
    }

    private fun setCouponInfoToListView(): Unit {
        MioManager.updateCoupon(activity, execFunc = { it ->
            startProgressDialog()

            // ExpandableListView のそれぞれの Group 要素の展開状況を控えておく
            val groupNum: Int? = couponListView.expandableListAdapter?.groupCount
            val expandStatus: List<Boolean> = if (groupNum != null) (0 until groupNum).map { couponListView.isGroupExpanded(it) } else ArrayList()
            groupNum?.let { Log.d("groupNum", groupNum.toString()) }

            val couponInfoJson: CouponInfoJson? = MioManager.parseJsonToCoupon(it)

            // 親要素のリスト
            val parents = ArrayList<CouponListItemParent>()
            // 子要素のリスト（親ごとに分類するため，リストのリストになる）
            val childrenList = ArrayList<List<CouponListItemChild>>()

            val couponInfoList = couponInfoJson?.couponInfo.orEmpty()
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

            val couponExpandableListAdapter = CouponExpandableListAdapter(activity, parents, childrenList, setCouponStatus = { serviceCode, status ->
                couponStatus.put(serviceCode, status)
                Log.d("coupon status", couponStatus.toString())
            },
                    getCouponStatus = { serviceCode -> couponStatus.getOrDefault(serviceCode, false) })
            couponListView.setAdapter(couponExpandableListAdapter)

            stopProgressDialog()
            couponInfoJson?.let { setCouponStatus(it) }

            // 控えておいた ExpandableListView の展開状況を復元する
            if (groupNum != null) {
                for (i in 0 until groupNum) {
                    if (expandStatus[i]) {
                        Log.d("expandablelistview", "expand")
                        couponListView.expandGroup(i)
                    }
                }
            }
        })
    }

    private fun startProgressDialog(): Unit {
        progressDialog.setTitle("読み込み中");
        progressDialog.setMessage("少々お待ちください")
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        progressDialog.show()
    }

    private fun stopProgressDialog(): Unit {
        progressDialog.dismiss()
    }

    private fun getVolume(couponInfo: CouponInfo): String {

        val plan: String = couponInfo.plan

        if (plan.contains("Eco")) {
            val mb = couponInfo.remains ?: 0
            if (mb > 1000) {
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
                val expireYear: Int = expire.subSequence(0, 4).toString().toInt()
                val expireMonth: Int = expire.subSequence(4, 6).toString().toInt()

                if (expireYear < nowYear && expireMonth < nowMonth) {
                    break
                }

                volume += coupon.volume
            }

            if (volume > 1000) {
                volume /= 1000
                return volume.toString() + "GB"
            }

            return volume.toString() + "MB"
        }

        return "1GB"
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
}
