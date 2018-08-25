package com.otk1fd.simplemio.fragments

import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ExpandableListView
import com.otk1fd.simplemio.R
import com.otk1fd.simplemio.activities.PacketLogActivity
import com.otk1fd.simplemio.mio.CouponInfoJson
import com.otk1fd.simplemio.mio.MioUtil
import com.otk1fd.simplemio.ui.PacketLogExpandableListAdapter
import com.otk1fd.simplemio.ui.listview_item.PacketLogListItemChild
import com.otk1fd.simplemio.ui.listview_item.PacketLogListItemParent
import org.json.JSONObject


/**
 * Created by otk1fd on 2018/02/24.
 */
class PacketLogFragment : Fragment() {

    private lateinit var packetLogListView: ExpandableListView

    private var expandAllGroup = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_packet_log, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        packetLogListView = activity.findViewById(R.id.packetLogListView)
        // ExpandableListView が展開されたときに自動スクロールするようにする
        packetLogListView.setOnGroupClickListener { parent, v, groupPosition, id ->
            packetLogListView.smoothScrollToPosition(groupPosition)
            false
        }
        packetLogListView.setOnChildClickListener { parent, v, groupPosition, childPosition, id ->
            val adapter = parent.expandableListAdapter
            val parent = adapter.getGroup(groupPosition) as PacketLogListItemParent
            val child = adapter.getChild(groupPosition, childPosition) as PacketLogListItemChild
            val hddServiceCode = parent.hddServiceCode
            val serviceCode = child.serviceCode

            Log.d("packetLog", "hddServiceCode : $hddServiceCode    serviceCode : $serviceCode")
            val intent = Intent(activity, PacketLogActivity::class.java)
            intent.putExtra("hddServiceCode", hddServiceCode)
            intent.putExtra("serviceCode", serviceCode)
            activity.startActivity(intent)

            false
        }

        setServiceListByCache()

        val preference = activity.getSharedPreferences(activity.getString(R.string.preference_file_name), Context.MODE_PRIVATE)
        expandAllGroup = preference.getBoolean(activity.getString(R.string.preference_key_expand_all_group), false)
    }

    override fun onStart() {
        super.onStart()

        setServiceListByCache()
    }

    private fun setServiceListByCache() {
        val jsonString = MioUtil.loadJsonStringFromCache(activity, activity.applicationContext.getString(R.string.preference_key_cache_coupon))
        if (jsonString != "{}") {
            val couponInfoJson = MioUtil.parseJsonToCoupon(JSONObject(jsonString))
            couponInfoJson?.let { setServiceList(it) }
        }
    }

    private fun setServiceList(couponInfoJson: CouponInfoJson) {
        // ExpandableListView のそれぞれの Group 要素の展開状況を控えておく
        val oldAdapter = packetLogListView.expandableListAdapter
        val oldGroupNum: Int = if (oldAdapter == null) 0 else packetLogListView.expandableListAdapter.groupCount
        val expandStatus: List<Boolean> = (0 until oldGroupNum).map { packetLogListView.isGroupExpanded(it) }


        // 親要素のリスト
        val parents = ArrayList<PacketLogListItemParent>()
        // 子要素のリスト（親ごとに分類するため，リストのリストになる）
        val childrenList = ArrayList<List<PacketLogListItemChild>>()

        val couponInfoList = couponInfoJson.couponInfo.orEmpty()
        for (couponInfo in couponInfoList) {
            val parent = PacketLogListItemParent(couponInfo.hddServiceCode, MioUtil.getJapanesePlanName(couponInfo.plan))
            parents.add(parent)

            val children = ArrayList<PacketLogListItemChild>()

            val hdoInfoList = couponInfo.hdoInfo.orEmpty()
            for (hdoInfo in hdoInfoList) {
                val type: String = if (hdoInfo.voice) "音声" else if (hdoInfo.sms) "SMS" else "データ"
                val child = PacketLogListItemChild(hdoInfo.number, hdoInfo.hdoServiceCode, type)
                children.add(child)
            }

            val hduInfoList = couponInfo.hduInfo.orEmpty()
            for (hduInfo in hduInfoList) {
                val type: String = if (hduInfo.voice) "音声" else if (hduInfo.sms) "SMS" else "データ"
                val child = PacketLogListItemChild(hduInfo.number, hduInfo.hduServiceCode, type)
                children.add(child)
            }

            childrenList.add(children)
        }

        val packetLogExpandableListAdapter = PacketLogExpandableListAdapter(activity, parents, childrenList)

        packetLogListView.setAdapter(packetLogExpandableListAdapter)

        // すべて展開するように設定されている場合はすべて展開する
        // そうでなければ，控えておいた ExpandableListView の展開状況を復元する
        val groupNum: Int = packetLogExpandableListAdapter.groupCount
        if (expandAllGroup) {
            for (i in 0 until groupNum) {
                packetLogListView.expandGroup(i)
            }
        } else if (groupNum == oldGroupNum) {
            for (i in 0 until groupNum) {
                if (expandStatus[i]) {
                    packetLogListView.expandGroup(i)
                }
            }
        }
    }
}
