package com.sunflower.shortcut.javascript.parser

private fun Char.isJsIdentifierStart(): Boolean = isLetter() || this == '_' || this == '$'
private fun Char.isJsIdentifierPart(): Boolean = isLetterOrDigit() || this == '_' || this == '$'

/**
 * Converts scenario source text into a flat token list ending in EOF. Not a
 * full ECMAScript lexer — see the limitations noted on [Expression] — but
 * everything it does accept, it tokenizes correctly, including nested
 * `${...}` template expressions and unicode escapes.
 */
class JsLexer(private val source: String) {
    private var pos = 0
    private var line = 1
    private var column = 1
    private val length = source.length

    fun tokenize(): List<Token> {
        val tokens = mutableListOf<Token>()
        while (true) {
            skipWhitespaceAndComments()
            if (pos >= length) {
                tokens += Token(TokenType.EOF, "", line, column)
                break
            }
            tokens += nextToken()
        }
        return tokens
    }

    private fun peekChar(offset: Int = 0): Char = if (pos + offset < length) source[pos + offset] else '\u0000'

    private fun advance(): Char {
        val c = source[pos]
        pos++
        if (c == '\n') {
            line++
            column = 1
        } else {
            column++
        }
        return c
    }

    private fun skipWhitespaceAndComments() {
        while (pos < length) {
            val c = peekChar()
            when {
                c == ' ' || c == '\t' || c == '\r' || c == '\n' -> advance()
                c == '/' && peekChar(1) == '/' -> while (pos < length && peekChar() != '\n') advance()
                c == '/' && peekChar(1) == '*' -> {
                    advance(); advance()
                    while (pos < length && !(peekChar() == '*' && peekChar(1) == '/')) advance()
                    if (pos < length) { advance(); advance() }
                }
                else -> return
            }
        }
    }

    private fun nextToken(): Token {
        val startLine = line
        val startColumn = column
        val c = peekChar()
        return when {
            c.isDigit() || (c == '.' && peekChar(1).isDigit()) -> readNumber(startLine, startColumn)
            c == '"' || c == '\'' -> readString(c, startLine, startColumn)
            c == '`' -> readTemplate(startLine, startColumn)
            c.isJsIdentifierStart() -> readIdentifierOrKeyword(startLine, startColumn)
            else -> readPunctuator(startLine, startColumn)
        }
    }

    private fun readNumber(startLine: Int, startColumn: Int): Token {
        val start = pos
        if (peekChar() == '0' && (peekChar(1) == 'x' || peekChar(1) == 'X')) {
            advance(); advance()
            while (pos < length && (peekChar().isDigit() || peekChar() in 'a'..'f' || peekChar() in 'A'..'F')) advance()
            return Token(TokenType.NUMBER, source.substring(start, pos), startLine, startColumn)
        }
        while (pos < length && peekChar().isDigit()) advance()
        if (peekChar() == '.') {
            advance()
            while (pos < length && peekChar().isDigit()) advance()
        }
        if (peekChar() == 'e' || peekChar() == 'E') {
            advance()
            if (peekChar() == '+' || peekChar() == '-') advance()
            while (pos < length && peekChar().isDigit()) advance()
        }
        return Token(TokenType.NUMBER, source.substring(start, pos), startLine, startColumn)
    }

    private fun readEscapeSequence(): String {
        advance() // backslash
        if (pos >= length) throw JsSyntaxException("Незавершённая escape-последовательность", line, column)
        return when (val c = advance()) {
            'n' -> "\n"
            't' -> "\t"
            'r' -> "\r"
            'b' -> "\b"
            '0' -> "\u0000"
            '\\' -> "\\"
            '\'' -> "'"
            '"' -> "\""
            '`' -> "`"
            '$' -> "$"
            'u' -> readUnicodeEscape()
            else -> c.toString()
        }
    }

    private fun readUnicodeEscape(): String {
        if (peekChar() == '{') {
            advance()
            val sb = StringBuilder()
            while (pos < length && peekChar() != '}') sb.append(advance())
            if (pos < length) advance()
            return runCatching { String(Character.toChars(sb.toString().toInt(16))) }
                .getOrElse { throw JsSyntaxException("Некорректный \\u{...} escape", line, column) }
        }
        val sb = StringBuilder()
        repeat(4) { if (pos < length) sb.append(advance()) }
        return runCatching { String(Character.toChars(sb.toString().toInt(16))) }
            .getOrElse { throw JsSyntaxException("Некорректный \\u escape", line, column) }
    }

