package com.sunflower.shortcut.sdk.ui

import com.sunflower.shortcut.sdk.SdkException
import com.sunflower.shortcut.sdk.SdkModule
import com.sunflower.shortcut.sdk.jsonOf
import com.sunflower.shortcut.sdk.stringArg
import org.json.JSONArray

/**
 * Backs the JS-side locator pattern from JsHostScript (`ui.findText("X").click()`):
 * `click`/`exists`/`readText` all receive `(strategy, value)` as one call —
 * "find" and "act" happen together natively, since there's no live handle
 * kept between the two on the JS side.
 *
 * Expects `accessibility.ShortcutAccessibilityService` (spec §42, not yet
 * generated) to expose, once built:
 *   - `companion object { val instance: ShortcutAccessibilityService? }`
 *   - `fun findNodeByText(text: String): AccessibilityNodeInfo?`
 *   - `fun findNodeById(resourceId: String): AccessibilityNodeInfo?`
 *   - `fun findNodeByDescription(description: String): AccessibilityNodeInfo?`
 *   - `fun clickNode(node: AccessibilityNodeInfo): Boolean`
 *   - `fun readNodeText(node: AccessibilityNodeInfo): String?`
 *   - `suspend fun waitForText(text: String, timeoutMs: Long): Boolean`
 *   - `fun typeText(text: String): Boolean`
 *   - `fun scroll(direction: String): Boolean`
 *   - `fun performBack(): Boolean`
 *   - `fun performHome(): Boolean`
 * `instance` is null whenever the user hasn't enabled the service — every
 * method below turns that into the same clear SdkException rather than a
 * silent no-op (spec §22: never hide that Accessibility is required).
 */
class UiModule : SdkModule {

    override val name: String = "ui"

    override suspend fun call(method: String, args: JSONArray): String = when (method) {
        "click" -> click(args)
        "exists" -> exists(args)
        "readText" -> readText(args)
        "waitForText" -> waitForText(args)
        "type" -> type(args)
        "scroll" -> scroll(args)
        "back" -> jsonOf(service().performBack())
        "home" -> jsonOf(service().performHome())
        else -> throw SdkException("Метод ui.$method не поддерживается")
    }

    private fun service() = com.sunflower.shortcut.accessibility.ShortcutAccessibilityService.instance
        ?: throw SdkException("Accessibility Service не включён — включите его в Настройки → Accessibility")

    private fun findNode(strategy: String, value: String) = when (strategy) {
        "text" -> service().findNodeByText(value)
        "id" -> service().findNodeById(value)
        "description" -> service().findNodeByDescription(value)
        else -> throw SdkException("Неизвестная стратегия поиска элемента: $strategy")
    }

    private fun click(args: JSONArray): String {
        val strategy = args.stringArg(0, "strategy")
        val value = args.stringArg(1, "value")
        val node = findNode(strategy, value) ?: throw SdkException("Элемент \"$value\" не найден")
        return jsonOf(service().clickNode(node))
    }

    private fun exists(args: JSONArray): String {
        val strategy = args.stringArg(0, "strategy")
        val value = args.stringArg(1, "value")
        return jsonOf(findNode(strategy, value) != null)
    }

    private fun readText(args: JSONArray): String {
        val strategy = args.stringArg(0, "strategy")
        val value = args.stringArg(1, "value")
        val node = findNode(strategy, value) ?: throw SdkException("Элемент \"$value\" не найден")
        return jsonOf(service().readNodeText(node))
    }

    private suspend fun waitForText(args: JSONArray): String {
        val text = args.stringArg(0, "text")
        val timeoutMs = if (args.length() > 1 && !args.isNull(1)) args.optLong(1, 5000L) else 5000L
        return jsonOf(service().waitForText(text, timeoutMs))
    }

    private fun type(args: JSONArray): String {
        val text = args.stringArg(0, "text")
        return jsonOf(service().typeText(text))
    }

    private fun scroll(args: JSONArray): String {
        val direction = args.stringArg(0, "direction")
        return jsonOf(service().scroll(direction))
    }
}
