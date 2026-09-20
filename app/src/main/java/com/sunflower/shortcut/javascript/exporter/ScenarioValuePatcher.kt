package com.sunflower.shortcut.javascript.exporter

import com.sunflower.shortcut.javascript.ast.AwaitExpression
import com.sunflower.shortcut.javascript.ast.CallExpression
import com.sunflower.shortcut.javascript.ast.ExpressionStatement
import com.sunflower.shortcut.javascript.ast.NumericLiteral
import com.sunflower.shortcut.javascript.ast.Statement
import com.sunflower.shortcut.javascript.ast.StringLiteral
import com.sunflower.shortcut.javascript.parser.JsParser
import com.sunflower.shortcut.javascript.validator.ScenarioValidator

/**
 * Given the stored source and a FlowStepUi id (same "kind_index" scheme
 * FlowStepExtractor generates — e.g. "delay_2", "action_0"), replaces
 * exactly that value's span in the original text and nothing else — no
 * re-serialization from AST, so comments/formatting elsewhere survive
 * untouched.
 *
 * Scope, stated plainly rather than faked: precise for the wait(ms) delay
 * argument and for a single leading string/number argument (covers
 * `apps.launch("Telegram")`, `apps.openUrl(url)`, `notifications.show(text)`,
 * `phone.call(number)`). A call whose first argument is an object literal
 * (`camera.takePhoto({camera:"front"})`) is reported as not yet editable
 * here rather than guessing which property the user meant.
 */
object ScenarioValuePatcher {

    fun patch(source: String, stepId: String, newValue: String): Result<String> = runCatching {
        val program = JsParser().parse(source)
        val validated = ScenarioValidator().validate(program)
        val statements = validated.handler.body.body

        val prefix = stepId.substringBefore('_', stepId)
        val index = stepId.substringAfter('_', "").toIntOrNull()
            ?: throw IllegalArgumentException("Шаг \"$stepId\" нередактируем")
        val stmt = statements.getOrNull(index)
            ?: throw IllegalArgumentException("Шаг \"$stepId\" не найден в сценарии")

        when (prefix) {
            "delay" -> patchDelay(source, stmt, newValue)
            "action" -> patchAction(source, stmt, newValue)
            else -> throw IllegalArgumentException("Шаг \"$stepId\" нередактируем")
        }
    }

    private fun callOf(stmt: Statement): CallExpression {
        val exprStmt = stmt as? ExpressionStatement ?: throw IllegalStateException("Это не шаг с вызовом функции")
        val expr = exprStmt.expression.let { if (it is AwaitExpression) it.argument else it }
        return expr as? CallExpression ?: throw IllegalStateException("Это не шаг с вызовом функции")
    }

    private fun patchDelay(source: String, stmt: Statement, newValue: String): String {
        val call = callOf(stmt)
        val arg = call.arguments.getOrNull(0) as? NumericLiteral
            ?: throw IllegalStateException("У wait() нет числового аргумента")
        val millis = parseDelayToMillis(newValue)
        val (start, end) = numericSpan(source, arg)
            ?: throw IllegalStateException("Не удалось найти значение задержки в исходном коде")
        return source.replaceRange(start, end, millis.toString())
    }

    private fun patchAction(source: String, stmt: Statement, newValue: String): String {
        val call = callOf(stmt)
        val firstArg = call.arguments.getOrNull(0)
            ?: throw IllegalStateException("У этого действия нет аргументов для редактирования")

        return when (firstArg) {
            is StringLiteral -> {
                val (start, end) = stringSpan(source, firstArg)
                    ?: throw IllegalStateException("Не удалось найти значение в исходном коде")
                source.replaceRange(start, end, "\"${escapeForJs(newValue)}\"")
            }
            is NumericLiteral -> {
                val numeric = newValue.toDoubleOrNull()
                    ?: throw IllegalArgumentException("Ожидалось число")
                val (start, end) = numericSpan(source, firstArg)
                    ?: throw IllegalStateException("Не удалось найти значение в исходном коде")
                source.replaceRange(start, end, formatNumber(numeric))
            }
            else -> throw IllegalStateException(
                "Этот параметр использует сложное значение и пока не редактируется в конструкторе — измените код напрямую"
            )
        }
    }

    // ---- exact source-span lookup, by (line, column) already on every AST node ----

    private fun offsetOf(source: String, line: Int, column: Int): Int {
        var currentLine = 1
        var currentColumn = 1
        for (i in source.indices) {
            if (currentLine == line && currentColumn == column) return i
            if (source[i] == '\n') { currentLine++; currentColumn = 1 } else { currentColumn++ }
        }
        return -1
    }

    private fun numericSpan(source: String, node: NumericLiteral): Pair<Int, Int>? {
        val start = offsetOf(source, node.line, node.column)
        if (start < 0) return null
        return start to (start + node.raw.length)
    }

    private fun stringSpan(source: String, node: StringLiteral): Pair<Int, Int>? {
        val start = offsetOf(source, node.line, node.column)
        if (start < 0 || start >= source.length) return null
        val quote = source[start]
        if (quote != '"' && quote != '\'' && quote != '`') return null
        var i = start + 1
        while (i < source.length && source[i] != quote) {
            if (source[i] == '\\') i++
            i++
        }
        return if (i < source.length) start to (i + 1) else null
    }

    private fun escapeForJs(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")

    private fun formatNumber(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

    private fun parseDelayToMillis(text: String): Long {
        val amount = text.trim().substringBefore(' ').toDoubleOrNull() ?: 1.0
        val unit = text.substringAfter(' ', "").trim().lowercase()
        val multiplier = when {
            unit.startsWith("мс") || unit.startsWith("миллисек") -> 1.0
            unit.startsWith("мин") -> 60_000.0
            unit.startsWith("час") -> 3_600_000.0
            else -> 1000.0 // default: секунды
        }
        return (amount * multiplier).toLong()
    }
}
