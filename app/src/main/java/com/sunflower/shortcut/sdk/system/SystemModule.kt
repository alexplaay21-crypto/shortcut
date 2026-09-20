package com.sunflower.shortcut.sdk.system

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import com.sunflower.shortcut.sdk.SdkException
import com.sunflower.shortcut.sdk.SdkModule
import com.sunflower.shortcut.sdk.intArg
import com.sunflower.shortcut.sdk.jsonOf
import org.json.JSONArray

/**
 * `system.*` — the catch-all for platform-level utilities that don't fit a
 * more specific module. `setBrightness` needs WRITE_SETTINGS, a special
 * "modify system settings" access Android only grants through its own
 * settings screen (`Settings.ACTION_MANAGE_WRITE_SETTINGS`) — same honest
 * pattern as wifi's enable/disable: check `canWrite`, fail clearly if not,
 * never pretend it silently worked. No `keepScreenOn` here — a wake lock
 * would need to outlive one execution, and JsRuntime tears everything down
 * right after the handler finishes (spec §28), so there is no session for
 * it to attach to yet.
 */
class SystemModule(private val context: Context) : SdkModule {

    override val name: String = "system"

    override suspend fun call(method: String, args: JSONArray): String = when (method) {
        "vibrate" -> vibrate(args)
        "getBrightness" -> getBrightness()
        "setBrightness" -> setBrightness(args)
        "isLocked" -> isLocked()
        else -> throw SdkException("Метод system.$method не поддерживается")
    }

    private fun vibrate(args: JSONArray): String {
        val durationMs = args.intArg(0, "durationMs").toLong().coerceIn(1, 10_000)
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: throw SdkException("Вибрация недоступна на этом устройстве")

        vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        return jsonOf(true)
    }

    private fun getBrightness(): String {
        val value = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, -1)
        return jsonOf(value)
    }

    private fun setBrightness(args: JSONArray): String {
        if (!Settings.System.canWrite(context)) {
            throw SdkException("Нет разрешения на изменение системных настроек — включите его в настройках приложения")
        }
        val level = args.intArg(0, "level").coerceIn(0, 255)
        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, level)
        return jsonOf(true)
    }

    private fun isLocked(): String {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return jsonOf(keyguardManager?.isKeyguardLocked == true)
    }
}
