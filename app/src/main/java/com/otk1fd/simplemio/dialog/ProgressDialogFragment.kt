package com.otk1fd.simplemio.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.otk1fd.simplemio.R

class ProgressDialogFragment private constructor() : DialogFragment() {
    companion object {
        private const val ARGUMENT_KEY = "alertDialogFragmentData";
        private const val FRAGMENT_KEY = "alertDialogFragment";

        fun show(
            fragmentActivity: FragmentActivity,
            progressDialogFragmentData: ProgressDialogFragmentData
        ): ProgressDialogFragment {
            val progressDialogFragment = ProgressDialogFragment()
            val bundle = Bundle()
            bundle.putSerializable(ARGUMENT_KEY, progressDialogFragmentData)
            progressDialogFragment.arguments = bundle
            progressDialogFragment.show(fragmentActivity.supportFragmentManager, FRAGMENT_KEY)
            return progressDialogFragment
        }
    }

    private lateinit var progressDialogFragmentData: ProgressDialogFragmentData

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        progressDialogFragmentData =
            arguments?.let { it.getSerializable(ARGUMENT_KEY) as ProgressDialogFragmentData }
                ?: ProgressDialogFragmentData()

        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.fragment_progress_dialog, null)

        return AlertDialog.Builder(requireActivity()).apply {
            if (progressDialogFragmentData.title.isNotEmpty()) {
                setTitle(progressDialogFragmentData.title)
            }

            val progressDialogTextView: TextView = view.findViewById(R.id.progressMessageTextView)
            if (progressDialogFragmentData.message.isNotEmpty()) {
                progressDialogTextView.text = progressDialogFragmentData.message
            } else {
                progressDialogTextView.visibility = View.GONE
            }

            setView(view)
        }.create()
    }
}