package com.otsuka.simplemio.fragments

import android.os.Bundle
import android.preference.PreferenceFragment
import com.otsuka.simplemio.R


/**
 * Created by otsuka on 2018/02/24.
 */
class ConfigFragment : PreferenceFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // /app/res/xml/preference.xml に定義されている設定画面を適用
        addPreferencesFromResource(R.xml.preference)
    }


}