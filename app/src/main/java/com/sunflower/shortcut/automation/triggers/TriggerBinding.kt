package com.sunflower.shortcut.automation.triggers

data class TriggerBinding(
    val automationId: String,
    val trigger: TriggerType
)

/** Fired when a system event matching a registered trigger occurs. */
fun interface TriggerListener {
    fun onTriggerFired(automationId: String, trigger: TriggerType, payloadJson: String)
}
