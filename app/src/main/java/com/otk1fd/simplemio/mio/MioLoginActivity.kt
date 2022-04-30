package com.otk1fd.simplemio.mio

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.otk1fd.simplemio.R

class MioLoginActivity : AppCompatActivity() {
    companion object {
        const val TOKEN_KEY = "token"
        const val LOGIN_FLAG_KEY = "login"
    }

    private val viewModel: MioLoginActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.getBooleanExtra(LOGIN_FLAG_KEY, false)) {
            startOAuth()
        }
    }

    override fun onResume() {
        super.onResume()

        if (viewModel.beforeLogin.value != false) {
            viewModel.beforeLogin.value = false
            if (intent.getBooleanExtra(LOGIN_FLAG_KEY, false)) {
                startOAuth()
            }
        } else {
            setResult(RESULT_CANCELED)
            finish()
        }
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

                val returnIntent = Intent()
                if (state != "success") {
                    setResult(RESULT_CANCELED)
                    finish()
                } else {
                    returnIntent.putExtra(TOKEN_KEY, token)
                    setResult(RESULT_OK, returnIntent)
                    Log.d("hoge", "return token $token")
                    finish()
                }
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

class MioLoginActivityViewModel(handle: SavedStateHandle) : ViewModel() {
    val beforeLogin: MutableLiveData<Boolean> = handle.getLiveData("BEFORE_LOGIN", true)
}