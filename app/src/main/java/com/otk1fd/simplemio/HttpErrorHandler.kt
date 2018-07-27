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

    fun handleHttpError(volleyError: VolleyError?, getError: Boolean = true, recoveryFunc: () -> Unit = {}) {
        if (volleyError == null || volleyError.networkResponse == null) {
            showErrorMessageFunc("不明なエラーが発生しました。\n少しお待ちの上、再度お試しください。")
            recoveryFunc()
            return
        }

        val errorCode = volleyError.networkResponse.statusCode

        when (errorCode) {
            429 -> {
                if (getError) {
                    showErrorMessageFunc("アクセス数制限のため、最新のデータを取得できませんでした。\n少しお待ちの上、再度お試しください。")
                    recoveryFunc()
                } else {
                    showErrorMessageFunc("クーポン切り替えは、1分以上時間を空けてから再度お試しください。")
                }
            }
            403 -> {
                loginFunc()
            }
            500 -> {
                showErrorMessageFunc("IIJ mioのサーバでエラーが発生し、最新のデータを取得できませんでした。\nしばらくお待ちの上、再度お試しください。")
                recoveryFunc()
            }
            503 -> {
                showErrorMessageFunc("IIJ mioのサーバがメンテナンス中のため、最新のデータを取得できませんでした。\nしばらくお待ちの上、再度お試しください。")
                recoveryFunc()
            }
            else -> {
                showErrorMessageFunc("エラーが発生し、最新のデータを取得できませんでした。\nばらくお待ちの上、再度お試しください。")
                recoveryFunc()
            }
        }
    }
}
