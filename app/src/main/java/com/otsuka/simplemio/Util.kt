package com.otsuka.simplemio

import android.app.Activity
import android.support.v7.app.AlertDialog


/**
 * Created by otsuka on 2018/03/14.
 */
class Util {

    companion object {
        fun showAlertDialog(activity: Activity, title: String, message: String,
                            positiveButtonText: String = "", neutralButtonText: String = "", negativeButtonText: String = "",
                            positiveFunc: () -> Unit = {}, neutralFunc: () -> Unit = {}, negativeFunc: () -> Unit = {}) {
            if (positiveButtonText == "" && negativeButtonText == "" && negativeButtonText == "") {
                return
            }

            val alertDialog = AlertDialog.Builder(activity)
            alertDialog.setTitle(title)
            alertDialog.setMessage(message)
            if (positiveButtonText != "") alertDialog.setPositiveButton(positiveButtonText, { dialog, which -> positiveFunc() })
            if (neutralButtonText != "") alertDialog.setNeutralButton(neutralButtonText, { dialog, which -> neutralFunc() })
            if (negativeButtonText != "") alertDialog.setNegativeButton(negativeButtonText, { dialog, which -> negativeFunc() })
            alertDialog.show()
        }
    }
}