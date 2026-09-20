package com.sunflower.shortcut.javascript.parser

import com.sunflower.shortcut.javascript.ast.ArrayExpression
import com.sunflower.shortcut.javascript.ast.AssignmentExpression
import com.sunflower.shortcut.javascript.ast.AwaitExpression
import com.sunflower.shortcut.javascript.ast.BinaryExpression
import com.sunflower.shortcut.javascript.ast.BlockStatement
import com.sunflower.shortcut.javascript.ast.BooleanLiteral
import com.sunflower.shortcut.javascript.ast.BreakStatement
import com.sunflower.shortcut.javascript.ast.CallExpression
import com.sunflower.shortcut.javascript.ast.CatchClause
import com.sunflower.shortcut.javascript.ast.ConditionalExpression
import com.sunflower.shortcut.javascript.ast.ContinueStatement
import com.sunflower.shortcut.javascript.ast.DoWhileStatement
import com.sunflower.shortcut.javascript.ast.EmptyStatement
import com.sunflower.shortcut.javascript.ast.Expression
import com.sunflower.shortcut.javascript.ast.ExpressionStatement
import com.sunflower.shortcut.javascript.ast.ForOfStatement
import com.sunflower.shortcut.javascript.ast.ForStatement
import com.sunflower.shortcut.javascript.ast.FunctionDeclaration
import com.sunflower.shortcut.javascript.ast.FunctionExpression
import com.sunflower.shortcut.javascript.ast.Identifier
import com.sunflower.shortcut.javascript.ast.IfStatement
import com.sunflower.shortcut.javascript.ast.LogicalExpression
import com.sunflower.shortcut.javascript.ast.MemberExpression
import com.sunflower.shortcut.javascript.ast.NewExpression
import com.sunflower.shortcut.javascript.ast.NullLiteral
import com.sunflower.shortcut.javascript.ast.NumericLiteral
import com.sunflower.shortcut.javascript.ast.ObjectExpression
import com.sunflower.shortcut.javascript.ast.ObjectProperty
import com.sunflower.shortcut.javascript.ast.Program
import com.sunflower.shortcut.javascript.ast.ReturnStatement
import com.sunflower.shortcut.javascript.ast.SequenceExpression
import com.sunflower.shortcut.javascript.ast.Statement
import com.sunflower.shortcut.javascript.ast.StringLiteral
import com.sunflower.shortcut.javascript.ast.TemplateLiteral
import com.sunflower.shortcut.javascript.ast.ThisExpression
import com.sunflower.shortcut.javascript.ast.ThrowStatement
import com.sunflower.shortcut.javascript.ast.TryStatement
import com.sunflower.shortcut.javascript.ast.UnaryExpression
import com.sunflower.shortcut.javascript.ast.UndefinedLiteral
import com.sunflower.shortcut.javascript.ast.UpdateExpression
import com.sunflower.shortcut.javascript.ast.VariableDeclaration
import com.sunflower.shortcut.javascript.ast.VariableDeclarator
import com.sunflower.shortcut.javascript.ast.WhileStatement

/** Entry point — `JsParser().parse(source)`. Stateless; safe to call repeatedly. */
class JsParser {
    fun parse(source: String): Program {
        val tokens = JsLexer(source).tokenize()
        return ParserEngine(tokens).parseProgram()
    }
}

/**
 * One parse = one instance. Index-based cursor over the token list, with
 * simple backtracking (save/restore [pos]) used only where the grammar is
 * genuinely ambiguous with one token of lookahead — arrow-function params
 * vs. a parenthesized expression.
 */
private class ParserEngine(private val tokens: List<Token>) {
    private var pos = 0

    fun parseProgram(): Program {
        val body = mutableListOf<Statement>()
        while (!isAtEnd()) body += parseStatement()
        return Program(body)
    }

    // ---- token helpers ----------------------------------------------------

