package com.sunflower.shortcut.sdk.phone

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.sunflower.shortcut.sdk.SdkException
import com.sunflower.shortcut.sdk.SdkModule
import com.sunflower.shortcut.sdk.jsonOf
import com.sunflower.shortcut.sdk.stringArg
import org.json.JSONArray

/**
 * `phone.call(number)` places the call directly (spec §7's install warning
 * + the CALL_PHONE runtime grant are exactly the two gates meant to cover
 * something this sensitive) — `phone.dial(number)` is the gentler variant
 * that just opens the dialer pre-filled, no permission needed, for scripts
 * that would rather not auto-dial.
 */
class PhoneModule(private val context: Context) : SdkModule {

    override val name: String = "phone"

    override suspend fun call(method: String, args: JSONArray): String = when (method) {
        "call" -> placeCall(args)
        "dial" -> dial(args)
        "getState" -> getState()
        else -> throw SdkException("Метод phone.$method не поддерживается")
    }

    private fun placeCall(args: JSONArray): String {
        val number = args.stringArg(0, "number")
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) throw SdkException("Нет разрешения на совершение звонков")

        val intent = Intent(Intent.ACTION_CALL, Uri.fromParts("tel", number, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure { throw SdkException("Не удалось совершить звонок: ${it.message}") }
        return jsonOf(true)
    }

    private fun dial(args: JSONArray): String {
        val number = args.stringArg(0, "number")
        val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure { throw SdkException("Не удалось открыть набор номера") }
        return jsonOf(true)
    }

    @Suppress("DEPRECATION")
    private fun getState(): String {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) throw SdkException("Нет разрешения на чтение состояния телефона")

        val manager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            ?: throw SdkException("Телефония недоступна на этом устройстве")
        val state = when (manager.callState) {
            TelephonyManager.CALL_STATE_RINGING -> "ringing"
            TelephonyManager.CALL_STATE_OFFHOOK -> "offhook"
            else -> "idle"
        }
        return jsonOf(state)
    }
}
