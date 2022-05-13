package com.otk1fd.simplemio

import android.content.Context


/**
 * Created by otk1fd on 2018/03/14.
 */
object Util {
    fun saveSimName(context: Context, serviceCode: String, simName: String) {
        val preference = context.getSharedPreferences(
            context.getString(R.string.sim_name_preference_file_name),
            Context.MODE_PRIVATE
        )
        val editor = preference.edit()
        editor.putString(serviceCode, simName)
        editor.apply()
    }

    fun loadSimName(context: Context, serviceCode: String): String {
        val preference = context.getSharedPreferences(
            context.getString(R.string.sim_name_preference_file_name),
            Context.MODE_PRIVATE
        )
        return preference.getString(serviceCode, "") ?: ""
    }
}