    private fun peek(offset: Int = 0): Token = tokens[minOf(pos + offset, tokens.size - 1)]
    private fun isAtEnd(): Boolean = peek().type == TokenType.EOF
    private fun advance(): Token = tokens[pos].also { if (pos < tokens.size - 1) pos++ }

    private fun check(type: TokenType, value: String? = null): Boolean =
        peek().type == type && (value == null || peek().value == value)

    private fun checkPunct(v: String) = check(TokenType.PUNCTUATOR, v)
    private fun checkKeyword(v: String) = check(TokenType.KEYWORD, v)

    private fun match(type: TokenType, value: String? = null): Boolean {
        if (check(type, value)) { advance(); return true }
        return false
    }

    private fun matchPunct(v: String) = match(TokenType.PUNCTUATOR, v)
    private fun matchKeyword(v: String) = match(TokenType.KEYWORD, v)

    private fun expectPunct(v: String): Token {
        if (!checkPunct(v)) fail("Ожидался '$v'")
        return advance()
    }

    private fun expectKeyword(v: String): Token {
        if (!checkKeyword(v)) fail("Ожидалось ключевое слово '$v'")
        return advance()
    }

    private fun expectIdentifier(): Token {
        if (peek().type != TokenType.IDENTIFIER) fail("Ожидался идентификатор")
        return advance()
    }

    /** Property/method names may legally be keywords too (`.catch`, `.delete`). */
    private fun expectIdentifierLike(): Token {
        if (peek().type == TokenType.IDENTIFIER || peek().type == TokenType.KEYWORD) return advance()
        fail("Ожидалось имя свойства")
    }

    private fun consumeSemicolon() {
        matchPunct(";") // pragmatic ASI: optional, never required
    }

    private fun fail(message: String): Nothing {
        val t = peek()
        val got = if (t.type == TokenType.EOF) "конец файла" else "'${t.value}'"
        throw JsSyntaxException("$message (получено: $got)", t.line, t.column)
    }

    // ---- statements ---------------------------------------------------------

    private fun parseStatement(): Statement {
        val t = peek()
        return when {
            checkPunct("{") -> parseBlock()
            checkPunct(";") -> { advance(); EmptyStatement(t.line, t.column) }
            checkKeyword("var") || checkKeyword("let") || checkKeyword("const") ->
                parseVariableDeclaration().also { consumeSemicolon() }
            checkKeyword("if") -> parseIf()
            checkKeyword("for") -> parseFor()
            checkKeyword("while") -> parseWhile()
            checkKeyword("do") -> parseDoWhile()
            checkKeyword("return") -> parseReturn()
            checkKeyword("break") -> { advance(); consumeSemicolon(); BreakStatement(t.line, t.column) }
            checkKeyword("continue") -> { advance(); consumeSemicolon(); ContinueStatement(t.line, t.column) }
            checkKeyword("throw") -> parseThrow()
            checkKeyword("try") -> parseTry()
            checkKeyword("function") -> parseFunctionDeclaration(isAsync = false)
            checkKeyword("async") && peek(1).type == TokenType.KEYWORD && peek(1).value == "function" -> {
                advance(); parseFunctionDeclaration(isAsync = true)
            }
            else -> {
                val expr = parseExpression()
                consumeSemicolon()
                ExpressionStatement(expr, t.line, t.column)
            }
        }
    }

    private fun parseBlock(): BlockStatement {
        val t = expectPunct("{")
        val body = mutableListOf<Statement>()
        while (!checkPunct("}") && !isAtEnd()) body += parseStatement()
        expectPunct("}")
        return BlockStatement(body, t.line, t.column)
    }

    private fun parseVariableDeclaration(): VariableDeclaration {
        val kindToken = advance()
        val decls = mutableListOf<VariableDeclarator>()
        do {
            val nameToken = expectIdentifier()
            var init: Expression? = null
            if (matchPunct("=")) init = parseAssignment()
            decls += VariableDeclarator(nameToken.value, init, nameToken.line, nameToken.column)
        } while (matchPunct(","))
        return VariableDeclaration(kindToken.value, decls, kindToken.line, kindToken.column)
    }

