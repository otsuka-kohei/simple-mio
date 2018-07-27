package com.otk1fd.simplemio.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.webkit.WebView
import com.otk1fd.simplemio.R
import kotlinx.android.synthetic.main.activity_open_source.*


class OpenSourceActivity : AppCompatActivity() {

    private lateinit var openSourceWebView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_open_source)

        setSupportActionBar(openSourceToolbar)

        supportActionBar?.title = "オープンソースライセンス"
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        openSourceWebView = findViewById(R.id.openSourceWebView)
        openSourceWebView.settings.useWideViewPort = true
        openSourceWebView.settings.loadWithOverviewMode = true
        openSourceWebView.settings.builtInZoomControls = true
        openSourceWebView.loadUrl("file:///android_asset/openSourse.html")
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return false
    }
}
