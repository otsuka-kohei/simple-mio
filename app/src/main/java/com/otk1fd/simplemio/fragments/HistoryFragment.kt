package com.otk1fd.simplemio.fragments

import android.app.Fragment
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ExpandableListView
import com.otk1fd.simplemio.R
import com.otk1fd.simplemio.activity.HistoryActivity
import com.otk1fd.simplemio.mio.CouponInfoJson
import com.otk1fd.simplemio.mio.MioUtil
import com.otk1fd.simplemio.ui.HistoryExpandableListAdapter
import com.otk1fd.simplemio.ui.listview_item.HistoryListItemChild
import com.otk1fd.simplemio.ui.listview_item.HistoryListItemParent


/**
 * Created by otk1fd on 2018/02/24.
 */
class HistoryFragment : Fragment() {

    //フラグメント上で発生するイベント（OnClickListenerとか）は極力フラグメントの中で済ませた方がいいと思う

    private lateinit var historyListView: ExpandableListView
    private lateinit var progressDialog: ProgressDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        historyListView = activity.findViewById(R.id.historyListView)
        // ExpandableListView が展開されたときに自動スクロールするようにする
        historyListView.setOnGroupClickListener() { parent, v, groupPosition, id ->
            historyListView.smoothScrollToPosition(groupPosition)
            false
        }
        historyListView.setOnChildClickListener() { parent, v, groupPosition, childPosition, id ->
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

        progressDialog = ProgressDialog(activity)

        setCouponInfoToListView()
    }

    private fun setCouponInfoToListView(): Unit {
        startProgressDialog()
        MioUtil.updateCoupon(activity, execFunc = { it ->

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

            stopProgressDialog()
        }, errorFunc = {
            stopProgressDialog()
        })
    }


    private fun startProgressDialog(): Unit {
        Log.d("progress", "start")
        progressDialog.setTitle("読み込み中")
        progressDialog.setMessage("少々お待ちください")
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        progressDialog.show()
    }

    private fun stopProgressDialog(): Unit {
        Log.d("progress", "stop")
        progressDialog.dismiss()
    }
}
