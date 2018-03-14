package com.otsuka.simplemio.fragments

import android.app.Fragment
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.otsuka.simplemio.MioUtil
import com.otsuka.simplemio.R

/**
 * Created by otsuka on 2018/02/24.
 */
class AboutFragment : Fragment(), View.OnClickListener {

    //フラグメント上で発生するイベント（OnClickListenerとか）は極力フラグメントの中で済ませた方がいいと思う
    private lateinit var logoutButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        Log.d("onCreateView", "before return")
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        logoutButton = activity.findViewById(R.id.logoutButton)
        logoutButton.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        if (v == logoutButton) {
            Log.d("login", "logout")
            MioUtil.deleteToken(activity)
        }
    }
}