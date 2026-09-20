package com.sunflower.shortcut.constructor

import com.sunflower.shortcut.ui.components.FlowStepKind
import com.sunflower.shortcut.ui.components.FlowStepUi

/**
 * One block of the constructor (spec §18): the trigger, a step of the
 * handler, or the synthetic end. Built by [FlowBuilder].
 *
 * @property id same scheme FlowStepExtractor and ScenarioValuePatcher use —
 *   "trigger", "delay_2", "action_0", "end" — so it identifies the same
 *   statement everywhere.
 * @property subtitle short display text for the card. It may join several
 *   arguments; use [editableValue] (not this) to prefill an editor.
 * @property line 1-based source line this node points at, or null for the
 *   synthetic end node. Used to jump to the code and to mark
 *   "Динамический код" regions (spec §19/§32).
 * @property column 1-based source column; null exactly when [line] is null.
 * @property editableValue what can be changed from the constructor, or null
 *   if this node is not editable.
 */
data class FlowNode(
    val id: String,
    val kind: FlowStepKind,
    val emoji: String,
    val title: String,
    val subtitle: String,
    val line: Int?,
    val column: Int?,
    val editableValue: EditableValue?
) {
    init {
        require((line == null) == (column == null)) {
            "Узел \"$id\": line и column должны быть заданы вместе"
        }
    }

    val editable: Boolean get() = editableValue != null

    val hasSource: Boolean get() = line != null

    /** Projection for the Compose layer, which stays independent of the
     *  constructor package (see FlowStepUi). */
    fun toStepUi(): FlowStepUi = FlowStepUi(
        id = id,
        emoji = emoji,
        title = title,
        subtitle = subtitle,
        kind = kind,
        editable = editable
    )
}
