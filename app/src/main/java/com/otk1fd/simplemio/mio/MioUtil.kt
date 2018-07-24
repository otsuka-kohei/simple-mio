package com.otk1fd.simplemio.mio

import android.app.Activity
import android.content.Context
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.otk1fd.simplemio.R
import com.otk1fd.simplemio.Util
import com.squareup.moshi.Moshi
import org.json.JSONObject


/**
 * Created by otk1fd on 2018/03/14.
 */


object MioUtil {

    private lateinit var queue: RequestQueue
    private lateinit var loginFunc: () -> Unit

    fun setUp(activity: Activity, loginFunc: () -> Unit) {
        queue = Volley.newRequestQueue(activity)
        this.loginFunc = loginFunc
    }

    fun saveToken(activity: Activity, token: String) {
        val preference = activity.getSharedPreferences(activity.getString(R.string.preference_file_name), Context.MODE_PRIVATE)
        val editor = preference.edit()
        editor.putString(activity.getString(R.string.preference_key_token), token)
        editor.apply()
    }

    fun loadToken(activity: Activity): String {
        val preference = activity.getSharedPreferences(activity.getString(R.string.preference_file_name), Context.MODE_PRIVATE)
        return preference.getString(activity.getString(R.string.preference_key_token), "")
    }

    fun deleteToken(activity: Activity) {
        val preference = activity.getSharedPreferences(activity.getString(R.string.preference_file_name), Context.MODE_PRIVATE)
        val editor = preference.edit()
        editor.remove(activity.getString(R.string.preference_key_token))
        editor.apply()
    }

    fun updateCoupon(activity: Activity, execFunc: (JSONObject) -> Unit = {}, errorFunc: (VolleyError) -> Unit = {}) {
        val url = "https://api.iijmio.jp/mobile/d/v2/coupon/"

        httpGet(activity, url, { execFunc(it) }, { errorFunc(it) })
    }

    fun updatePacket(activity: Activity, execFunc: (JSONObject) -> Unit = {}, errorFunc: (VolleyError) -> Unit = {}) {
        val url = "https://api.iijmio.jp/mobile/d/v2/log/packet/"

        httpGet(activity, url, { execFunc(it) }, { errorFunc(it) })
    }

    fun applyCouponStatus(activity: Activity, coupomStatusMap: Map<String, Boolean>, execFunc: (JSONObject) -> Unit = {}, errorFunc: (VolleyError) -> Unit = {}) {
        val url = "https://api.iijmio.jp/mobile/d/v2/coupon/"

        val hdoList = ArrayList<CouponStatus>()
        val hduList = ArrayList<CouponStatus>()

        for ((serviceCode, status) in coupomStatusMap) {
            if (serviceCode.contains("hdo")) {
                hdoList.add(CouponStatus(serviceCode, status))
            } else if (serviceCode.contains("hdu")) {
                hduList.add(CouponStatus(serviceCode, status))
            }
        }

        val postJsonObject: JSONObject = generateCouponPostJsonObject(hdoList, hduList)

        httpPut(activity, url, postJsonObject, { execFunc(it) }, { errorFunc(it) })
    }

    private data class CouponStatus(
            val hdxServiceCode: String,
            val coupon: Boolean
    )

    private fun generateCouponPostJsonObject(hdoList: List<CouponStatus>, hduList: List<CouponStatus>): JSONObject {
        var hdoStr = """"hdoInfo":["""
        var hduStr = """"hduInfo":["""

        for ((index, couponStatus) in hdoList.withIndex()) {

            val hdo = couponStatus.hdxServiceCode
            val on = couponStatus.coupon
            val str = """{"hdoServiceCode":"$hdo","couponUse":$on}"""

            hdoStr += str

            if (index < hdoList.size - 1) {
                hdoStr += ", "
            }
        }
        hdoStr += " ]"

        for ((index, couponStatus) in hduList.withIndex()) {

            val hdu = couponStatus.hdxServiceCode
            val on = couponStatus.coupon
            val str = """{"hdoServiceCode":"$hdu","couponUse":$on}"""

            hduStr += str

            if (index < hduList.size - 1) {
                hduStr += ",\n"
            }
        }
        hduStr += " ]"

        val jsonStr = """{"couponInfo":[{$hdoStr,$hduStr}]}"""

        return JSONObject(jsonStr)
    }

    private fun httpGet(activity: Activity, url: String, successFunc: (JSONObject) -> Unit, errorFunc: (VolleyError) -> Unit) {

        val jsonRequest = object : JsonObjectRequest(Request.Method.GET, url, null,
                Response.Listener<JSONObject> { successFunc(it) },
                Response.ErrorListener { errorFunc(it) }) {
            // ヘッダの追加
            @Throws(AuthFailureError::class)
            override fun getHeaders(): MutableMap<String, String> {
                val headers = super.getHeaders()
                val newHeaders = HashMap<String, String>()
                newHeaders.putAll(headers)
                newHeaders["X-IIJmio-Developer"] = Util.getDeveloperId(activity)
                val token = loadToken(activity)
                newHeaders["X-IIJmio-Authorization"] = token
                return newHeaders
            }
        }

        queue.add(jsonRequest)
        queue.start()
    }

    private fun httpPut(activity: Activity, url: String, jsonObject: JSONObject, successFunc: (JSONObject) -> Unit, errorFunc: (VolleyError) -> Unit) {

        val jsonRequest = object : JsonObjectRequest(Request.Method.PUT, url, jsonObject,
                Response.Listener<JSONObject> { successFunc(it) },
                Response.ErrorListener { errorFunc(it) }) {

            // ヘッダの追加
            @Throws(AuthFailureError::class)
            override fun getHeaders(): MutableMap<String, String> {
                val headers = super.getHeaders()
                val newHeaders = HashMap<String, String>()
                newHeaders.putAll(headers)
                newHeaders["X-IIJmio-Developer"] = Util.getDeveloperId(activity)
                val token = loadToken(activity)
                newHeaders["X-IIJmio-Authorization"] = token
                return newHeaders
            }

            override fun getBodyContentType(): String {
                return "application/json"
            }
        }

        queue.add(jsonRequest)
        queue.start()
    }

    fun parseJsonToCoupon(json: JSONObject): CouponInfoJson? {
        val adapter = Moshi.Builder().build().adapter(CouponInfoJson::class.java)
        return adapter.fromJson(json.toString())
    }

    fun parseJsonToApplyCouponResponse(json: JSONObject): ApplyCouponStatusResultJson? {
        val adapter = Moshi.Builder().build().adapter(ApplyCouponStatusResultJson::class.java)
        return adapter.fromJson(json.toString())
    }

    fun getJapanesePlanName(plan: String): String {
        if (plan == "Family Share") return "ファミリーシェアプラン"
        if (plan == "Minimum Start") return "ミニマムスタートプラン"
        if (plan == "Light Start") return "ライトスタートプラン"
        if (plan == "Eco Minimum") return "エコプランミニマム"
        if (plan == "Eco Standard") return "エコプランスタンダード"
        return ""
    }
}
