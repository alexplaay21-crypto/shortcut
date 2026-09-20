package com.sunflower.shortcut.javascript.ast

/**
 * Deliberately a *subset* of full ECMAScript, matched to what spec §15/§17
 * scenario examples actually use: plain values, object/array literals,
 * template strings, arrow/async functions, member/call chains, the usual
 * operators, ternary, await. NOT supported (rejected by the parser with a
 * clear syntax error rather than silently misparsed): destructuring, spread/
 * rest, generators, classes, regex literals, optional chaining, getters/
 * setters, labeled statements. If a scenario genuinely needs one of these,
 * that's a real gap to revisit — not something to fake support for here.
 */
sealed interface Expression : JsNode

data class Identifier(val name: String, override val line: Int, override val column: Int) : Expression
data class ThisExpression(override val line: Int, override val column: Int) : Expression

data class NumericLiteral(val value: Double, val raw: String, override val line: Int, override val column: Int) : Expression
data class StringLiteral(val value: String, override val line: Int, override val column: Int) : Expression
data class BooleanLiteral(val value: Boolean, override val line: Int, override val column: Int) : Expression
data class NullLiteral(override val line: Int, override val column: Int) : Expression
data class UndefinedLiteral(override val line: Int, override val column: Int) : Expression

/** quasis.size == expressions.size + 1 — text segments interleaved with `${expr}` parts. */
data class TemplateLiteral(
    val quasis: List<String>,
    val expressions: List<Expression>,
    override val line: Int,
    override val column: Int
) : Expression

data class ArrayExpression(val elements: List<Expression>, override val line: Int, override val column: Int) : Expression

data class ObjectProperty(
    val key: String,
    val computedKey: Expression?,
    val value: Expression,
    override val line: Int,
    override val column: Int
) : JsNode

data class ObjectExpression(val properties: List<ObjectProperty>, override val line: Int, override val column: Int) : Expression

/** Represents function expressions, async functions, and arrow functions
 *  uniformly. An arrow with an expression body (`x => x + 1`, no braces) is
 *  normalized into a BlockStatement holding a single ReturnStatement, so
 *  downstream code (analyzer, flow builder) only ever deals with one shape. */
data class FunctionExpression(
    val name: String?,
    val params: List<String>,
    val body: BlockStatement,
    val isAsync: Boolean,
    val isArrow: Boolean,
    override val line: Int,
    override val column: Int
) : Expression

data class CallExpression(
    val callee: Expression,
    val arguments: List<Expression>,
    override val line: Int,
    override val column: Int
) : Expression

data class NewExpression(
    val callee: Expression,
    val arguments: List<Expression>,
    override val line: Int,
    override val column: Int
) : Expression

/** `object.property` when computed == false (propertyName set); `object[expr]` when true (computedProperty set). */
data class MemberExpression(
    val objectExpr: Expression,
    val propertyName: String?,
    val computedProperty: Expression?,
    val computed: Boolean,
    override val line: Int,
    override val column: Int
) : Expression

data class UnaryExpression(
    val operator: String, // ! - + typeof void
    val argument: Expression,
    override val line: Int,
    override val column: Int
) : Expression

data class UpdateExpression(
    val operator: String, // ++ --
    val argument: Expression,
    val prefix: Boolean,
    override val line: Int,
    override val column: Int
) : Expression

data class BinaryExpression(
    val operator: String, // + - * / % < > <= >= == === != !==
    val left: Expression,
    val right: Expression,
    override val line: Int,
    override val column: Int
) : Expression

data class LogicalExpression(
    val operator: String, // && || ??
    val left: Expression,
    val right: Expression,
    override val line: Int,
    override val column: Int
) : Expression

data class AssignmentExpression(
    val operator: String, // = += -= *= /= %=
    val target: Expression,
    val value: Expression,
    override val line: Int,
    override val column: Int
) : Expression

data class ConditionalExpression(
    val test: Expression,
    val consequent: Expression,
    val alternate: Expression,
    override val line: Int,
    override val column: Int
) : Expression

data class AwaitExpression(val argument: Expression, override val line: Int, override val column: Int) : Expression

data class SequenceExpression(val expressions: List<Expression>, override val line: Int, override val column: Int) : Expression
