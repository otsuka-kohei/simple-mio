package com.otsuka.simplemio.mio

import android.app.Activity
import android.content.Context
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.otsuka.simplemio.R
import org.json.JSONObject
import java.io.Serializable
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


/**
 * Created by otsuka on 2018/03/14.
 */
class MioManager(val activity: Activity, val loginFunc: () -> Unit) : Serializable {
    val lock = ReentrantLock()

    fun saveToken(token: String) {
        val preference = activity.getSharedPreferences(activity.getString(R.string.preference_file_name), Context.MODE_PRIVATE)
        val editor = preference.edit()
        editor.putString("token", token)
        editor.apply()
    }

    fun loadToken(): String {
        val preference = activity.getSharedPreferences(activity.getString(R.string.preference_file_name), Context.MODE_PRIVATE)
        return preference.getString("token", "")
    }

    fun deleteToken() {
        val preference = activity.getSharedPreferences(activity.getString(R.string.preference_file_name), Context.MODE_PRIVATE)
        val editor = preference.edit()
        editor.remove("token")
        editor.apply()
    }

    fun update(execFunc: () -> Unit = {}): Unit {

    }

    private fun httpGet(successFunc: (JSONObject) -> Unit, errorFunc: (VolleyError) -> Unit): Unit {

        // Volleyのキャッシュ処理を排他的に行うため，RequestQueueの複数生成と実行を防ぐ
        lock.withLock {
            val jsonRequest = object : JsonObjectRequest(Request.Method.GET, "https://api.iijmio.jp/mobile/d/v2/coupon/", null,
                    object : Response.Listener<JSONObject> {
                        override fun onResponse(result: JSONObject) {
                            successFunc(result)
                        }
                    },
                    object : Response.ErrorListener {
                        override fun onErrorResponse(error: VolleyError) {
                            errorFunc(error)
                        }
                    }) {

                // ヘッダの追加
                @Throws(AuthFailureError::class)
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = super.getHeaders()
                    val newHeaders = HashMap<String, String>()
                    newHeaders.putAll(headers)
                    newHeaders.put("X-IIJmio-Developer", activity.getString(R.string.developer_id))
                    val token = loadToken()
                    newHeaders.put("X-IIJmio-Authorization", token)
                    return newHeaders
                }
            }

            val queue = Volley.newRequestQueue(activity)
            queue.add(jsonRequest)
            queue.start()
        }
    }
}