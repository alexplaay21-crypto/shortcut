package com.sunflower.shortcut.android.system

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings

/**
 * Opens the general battery-optimization settings *list* rather than firing
 * `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` directly — that direct-
 * request intent is restricted by Play Store policy to a narrow set of
 * approved use cases, and the settings-list path works for every app
 * without needing to claim one of those exemptions.
 */
object BatteryOptimizationHelper {

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun openBatteryOptimizationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
