package com.sunflower.shortcut.android.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.sunflower.shortcut.android.intents.AppIntents

private const val CHANNEL_ID = "shortcut_monitor"
private const val NOTIFICATION_ID = 4200

/**
 * The honest answer to "how do background triggers (charging/battery/
 * bluetooth/wifi) fire when the app isn't open" without ever being a
 * permanent Foreground Service (spec §29): automation.triggers.
 * TriggerRegistry starts this only while at least one enabled automation
 * actually needs a system-broadcast trigger, and stops it the moment none
 * do (see TriggerRegistry.refreshListeners()). The notification is
 * required by Android for any foreground service — low importance, no
 * sound, with its own "Отключить" action so the user can stop background
 * monitoring without hunting through settings.
 *
 * Requires in AndroidManifest.xml (not yet generated):
 * ```
 * <service android:name=".android.services.TriggerMonitorService"
 *          android:foregroundServiceType="dataSync"
 *          android:exported="false"/>
 * ```
 */
class TriggerMonitorService : Service() {

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AppIntents.ACTION_STOP_MONITORING) stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        ContextCompat.registerReceiver(
            this,
            stopReceiver,
            IntentFilter(AppIntents.ACTION_STOP_MONITORING),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        runCatching { unregisterReceiver(stopReceiver) }
        super.onDestroy()
    }

    private fun ensureChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Мониторинг автоматизаций",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Позволяет сценариям срабатывать по событиям устройства в фоне"
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle("Shortcut следит за событиями")
            .setContentText("Автоматизации по зарядке, Bluetooth и Wi-Fi готовы сработать")
            .setContentIntent(AppIntents.openApp(this))
            .addAction(0, "Отключить", AppIntents.stopMonitoring(this))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    companion object {
        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, TriggerMonitorService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, TriggerMonitorService::class.java))
        }
    }
}
