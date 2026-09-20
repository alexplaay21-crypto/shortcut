package com.sunflower.shortcut.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Backs `sdk.ui.UiModule` — this class's public methods are exactly the
 * contract that module already documented and forward-referenced. Node
 * search (UiNodeFinder) and node interaction (UiActions) are separate,
 * not-yet-generated files this class delegates to, matching the original
 * project tree's own split; `waitForText` stays here rather than in
 * UiNodeFinder because it's the one operation that genuinely needs the
 * service's own event stream, not just a one-shot tree query.
 *
 * Event-driven throughout (spec §17/§29: "Не выполнять постоянное
 * сканирование экрана") — `eventTypes` below is scoped to window/content/
 * click changes, not TYPES_ALL_MASK, and nothing here polls on a timer;
 * `waitForText` re-checks only when a relevant event actually arrives.
 *
 * AndroidManifest.xml (not yet generated) needs a `<service>` entry for
 * this class requiring `android.permission.BIND_ACCESSIBILITY_SERVICE`,
 * ideally with an `accessibility_service_config.xml` meta-data resource
 * for the user-facing description shown in Android's settings — the
 * `serviceInfo` block below is what actually drives runtime behavior
 * either way, so the app works correctly before that XML polish exists.
 */
class ShortcutAccessibilityService : AccessibilityService() {

    companion object {
        var instance: ShortcutAccessibilityService? = null
            private set
    }

    private val nodeFinder = UiNodeFinder()
    private val uiActions = UiActions()
    private val eventProcessor = UiEventProcessor()

    private data class PendingWait(val text: String, val continuation: CancellableContinuation<Boolean>)
    private val pendingWaits = mutableListOf<PendingWait>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_VIEW_CLICKED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        eventProcessor.onEvent(event)
        if (pendingWaits.isNotEmpty()) checkPendingWaits()
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        super.onDestroy()
        synchronized(pendingWaits) {
            pendingWaits.forEach { if (it.continuation.isActive) it.continuation.resume(false) }
            pendingWaits.clear()
        }
        if (instance === this) instance = null
    }

    // ---- public API — the exact contract sdk.ui.UiModule already expects ----

    fun findNodeByText(text: String): AccessibilityNodeInfo? = nodeFinder.findByText(rootInActiveWindow, text)

    fun findNodeById(resourceId: String): AccessibilityNodeInfo? = nodeFinder.findById(rootInActiveWindow, resourceId)

    fun findNodeByDescription(description: String): AccessibilityNodeInfo? =
        nodeFinder.findByDescription(rootInActiveWindow, description)

    fun clickNode(node: AccessibilityNodeInfo): Boolean = uiActions.click(node)

    fun readNodeText(node: AccessibilityNodeInfo): String? = uiActions.readText(node)

    fun typeText(text: String): Boolean = uiActions.typeText(rootInActiveWindow, text)

    fun scroll(direction: String): Boolean = uiActions.scroll(rootInActiveWindow, direction)

    fun performBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)

    fun performHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)

    suspend fun waitForText(text: String, timeoutMs: Long): Boolean {
        if (nodeFinder.findByText(rootInActiveWindow, text) != null) return true

        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                val pending = PendingWait(text, cont)
                synchronized(pendingWaits) { pendingWaits += pending }
                cont.invokeOnCancellation { synchronized(pendingWaits) { pendingWaits.remove(pending) } }
            }
        } ?: false
    }

    private fun checkPendingWaits() {
        val root = rootInActiveWindow ?: return
        synchronized(pendingWaits) {
            val iterator = pendingWaits.iterator()
            while (iterator.hasNext()) {
                val pending = iterator.next()
                if (nodeFinder.findByText(root, pending.text) != null) {
                    iterator.remove()
                    if (pending.continuation.isActive) pending.continuation.resume(true)
                }
            }
        }
    }
}