    private fun readString(quote: Char, startLine: Int, startColumn: Int): Token {
        advance() // opening quote
        val sb = StringBuilder()
        while (true) {
            if (pos >= length) throw JsSyntaxException("Незакрытая строка", startLine, startColumn)
            val c = peekChar()
            if (c == quote) { advance(); break }
            if (c == '\n') throw JsSyntaxException("Незакрытая строка", startLine, startColumn)
            sb.append(if (c == '\\') readEscapeSequence() else advance().toString())
        }
        return Token(TokenType.STRING, sb.toString(), startLine, startColumn)
    }

    private fun skipStringLiteralRaw(quote: Char) {
        advance()
        while (pos < length && peekChar() != quote) {
            if (peekChar() == '\\') advance()
            advance()
        }
        if (pos < length) advance()
    }

    private fun skipNestedTemplateRaw() {
        advance() // opening backtick
        while (pos < length && peekChar() != '`') {
            if (peekChar() == '\\') { advance(); if (pos < length) advance(); continue }
            if (peekChar() == '$' && peekChar(1) == '{') {
                advance(); advance()
                var depth = 1
                while (pos < length && depth > 0) {
                    when (peekChar()) {
                        '{' -> { depth++; advance() }
                        '}' -> { depth--; advance() }
                        '"', '\'' -> skipStringLiteralRaw(peekChar())
                        '`' -> skipNestedTemplateRaw()
                        else -> advance()
                    }
                }
                continue
            }
            advance()
        }
        if (pos < length) advance()
    }

    /** Splits ``` `text ${expr} more` ``` into quasis=["text ", " more"], exprSources=["expr"]. */
    private fun readTemplate(startLine: Int, startColumn: Int): Token {
        advance() // opening backtick
        val quasis = mutableListOf<String>()
        val exprSources = mutableListOf<String>()
        var current = StringBuilder()
        while (true) {
            if (pos >= length) throw JsSyntaxException("Незакрытый шаблонный литерал", startLine, startColumn)
            val c = peekChar()
            when {
                c == '`' -> { advance(); quasis += current.toString(); break }
                c == '\\' -> current.append(readEscapeSequence())
                c == '$' && peekChar(1) == '{' -> {
                    quasis += current.toString()
                    current = StringBuilder()
                    advance(); advance() // ${
                    val exprStart = pos
                    var depth = 1
                    while (pos < length && depth > 0) {
                        when (peekChar()) {
                            '{' -> { depth++; advance() }
                            '}' -> { depth--; if (depth > 0) advance() }
                            '"', '\'' -> skipStringLiteralRaw(peekChar())
                            '`' -> skipNestedTemplateRaw()
                            else -> advance()
                        }
                    }
                    if (pos >= length) throw JsSyntaxException("Незакрытое \${...} в шаблонной строке", startLine, startColumn)
                    exprSources += source.substring(exprStart, pos)
                    advance() // closing }
                }
                else -> { current.append(c); advance() }
            }
        }
        return Token(
            TokenType.TEMPLATE, "", startLine, startColumn,
            templateQuasis = quasis, templateExprSources = exprSources
        )
    }

    private fun readIdentifierOrKeyword(startLine: Int, startColumn: Int): Token {
        val start = pos
        while (pos < length && peekChar().isJsIdentifierPart()) advance()
        val text = source.substring(start, pos)
        val type = if (text in JS_KEYWORDS) TokenType.KEYWORD else TokenType.IDENTIFIER
        return Token(type, text, startLine, startColumn)
    }

    private val threeCharOps = setOf("===", "!==", "...")
    private val twoCharOps = setOf("=>", "==", "!=", "<=", ">=", "&&", "||", "??", "++", "--", "+=", "-=", "*=", "/=", "%=", "?.")

    private fun readPunctuator(startLine: Int, startColumn: Int): Token {
        val three = if (pos + 3 <= length) source.substring(pos, pos + 3) else ""
        if (three in threeCharOps) {
            repeat(3) { advance() }
            return Token(TokenType.PUNCTUATOR, three, startLine, startColumn)
        }
        val two = if (pos + 2 <= length) source.substring(pos, pos + 2) else ""
        if (two in twoCharOps) {
            repeat(2) { advance() }
            return Token(TokenType.PUNCTUATOR, two, startLine, startColumn)
        }
        val one = advance().toString()
        return Token(TokenType.PUNCTUATOR, one, startLine, startColumn)
    }
}
