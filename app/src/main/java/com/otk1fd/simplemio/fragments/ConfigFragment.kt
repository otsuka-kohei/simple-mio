package com.otk1fd.simplemio.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.otk1fd.simplemio.R
import com.otk1fd.simplemio.Util
import com.otk1fd.simplemio.activities.MainActivity
import com.otk1fd.simplemio.mio.MioUtil


/**
 * Created by otk1fd on 2018/02/24.
 */
class ConfigFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {

    private val PERMISSIONS_REQUEST_READ_PHONE_STATE = 12345
    private lateinit var showPhoneNumberKey: String

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = activity!!.getString(R.string.preference_file_name)
        // /app/res/xml/preference.xml に定義されている設定画面を適用
        addPreferencesFromResource(R.xml.preference)

        showPhoneNumberKey = getString(R.string.preference_key_show_phone_number)

        (findPreference(showPhoneNumberKey) as SwitchPreference?)?.onPreferenceChangeListener = this

        val logoutButtonKey = getString(R.string.preference_key_logout)
        val logoutButton = findPreference(logoutButtonKey) as Preference?
        logoutButton?.setOnPreferenceClickListener {
            Util.showAlertDialog(activity!!, "ログアウト", "IIJmioからログアウトしてもよろしいですか？",
                    "はい", negativeButtonText = "いいえ",
                    positiveFunc = {
                        MioUtil.deleteToken(activity!!)
                        Util.showAlertDialog(activity!!, "ログアウト完了", "IIJmioからログアウトしました．\nアプリを終了します",
                                "はい",
                                positiveFunc = {
                                    activity!!.finish()
                                })
                    })
            return@setOnPreferenceClickListener true
        }
    }

    override fun onResume() {
        super.onResume()

        if (preferenceManager.sharedPreferences.getBoolean(showPhoneNumberKey, false)) {
            if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                preferenceManager.sharedPreferences.edit { putBoolean(showPhoneNumberKey, false) }
                (findPreference(showPhoneNumberKey) as SwitchPreference?)?.isChecked = false
            }
        }

        (activity!! as MainActivity).updatePhoneNumberOnNavigationHeader()
    }


    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        if (preference?.key == showPhoneNumberKey) {
            if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity!!, arrayOf(Manifest.permission.READ_PHONE_STATE), PERMISSIONS_REQUEST_READ_PHONE_STATE)
            } else {
                (activity!! as MainActivity).updatePhoneNumberOnNavigationHeader(usePreference = false, showPhoneNumberParameter = newValue as Boolean)
            }
        }

        return true
    }
}