    private fun parseIf(): IfStatement {
        val t = expectKeyword("if")
        expectPunct("("); val test = parseExpression(); expectPunct(")")
        val consequent = parseStatement()
        val alternate = if (matchKeyword("else")) parseStatement() else null
        return IfStatement(test, consequent, alternate, t.line, t.column)
    }

    private fun parseFor(): Statement {
        val t = expectKeyword("for")
        expectPunct("(")

        val looksLikeForOf = (checkKeyword("const") || checkKeyword("let")) &&
            peek(1).type == TokenType.IDENTIFIER &&
            peek(2).type == TokenType.KEYWORD && peek(2).value == "of"

        if (looksLikeForOf) {
            val kind = advance().value
            val name = advance().value
            advance() // 'of'
            val iterable = parseExpression()
            expectPunct(")")
            val body = parseStatement()
            return ForOfStatement(kind, name, iterable, body, t.line, t.column)
        }

        val init: Statement? = when {
            checkPunct(";") -> null
            checkKeyword("var") || checkKeyword("let") || checkKeyword("const") -> parseVariableDeclaration()
            else -> {
                val et = peek()
                ExpressionStatement(parseExpression(), et.line, et.column)
            }
        }
        expectPunct(";")
        val test = if (!checkPunct(";")) parseExpression() else null
        expectPunct(";")
        val update = if (!checkPunct(")")) parseExpression() else null
        expectPunct(")")
        val body = parseStatement()
        return ForStatement(init, test, update, body, t.line, t.column)
    }

    private fun parseWhile(): WhileStatement {
        val t = expectKeyword("while")
        expectPunct("("); val test = parseExpression(); expectPunct(")")
        return WhileStatement(test, parseStatement(), t.line, t.column)
    }

    private fun parseDoWhile(): DoWhileStatement {
        val t = expectKeyword("do")
        val body = parseStatement()
        expectKeyword("while")
        expectPunct("("); val test = parseExpression(); expectPunct(")")
        consumeSemicolon()
        return DoWhileStatement(body, test, t.line, t.column)
    }

    private fun parseReturn(): ReturnStatement {
        val t = expectKeyword("return")
        val arg = if (checkPunct(";") || checkPunct("}") || isAtEnd() || peek().line != t.line) {
            null
        } else {
            parseExpression()
        }
        consumeSemicolon()
        return ReturnStatement(arg, t.line, t.column)
    }

    private fun parseThrow(): ThrowStatement {
        val t = expectKeyword("throw")
        val arg = parseExpression()
        consumeSemicolon()
        return ThrowStatement(arg, t.line, t.column)
    }

    private fun parseTry(): TryStatement {
        val t = expectKeyword("try")
        val block = parseBlock()
        var handler: CatchClause? = null
        var finalizer: BlockStatement? = null
        if (checkKeyword("catch")) {
            val ct = advance()
            var paramName: String? = null
            if (matchPunct("(")) { paramName = expectIdentifier().value; expectPunct(")") }
            handler = CatchClause(paramName, parseBlock(), ct.line, ct.column)
        }
        if (matchKeyword("finally")) finalizer = parseBlock()
        if (handler == null && finalizer == null) fail("try требует catch или finally")
        return TryStatement(block, handler, finalizer, t.line, t.column)
    }

    private fun parseFunctionDeclaration(isAsync: Boolean): FunctionDeclaration {
        val t = expectKeyword("function")
        val name = expectIdentifier().value
        val params = parseParamList()
        val body = parseBlock()
        return FunctionDeclaration(name, params, body, isAsync, t.line, t.column)
    }

    private fun parseParamList(): List<String> {
        expectPunct("(")
        val params = mutableListOf<String>()
        if (!checkPunct(")")) {
            do { params += expectIdentifier().value } while (matchPunct(","))
        }
        expectPunct(")")
        return params
    }

    // ---- expressions (precedence climbing, weakest to strongest) ------------

