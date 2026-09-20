package com.sunflower.shortcut.accessibility

import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo

class UiActions {

    /** Many matched nodes (e.g. a TextView showing the label) aren't
     *  themselves clickable — the actual click target is usually a
     *  clickable ancestor container, so this walks up until it finds one. */
    fun click(node: AccessibilityNodeInfo): Boolean {
        val target = clickableSelfOrAncestor(node) ?: return false
        return target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun clickableSelfOrAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) return current
            current = current.parent
        }
        return null
    }

    fun readText(node: AccessibilityNodeInfo): String? =
        node.text?.toString()?.takeIf { it.isNotBlank() } ?: node.contentDescription?.toString()

    fun typeText(root: AccessibilityNodeInfo?, text: String): Boolean {
        val target = findFocusedEditable(root) ?: return false
        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    private fun findFocusedEditable(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (root == null) return null
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused != null && focused.isEditable) return focused
        return findFirst(root) { it.isEditable }
    }

    fun scroll(root: AccessibilityNodeInfo?, direction: String): Boolean {
        val scrollable = findFirst(root) { it.isScrollable } ?: return false
        val action = if (direction.equals("up", ignoreCase = true) || direction.equals("left", ignoreCase = true)) {
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        } else {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        }
        return scrollable.performAction(action)
    }

    private fun findFirst(root: AccessibilityNodeInfo?, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (root == null) return null
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (predicate(node)) return node
            for (i in 0 until node.childCount) node.getChild(i)?.let { queue.add(it) }
        }
        return null
    }
}
