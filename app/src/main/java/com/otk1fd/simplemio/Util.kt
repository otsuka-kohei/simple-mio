package com.otk1fd.simplemio

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.util.Log
import androidx.appcompat.app.AlertDialog
import org.xmlpull.v1.XmlPullParser


/**
 * Created by otk1fd on 2018/03/14.
 */
object Util {

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

    /*
    /app/src/main/res/xml/developper_id.xml にデベロッパIDを設定します．

    developper_id.xml :
    <?xml version="1.0" encoding="utf-8"?>
    <resources>
        <string name="developer_id">dyyfUo0KRtQoAqWML3Y</string>
    </resources>
     */
    fun getDeveloperId(activity: Activity): String {
        val resources: Resources = activity.applicationContext.resources
        val xmlResourceParser: XmlResourceParser = resources.getXml(R.xml.developper_id)

        var eventType = xmlResourceParser.eventType
        var inDeveloperId = false
        while (eventType != XmlResourceParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                if (xmlResourceParser.name == "string" && xmlResourceParser.getAttributeValue(null, "name") == "developer_id") {
                    Log.d("XML", "enter to string tag")
                    inDeveloperId = true
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (xmlResourceParser.name == "string") {
                    Log.d("XML", "exit from string tag")
                    inDeveloperId = false
                }
            } else if (eventType == XmlPullParser.TEXT) {
                if (inDeveloperId) {
                    return xmlResourceParser.text
                }
            }

            eventType = xmlResourceParser.next()
        }

        Log.d("real id from xml", "none")

        return ""
    }

    fun saveSimName(activity: Activity, serviceCode: String, simName: String) {
        val preference = activity.applicationContext.getSharedPreferences(activity.applicationContext.getString(R.string.sim_name_preference_file_name), Context.MODE_PRIVATE)
        val editor = preference.edit()
        editor.putString(serviceCode, simName)
        editor.apply()
    }

    fun loadSimName(activity: Activity, serviceCode: String): String {
        val preference = activity.applicationContext.getSharedPreferences(activity.getString(R.string.sim_name_preference_file_name), Context.MODE_PRIVATE)
        return preference.getString(serviceCode, "")
    }
}