package com.otk1fd.simplemio.dialog

import java.io.Serializable

data class ProgressDialogFragmentData(
    val title: String = "",
    val message: String = ""
) : Serializable
