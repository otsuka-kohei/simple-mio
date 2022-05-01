package com.otk1fd.simplemio.dialog

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity

class EditTextDialogFragment private constructor() : DialogFragment() {
    companion object {
        private const val ARGUMENT_KEY = "editTextDialogFragmentData";
        private const val FRAGMENT_KEY = "editTextDialogFragment";

        fun show(
            fragmentActivity: FragmentActivity,
            editTextDialogFragmentData: EditTextDialogFragmentData
        ): EditTextDialogFragment {
            val editTextDialogFragment = EditTextDialogFragment()
            val bundle = Bundle()
            bundle.putSerializable(ARGUMENT_KEY, editTextDialogFragmentData)
            editTextDialogFragment.arguments = bundle
            editTextDialogFragment.show(fragmentActivity.supportFragmentManager, FRAGMENT_KEY)
            return editTextDialogFragment
        }
    }

    private lateinit var editTextDialogFragmentData: EditTextDialogFragmentData

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        editTextDialogFragmentData =
            arguments?.let { it.getSerializable(ARGUMENT_KEY) as EditTextDialogFragmentData }
                ?: EditTextDialogFragmentData()

        val editText = EditText(requireActivity())
        editText.setText(editTextDialogFragmentData.defaultText, TextView.BufferType.NORMAL)
        editText.setSelection(editText.text.length)
        editText.hint = editTextDialogFragmentData.hint

        return AlertDialog.Builder(requireActivity()).apply {
            if (editTextDialogFragmentData.title.isNotEmpty()) {
                setTitle(editTextDialogFragmentData.title)
            }

            if (editTextDialogFragmentData.message.isNotEmpty()) {
                setMessage(editTextDialogFragmentData.message)
            }

            if (editTextDialogFragmentData.positiveButtonText.isNotEmpty()) {
                setPositiveButton(
                    editTextDialogFragmentData.positiveButtonText
                ) { _, _ ->
                    val text: String = editText.text.toString()
                    editTextDialogFragmentData.positiveButtonFunc(requireActivity(), text)
                }
            }

            if (editTextDialogFragmentData.neutralButtonText.isNotEmpty()) {
                setNeutralButton(
                    editTextDialogFragmentData.neutralButtonText
                ) { _, _ ->
                    val text: String = editText.text.toString()
                    editTextDialogFragmentData.neutralButtonFunc(requireActivity(), text)
                }
            }

            if (editTextDialogFragmentData.negativeButtonText.isNotEmpty()) {
                setNegativeButton(
                    editTextDialogFragmentData.negativeButtonText
                ) { _, _ ->
                    val text: String = editText.text.toString()
                    editTextDialogFragmentData.negativeButtonFunc(requireActivity(), text)
                }
            }

            setView(editText)
        }.create()
    }
}