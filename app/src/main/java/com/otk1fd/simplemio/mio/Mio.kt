package com.otk1fd.simplemio.mio

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
            if (plan == "Family Share") return "ファミリーシェアプラン"
            if (plan == "Minimum Start") return "ミニマムスタートプラン"
            if (plan == "Light Start") return "ライトスタートプラン"
            if (plan == "Eco Minimum") return "エコプランミニマム"
            if (plan == "Eco Standard") return "エコプランスタンダード"
            return ""
        }
    }

    private lateinit var loginContinuation: Continuation<Boolean>

    private val activityResultLauncher =
        fragmentActivity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult: ActivityResult ->
            activityResult.data?.let { intent: Intent ->
                intent.getStringExtra(MioLoginActivity.TOKEN_KEY)?.let { token: String ->
                    Log.d("hoge","token: $token")
                    saveToken(token)
                    loginContinuation.resume(true)
                }
            }?:let{
                Log.d("hoge","no token")
                loginContinuation.resume(false)
            }
        }

    suspend fun login() = suspendCoroutine<Boolean> { continuation ->
        Log.d("hoge","start login")
        loginContinuation = continuation
        val intent = Intent(fragmentActivity, MioLoginActivity::class.java)
        intent.putExtra(MioLoginActivity.LOGIN_FLAG_KEY, true)
        activityResultLauncher.launch(intent)
    }

    private fun saveToken(token: String) {
        val preference = fragmentActivity.applicationContext.getSharedPreferences(
            fragmentActivity.applicationContext.getString(R.string.preference_file_name),
            Context.MODE_PRIVATE
        )
        val editor = preference.edit()
        editor.putString(
            fragmentActivity.applicationContext.getString(R.string.preference_key_token),
            token
        )
        editor.apply()
    }

    private fun loadToken(): String {
        val preference = fragmentActivity.applicationContext.getSharedPreferences(
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
                    Pair(null, result.error.response.statusCode)
                }
                is Result.Success -> {
                    val responseBody: String = result.get()
                    val httpStatusCode: Int = response.statusCode
                    Pair(responseBody, httpStatusCode)
                }
            }
        }

    suspend fun applyCouponSetting(couponStatusMap: Map<String, Boolean>): Int {
        val hdoList = ArrayList<CouponSetting>()
        val hduList = ArrayList<CouponSetting>()
        for ((serviceCode, status) in couponStatusMap) {
            if (serviceCode.contains("hdo")) {
                hdoList.add(CouponSetting(serviceCode, status))
            } else if (serviceCode.contains("hdu")) {
                hduList.add(CouponSetting(serviceCode, status))
            }
        }
        val jsonString: String = getJsonStringForApplyCouponSetting(hdoList, hduList)

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
        hduList: List<CouponSetting>
    ): String {
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

        return """{"couponInfo":[{$hdoStr,$hduStr}]}"""
    }

    fun cacheJsonString(jsonString: String, jsonDataType: String) {
        val preference = fragmentActivity.applicationContext.getSharedPreferences(
            fragmentActivity.applicationContext.getString(R.string.preference_file_name),
            Context.MODE_PRIVATE
        )
        val editor = preference.edit()
        editor.putString(jsonDataType, jsonString)
        editor.apply()
    }

    fun loadCachedJsonString(key: String): String {
        val preference = fragmentActivity.applicationContext.getSharedPreferences(
            fragmentActivity.applicationContext.getString(R.string.preference_file_name),
            Context.MODE_PRIVATE
        )
        return preference.getString(key, "{}") ?: "{}"
    }
}
