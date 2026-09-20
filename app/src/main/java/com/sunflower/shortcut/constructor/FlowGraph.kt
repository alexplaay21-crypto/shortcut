package com.sunflower.shortcut.constructor

import com.sunflower.shortcut.ui.components.FlowStepKind
import com.sunflower.shortcut.ui.components.FlowStepUi

/**
 * A directed connection between two nodes. Today every edge is a plain
 * "next step" ([label] == null); the label is there so conditions can later
 * carry "да" / "нет" without changing the graph shape.
 */
data class FlowEdge(
    val fromId: String,
    val toId: String,
    val label: String? = null
)

/**
 * The constructor's model of one scenario (spec §18), built by [FlowBuilder].
 *
 * [nodes] are kept in display order (source order), so the list the screen
 * shows is simply the nodes as they are — no traversal needed. The graph is
 * a value: editing a scenario means patching the JS and rebuilding, never
 * mutating a graph.
 */
data class FlowGraph(
    val nodes: List<FlowNode>,
    val edges: List<FlowEdge>
) {
    private val byId: Map<String, FlowNode> = nodes.associateBy { it.id }

    init {
        require(byId.size == nodes.size) { "В графе есть узлы с одинаковым id" }
        edges.forEach { edge ->
            require(edge.fromId in byId && edge.toId in byId) {
                "Ребро ${edge.fromId} → ${edge.toId} ссылается на несуществующий узел"
            }
        }
    }

    /** The trigger node, where the scenario starts. */
    val entry: FlowNode? get() = nodes.firstOrNull { it.kind == FlowStepKind.TRIGGER }

    /** Nodes the user can change from the constructor. */
    val editableNodes: List<FlowNode> get() = nodes.filter { it.editable }

    /** Number of real actions — what AutomationUi.actionCount shows. */
    val actionCount: Int get() = nodes.count { it.kind == FlowStepKind.ACTION }

    fun node(id: String): FlowNode? = byId[id]

    /** Where to write when the user saves an edit; null if [stepId] isn't editable. */
    fun editableValueOf(stepId: String): EditableValue? = byId[stepId]?.editableValue

    /** Nodes reached directly from [id], in edge order. */
    fun successors(id: String): List<FlowNode> =
        edges.filter { it.fromId == id }.mapNotNull { byId[it.toId] }

    /** What ConstructorScreen renders (via AutomationRepository.observeSteps). */
    fun toStepUi(): List<FlowStepUi> = nodes.map { it.toStepUi() }

    companion object {
        /** For "no valid scenario yet" — the screen shows an empty list. */
        val EMPTY = FlowGraph(nodes = emptyList(), edges = emptyList())
    }
}
