package com.otk1fd.simplemio.dialog

import androidx.fragment.app.FragmentActivity
import java.io.Serializable

data class EditTextDialogFragmentData(
    val title: String = "",
    val message: String = "",
    val positiveButtonText: String = "",
    val positiveButtonFunc: (fragmentActivity: FragmentActivity, text: String) -> Unit = { _: FragmentActivity, _: String -> },
    val neutralButtonText: String = "",
    val neutralButtonFunc: (fragmentActivity: FragmentActivity, text: String) -> Unit = { _: FragmentActivity, _: String -> },
    val negativeButtonText: String = "",
    val negativeButtonFunc: (fragmentActivity: FragmentActivity, text: String) -> Unit = { _: FragmentActivity, _: String -> },
    val defaultText: String = "",
    val hint: String = ""
) : Serializable
