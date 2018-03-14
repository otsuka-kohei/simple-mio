package com.otsuka.simplemio

import android.app.Activity
import android.content.Context


/**
 * Created by otsuka on 2018/03/14.
 */
class MioUtil {

    companion object {
        fun saveToken(activity: Activity, token: String) {
            val preference = activity.getSharedPreferences(activity.getString(R.string.preference_file_name), Context.MODE_PRIVATE)
            val editor = preference.edit()
            editor.putString("token", token)
            editor.apply()
        }

        fun loadToken(activity: Activity): String {
            val preference = activity.getSharedPreferences(activity.getString(R.string.preference_file_name), Context.MODE_PRIVATE)
            return preference.getString("token", "")
        }

        fun deleteToken(activity: Activity) {
            val preference = activity.getSharedPreferences(activity.getString(R.string.preference_file_name), Context.MODE_PRIVATE)
            val editor = preference.edit()
            editor.remove("token")
            editor.apply()
        }

        fun checkTokenAvailable(token: String): Boolean {
            return true
        }
    }
}