    private fun parseExpression(): Expression {
        var expr = parseAssignment()
        if (checkPunct(",")) {
            val exprs = mutableListOf(expr)
            while (matchPunct(",")) exprs += parseAssignment()
            expr = SequenceExpression(exprs, expr.line, expr.column)
        }
        return expr
    }

    private val assignmentOps = setOf("=", "+=", "-=", "*=", "/=", "%=")

    private fun parseAssignment(): Expression {
        tryParseArrowFunction()?.let { return it }

        val left = parseConditional()
        if (peek().type == TokenType.PUNCTUATOR && peek().value in assignmentOps) {
            val op = advance()
            val right = parseAssignment()
            return AssignmentExpression(op.value, left, right, left.line, left.column)
        }
        return left
    }

    private fun tryParseArrowFunction(): FunctionExpression? {
        val startPos = pos
        val startToken = peek()
        var isAsync = false

        if (checkKeyword("async") &&
            (peek(1).value == "(" || peek(1).type == TokenType.IDENTIFIER) &&
            peek(1).line == startToken.line
        ) {
            isAsync = true
            advance()
        }

        val params: List<String> = when {
            checkPunct("(") -> tryParseSimpleParamList() ?: run { pos = startPos; return null }
            peek().type == TokenType.IDENTIFIER -> listOf(advance().value)
            else -> { pos = startPos; return null }
        }

        if (!checkPunct("=>")) { pos = startPos; return null }
        advance()

        val body = if (checkPunct("{")) {
            parseBlock()
        } else {
            val exprToken = peek()
            val expr = parseAssignment()
            BlockStatement(listOf(ReturnStatement(expr, exprToken.line, exprToken.column)), exprToken.line, exprToken.column)
        }
        return FunctionExpression(null, params, body, isAsync, isArrow = true, startToken.line, startToken.column)
    }

    /** Only succeeds for a bare `(a, b, c)` name list — no defaults/destructuring. */
    private fun tryParseSimpleParamList(): List<String>? {
        val saved = pos
        if (!matchPunct("(")) return null
        val params = mutableListOf<String>()
        if (!checkPunct(")")) {
            do {
                if (peek().type != TokenType.IDENTIFIER) { pos = saved; return null }
                params += advance().value
            } while (matchPunct(","))
        }
        if (!matchPunct(")")) { pos = saved; return null }
        return params
    }

    private fun parseConditional(): Expression {
        val test = parseNullish()
        if (matchPunct("?")) {
            val consequent = parseAssignment()
            expectPunct(":")
            val alternate = parseAssignment()
            return ConditionalExpression(test, consequent, alternate, test.line, test.column)
        }
        return test
    }

    private fun parseNullish(): Expression {
        var left = parseLogicalOr()
        while (checkPunct("??")) {
            val op = advance()
            left = LogicalExpression(op.value, left, parseLogicalOr(), left.line, left.column)
        }
        return left
    }

    private fun parseLogicalOr(): Expression {
        var left = parseLogicalAnd()
        while (checkPunct("||")) {
            val op = advance()
            left = LogicalExpression(op.value, left, parseLogicalAnd(), left.line, left.column)
        }
        return left
    }

    private fun parseLogicalAnd(): Expression {
        var left = parseEquality()
        while (checkPunct("&&")) {
            val op = advance()
            left = LogicalExpression(op.value, left, parseEquality(), left.line, left.column)
        }
        return left
    }

    private val equalityOps = setOf("==", "!=", "===", "!==")

    private fun parseEquality(): Expression {
        var left = parseRelational()
        while (peek().type == TokenType.PUNCTUATOR && peek().value in equalityOps) {
            val op = advance()
            left = BinaryExpression(op.value, left, parseRelational(), left.line, left.column)
        }
        return left
    }

    private val relationalOps = setOf("<", "<=", ">", ">=")

