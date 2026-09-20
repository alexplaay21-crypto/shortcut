package com.sunflower.shortcut.javascript.parser

enum class TokenType { IDENTIFIER, KEYWORD, NUMBER, STRING, TEMPLATE, PUNCTUATOR, EOF }

val JS_KEYWORDS = setOf(
    "var", "let", "const", "function", "return", "if", "else", "for", "of", "while", "do",
    "break", "continue", "try", "catch", "finally", "throw", "new", "typeof", "instanceof",
    "in", "true", "false", "null", "undefined", "this", "async", "await", "void", "delete"
)

/**
 * One lexical token. [templateQuasis]/[templateExprSources] are only set for
 * TEMPLATE tokens — the lexer already splits a template literal's text
 * segments from its `${...}` expression source spans (see JsLexer.readTemplate);
 * JsParser re-parses each expression span on demand.
 */
data class Token(
    val type: TokenType,
    val value: String,
    val line: Int,
    val column: Int,
    val templateQuasis: List<String>? = null,
    val templateExprSources: List<String>? = null
)
