package com.otsuka.simplemio.fragments

import android.app.Fragment
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.TextView
import com.otsuka.simplemio.R
import com.otsuka.simplemio.Util
import com.otsuka.simplemio.mio.MioManager

/**
 * Created by otsuka on 2018/02/24.
 */
class AboutFragment : Fragment(), View.OnClickListener {

    //フラグメント上で発生するイベント（OnClickListenerとか）は極力フラグメントの中で済ませた方がいいと思う
    private lateinit var logoutButton: Button
    private lateinit var aboutTextView: TextView
    private lateinit var openSourseWebView: WebView

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

        aboutTextView = activity.findViewById(R.id.aboutTextView)
        aboutTextView.text = activity.getString(R.string.about)

        openSourseWebView = activity.findViewById(R.id.openSourseWebView)
        openSourseWebView.settings.useWideViewPort = true
        openSourseWebView.settings.loadWithOverviewMode = true
        openSourseWebView.settings.builtInZoomControls = true
        openSourseWebView.loadUrl("file:///android_asset/openSourse.html")

    }

    override fun onClick(v: View?) {
        if (v == logoutButton) {
            Log.d("login", "logout")
            Util.showAlertDialog(activity, "ログアウト", "IIJmioからログアウトしてもよろしいですか？",
                    "はい", negativeButtonText = "いいえ",
                    positiveFunc = {
                        MioManager.deleteToken(activity)
                        Util.showAlertDialog(activity, "ログアウト完了", "IIJmioからログアウトしました．\nアプリを終了します",
                                "はい",
                                positiveFunc = {
                                    MioManager.deleteToken(activity)
                                    activity.finish()
                                })
                    })
        }
    }
}