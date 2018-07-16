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
import com.otsuka.simplemio.R
import com.otsuka.simplemio.mio.CouponInfo
import com.otsuka.simplemio.mio.CouponInfoJson
import com.otsuka.simplemio.mio.MioManager
import com.otsuka.simplemio.ui.CouponExpandableListAdapter
import com.otsuka.simplemio.ui.listview_item.CouponListItemChild
import com.otsuka.simplemio.ui.listview_item.CouponListItemParent


/**
 * Created by otsuka on 2018/02/24.
 */
class CouponFragment : Fragment(), View.OnClickListener {

    //フラグメント上で発生するイベント（OnClickListenerとか）は極力フラグメントの中で済ませた方がいいと思う

    private lateinit var updateButton: Button
    private lateinit var couponListView: ExpandableListView
    private lateinit var progressDialog: ProgressDialog


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
        couponListView = activity.findViewById(R.id.couponListView)
        progressDialog = ProgressDialog(activity)
        setCouponInfoToListView()
    }

    override fun onClick(v: View?) {
        if (v == updateButton) {
            setCouponInfoToListView()
        }
    }

    private fun setCouponInfoToListView(): Unit {
        MioManager.updateCoupon(activity, execFunc = { it ->
            startProgressDialog()

            val couponInfoJson: CouponInfoJson? = MioManager.parseJsonToCoupon(it)

            val parents = ArrayList<CouponListItemParent>()
            val children = ArrayList<List<CouponListItemChild>>()

            val couponInfoList = couponInfoJson?.couponInfo.orEmpty()
            for (couponInfo in couponInfoList) {
                val parent = CouponListItemParent(couponInfo.hddServiceCode, couponInfo.plan, getVolume(couponInfo))
                parents.add(parent)

                val hdoInfoList = couponInfo.hdoInfo.orEmpty()
                val hdoChildren = ArrayList<CouponListItemChild>()
                for (hdoInfo in hdoInfoList) {
                    val type: String = if (hdoInfo.voice) "音声" else if (hdoInfo.sms) "SMS" else "データ"
                    val child = CouponListItemChild(hdoInfo.number, hdoInfo.hdoServiceCode, type, hdoInfo.couponUse)
                    hdoChildren.add(child)
                }
                children.add(hdoChildren)

                val hduInfoList = couponInfo.hduInfo.orEmpty()
                val hduChildren = ArrayList<CouponListItemChild>()
                for (hduInfo in hduInfoList) {
                    val type: String = if (hduInfo.voice) "音声" else if (hduInfo.sms) "SMS" else "データ"
                    val child = CouponListItemChild(hduInfo.number, hduInfo.hduServiceCode, type, hduInfo.couponUse)
                    hduChildren.add(child)
                }
                children.add(hduChildren)
            }

            val couponExpandableListAdapter = CouponExpandableListAdapter(activity, parents, children)
            couponListView.setAdapter(couponExpandableListAdapter)

            stopProgressDialog()
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
        return "1GB"
    }
}
