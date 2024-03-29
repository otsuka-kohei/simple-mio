package com.otk1fd.simplemio.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity

class AlertDialogFragment private constructor() : DialogFragment() {
    companion object {
        private const val ARGUMENT_KEY = "alertDialogFragmentData";
        private const val FRAGMENT_KEY = "alertDialogFragment";

        fun show(
            fragmentActivity: FragmentActivity,
            alertDialogFragmentData: AlertDialogFragmentData
        ): AlertDialogFragment {
            val alertDialogFragment = AlertDialogFragment()
            val bundle = Bundle()
            bundle.putSerializable(ARGUMENT_KEY, alertDialogFragmentData)
            alertDialogFragment.arguments = bundle
            alertDialogFragment.show(fragmentActivity.supportFragmentManager, FRAGMENT_KEY)
            return alertDialogFragment
        }
    }

    private lateinit var alertDialogFragmentData: AlertDialogFragmentData

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        alertDialogFragmentData =
            arguments?.let { it.getSerializable(ARGUMENT_KEY) as AlertDialogFragmentData }
                ?: AlertDialogFragmentData()

        return AlertDialog.Builder(requireActivity()).apply {
            if (alertDialogFragmentData.title.isNotEmpty()) {
                setTitle(alertDialogFragmentData.title)
            }

            if (alertDialogFragmentData.message.isNotEmpty()) {
                setMessage(alertDialogFragmentData.message)
            }

            if (alertDialogFragmentData.positiveButtonText.isNotEmpty()) {
                setPositiveButton(
                    alertDialogFragmentData.positiveButtonText
                ) { _, _ ->
                    alertDialogFragmentData.positiveButtonFunc(requireActivity())
                }
            }

            if (alertDialogFragmentData.neutralButtonText.isNotEmpty()) {
                setNeutralButton(
                    alertDialogFragmentData.neutralButtonText
                ) { _, _ ->
                    alertDialogFragmentData.neutralButtonFunc(requireActivity())
                }
            }

            if (alertDialogFragmentData.negativeButtonText.isNotEmpty()) {
                setNegativeButton(
                    alertDialogFragmentData.negativeButtonText
                ) { _, _ ->
                    alertDialogFragmentData.negativeButtonFunc(requireActivity())
                }
            }
        }.create()
    }
}