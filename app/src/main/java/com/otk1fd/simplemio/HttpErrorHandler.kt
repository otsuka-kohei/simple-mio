package com.otk1fd.simplemio


/**
 * Created by otk1fd on 2018/03/14.
 */
object HttpErrorHandler {

    const val HTTP_OK = 200

    private lateinit var loginFunc: () -> Unit
    private lateinit var showErrorMessageFunc: (String) -> Unit

    fun setUp(loginFunc: () -> Unit, showErrorMessageFunc: (errorMessage: String) -> Unit) {
        this.loginFunc = loginFunc
        this.showErrorMessageFunc = showErrorMessageFunc
    }

    fun handleHttpError(
        httpStatusCode: Int,
        errorByHttpGetRequest: Boolean = true,
        recoveryFunc: () -> Unit = {}
    ) {
        when (httpStatusCode) {
            429 -> {
                if (errorByHttpGetRequest) {
                    showErrorMessageFunc("アクセス数制限のため、最新のデータを取得できませんでした。\nしばらくお待ちの上、再度お試しください。")
                    recoveryFunc()
                } else {
                    showErrorMessageFunc("クーポン切り替えは、1分以上時間を空けてから再度お試しください。")
                    recoveryFunc()
                }
            }
            403 -> {
                loginFunc()
            }
            500 -> {
                showErrorMessageFunc("IIJmioのサーバでエラーが発生し、最新のデータを取得できませんでした。\nしばらくお待ちの上、再度お試しください。")
                recoveryFunc()
            }
            503 -> {
                showErrorMessageFunc("IIJmioのサーバがメンテナンス中のため、最新のデータを取得できませんでした。\nしばらくお待ちの上、再度お試しください。")
                recoveryFunc()
            }
            200 -> {
                /* 正常レスポンス */
            }
            else -> {
                showErrorMessageFunc("通信エラーが発生し、最新のデータを取得できませんでした。\n少しお待ちの上、再度お試しください。")
                recoveryFunc()
            }
        }
    }
}
