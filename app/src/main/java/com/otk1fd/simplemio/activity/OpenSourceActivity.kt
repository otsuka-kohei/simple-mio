package com.otk1fd.simplemio.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.webkit.WebView
import com.otk1fd.simplemio.R

class OpenSourceActivity : AppCompatActivity() {

    private lateinit var openSourceWebView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_open_source)

        val toolbar: Toolbar = findViewById(R.id.openSourceToolbar)
        toolbar.title = "オープンソースライセンス"

        openSourceWebView = findViewById(R.id.openSourceWebView)
        openSourceWebView.settings.useWideViewPort = true
        openSourceWebView.settings.loadWithOverviewMode = true
        openSourceWebView.settings.builtInZoomControls = true
        openSourceWebView.loadUrl("file:///android_asset/openSourse.html")
    }
}
