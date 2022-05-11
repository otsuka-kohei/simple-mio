package com.otk1fd.simplemio.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.otk1fd.simplemio.databinding.FragmentProgressDialogBinding

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

        val binding: FragmentProgressDialogBinding =
            FragmentProgressDialogBinding.inflate(requireActivity().layoutInflater)

        return AlertDialog.Builder(requireActivity()).apply {
            if (progressDialogFragmentData.title.isNotEmpty()) {
                setTitle(progressDialogFragmentData.title)
            }

            if (progressDialogFragmentData.message.isNotEmpty()) {
                binding.progressMessageTextView.text = progressDialogFragmentData.message
            } else {
                binding.progressMessageTextView.visibility = View.GONE
            }

            setView(binding.root)
        }.create()
    }
}