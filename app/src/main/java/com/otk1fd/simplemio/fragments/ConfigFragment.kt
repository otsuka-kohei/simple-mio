package com.otk1fd.simplemio.fragments

import android.os.Bundle
import android.preference.PreferenceFragment
import com.otk1fd.simplemio.R


/**
 * Created by otk1fd on 2018/02/24.
 */
class ConfigFragment : PreferenceFragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        preferenceManager.sharedPreferencesName = activity.getString(R.string.preference_file_name)

        // /app/res/xml/preference.xml に定義されている設定画面を適用
        addPreferencesFromResource(R.xml.preference)
    }
}