package com.sunflower.shortcut.constructor

import com.sunflower.shortcut.javascript.analyzer.FlowStepExtractor
import com.sunflower.shortcut.javascript.ast.AwaitExpression
import com.sunflower.shortcut.javascript.ast.CallExpression
import com.sunflower.shortcut.javascript.ast.ExpressionStatement
import com.sunflower.shortcut.javascript.ast.Identifier
import com.sunflower.shortcut.javascript.ast.MemberExpression
import com.sunflower.shortcut.javascript.ast.NumericLiteral
import com.sunflower.shortcut.javascript.ast.Statement
import com.sunflower.shortcut.javascript.ast.StringLiteral
import com.sunflower.shortcut.javascript.parser.JsParser
import com.sunflower.shortcut.javascript.validator.ScenarioValidator
import com.sunflower.shortcut.javascript.validator.ValidatedScenario
import com.sunflower.shortcut.ui.components.FlowStepKind
import com.sunflower.shortcut.ui.components.FlowStepUi

/**
 * Builds the [FlowGraph] the Constructor screen works from (spec §18/§32,
 * last stage of the pipeline: ... → Flow Builder).
 *
 * It does NOT re-derive titles/emoji/kinds — those come from
 * [FlowStepExtractor], so the ids ("trigger", "delay_2", "action_0", "end")
 * are exactly the ones ScenarioValuePatcher resolves back to a statement,
 * and the constructor can never drift from the import/AI preview. What this
 * class adds on top of the extractor's flat list:
 *
 *  - a graph shape (nodes + edges), ready for branching later;
 *  - the source position of every statement-backed node, so the code view
 *    and "Динамический код" fallback regions can point at the exact line
 *    (spec §19/§32);
 *  - an [EditableValue] only where ScenarioValuePatcher can really patch
 *    the value — a `wait(ms)` numeric literal, or a leading string/number
 *    argument of a module call. `camera.takePhoto({camera: "front"})` or
 *    `wait(delayVar)` are NOT editable here, instead of showing a pencil
 *    that fails after "Готово".
 *
 * Nested control flow (loop / try / if bodies) is intentionally not
 * expanded: it stays one node, same as in the extractor (spec §19 — JS
 * остаётся источником истины, а не превращается в набор блоков).
 */
object FlowBuilder {

    /** Which editor a string argument deserves, by SDK call. Anything not
     *  listed falls back to plain [EditableValueType.TEXT]. */
    private val STRING_VALUE_TYPES = mapOf(
        "apps.launch" to EditableValueType.APP,
        "apps.openUrl" to EditableValueType.URL,
        "phone.call" to EditableValueType.PHONE
    )

    /**
     * Parse → validate → build in one go, for callers that only hold the
     * stored JS (e.g. AutomationRepository.observeSteps). Never throws: a
     * JsSyntaxException / ScenarioValidationException comes back as
     * [Result.failure] with its `line`/`column` intact.
     */
    fun buildFromSource(source: String): Result<FlowGraph> = runCatching {
        val program = JsParser().parse(source)
        build(ScenarioValidator().validate(program))
    }

    fun build(validated: ValidatedScenario): FlowGraph {
        val statements = validated.handler.body.body
        val nodes = FlowStepExtractor.extract(validated).map { step ->
            nodeFor(step, validated, statements)
        }
        val edges = nodes.zipWithNext { from, to -> FlowEdge(fromId = from.id, toId = to.id) }
        return FlowGraph(nodes = nodes, edges = edges)
    }

    private fun nodeFor(
        step: FlowStepUi,
        validated: ValidatedScenario,
        statements: List<Statement>
    ): FlowNode {
        val statement = statementFor(step, statements)

        // Trigger has no statement of its own — point at the handler that
        // implements it. The synthetic END node has no source at all.
        val line: Int?
        val column: Int?
        when {
            statement != null -> { line = statement.line; column = statement.column }
            step.kind == FlowStepKind.TRIGGER -> { line = validated.handler.line; column = validated.handler.column }
            else -> { line = null; column = null }
        }

        return FlowNode(
            id = step.id,
            kind = step.kind,
            emoji = step.emoji,
            title = step.title,
            subtitle = step.subtitle,
            line = line,
            column = column,
            editableValue = statement?.let { editableValueFor(step, it) }
        )
    }

    /** Same "kind_index" scheme ScenarioValuePatcher uses; "trigger"/"end"
     *  have no numeric suffix and correctly resolve to null. */
    private fun statementFor(step: FlowStepUi, statements: List<Statement>): Statement? {
        val index = step.id.substringAfter('_', "").toIntOrNull() ?: return null
        return statements.getOrNull(index)
    }

    private fun editableValueFor(step: FlowStepUi, statement: Statement): EditableValue? = when (step.kind) {
        FlowStepKind.DELAY -> delayValue(step, statement)
        FlowStepKind.ACTION -> actionValue(statement)
        else -> null
    }

    private fun delayValue(step: FlowStepUi, statement: Statement): EditableValue? {
        val argument = callOf(statement)?.arguments?.getOrNull(0) as? NumericLiteral ?: return null
        return EditableValue(
            type = EditableValueType.DELAY,
            current = step.subtitle,
            line = argument.line,
            column = argument.column
        )
    }

    private fun actionValue(statement: Statement): EditableValue? {
        val call = callOf(statement) ?: return null
        return when (val argument = call.arguments.getOrNull(0)) {
            is StringLiteral -> EditableValue(
                type = STRING_VALUE_TYPES[calleeKey(call)] ?: EditableValueType.TEXT,
                current = argument.value,
                line = argument.line,
                column = argument.column
            )
            is NumericLiteral -> EditableValue(
                type = EditableValueType.NUMBER,
                current = argument.raw,
                line = argument.line,
                column = argument.column
            )
            else -> null
        }
    }

    /** `await sdk.call(...)` and `sdk.call(...)` both resolve to the call. */
    private fun callOf(statement: Statement): CallExpression? {
        val expression = (statement as? ExpressionStatement)?.expression ?: return null
        val unwrapped = if (expression is AwaitExpression) expression.argument else expression
        return unwrapped as? CallExpression
    }

    /** "apps.launch"-style key, or null for anything but `module.method(...)`. */
    private fun calleeKey(call: CallExpression): String? {
        val callee = call.callee as? MemberExpression ?: return null
        if (callee.computed) return null
        val moduleName = (callee.objectExpr as? Identifier)?.name ?: return null
        val methodName = callee.propertyName ?: return null
        return "$moduleName.$methodName"
    }
}
