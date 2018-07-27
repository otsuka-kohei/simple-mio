package com.otk1fd.simplemio

import com.android.volley.VolleyError


/**
 * Created by otk1fd on 2018/03/14.
 */
object HttpErrorHandler {
    private lateinit var loginFunc: () -> Unit
    private lateinit var showErrorMessageFunc: (String) -> Unit

    fun setUp(loginFunc: () -> Unit, showErrorMessageFunc: (errorMessage: String) -> Unit) {
        this.loginFunc = loginFunc
        this.showErrorMessageFunc = showErrorMessageFunc
    }

    fun handleHttpError(volleyError: VolleyError?, recoveryFunc: () -> Unit = {}) {
        if (volleyError == null || volleyError.networkResponse == null) {
            showErrorMessageFunc("不明なエラーが発生しました。\n少しお待ちの上、再度お試しください。")
            recoveryFunc()
            return
        }

        val errorCode = volleyError.networkResponse.statusCode

        when (errorCode) {
            429 -> {
                showErrorMessageFunc("1分以上時間を空けてからもう一度お試しください。")
                recoveryFunc()
            }
            403 -> {
                loginFunc()
            }
            500 -> {
                showErrorMessageFunc("IIJ mioのサーバでエラーが発生しました。\nしばらくお待ちの上、再度お試しください。")
                recoveryFunc()
            }
            503 -> {
                showErrorMessageFunc("IIJ mioのサーバがメンテナンス中です。\nしばらくお待ちの上、再度お試しください。")
                recoveryFunc()
            }
            else -> {
                showErrorMessageFunc("予期しないエラーが発生しました。\nしばらく待ってもこのエラーが発生する場合は、本アプリ開発者にお問い合わせください。")
                recoveryFunc()
            }
        }
    }
}