    private fun parseRelational(): Expression {
        var left = parseAdditive()
        while (true) {
            when {
                peek().type == TokenType.PUNCTUATOR && peek().value in relationalOps -> {
                    val op = advance(); left = BinaryExpression(op.value, left, parseAdditive(), left.line, left.column)
                }
                checkKeyword("instanceof") || checkKeyword("in") -> {
                    val op = advance(); left = BinaryExpression(op.value, left, parseAdditive(), left.line, left.column)
                }
                else -> return left
            }
        }
    }

    private fun parseAdditive(): Expression {
        var left = parseMultiplicative()
        while (peek().type == TokenType.PUNCTUATOR && (peek().value == "+" || peek().value == "-")) {
            val op = advance()
            left = BinaryExpression(op.value, left, parseMultiplicative(), left.line, left.column)
        }
        return left
    }

    private fun parseMultiplicative(): Expression {
        var left = parseUnary()
        while (peek().type == TokenType.PUNCTUATOR && peek().value in setOf("*", "/", "%")) {
            val op = advance()
            left = BinaryExpression(op.value, left, parseUnary(), left.line, left.column)
        }
        return left
    }

    private val unaryPunct = setOf("!", "-", "+")
    private val unaryKeywords = setOf("typeof", "void", "delete")

    private fun parseUnary(): Expression {
        val t = peek()
        if ((t.type == TokenType.PUNCTUATOR && t.value in unaryPunct) ||
            (t.type == TokenType.KEYWORD && t.value in unaryKeywords)
        ) {
            advance()
            return UnaryExpression(t.value, parseUnary(), t.line, t.column)
        }
        if (t.type == TokenType.PUNCTUATOR && (t.value == "++" || t.value == "--")) {
            advance()
            return UpdateExpression(t.value, parseUnary(), prefix = true, t.line, t.column)
        }
        if (checkKeyword("await")) {
            advance()
            return AwaitExpression(parseUnary(), t.line, t.column)
        }
        return parsePostfix()
    }

    private fun parsePostfix(): Expression {
        val expr = parseCallOrMember()
        if (peek().type == TokenType.PUNCTUATOR && (peek().value == "++" || peek().value == "--") && peek().line == expr.line) {
            val op = advance()
            return UpdateExpression(op.value, expr, prefix = false, expr.line, expr.column)
        }
        return expr
    }

    private fun parseCallOrMember(): Expression {
        var expr = if (checkKeyword("new")) parseNew() else parsePrimary()
        while (true) {
            expr = when {
                matchPunct(".") -> {
                    val name = expectIdentifierLike()
                    MemberExpression(expr, name.value, null, computed = false, expr.line, expr.column)
                }
                matchPunct("[") -> {
                    val idx = parseExpression()
                    expectPunct("]")
                    MemberExpression(expr, null, idx, computed = true, expr.line, expr.column)
                }
                checkPunct("(") -> CallExpression(expr, parseArguments(), expr.line, expr.column)
                else -> return expr
            }
        }
    }

    private fun parseNew(): Expression {
        val t = expectKeyword("new")
        var callee: Expression = parsePrimary()
        while (matchPunct(".")) {
            val name = expectIdentifierLike()
            callee = MemberExpression(callee, name.value, null, computed = false, callee.line, callee.column)
        }
        val args = if (checkPunct("(")) parseArguments() else emptyList()
        return NewExpression(callee, args, t.line, t.column)
    }

    private fun parseArguments(): List<Expression> {
        expectPunct("(")
        val args = mutableListOf<Expression>()
        if (!checkPunct(")")) {
            do { args += parseAssignment() } while (matchPunct(","))
        }
        expectPunct(")")
        return args
    }

    private fun parseNumberValue(raw: String): Double =
        if (raw.startsWith("0x") || raw.startsWith("0X")) raw.substring(2).toLong(16).toDouble() else raw.toDouble()

