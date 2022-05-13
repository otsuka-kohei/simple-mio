package com.otk1fd.simplemio.fragments

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.otk1fd.simplemio.R
import com.otk1fd.simplemio.activities.FinishActivity
import com.otk1fd.simplemio.activities.MainActivity
import com.otk1fd.simplemio.dialog.AlertDialogFragment
import com.otk1fd.simplemio.dialog.AlertDialogFragmentData
import com.otk1fd.simplemio.mio.Mio


/**
 * Created by otk1fd on 2018/02/24.
 */
class ConfigFragment : PreferenceFragmentCompat() {

    private lateinit var mio: Mio

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        mio = (requireActivity() as MainActivity).mio

        preferenceManager.sharedPreferencesName =
            requireActivity().getString(R.string.preference_file_name)
        // /app/res/xml/preference.xml に定義されている設定画面を適用
        addPreferencesFromResource(R.xml.preference)

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
                            FinishActivity.finishApplication(fragmentActivityForLogoutMessage)
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
}