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
import com.otk1fd.simplemio.activities.MainActivity
import com.otk1fd.simplemio.dialog.AlertDialogFragment
import com.otk1fd.simplemio.dialog.AlertDialogFragmentData
import com.otk1fd.simplemio.mio.Mio


/**
 * Created by otk1fd on 2018/02/24.
 */
class ConfigFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {

    companion object {
        private const val PERMISSIONS_REQUEST_READ_PHONE_STATE: Int = 12345
    }

    private lateinit var showPhoneNumberKey: String

    private lateinit var mio: Mio

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        mio = (requireActivity() as MainActivity).mio

        preferenceManager.sharedPreferencesName =
            requireActivity().getString(R.string.preference_file_name)
        // /app/res/xml/preference.xml に定義されている設定画面を適用
        addPreferencesFromResource(R.xml.preference)

        showPhoneNumberKey = getString(R.string.preference_key_show_phone_number)

        (findPreference(showPhoneNumberKey) as SwitchPreference?)?.onPreferenceChangeListener = this

        val logoutButtonKey = getString(R.string.preference_key_logout)
        val logoutButton = findPreference(logoutButtonKey) as Preference?
        logoutButton?.setOnPreferenceClickListener {
            val alertDialogFragmentDataForLogoutConfirmation = AlertDialogFragmentData(
                title = "ログアウト",
                message = "IIJmioからログアウトしますか？",
                positiveButtonText = "はい",
                positiveButtonFunc = { fragmentActivityForLogoutConfirmation ->
                    mio.deleteToken()

                    val alertDialogFragmentDataForLogoutMessage = AlertDialogFragmentData(
                        title = "ログアウト完了",
                        message = "IIJmioからログアウトしました。\nアプリを終了します。",
                        positiveButtonText = "はい",
                        positiveButtonFunc = { fragmentActivityForLogoutMessage ->
                            fragmentActivityForLogoutMessage.finish()
                        }
                    )
                    AlertDialogFragment.show(
                        fragmentActivityForLogoutConfirmation,
                        alertDialogFragmentDataForLogoutMessage
                    )
                },
                negativeButtonText = "いいえ"
            )
            AlertDialogFragment.show(
                requireActivity(),
                alertDialogFragmentDataForLogoutConfirmation
            )

            return@setOnPreferenceClickListener true
        }
    }

    override fun onResume() {
        super.onResume()

        if (preferenceManager.sharedPreferences?.getBoolean(showPhoneNumberKey, false) == true) {
            if (ContextCompat.checkSelfPermission(
                    requireActivity(),
                    Manifest.permission.READ_PHONE_STATE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                preferenceManager.sharedPreferences?.edit { putBoolean(showPhoneNumberKey, false) }
                (findPreference(showPhoneNumberKey) as SwitchPreference?)?.isChecked = false
            }
        }

        (requireActivity() as MainActivity).updatePhoneNumberOnNavigationHeader()
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        if (preference.key == showPhoneNumberKey) {
            if (ContextCompat.checkSelfPermission(
                    requireActivity(),
                    Manifest.permission.READ_PHONE_STATE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.READ_PHONE_STATE),
                    PERMISSIONS_REQUEST_READ_PHONE_STATE
                )
            } else {
                (requireActivity() as MainActivity).updatePhoneNumberOnNavigationHeader(
                    usePreference = false,
                    showPhoneNumberParameter = newValue as Boolean
                )
            }
        }

        return true
    }
}