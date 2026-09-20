package com.sunflower.shortcut.javascript.parser

/**
 * Thrown by JsLexer/JsParser on any malformed input. [line] (1-based) is
 * what ImportJsScreen/WriteCodeScreen display as "Строка N:" (spec §8) —
 * null only if a position genuinely couldn't be determined.
 */
class JsSyntaxException(
    message: String,
    val line: Int? = null,
    val column: Int? = null
) : Exception(message)
