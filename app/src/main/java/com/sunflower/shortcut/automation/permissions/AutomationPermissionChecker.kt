package com.sunflower.shortcut.automation.permissions

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat

private const val ACCESSIBILITY_SERVICE_ID =
    "com.sunflower.shortcut/.accessibility.ShortcutAccessibilityService"

/**
 * Single source of truth for "is this capability usable right now". Both
 * ui.settings.PermissionsScreen (via data.repositories.AutomationRepository)
 * and ui.settings.AccessibilityStatusScreen should read status through here
 * rather than duplicating the check (spec §44: единая реализация).
 */
class AutomationPermissionChecker(private val context: Context) {

    fun isGranted(permission: RequiredPermission): Boolean {
        if (permission == RequiredPermission.ACCESSIBILITY) return isAccessibilityServiceEnabled()

        val androidPermissions = PermissionMapping.androidPermissions(permission)
        if (androidPermissions.isEmpty()) return true // nothing to grant on this API level
        return androidPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun missing(requirements: Collection<RequiredPermission>): List<RequiredPermission> =
        requirements.filterNot(::isGranted)

    fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.split(':').any { it.equals(ACCESSIBILITY_SERVICE_ID, ignoreCase = true) }
    }
}
