package com.sunflower.shortcut.javascript.validator

import com.sunflower.shortcut.javascript.ast.AssignmentExpression
import com.sunflower.shortcut.javascript.ast.CallExpression
import com.sunflower.shortcut.javascript.ast.CatchClause
import com.sunflower.shortcut.javascript.ast.ExpressionStatement
import com.sunflower.shortcut.javascript.ast.ForOfStatement
import com.sunflower.shortcut.javascript.ast.FunctionDeclaration
import com.sunflower.shortcut.javascript.ast.FunctionExpression
import com.sunflower.shortcut.javascript.ast.Identifier
import com.sunflower.shortcut.javascript.ast.MemberExpression
import com.sunflower.shortcut.javascript.ast.Program
import com.sunflower.shortcut.javascript.ast.StringLiteral
import com.sunflower.shortcut.javascript.ast.VariableDeclaration
import com.sunflower.shortcut.javascript.ast.VariableDeclarator
import com.sunflower.shortcut.javascript.ast.walk

/** What the validator hands the analyzer once a scenario checks out. */
data class ValidatedScenario(
    val triggerKey: String,
    val handler: FunctionExpression,
    /** Every name declared anywhere in the script (functions, params, vars,
     *  catch bindings, for-of bindings) — lets the security check allow
     *  calls to the scenario's own helper functions without a real scope
     *  resolver (spec §19 keeps JS "обычным", helper functions are normal). */
    val declaredNames: Set<String>
)

class ScenarioValidator {

    fun validate(program: Program): ValidatedScenario {
        val onCalls = program.body
            .filterIsInstance<ExpressionStatement>()
            .mapNotNull { stmt -> (stmt.expression as? CallExpression)?.let { stmt to it } }
            .filter { (_, call) -> (call.callee as? Identifier)?.name == "on" }

        if (onCalls.isEmpty()) {
            val loc = program.body.firstOrNull() ?: program
            fail("Сценарий должен содержать обработчик on(\"событие\", ...)", loc.line, loc.column)
        }
        if (onCalls.size > 1) {
            val (_, secondCall) = onCalls[1]
            fail("Сценарий должен содержать только один обработчик on(...)", secondCall.line, secondCall.column)
        }

        val (onStatement, onCall) = onCalls.single()
        if (onCall.arguments.size != 2) {
            fail("on(...) должен принимать два аргумента: событие и функцию-обработчик", onCall.line, onCall.column)
        }

        val triggerArg = onCall.arguments[0]
        val triggerKey = (triggerArg as? StringLiteral)?.value
            ?: fail("Первый аргумент on() должен быть строкой с названием события", triggerArg.line, triggerArg.column)

        val handlerArg = onCall.arguments[1]
        val handler = handlerArg as? FunctionExpression
            ?: fail("Второй аргумент on() должен быть функцией-обработчиком", handlerArg.line, handlerArg.column)

        if (!handler.isAsync) {
            fail("Обработчик события должен быть объявлен как async", handler.line, handler.column)
        }

        program.body.forEach { stmt ->
            val allowedAtTopLevel = stmt is VariableDeclaration || stmt is FunctionDeclaration || stmt === onStatement
            if (!allowedAtTopLevel) {
                fail(
                    "На верхнем уровне сценария разрешены только объявления и один обработчик on(...)",
                    stmt.line, stmt.column
                )
            }
        }

        val declaredNames = collectDeclaredNames(program)
        validateSandbox(program, declaredNames)

        return ValidatedScenario(triggerKey, handler, declaredNames)
    }

    private fun collectDeclaredNames(program: Program): Set<String> {
        val names = mutableSetOf<String>()
        program.walk { node ->
            when (node) {
                is FunctionDeclaration -> { names += node.name; names += node.params }
                is FunctionExpression -> { node.name?.let { names += it }; names += node.params }
                is VariableDeclarator -> names += node.name
                is CatchClause -> node.paramName?.let { names += it }
                is ForOfStatement -> names += node.variableName
                else -> {}
            }
        }
        return names
    }

    private fun validateSandbox(program: Program, declaredNames: Set<String>) {
        program.walk { node ->
            when (node) {
                is Identifier -> if (node.name in AllowedGlobals.DENIED_IDENTIFIERS) {
                    fail(
                        "Использование \"${node.name}\" запрещено — доступ к устройству возможен только через модули SDK",
                        node.line, node.column
                    )
                }

                is MemberExpression -> if (!node.computed && node.propertyName in AllowedGlobals.DENIED_PROPERTY_NAMES) {
                    fail("Обращение к \".${node.propertyName}\" запрещено", node.line, node.column)
                }

                is CallExpression -> {
                    val callee = node.callee
                    if (callee is Identifier &&
                        callee.name !in AllowedGlobals.SAFE_GLOBAL_CALLEES &&
                        callee.name !in declaredNames
                    ) {
                        fail("Вызов неизвестной функции \"${callee.name}\" запрещён", callee.line, callee.column)
                    }
                }

                is AssignmentExpression -> {
                    val target = node.target
                    if (target is Identifier && target.name !in declaredNames) {
                        fail(
                            "Присваивание необъявленной переменной \"${target.name}\" запрещено",
                            target.line, target.column
                        )
                    }
                }

                else -> {}
            }
        }
    }

    private fun fail(message: String, line: Int, column: Int): Nothing =
        throw ScenarioValidationException(message, line, column)
}
