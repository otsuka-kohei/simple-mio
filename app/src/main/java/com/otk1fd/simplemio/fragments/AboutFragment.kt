package com.otk1fd.simplemio.fragments

import android.app.Fragment
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.otk1fd.simplemio.R
import com.otk1fd.simplemio.Util
import com.otk1fd.simplemio.activities.OpenSourceActivity
import com.otk1fd.simplemio.mio.MioUtil


/**
 * Created by otk1fd on 2018/02/24.
 */
class AboutFragment : Fragment(), View.OnClickListener {

    //フラグメント上で発生するイベント（OnClickListenerとか）は極力フラグメントの中で済ませた方がいいと思う
    private lateinit var logoutButton: Button
    private lateinit var aboutTextView: TextView
    private lateinit var openSourceTextView: TextView

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

        openSourceTextView = activity.findViewById(R.id.openSourceTitleTextView)
        openSourceTextView.setOnClickListener(this)

    }

    override fun onClick(v: View?) {
        if (v == logoutButton) {
            Log.d("login", "logout")
            Util.showAlertDialog(activity, "ログアウト", "IIJmioからログアウトしてもよろしいですか？",
                    "はい", negativeButtonText = "いいえ",
                    positiveFunc = {
                        MioUtil.deleteToken(activity)
                        Util.showAlertDialog(activity, "ログアウト完了", "IIJmioからログアウトしました．\nアプリを終了します",
                                "はい",
                                positiveFunc = {
                                    activity.finish()
                                })
                    })
        }

        if (v == openSourceTextView) {
            val intent = Intent(activity, OpenSourceActivity::class.java)
            activity.startActivity(intent)
        }
    }
}