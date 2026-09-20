package com.sunflower.shortcut.sdk.android

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import com.sunflower.shortcut.sdk.SdkException
import com.sunflower.shortcut.sdk.SdkModule
import com.sunflower.shortcut.sdk.jsonObjectOf
import com.sunflower.shortcut.sdk.jsonOf
import org.json.JSONArray
import java.time.Instant
import java.util.TimeZone

/**
 * `android.*` — general platform info that doesn't belong to a more
 * specific module (device identity, battery, screen state, date/time).
 * Everything here is a read-only query; nothing in this module changes
 * device state. Requires minSdk 26+ (java.time) or core-library
 * desugaring enabled in the not-yet-generated build.gradle.kts.
 */
class AndroidModule(private val context: Context) : SdkModule {

    override val name: String = "android"

    override suspend fun call(method: String, args: JSONArray): String = when (method) {
        "getInfo" -> getInfo()
        "battery" -> battery()
        "isScreenOn" -> isScreenOn()
        "now" -> now()
        "timeZone" -> jsonOf(TimeZone.getDefault().id)
        else -> throw SdkException("Метод android.$method не поддерживается")
    }

    private fun getInfo(): String = jsonObjectOf(
        "manufacturer" to Build.MANUFACTURER,
        "model" to Build.MODEL,
        "androidVersion" to Build.VERSION.RELEASE,
        "sdkInt" to Build.VERSION.SDK_INT
    ).toString()

    private fun battery(): String {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val level = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val isCharging = batteryManager?.isCharging ?: false
        return jsonObjectOf("level" to level, "isCharging" to isCharging).toString()
    }

    private fun isScreenOn(): String {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return jsonOf(powerManager?.isInteractive ?: true)
    }

    private fun now(): String {
        val epochMillis = System.currentTimeMillis()
        val iso = Instant.ofEpochMilli(epochMillis).toString()
        return jsonObjectOf("epochMillis" to epochMillis, "iso" to iso).toString()
    }
}