    private fun parsePrimary(): Expression {
        val t = peek()
        return when {
            t.type == TokenType.NUMBER -> { advance(); NumericLiteral(parseNumberValue(t.value), t.value, t.line, t.column) }
            t.type == TokenType.STRING -> { advance(); StringLiteral(t.value, t.line, t.column) }
            t.type == TokenType.TEMPLATE -> { advance(); buildTemplateLiteral(t) }
            checkKeyword("true") -> { advance(); BooleanLiteral(true, t.line, t.column) }
            checkKeyword("false") -> { advance(); BooleanLiteral(false, t.line, t.column) }
            checkKeyword("null") -> { advance(); NullLiteral(t.line, t.column) }
            checkKeyword("undefined") -> { advance(); UndefinedLiteral(t.line, t.column) }
            checkKeyword("this") -> { advance(); ThisExpression(t.line, t.column) }
            checkKeyword("function") -> parseFunctionExpression(isAsync = false)
            checkKeyword("async") && peek(1).type == TokenType.KEYWORD && peek(1).value == "function" -> {
                advance(); parseFunctionExpression(isAsync = true)
            }
            t.type == TokenType.IDENTIFIER -> { advance(); Identifier(t.value, t.line, t.column) }
            checkPunct("(") -> { advance(); val expr = parseExpression(); expectPunct(")"); expr }
            checkPunct("[") -> parseArrayLiteral()
            checkPunct("{") -> parseObjectLiteral()
            else -> fail("Неожиданный токен")
        }
    }

    private fun parseFunctionExpression(isAsync: Boolean): FunctionExpression {
        val t = expectKeyword("function")
        val name = if (peek().type == TokenType.IDENTIFIER) advance().value else null
        val params = parseParamList()
        val body = parseBlock()
        return FunctionExpression(name, params, body, isAsync, isArrow = false, t.line, t.column)
    }

    private fun parseArrayLiteral(): ArrayExpression {
        val t = expectPunct("[")
        val elements = mutableListOf<Expression>()
        if (!checkPunct("]")) {
            do { elements += parseAssignment() } while (matchPunct(","))
        }
        expectPunct("]")
        return ArrayExpression(elements, t.line, t.column)
    }

    private fun parseObjectLiteral(): ObjectExpression {
        val t = expectPunct("{")
        val props = mutableListOf<ObjectProperty>()
        if (!checkPunct("}")) {
            do {
                if (checkPunct("}")) break // trailing comma
                val propToken = peek()
                var computedKey: Expression? = null
                val key: String = when {
                    checkPunct("[") -> { advance(); computedKey = parseAssignment(); expectPunct("]"); "" }
                    peek().type == TokenType.STRING -> advance().value
                    peek().type == TokenType.NUMBER -> advance().value
                    else -> expectIdentifierLike().value
                }
                val value: Expression = when {
                    matchPunct(":") -> parseAssignment()
                    checkPunct("(") -> {
                        val params = parseParamList()
                        val body = parseBlock()
                        FunctionExpression(key, params, body, isAsync = false, isArrow = false, propToken.line, propToken.column)
                    }
                    else -> Identifier(key, propToken.line, propToken.column) // shorthand { foo }
                }
                props += ObjectProperty(key, computedKey, value, propToken.line, propToken.column)
            } while (matchPunct(","))
        }
        expectPunct("}")
        return ObjectExpression(props, t.line, t.column)
    }

    /** Nested expressions inside `${...}` are parsed independently, so their
     *  own line numbers restart at 1 — on error we report the template's
     *  start line instead of a misleading "line 1". A known, documented
     *  imprecision rather than a silent failure. */
    private fun buildTemplateLiteral(token: Token): TemplateLiteral {
        val quasis = token.templateQuasis.orEmpty()
        val exprSources = token.templateExprSources.orEmpty()
        val expressions = exprSources.map { src ->
            try {
                val nested = JsParser().parse(src)
                val stmt = nested.body.firstOrNull() as? ExpressionStatement
                    ?: throw JsSyntaxException("Пустое выражение в \${...}", token.line, token.column)
                stmt.expression
            } catch (e: JsSyntaxException) {
                throw JsSyntaxException(e.message ?: "Ошибка в \${...}", token.line, token.column)
            }
        }
        return TemplateLiteral(quasis, expressions, token.line, token.column)
    }
}
