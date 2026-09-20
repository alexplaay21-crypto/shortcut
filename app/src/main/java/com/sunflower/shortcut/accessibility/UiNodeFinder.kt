package com.sunflower.shortcut.accessibility

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Breadth-first search over whatever `rootInActiveWindow` currently is —
 * called fresh on every find, never cached (the tree changes constantly
 * as the foreground app's UI changes, so a cached result would just be
 * wrong a moment later).
 */
class UiNodeFinder {

    fun findByText(root: AccessibilityNodeInfo?, text: String): AccessibilityNodeInfo? =
        find(root) { node -> node.text?.toString()?.contains(text, ignoreCase = true) == true }

    fun findById(root: AccessibilityNodeInfo?, resourceId: String): AccessibilityNodeInfo? =
        find(root) { node ->
            val fullId = node.viewIdResourceName
            fullId != null && (fullId == resourceId || fullId.endsWith(":id/$resourceId"))
        }

    fun findByDescription(root: AccessibilityNodeInfo?, description: String): AccessibilityNodeInfo? =
        find(root) { node -> node.contentDescription?.toString()?.contains(description, ignoreCase = true) == true }

    private fun find(
        root: AccessibilityNodeInfo?,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        if (root == null) return null
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (predicate(node)) return node
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }
}
