package com.otk1fd.simplemio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat


/**
 * Created by otk1fd on 2018/03/14.
 */
object Util {
    fun isPhoneNumberPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_NUMBERS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

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