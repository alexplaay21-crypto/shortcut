package com.sunflower.shortcut.permissions

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.sunflower.shortcut.automation.permissions.AutomationPermissionChecker
import com.sunflower.shortcut.automation.permissions.PermissionMapping
import com.sunflower.shortcut.automation.permissions.RequiredPermission
import com.sunflower.shortcut.utils.findActivity

/** Where one capability stands after [PermissionRequester.request]. */
enum class PermissionResult {
    GRANTED,

    /** Refused this time, but Android will show its dialog again if asked. */
    DENIED,

    /**
     * No dialog can help any more — Accessibility, or the user chose "не
     * спрашивать снова". Offer [PermissionRequester.openSettings].
     */
    NEEDS_SETTINGS
}

/**
 * Asks the user for capabilities (spec §20/§21) instead of just pointing at
 * system settings. Obtain one with [rememberPermissionRequester].
 *
 * The outcome is never trusted from the system callback alone: after the
 * dialog every capability is re-checked through AutomationPermissionChecker,
 * the same source of truth the engine and the permissions screen use.
 *
 * Note for callers: this class does not open settings by itself and does not
 * survive an Activity re-creation mid-dialog (the callback is dropped). Screens
 * should re-read status in the callback and again on ON_RESUME.
 */
class PermissionRequester internal constructor(private val context: Context) {

    private val checker = AutomationPermissionChecker(context.applicationContext)

    /** Set by [rememberPermissionRequester] once the launcher exists. */
    internal var launcher: ((Array<String>) -> Unit)? = null

    private var pending: Pending? = null

    private class Pending(
        val requested: List<RequiredPermission>,
        val onResult: (Map<RequiredPermission, PermissionResult>) -> Unit
    )

    /**
     * Shows the system dialog for whatever in [permissions] is still missing,
     * then reports every requested capability in [onResult]. Already-granted
     * ones are reported as GRANTED without any dialog. A second call while a
     * dialog is open is ignored (the dialog is modal, so it can't be a user tap).
     */
    fun request(
        permissions: Collection<RequiredPermission>,
        onResult: (Map<RequiredPermission, PermissionResult>) -> Unit
    ) {
        if (pending != null) return

        val missing = checker.missing(permissions).distinct()
        if (missing.isEmpty()) {
            onResult(permissions.associateWith { PermissionResult.GRANTED })
            return
        }

        val plan = PermissionPlanner.plan(missing, ::isAndroidPermissionGranted)
        val launch = launcher
        if (plan.runtimePermissions.isEmpty() || launch == null) {
            // Nothing a dialog can grant (only Accessibility is missing), or
            // the launcher isn't attached yet — report without asking.
            onResult(permissions.associateWith { evaluate(it, dialogWasShown = false) })
            return
        }

        pending = Pending(permissions.toList(), onResult)
        launch(plan.runtimePermissions.toTypedArray())
    }

    /** Opens the system screen where [permission] can be switched on by hand. */
    fun openSettings(permission: RequiredPermission): Boolean =
        PermissionSettings.openFor(context, permission)

    internal fun onSystemResult() {
        val current = pending ?: return
        pending = null
        current.onResult(current.requested.associateWith { evaluate(it, dialogWasShown = true) })
    }

    private fun evaluate(permission: RequiredPermission, dialogWasShown: Boolean): PermissionResult = when {
        checker.isGranted(permission) -> PermissionResult.GRANTED
        PermissionPlanner.isSpecialAccess(permission) -> PermissionResult.NEEDS_SETTINGS
        dialogWasShown && isBlocked(permission) -> PermissionResult.NEEDS_SETTINGS
        else -> PermissionResult.DENIED
    }

    /**
     * After a refusal, "no rationale to show" means Android has stopped
     * asking. Only meaningful right after a dialog — before the first
     * request the same call also returns false.
     */
    private fun isBlocked(permission: RequiredPermission): Boolean {
        val activity = context.findActivity() ?: return false
        return PermissionMapping.androidPermissions(permission).any { name ->
            !isAndroidPermissionGranted(name) &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, name)
        }
    }

    private fun isAndroidPermissionGranted(name: String): Boolean =
        ContextCompat.checkSelfPermission(context, name) == PackageManager.PERMISSION_GRANTED
}

/**
 * ```
 * val requester = rememberPermissionRequester()
 * requester.request(listOf(RequiredPermission.CAMERA)) { results ->
 *     if (results[RequiredPermission.CAMERA] == PermissionResult.NEEDS_SETTINGS) { ... }
 * }
 * ```
 */
@Composable
fun rememberPermissionRequester(): PermissionRequester {
    val context = LocalContext.current
    val requester = remember(context) { PermissionRequester(context) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        // The map is ignored on purpose — see PermissionRequester's KDoc.
        requester.onSystemResult()
    }
    SideEffect { requester.launcher = { permissions -> launcher.launch(permissions) } }
    return requester
}
