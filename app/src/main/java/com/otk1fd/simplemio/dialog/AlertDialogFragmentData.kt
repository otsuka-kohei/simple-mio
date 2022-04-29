package com.otk1fd.simplemio.dialog

import androidx.fragment.app.FragmentActivity
import java.io.Serializable

data class AlertDialogFragmentData(
    val title: String = "",
    val message: String = "",
    val positiveButtonText: String = "",
    val positiveButtonFunc: (fragmentActivity: FragmentActivity) -> Unit = {},
    val neutralButtonText: String = "",
    val neutralButtonFunc: (fragmentActivity: FragmentActivity) -> Unit = {},
    val negativeButtonText: String = "",
    val negativeButtonFunc: (fragmentActivity: FragmentActivity) -> Unit = {}
) : Serializable
