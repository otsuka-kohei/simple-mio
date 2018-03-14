package com.otsuka.simplemio

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v4.content.ContextCompat.startActivity


/**
 * Created by otsuka on 2018/03/14.
 */
class MioUtil {

    companion object {
        fun saveToken(activity: Activity, token: String) {
            val preference = activity.getSharedPreferences(activity.getString(R.string.preferenceFileName), Context.MODE_PRIVATE)
            val editor = preference.edit()
            editor.putString("token", token)
            editor.apply()
        }

        fun loadToken(activity: Activity): String {
            val preference = activity.getSharedPreferences(activity.getString(R.string.preferenceFileName), Context.MODE_PRIVATE)
            preference.getString("token", "")
        }

        fun deleteToken(activity: Activity) {
            val preference = activity.getSharedPreferences(activity.getString(R.string.preferenceFileName), Context.MODE_PRIVATE)
            val editor = preference.edit()
            editor.remove("token")
            editor.apply()
        }

        fun checkTokenAvailable(token: String): Boolean {
            return true
        }
    }
}