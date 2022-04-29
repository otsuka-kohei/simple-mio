package com.otk1fd.simplemio

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AlertDialog


/**
 * Created by otk1fd on 2018/03/14.
 */
object Util {

    fun showAlertDialog(
        activity: Activity,
        title: String,
        message: String,
        positiveButtonText: String = "",
        neutralButtonText: String = "",
        negativeButtonText: String = "",
        positiveFunc: () -> Unit = {},
        neutralFunc: () -> Unit = {},
        negativeFunc: () -> Unit = {}
    ) {
        if (positiveButtonText == "" && negativeButtonText == "" && negativeButtonText == "") {
            return
        }

        val alertDialog = AlertDialog.Builder(activity)
        alertDialog.setTitle(title)
        alertDialog.setMessage(message)
        if (positiveButtonText != "") alertDialog.setPositiveButton(
            positiveButtonText,
            { dialog, which -> positiveFunc() })
        if (neutralButtonText != "") alertDialog.setNeutralButton(
            neutralButtonText,
            { dialog, which -> neutralFunc() })
        if (negativeButtonText != "") alertDialog.setNegativeButton(
            negativeButtonText,
            { dialog, which -> negativeFunc() })
        alertDialog.show()
    }


    fun saveSimName(activity: Activity, serviceCode: String, simName: String) {
        val preference = activity.applicationContext.getSharedPreferences(
            activity.applicationContext.getString(R.string.sim_name_preference_file_name),
            Context.MODE_PRIVATE
        )
        val editor = preference.edit()
        editor.putString(serviceCode, simName)
        editor.apply()
    }

    fun loadSimName(activity: Activity, serviceCode: String): String {
        val preference = activity.applicationContext.getSharedPreferences(
            activity.getString(R.string.sim_name_preference_file_name),
            Context.MODE_PRIVATE
        )
        return preference.getString(serviceCode, "") ?: ""
    }
}