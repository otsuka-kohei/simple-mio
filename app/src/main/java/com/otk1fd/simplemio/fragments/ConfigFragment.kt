package com.otk1fd.simplemio.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.SwitchPreference
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import androidx.core.content.edit
import com.otk1fd.simplemio.R
import com.otk1fd.simplemio.activities.MainActivity


/**
 * Created by otk1fd on 2018/02/24.
 */
class ConfigFragment : PreferenceFragment(), Preference.OnPreferenceChangeListener {

    private val PERMISSIONS_REQUEST_READ_PHONE_STATE = 12345
    private lateinit var showPhoneNumberKey: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferenceManager.sharedPreferencesName = activity.getString(R.string.preference_file_name)
        // /app/res/xml/preference.xml に定義されている設定画面を適用
        addPreferencesFromResource(R.xml.preference)

        showPhoneNumberKey = getString(R.string.preference_key_show_phone_number)

        findPreference(showPhoneNumberKey).onPreferenceChangeListener = this
    }

    override fun onResume() {
        super.onResume()

        if (preferenceManager.sharedPreferences.getBoolean(showPhoneNumberKey, false)) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                preferenceManager.sharedPreferences.edit { putBoolean(showPhoneNumberKey, false) }
                (findPreference(showPhoneNumberKey) as SwitchPreference).isChecked = false
            }
        }

        (activity as MainActivity).updatePhoneNumberOnNavigationHeader()
    }


    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        if (preference?.key == showPhoneNumberKey) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.READ_PHONE_STATE), PERMISSIONS_REQUEST_READ_PHONE_STATE)
            } else {
                (activity as MainActivity).updatePhoneNumberOnNavigationHeader(usePreference = false, showPhoneNumberParameter = newValue as Boolean)
            }
        }

        return true
    }
}