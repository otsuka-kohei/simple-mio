package com.otk1fd.simplemio.mio

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPut
import com.github.kittinunf.result.Result
import com.otk1fd.simplemio.R
import com.otk1fd.simplemio.mio.json.CouponInfoResponse
import com.otk1fd.simplemio.mio.json.CouponInfoResponseWithHttpResponseCode
import com.otk1fd.simplemio.mio.json.PacketLogInfoResponse
import com.otk1fd.simplemio.mio.json.PacketLogInfoResponseWithHttpResponseCode
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


/**
 * Created by otk1fd on 2018/03/14.
 */


class Mio(private val fragmentActivity: FragmentActivity) {
    companion object {
        fun parseJsonToCoupon(jsonString: String): CouponInfoResponse? {
            val adapter = Moshi.Builder().build().adapter(CouponInfoResponse::class.java)
            return try {
                adapter.fromJson(jsonString)
            } catch (e: Exception) {
                null
            }
        }

        fun parseCouponToJson(couponInfoResponse: CouponInfoResponse): String {
            val adapter = Moshi.Builder().build().adapter(CouponInfoResponse::class.java)
            return adapter.toJson(couponInfoResponse)
        }

        fun parseJsonToPacketLog(jsonString: String): PacketLogInfoResponse? {
            val adapter = Moshi.Builder().build().adapter(PacketLogInfoResponse::class.java)
            return try {
                adapter.fromJson(jsonString)
            } catch (e: Exception) {
                null
            }
        }

        fun parsePacketLogToJson(packetLogInfoResponse: PacketLogInfoResponse): String {
            val adapter = Moshi.Builder().build().adapter(PacketLogInfoResponse::class.java)
            return adapter.toJson(packetLogInfoResponse)
        }

        fun getJapanesePlanName(plan: String): String {
            return when (plan) {
                "Family Share" -> "ファミリーシェアプラン"
                "Minimum Start" -> "ミニマムスタートプラン"
                "Light Start" -> "ライトスタートプラン"
                "Eco Minimum" -> "エコプランミニマム"
                "Eco Standard" -> "エコプランスタンダード"
                "Pay as you go" -> "プリペイドパック"
                else -> ""
            }
        }
    }

    private lateinit var loginContinuation: Continuation<Boolean>

