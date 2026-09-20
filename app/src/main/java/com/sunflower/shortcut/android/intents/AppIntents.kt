package com.sunflower.shortcut.android.intents

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.sunflower.shortcut.MainActivity

/**
 * Centralizes the two PendingIntents the app currently needs — opening
 * MainActivity from a notification tap, and the "Отключить" action on
 * TriggerMonitorService's persistent notification. Deliberately small: not
 * a general intent-building framework, just the app's own internal signals
 * kept in one place instead of scattered across callers.
 */
object AppIntents {

    const val ACTION_STOP_MONITORING = "com.sunflower.shortcut.action.STOP_MONITORING"

    fun openApp(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(context, REQUEST_OPEN_APP, intent, pendingIntentFlags())
    }

    fun stopMonitoring(context: Context): PendingIntent {
        val intent = Intent(ACTION_STOP_MONITORING).setPackage(context.packageName)
        return PendingIntent.getBroadcast(context, REQUEST_STOP_MONITORING, intent, pendingIntentFlags())
    }

    private fun pendingIntentFlags(): Int {
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags = flags or PendingIntent.FLAG_IMMUTABLE
        return flags
    }

    private const val REQUEST_OPEN_APP = 1001
    private const val REQUEST_STOP_MONITORING = 1002
}
