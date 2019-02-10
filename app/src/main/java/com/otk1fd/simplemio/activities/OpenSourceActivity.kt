package com.otk1fd.simplemio.activities

import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import com.otk1fd.simplemio.R
import kotlinx.android.synthetic.main.activity_open_source.*


/**
 * オープンソース情報を表示するActivity．
 * [AboutFragment][com.otk1fd.simplemio.fragments.AboutFragment]から呼び出される．
 * オープンソース情報の表示には，アプリに同梱したHTMLファイルをWebViewで表示する方法を採用した．
 */
class OpenSourceActivity : AppCompatActivity() {

    private lateinit var openSourceWebView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_open_source)

        setSupportActionBar(openSourceToolbar)

        // Toolbarにタイトルを設定
        supportActionBar?.title = "オープンソースライセンス"
        // Toolbarに×ボタンを左上に設定
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close)
        // Toolbarの×ボタンを表示する
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // WebViewの初期設定
        openSourceWebView = findViewById(R.id.openSourceWebView)
        openSourceWebView.settings.useWideViewPort = true
        openSourceWebView.settings.loadWithOverviewMode = true
        openSourceWebView.settings.builtInZoomControls = true
    }

    override fun onStart() {
        super.onStart()

        // WebViewでHTMLを読み込み
        openSourceWebView.loadUrl("file:///android_asset/openSourse.html")
    }

    override fun onSupportNavigateUp(): Boolean {
        // ×ボタンを押したときの動作を，戻るキーを押したときと同じ動作にする．
        onBackPressed()
        return false
    }
}
