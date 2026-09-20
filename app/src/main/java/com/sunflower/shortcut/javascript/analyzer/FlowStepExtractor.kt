package com.sunflower.shortcut.javascript.analyzer

import com.sunflower.shortcut.automation.triggers.TriggerType
import com.sunflower.shortcut.javascript.ast.AwaitExpression
import com.sunflower.shortcut.javascript.ast.CallExpression
import com.sunflower.shortcut.javascript.ast.DoWhileStatement
import com.sunflower.shortcut.javascript.ast.Expression
import com.sunflower.shortcut.javascript.ast.ExpressionStatement
import com.sunflower.shortcut.javascript.ast.ForOfStatement
import com.sunflower.shortcut.javascript.ast.ForStatement
import com.sunflower.shortcut.javascript.ast.Identifier
import com.sunflower.shortcut.javascript.ast.IfStatement
import com.sunflower.shortcut.javascript.ast.MemberExpression
import com.sunflower.shortcut.javascript.ast.NumericLiteral
import com.sunflower.shortcut.javascript.ast.ObjectExpression
import com.sunflower.shortcut.javascript.ast.Statement
import com.sunflower.shortcut.javascript.ast.StringLiteral
import com.sunflower.shortcut.javascript.ast.TryStatement
import com.sunflower.shortcut.javascript.ast.VariableDeclaration
import com.sunflower.shortcut.javascript.ast.WhileStatement
import com.sunflower.shortcut.javascript.validator.ValidatedScenario
import com.sunflower.shortcut.ui.components.FlowStepKind
import com.sunflower.shortcut.ui.components.FlowStepUi

/**
 * Deliberately shallow: only the handler's top-level statements become
 * steps. Nested control flow (loop/try bodies) is summarized as one
 * DYNAMIC step rather than recursed into — spec §19's own escape hatch
 * ("Динамический код") for exactly this, rather than forcing every JS
 * construct into a visual block it doesn't fit.
 */
object FlowStepExtractor {

    private val FRIENDLY_TITLES = mapOf(
        "camera.takePhoto" to "Сделать фото",
        "apps.launch" to "Открыть приложение",
        "apps.openUrl" to "Открыть URL",
        "apps.share" to "Поделиться",
        "apps.sendIntent" to "Отправить intent",
        "notifications.show" to "Показать уведомление",
        "location.current" to "Получить местоположение",
        "clipboard.copy" to "Скопировать в буфer обмена",
        "phone.call" to "Позвонить",
        "system.vibrate" to "Вибрация",
        "network.send" to "Отправить данные"
    )

    private val MODULE_EMOJI = mapOf(
        "camera" to "📷", "location" to "📍", "apps" to "📱", "notifications" to "🔔",
        "network" to "🌐", "files" to "📁", "clipboard" to "📋", "bluetooth" to "🔷",
        "wifi" to "📶", "contacts" to "👤", "phone" to "📞", "audio" to "🔊",
        "ui" to "🖱️", "system" to "⚙️", "sensors" to "📈", "media" to "🎞️", "storage" to "💾"
    )

    fun extract(validated: ValidatedScenario): List<FlowStepUi> {
        val trigger = TriggerType.fromKey(validated.triggerKey)
        val steps = mutableListOf(
            FlowStepUi(
                id = "trigger",
                emoji = triggerEmoji(trigger),
                title = trigger?.label ?: validated.triggerKey,
                subtitle = "Триггер",
                kind = FlowStepKind.TRIGGER
            )
        )

        validated.handler.body.body.forEachIndexed { index, stmt ->
            steps += stepFor(stmt, index)
        }

        steps += FlowStepUi(
            id = "end",
            emoji = "✅",
            title = "Завершить сценарий",
            subtitle = "",
            kind = FlowStepKind.END
        )
        return steps
    }

