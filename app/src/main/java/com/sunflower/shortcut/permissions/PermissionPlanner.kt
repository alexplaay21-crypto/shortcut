package com.sunflower.shortcut.permissions

import com.sunflower.shortcut.automation.permissions.PermissionMapping
import com.sunflower.shortcut.automation.permissions.RequiredPermission

/**
 * What has to happen to obtain a set of capabilities, split by *how* the user
 * can grant them.
 *
 * @property runtimePermissions Android permission strings for ONE system
 *   dialog (Android shows the groups one after another itself).
 * @property specialAccess Capabilities no dialog can grant — the user has to
 *   switch them on in system settings (Accessibility, spec §22).
 */
data class PermissionRequestPlan(
    val runtimePermissions: List<String>,
    val specialAccess: List<RequiredPermission>
)

/**
 * Pure logic, no Android objects — the granted-check is passed in, so it can
 * be unit-tested. The Android permission strings come only from
 * [PermissionMapping] (spec §44: one implementation), so what is requested is
 * exactly what AutomationPermissionChecker later verifies.
 */
object PermissionPlanner {

    fun isSpecialAccess(permission: RequiredPermission): Boolean =
        permission == RequiredPermission.ACCESSIBILITY

    /** Only strings that are not granted yet are included — no point asking twice. */
    fun plan(
        needed: Collection<RequiredPermission>,
        isAndroidPermissionGranted: (String) -> Boolean
    ): PermissionRequestPlan {
        val specialAccess = needed.filter(::isSpecialAccess).distinct()
        val runtimePermissions = needed.asSequence()
            .filterNot(::isSpecialAccess)
            .flatMap { PermissionMapping.androidPermissions(it).asSequence() }
            .filterNot(isAndroidPermissionGranted)
            .distinct()
            .toList()
        return PermissionRequestPlan(runtimePermissions, specialAccess)
    }
}
