package com.sunflower.shortcut.android.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sunflower.shortcut.ShortcutApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ACTION_BOOT_COMPLETED (and ACTION_MY_PACKAGE_REPLACED, for the same
 * reason — the process is gone either way) are the two implicit broadcasts
 * exempt from Android's post-O context-registration-only restriction, so
 * they're the only triggers that belong in the manifest rather than in
 * automation.triggers.TriggerRegistry's dynamic registration.
 *
 * Spec §40: only restores AutomationEngine's trigger listeners — no heavy
 * UI, no AI, no JS Runtime kept alive. `goAsync()` keeps the process alive
 * just long enough for that one DB read + receiver registration to finish;
 * `pendingResult.finish()` releases it immediately after.
 *
 * Requires in AndroidManifest.xml (not yet generated):
 * ```
 * <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
 * <receiver android:name=".android.receivers.BootCompletedReceiver" android:exported="true">
 *     <intent-filter>
 *         <action android:name="android.intent.action.BOOT_COMPLETED"/>
 *         <action android:name="android.intent.action.MY_PACKAGE_REPLACED"/>
 *     </intent-filter>
 * </receiver>
 * ```
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val app = context.applicationContext as? ShortcutApplication ?: return
        val pendingResult = goAsync()

        app.applicationScope.launch {
            try {
                // Reboot specifically respects the user's "Запускать при
                // включении телефона" toggle (spec §21); an app self-update
                // always restores triggers regardless — the process was
                // running before the update, so the user already had them active.
                val shouldRestore = if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
                    true
                } else {
                    app.settingsDataStore.settingsFlow.first()?.launchOnBoot == true
                }
                if (shouldRestore) {
                    app.automationEngine.start()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
