package com.sunflower.shortcut.automation.runtime

data class RunResult(
    val success: Boolean,
    val resultJson: String? = null,
    val errorMessage: String? = null
)