    private fun triggerEmoji(trigger: TriggerType?): String = when (trigger) {
        TriggerType.CHARGING_CONNECTED, TriggerType.CHARGING_DISCONNECTED -> "🔌"
        TriggerType.BATTERY_LOW -> "🪫"
        TriggerType.WIFI_CONNECTED, TriggerType.WIFI_DISCONNECTED -> "📶"
        TriggerType.BLUETOOTH_CONNECTED, TriggerType.BLUETOOTH_DISCONNECTED -> "🔷"
        TriggerType.BOOT_COMPLETED -> "🔁"
        else -> "⚡"
    }

    private fun stepFor(stmt: Statement, index: Int): FlowStepUi = when (stmt) {
        is ExpressionStatement -> stepForExpressionStatement(stmt, index)
        is IfStatement -> FlowStepUi(
            id = "if_$index", emoji = "❓", title = "Условие",
            subtitle = summarize(stmt.test), kind = FlowStepKind.CONDITION
        )
        is ForStatement, is ForOfStatement, is WhileStatement, is DoWhileStatement -> FlowStepUi(
            id = "loop_$index", emoji = "🔁", title = "Цикл", subtitle = "", kind = FlowStepKind.DYNAMIC
        )
        is TryStatement -> FlowStepUi(
            id = "try_$index", emoji = "🛡️", title = "Обработка ошибок (try/catch)",
            subtitle = "", kind = FlowStepKind.DYNAMIC
        )
        is VariableDeclaration -> FlowStepUi(
            id = "var_$index", emoji = "🔤", title = "Переменная",
            subtitle = stmt.declarations.joinToString(", ") { it.name }, kind = FlowStepKind.DYNAMIC
        )
        else -> dynamicStep(index)
    }

    private fun stepForExpressionStatement(stmt: ExpressionStatement, index: Int): FlowStepUi {
        val expr = unwrapAwait(stmt.expression)
        if (expr !is CallExpression) return dynamicStep(index)
        val callee = expr.callee

        if (callee is Identifier && callee.name == "wait") {
            val ms = (expr.arguments.getOrNull(0) as? NumericLiteral)?.value?.toInt() ?: 0
            return FlowStepUi(
                id = "delay_$index", emoji = "⏱", title = "Подождать",
                subtitle = "${ms / 1000} секунда", kind = FlowStepKind.DELAY, editable = true
            )
        }

        if (callee is MemberExpression && !callee.computed) {
            val moduleName = (callee.objectExpr as? Identifier)?.name
            val methodName = callee.propertyName
            if (moduleName != null && methodName != null) {
                val key = "$moduleName.$methodName"
                return FlowStepUi(
                    id = "action_$index",
                    emoji = MODULE_EMOJI[moduleName] ?: "🔧",
                    title = FRIENDLY_TITLES[key] ?: key,
                    subtitle = summarizeArguments(expr.arguments),
                    kind = FlowStepKind.ACTION,
                    editable = true
                )
            }
        }
        return dynamicStep(index)
    }

    private fun dynamicStep(index: Int) = FlowStepUi(
        id = "stmt_$index", emoji = "⚙️", title = "Динамический код", subtitle = "", kind = FlowStepKind.DYNAMIC
    )

    private fun unwrapAwait(expr: Expression): Expression = if (expr is AwaitExpression) expr.argument else expr

    private fun summarize(expr: Expression): String = when (expr) {
        is Identifier -> expr.name
        is StringLiteral -> "\"${expr.value}\""
        is NumericLiteral -> expr.raw
        else -> ""
    }

    private fun summarizeArguments(args: List<Expression>): String =
        args.joinToString(", ") { arg ->
            when (arg) {
                is StringLiteral -> arg.value
                is NumericLiteral -> arg.raw
                is Identifier -> arg.name
                is ObjectExpression -> arg.properties.joinToString(", ") { p -> "${p.key}: ${summarize(p.value)}" }
                else -> ""
            }
        }
}
