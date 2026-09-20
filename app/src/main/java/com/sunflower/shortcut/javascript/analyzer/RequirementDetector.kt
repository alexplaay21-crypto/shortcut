package com.sunflower.shortcut.javascript.analyzer

import com.sunflower.shortcut.automation.permissions.RequiredPermission
import com.sunflower.shortcut.javascript.ast.Identifier
import com.sunflower.shortcut.javascript.ast.MemberExpression
import com.sunflower.shortcut.javascript.ast.walk
import com.sunflower.shortcut.javascript.validator.ValidatedScenario

/** Which sdk.* module name implies which capability — only modules that
 *  actually gate on a permission are listed (files/network/clipboard/audio-
 *  playback/system/storage need none). */
private val MODULE_TO_PERMISSION = mapOf(
    "camera" to RequiredPermission.CAMERA,
    "location" to RequiredPermission.LOCATION,
    "notifications" to RequiredPermission.NOTIFICATIONS,
    "bluetooth" to RequiredPermission.BLUETOOTH,
    "contacts" to RequiredPermission.CONTACTS,
    "phone" to RequiredPermission.PHONE,
    "ui" to RequiredPermission.ACCESSIBILITY
)

object RequirementDetector {

    fun detect(validated: ValidatedScenario): List<RequiredPermission> {
        val used = mutableSetOf<RequiredPermission>()
        validated.handler.walk { node ->
            if (node is MemberExpression && !node.computed) {
                val moduleName = (node.objectExpr as? Identifier)?.name
                MODULE_TO_PERMISSION[moduleName]?.let { used += it }
            }
        }
        return used.toList()
    }
}
