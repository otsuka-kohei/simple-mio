package com.otk1fd.simplemio.mio

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.otk1fd.simplemio.R
import com.otk1fd.simplemio.activities.FinishActivity
import com.otk1fd.simplemio.databinding.ActivityMioLoginBinding

class MioLoginActivity : FragmentActivity() {

    private lateinit var binding: ActivityMioLoginBinding

    private val mio = Mio(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMioLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginButon.setOnClickListener {
            startOAuth()
        }

        binding.exitButton.setOnClickListener {
            FinishActivity.finishApplication(this)
        }
    }

    override fun onBackPressed() {
        setResult(RESULT_CANCELED)
        finish()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        intent?.data?.let { uri: Uri ->
            if (uri.toString().contains("simplemio")) {
                // 受け取るURIが
                // simplemio://callback#access_token=token&state=success&token_type=Bearer&expires_in=7776000
                // となっていて，正しくエンコードできないので # を ? に置き換える
                var uriString = uri.toString()
                uriString = uriString.replace('#', '?')
                val replacedUri: Uri = Uri.parse(uriString)

                val token: String = replacedUri.getQueryParameter("access_token") ?: ""
                val state: String = replacedUri.getQueryParameter("state") ?: ""

                if (state != "success") {
                    setResult(RESULT_CANCELED)
                } else {
                    mio.saveToken(token)
                    setResult(RESULT_OK)
                }
                finish()
            }
        }?.let {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun startOAuth() {
        val uri = "https://api.iijmio.jp/mobile/d/v1/authorization/?" +
                "response_type=token" +
                "&client_id=" + getString(R.string.developer_id) +
                "&redirect_uri=" + getString(R.string.simple_app_name) + "%3A%2F%2Fcallback" +
                "&state=" + "success"

        // リクエスト用URIを渡して，外部ブラウザのActivityを起動する．
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        startActivity(intent)
    }
}