package com.sunflower.shortcut.javascript.ast

/**
 * Every parsed node knows where it came from in the source — this is what
 * lets ImportJsScreen show "Строка 18: Unexpected token" instead of a bare
 * message (spec §8), and later lets the constructor (FlowBuilder, built
 * separately) point back at exact source spans for "Динамический код"
 * fallback regions (spec §19/§32).
 */
interface JsNode {
    val line: Int
    val column: Int
}

/** Root of a parsed scenario file. */
data class Program(
    val body: List<Statement>,
    override val line: Int = 1,
    override val column: Int = 1
) : JsNode
