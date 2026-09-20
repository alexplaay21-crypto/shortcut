package com.sunflower.shortcut.accessibility

import android.view.accessibility.AccessibilityEvent

/**
 * Processes the raw AccessibilityEvent stream the service forwards to it.
 * Right now this tracks which app is in the foreground, updated only on
 * TYPE_WINDOW_STATE_CHANGED — never polled. [addListener] exists as an
 * extension point for a future UI-based trigger (e.g. "when app X opens")
 * without speculatively wiring that into automation.triggers now — that
 * would need a new TriggerType case and a TriggerRegistry binding, both
 * real design decisions to make when that feature is actually requested,
 * not implied by adding a hook here.
 */
class UiEventProcessor {

    @Volatile
    var foregroundPackage: String? = null
        private set

    private val listeners = mutableListOf<(AccessibilityEvent) -> Unit>()

    fun addListener(listener: (AccessibilityEvent) -> Unit) {
        synchronized(listeners) { listeners += listener }
    }

    fun removeListener(listener: (AccessibilityEvent) -> Unit) {
        synchronized(listeners) { listeners -= listener }
    }

    fun onEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            event.packageName?.toString()?.let { foregroundPackage = it }
        }
        val snapshot = synchronized(listeners) { listeners.toList() }
        snapshot.forEach { it(event) }
    }
}
