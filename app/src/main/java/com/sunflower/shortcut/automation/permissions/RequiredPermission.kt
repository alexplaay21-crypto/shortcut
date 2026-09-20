package com.sunflower.shortcut.automation.permissions

import android.Manifest
import android.os.Build

/**
 * One capability a scenario can declare (spec §20 "Этот сценарий использует" /
 * §21 permissions screen). Deliberately smaller than the full sdk.* module
 * list — only capabilities that actually gate on an Android permission or
 * special access are represented here; modules like storage.* (app-scoped)
 * or system.* need no runtime grant.
 */
enum class RequiredPermission(val label: String, val emoji: String) {
    CAMERA("Камера", "📷"),
    LOCATION("Местоположение", "📍"),
    MICROPHONE("Микрофон", "🎙️"),
    NOTIFICATIONS("Уведомления", "🔔"),
    FILES("Файлы", "📁"),
    BLUETOOTH("Bluetooth", "🔷"),
    CONTACTS("Контакты", "👤"),
    PHONE("Телефон", "📞"),
    /** Not a runtime permission — a special access toggle, checked separately
     *  (spec §22). Kept in this enum so it still appears in requirement lists. */
    ACCESSIBILITY("Accessibility", "♿")
}

/**
 * Real Android permission strings for each capability, resolved per API level
 * where the platform changed the permission (e.g. granular media permissions
 * on API 33+, BLUETOOTH_CONNECT on API 31+). ACCESSIBILITY has no entry here —
 * it is checked via Settings.Secure, see AutomationPermissionChecker.
 */
object PermissionMapping {

    fun androidPermissions(permission: RequiredPermission): List<String> = when (permission) {
        RequiredPermission.CAMERA -> listOf(Manifest.permission.CAMERA)

        RequiredPermission.LOCATION -> listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        RequiredPermission.MICROPHONE -> listOf(Manifest.permission.RECORD_AUDIO)

        RequiredPermission.NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyList() // granted implicitly pre-API 33
        }

        RequiredPermission.FILES -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        RequiredPermission.BLUETOOTH -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            listOf(Manifest.permission.BLUETOOTH)
        }

        RequiredPermission.CONTACTS -> listOf(Manifest.permission.READ_CONTACTS)

        RequiredPermission.PHONE -> listOf(Manifest.permission.READ_PHONE_STATE)

        RequiredPermission.ACCESSIBILITY -> emptyList()
    }
}
