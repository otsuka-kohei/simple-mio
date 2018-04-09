package com.otsuka.simplemio.fragments

import android.app.Fragment
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.otsuka.simplemio.R
import com.otsuka.simplemio.mio.CouponInfoJson
import com.otsuka.simplemio.mio.MioManager


/**
 * Created by otsuka on 2018/02/24.
 */
class CouponFragment : Fragment(), View.OnClickListener {

    //フラグメント上で発生するイベント（OnClickListenerとか）は極力フラグメントの中で済ませた方がいいと思う

    private lateinit var testButton: Button
    private lateinit var testTextView: TextView

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

        testButton = activity.findViewById(R.id.testButton)
        testButton.setOnClickListener(this)
        testTextView = activity.findViewById(R.id.testTextView)
    }

    override fun onClick(v: View?) {
        if (v == testButton) {
            MioManager.updateCoupon(activity, { it ->
                testTextView.text = it.toString()
            })
        }
    }

    private fun setCouponInfoToListView(couponInfoJson: CouponInfoJson) {

    }
}