    private val activityResultLauncher =
        fragmentActivity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult: ActivityResult ->
            loginContinuation.resume(activityResult.resultCode == Activity.RESULT_OK)
        }

    suspend fun login() = suspendCoroutine<Boolean> {
        loginContinuation = it
        val intent = Intent(fragmentActivity, MioLoginActivity::class.java)
        activityResultLauncher.launch(intent)
    }

    fun saveToken(token: String) {
        val preference = fragmentActivity.getSharedPreferences(
            fragmentActivity.getString(R.string.preference_file_name),
            Context.MODE_PRIVATE
        )
        val editor = preference.edit()
        editor.putString(
            fragmentActivity.getString(R.string.preference_key_token),
            token
        )
        editor.apply()
    }

    private fun loadToken(): String {
        val preference = fragmentActivity.getSharedPreferences(
            fragmentActivity.getString(R.string.preference_file_name),
            Context.MODE_PRIVATE
        )
        return preference.getString(fragmentActivity.getString(R.string.preference_key_token), "")
            ?: ""
    }

    fun deleteToken() {
        val preference = fragmentActivity.getSharedPreferences(
            fragmentActivity.getString(R.string.preference_file_name),
            Context.MODE_PRIVATE
        )
        val editor = preference.edit()
        editor.remove(fragmentActivity.getString(R.string.preference_key_token))
        editor.apply()
    }

    suspend fun getCouponInfo(): CouponInfoResponseWithHttpResponseCode {
        val (response, httpStatusCode) = getRequestIijmio("https://api.iijmio.jp/mobile/d/v2/coupon/")

        val couponInfoResponse: CouponInfoResponse? = response?.let {
            parseJsonToCoupon(it)
        }
        return CouponInfoResponseWithHttpResponseCode(couponInfoResponse, httpStatusCode)
    }

    suspend fun getUsageInfo(): PacketLogInfoResponseWithHttpResponseCode {
        val (response, httpStatusCode) = getRequestIijmio("https://api.iijmio.jp/mobile/d/v2/log/packet/")

        val packetLogInfoResponse: PacketLogInfoResponse? = response?.let {
            parseJsonToPacketLog(it)
        }
        return PacketLogInfoResponseWithHttpResponseCode(packetLogInfoResponse, httpStatusCode)
    }

    private suspend fun getRequestIijmio(url: String): Pair<String?, Int> =
        withContext(Dispatchers.IO) {
            val (_, response, result) = url.httpGet()
                .header("X-IIJmio-Developer", fragmentActivity.getString(R.string.developer_id))
                .header("X-IIJmio-Authorization", loadToken())
                .responseString()

            when (result) {
                is Result.Failure -> {
                    val statusCode = result.error.response.statusCode
                    Log.d("HTTP GET Response", "Failed: $statusCode")
                    Pair(null, statusCode)
                }
                is Result.Success -> {
                    val responseBody: String = result.get()
                    Log.d("HTTP GET Response", "Success: $responseBody")
                    val httpStatusCode: Int = response.statusCode
                    Pair(responseBody, httpStatusCode)
                }
            }
        }

    suspend fun applyCouponSetting(couponStatusMap: Map<String, Boolean>): Int {
        val hdoList = ArrayList<CouponSetting>()
        val hduList = ArrayList<CouponSetting>()
        val hdxList = ArrayList<CouponSetting>()
        for ((serviceCode, status) in couponStatusMap) {
            with(serviceCode) {
                when {
                    contains("hdo") -> hdoList.add(CouponSetting(serviceCode, status))
                    contains("hdu") -> hduList.add(CouponSetting(serviceCode, status))
                    contains("hdx") -> hdxList.add(CouponSetting(serviceCode, status))
                    else -> {}
                }
            }
        }
        val jsonString: String = getJsonStringForApplyCouponSetting(hdoList, hduList, hdxList)

        return putRequestIijmio("https://api.iijmio.jp/mobile/d/v2/coupon/", jsonString)
    }

    private suspend fun putRequestIijmio(url: String, requestJsonBody: String): Int =
        withContext(Dispatchers.IO) {
            val (_, response, result) = url.httpPut()
                .header(Headers.CONTENT_TYPE, "application/json")
                .header("X-IIJmio-Developer", fragmentActivity.getString(R.string.developer_id))
                .header("X-IIJmio-Authorization", loadToken())
                .jsonBody(requestJsonBody)
                .responseString()

            when (result) {
                is Result.Failure -> {
                    result.error.response.statusCode
                }
                is Result.Success -> {
                    response.statusCode
                }
            }
        }

    private data class CouponSetting(
        val hdxServiceCode: String,
        val coupon: Boolean
    )

    private fun getJsonStringForApplyCouponSetting(
        hdoList: List<CouponSetting>,
        hduList: List<CouponSetting>,
        hdxList: List<CouponSetting>
    ): String {
        var hdoStr = """"hdoInfo":["""
        var hduStr = """"hduInfo":["""
        var hdxStr = """"hdxInfo":["""

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
            val str = """{"hduServiceCode":"$hdu","couponUse":$on}"""

            hduStr += str

            if (index < hduList.size - 1) {
                hduStr += ",\n"
            }
        }
        hduStr += " ]"

        return """
            {
                "couponInfo":[
                    {
                        ${getHdzInfoJsonString("hdo", hdoList)},
                        ${getHdzInfoJsonString("hdu", hduList)},
                        ${getHdzInfoJsonString("hdx", hdxList)}
                    }
                ]
            }
            """
    }

    private fun getHdzInfoJsonString(hdz: String, hdzList: List<CouponSetting>): String {
        var hdzStr = """"${hdz}Info":["""

        for ((index, couponStatus) in hdzList.withIndex()) {
            val hdo = couponStatus.hdxServiceCode
            val on = couponStatus.coupon
            val str = """{"${hdz}ServiceCode":"$hdo","couponUse":$on}"""

            hdzStr += str

            if (index < hdzList.size - 1) {
                hdzStr += ", "
            }
        }
        hdzStr += " ]"

        return hdzStr
    }

    fun cacheJsonString(jsonString: String, jsonDataType: String) {
        val preference = fragmentActivity.getSharedPreferences(
            fragmentActivity.getString(R.string.preference_file_name),
            Context.MODE_PRIVATE
        )
        Log.d("Cache JSON", jsonString)
        val editor = preference.edit()
        editor.putString(jsonDataType, jsonString)
        editor.apply()
    }

    fun loadCachedJsonString(key: String): String {
        val preference = fragmentActivity.getSharedPreferences(
            fragmentActivity.getString(R.string.preference_file_name),
            Context.MODE_PRIVATE
        )
        return preference.getString(key, "{}") ?: "{}"
    }
}
