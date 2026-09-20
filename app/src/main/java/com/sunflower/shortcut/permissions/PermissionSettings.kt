package com.sunflower.shortcut.permissions

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.sunflower.shortcut.automation.permissions.RequiredPermission

/**
 * The system screens where a permission can be changed by hand — the only
 * way out once Android stops showing the dialog ("не спрашивать снова"), and
 * the only way at all for Accessibility.
 *
 * Every function returns false instead of throwing if the device has no
 * activity for the intent (some OEM builds strip settings screens).
 */
object PermissionSettings {

    /** The right screen for [permission]: Accessibility's own list, app details for the rest. */
    fun openFor(context: Context, permission: RequiredPermission): Boolean =
        if (PermissionPlanner.isSpecialAccess(permission)) {
            openAccessibility(context)
        } else {
            openAppDetails(context)
        }

    fun openAppDetails(context: Context): Boolean = start(
        context,
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    )

    fun openAccessibility(context: Context): Boolean =
        start(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    private fun start(context: Context, intent: Intent): Boolean {
        // From a Service/Application context Android refuses to start an activity without this.
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}
