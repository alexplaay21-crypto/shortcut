package com.sunflower.shortcut.javascript.validator

class ScenarioValidationException(
    message: String,
    val line: Int? = null,
    val column: Int? = null
) : Exception(message)
