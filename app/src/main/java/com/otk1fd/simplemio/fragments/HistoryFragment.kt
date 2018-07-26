package com.otk1fd.simplemio.fragments

import android.app.Fragment
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ExpandableListView
import com.otk1fd.simplemio.HttpErrorHandler
import com.otk1fd.simplemio.R
import com.otk1fd.simplemio.activity.HistoryActivity
import com.otk1fd.simplemio.mio.CouponInfoJson
import com.otk1fd.simplemio.mio.MioUtil
import com.otk1fd.simplemio.ui.HistoryExpandableListAdapter
import com.otk1fd.simplemio.ui.listview_item.HistoryListItemChild
import com.otk1fd.simplemio.ui.listview_item.HistoryListItemParent
import kotlinx.android.synthetic.main.fragment_history.*


/**
 * Created by otk1fd on 2018/02/24.
 */
class HistoryFragment : Fragment() {

    //フラグメント上で発生するイベント（OnClickListenerとか）は極力フラグメントの中で済ませた方がいいと思う

    private lateinit var historyListView: ExpandableListView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        historyListView = activity.findViewById(R.id.historyListView)
        // ExpandableListView が展開されたときに自動スクロールするようにする
        historyListView.setOnGroupClickListener { parent, v, groupPosition, id ->
            historyListView.smoothScrollToPosition(groupPosition)
            false
        }
        historyListView.setOnChildClickListener { parent, v, groupPosition, childPosition, id ->
            val adapter = parent.expandableListAdapter
            val parent = adapter.getGroup(groupPosition) as HistoryListItemParent
            val child = adapter.getChild(groupPosition, childPosition) as HistoryListItemChild
            val hddServiceCode = parent.hddServiceCode
            val serviceCode = child.serviceCode

            Log.d("history", "hddServiceCode : $hddServiceCode    serviceCode : $serviceCode")
            val intent = Intent(activity, HistoryActivity::class.java)
            intent.putExtra("hddServiceCode", hddServiceCode)
            intent.putExtra("serviceCode", serviceCode)
            activity.startActivity(intent)

            false
        }

        historySwipeRefreshLayout.setOnRefreshListener {
            Log.d("History", "refresh")
            setServiceListToListView()
        }

        historySwipeRefreshLayout.post {
            historySwipeRefreshLayout.isRefreshing = true
            setServiceListToListView()
        }
    }

    private fun setServiceListToListView() {
        MioUtil.updateCoupon(activity, execFunc = { it ->

            // ExpandableListView のそれぞれの Group 要素の展開状況を控えておく
            val groupNum: Int? = historyListView.expandableListAdapter?.groupCount
            val expandStatus: List<Boolean> = if (groupNum != null) (0 until groupNum).map { historyListView.isGroupExpanded(it) } else ArrayList()

            val couponInfoJson: CouponInfoJson? = MioUtil.parseJsonToCoupon(it)

            // 親要素のリスト
            val parents = ArrayList<HistoryListItemParent>()
            // 子要素のリスト（親ごとに分類するため，リストのリストになる）
            val childrenList = ArrayList<List<HistoryListItemChild>>()

            val couponInfoList = couponInfoJson?.couponInfo.orEmpty()
            for (couponInfo in couponInfoList) {
                val parent = HistoryListItemParent(couponInfo.hddServiceCode, MioUtil.getJapanesePlanName(couponInfo.plan))
                parents.add(parent)

                val children = ArrayList<HistoryListItemChild>()

                val hdoInfoList = couponInfo.hdoInfo.orEmpty()
                for (hdoInfo in hdoInfoList) {
                    val type: String = if (hdoInfo.voice) "音声" else if (hdoInfo.sms) "SMS" else "データ"
                    val child = HistoryListItemChild(hdoInfo.number, hdoInfo.hdoServiceCode, type)
                    children.add(child)
                }

                val hduInfoList = couponInfo.hduInfo.orEmpty()
                for (hduInfo in hduInfoList) {
                    val type: String = if (hduInfo.voice) "音声" else if (hduInfo.sms) "SMS" else "データ"
                    val child = HistoryListItemChild(hduInfo.number, hduInfo.hduServiceCode, type)
                    children.add(child)
                }

                childrenList.add(children)
            }

            val historyExpandableListAdapter = HistoryExpandableListAdapter(activity, parents, childrenList)

            historyListView.setAdapter(historyExpandableListAdapter)

            // 控えておいた ExpandableListView の展開状況を復元する
            if (groupNum != null) {
                for (i in 0 until groupNum) {
                    if (expandStatus[i]) {
                        historyListView.expandGroup(i)
                    }
                }
            }

            historySwipeRefreshLayout.isRefreshing = false
        }, errorFunc = {
            HttpErrorHandler.handleHttpError(it)
            historySwipeRefreshLayout.isRefreshing = false
        })
    }
}
