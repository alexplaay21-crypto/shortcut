package com.sunflower.shortcut.javascript.ast

/** Direct child nodes of [this], in source order. Leaf nodes return empty. */
fun JsNode.children(): List<JsNode> = when (this) {
    is Program -> body
    is BlockStatement -> body
    is ExpressionStatement -> listOf(expression)
    is EmptyStatement -> emptyList()
    is VariableDeclaration -> declarations
    is VariableDeclarator -> listOfNotNull(init)
    is IfStatement -> listOfNotNull<JsNode>(test, consequent, alternate)
    is ForStatement -> listOfNotNull<JsNode>(init, test, update, body)
    is ForOfStatement -> listOf(iterable, body)
    is WhileStatement -> listOf(test, body)
    is DoWhileStatement -> listOf(body, test)
    is ReturnStatement -> listOfNotNull(argument)
    is BreakStatement -> emptyList()
    is ContinueStatement -> emptyList()
    is ThrowStatement -> listOf(argument)
    is CatchClause -> listOf(body)
    is TryStatement -> listOfNotNull<JsNode>(block, handler, finalizer)
    is FunctionDeclaration -> listOf(body)
    is Identifier -> emptyList()
    is ThisExpression -> emptyList()
    is NumericLiteral -> emptyList()
    is StringLiteral -> emptyList()
    is BooleanLiteral -> emptyList()
    is NullLiteral -> emptyList()
    is UndefinedLiteral -> emptyList()
    is TemplateLiteral -> expressions
    is ArrayExpression -> elements
    is ObjectProperty -> listOfNotNull<JsNode>(computedKey, value)
    is ObjectExpression -> properties
    is FunctionExpression -> listOf(body)
    is CallExpression -> listOf<JsNode>(callee) + arguments
    is NewExpression -> listOf<JsNode>(callee) + arguments
    is MemberExpression -> listOfNotNull<JsNode>(objectExpr, computedProperty)
    is UnaryExpression -> listOf(argument)
    is UpdateExpression -> listOf(argument)
    is BinaryExpression -> listOf(left, right)
    is LogicalExpression -> listOf(left, right)
    is AssignmentExpression -> listOf(target, value)
    is ConditionalExpression -> listOf(test, consequent, alternate)
    is AwaitExpression -> listOf(argument)
    is SequenceExpression -> expressions
    else -> emptyList()
}

/** Visits [this] and every descendant, depth-first, pre-order. */
fun JsNode.walk(visit: (JsNode) -> Unit) {
    visit(this)
    children().forEach { it.walk(visit) }
}
