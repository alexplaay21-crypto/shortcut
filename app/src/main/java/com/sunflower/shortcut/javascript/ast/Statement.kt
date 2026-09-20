package com.sunflower.shortcut.javascript.ast

sealed interface Statement : JsNode

data class BlockStatement(val body: List<Statement>, override val line: Int, override val column: Int) : Statement
data class ExpressionStatement(val expression: Expression, override val line: Int, override val column: Int) : Statement
data class EmptyStatement(override val line: Int, override val column: Int) : Statement

data class VariableDeclarator(
    val name: String,
    val init: Expression?,
    override val line: Int,
    override val column: Int
) : JsNode

data class VariableDeclaration(
    val kind: String, // "var" | "let" | "const"
    val declarations: List<VariableDeclarator>,
    override val line: Int,
    override val column: Int
) : Statement

data class IfStatement(
    val test: Expression,
    val consequent: Statement,
    val alternate: Statement?,
    override val line: Int,
    override val column: Int
) : Statement

/** Classic `for (init; test; update) body`. [init] is a VariableDeclaration
 *  or ExpressionStatement or null. */
data class ForStatement(
    val init: Statement?,
    val test: Expression?,
    val update: Expression?,
    val body: Statement,
    override val line: Int,
    override val column: Int
) : Statement

/** `for (const x of iterable) body` — the loop shape spec §18 "циклы" and
 *  the contacts/apps-list examples in §15/§16 actually need; classic
 *  for-in over object keys is not supported. */
data class ForOfStatement(
    val declarationKind: String, // "const" | "let"
    val variableName: String,
    val iterable: Expression,
    val body: Statement,
    override val line: Int,
    override val column: Int
) : Statement

data class WhileStatement(
    val test: Expression,
    val body: Statement,
    override val line: Int,
    override val column: Int
) : Statement

data class DoWhileStatement(
    val body: Statement,
    val test: Expression,
    override val line: Int,
    override val column: Int
) : Statement

data class ReturnStatement(val argument: Expression?, override val line: Int, override val column: Int) : Statement
data class BreakStatement(override val line: Int, override val column: Int) : Statement
data class ContinueStatement(override val line: Int, override val column: Int) : Statement
data class ThrowStatement(val argument: Expression, override val line: Int, override val column: Int) : Statement

data class CatchClause(
    val paramName: String?,
    val body: BlockStatement,
    override val line: Int,
    override val column: Int
) : JsNode

data class TryStatement(
    val block: BlockStatement,
    val handler: CatchClause?,
    val finalizer: BlockStatement?,
    override val line: Int,
    override val column: Int
) : Statement

data class FunctionDeclaration(
    val name: String,
    val params: List<String>,
    val body: BlockStatement,
    val isAsync: Boolean,
    override val line: Int,
    override val column: Int
) : Statement
