package com.sunflower.shortcut.sdk.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sunflower.shortcut.sdk.SdkException
import com.sunflower.shortcut.sdk.SdkModule
import com.sunflower.shortcut.sdk.intArg
import com.sunflower.shortcut.sdk.jsonOf
import com.sunflower.shortcut.sdk.objectArgOrNull
import com.sunflower.shortcut.sdk.stringArgOrNull
import org.json.JSONArray
import kotlin.random.Random

private const val CHANNEL_ID = "shortcut_automation"

/**
 * `notifications.show(text)` or `notifications.show({title, text, id})`.
 * Uses android.R.drawable.ic_dialog_info as the small icon until the app
 * has its own notification icon in res/drawable (not yet generated) —
 * swap that one constant then, nothing else here needs to change.
 */
class NotificationsModule(private val context: Context) : SdkModule {

    override val name: String = "notifications"

    override suspend fun call(method: String, args: JSONArray): String = when (method) {
        "show" -> show(args)
        "cancel" -> cancel(args)
        "cancelAll" -> cancelAll()
        else -> throw SdkException("Метод notifications.$method не поддерживается")
    }

    private fun ensurePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted) throw SdkException("Нет разрешения на показ уведомлений")
        }
    }

    private fun ensureChannel() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Автоматизации Shortcut",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Уведомления, отправленные вашими сценариями" }
            manager.createNotificationChannel(channel)
        }
    }

    private fun show(args: JSONArray): String {
        ensurePermission()
        ensureChannel()

        val firstIsText = args.length() > 0 && args.opt(0) is String
        val title: String
        val text: String
        val id: Int

        if (firstIsText) {
            title = "Shortcut"
            text = args.stringArgOrNull(0).orEmpty()
            id = Random.nextInt(1, Int.MAX_VALUE)
        } else {
            val options = args.objectArgOrNull(0) ?: throw SdkException("Ожидался текст или объект {title, text}")
            title = options.optString("title", "Shortcut")
            text = options.optString("text", "")
            id = if (options.has("id")) options.optInt("id") else Random.nextInt(1, Int.MAX_VALUE)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
            .onFailure { throw SdkException("Не удалось показать уведомление: ${it.message}") }

        return jsonOf(id)
    }

    private fun cancel(args: JSONArray): String {
        val id = args.intArg(0, "id")
        NotificationManagerCompat.from(context).cancel(id)
        return jsonOf(true)
    }

    private fun cancelAll(): String {
        NotificationManagerCompat.from(context).cancelAll()
        return jsonOf(true)
    }
}
