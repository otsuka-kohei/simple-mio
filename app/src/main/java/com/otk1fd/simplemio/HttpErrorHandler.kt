package com.otk1fd.simplemio

import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.otk1fd.simplemio.activities.FinishActivity
import com.otk1fd.simplemio.dialog.AlertDialogFragment
import com.otk1fd.simplemio.dialog.AlertDialogFragmentData
import com.otk1fd.simplemio.mio.Mio
import kotlinx.coroutines.launch


/**
 * Created by otk1fd on 2018/03/14.
 */
class HttpErrorHandler(private val fragmentActivity: FragmentActivity, private val mio: Mio) {

    companion object {
        const val HTTP_OK = 200
    }

    private fun showErrorMessage(errorMessage: String) {
        val alertDialogFragmentData = AlertDialogFragmentData(
            message = errorMessage,
            positiveButtonText = "OK"
        )
        AlertDialogFragment.show(
            fragmentActivity,
            alertDialogFragmentData
        )
    }

    fun handleHttpError(
        httpStatusCode: Int,
        errorByHttpGetRequest: Boolean = true
    ) {
        Log.d("HTTP Error Handling", "HTTP status code: $httpStatusCode")
        when (httpStatusCode) {
            429 -> {
                if (errorByHttpGetRequest) {
                    showErrorMessage("アクセス数制限のため、最新のデータを取得できませんでした。\nしばらくお待ちの上、再度お試しください。")
                } else {
                    showErrorMessage("クーポン切り替えは、1分以上時間を空けてから再度お試しください。")
                }
            }
            403 -> {
                fragmentActivity.lifecycleScope.launch {
                    val result = mio.login()
                    if (!result) {
                        FinishActivity.finishApplication(fragmentActivity)
                    }
                }
            }
            500 -> {
                showErrorMessage("IIJmioのサーバでエラーが発生し、最新のデータを取得できませんでした。\nしばらくお待ちの上、再度お試しください。")
            }
            503 -> {
                showErrorMessage("IIJmioのサーバがメンテナンス中のため、最新のデータを取得できませんでした。\nしばらくお待ちの上、再度お試しください。")
            }
            200 -> {
                /* 正常レスポンス */
            }
            else -> {
                showErrorMessage("通信エラーが発生し、最新のデータを取得できませんでした。\n少しお待ちの上、再度お試しください。")
            }
        }
    }
}
