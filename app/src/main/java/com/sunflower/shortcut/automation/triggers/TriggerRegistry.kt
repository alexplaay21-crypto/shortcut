package com.sunflower.shortcut.automation.triggers

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.ContextCompat
import com.sunflower.shortcut.android.services.TriggerMonitorService

/**
 * Owns the dynamic (context-registered) listeners for triggers that cannot be
 * declared in the manifest on modern Android (implicit-broadcast limits since
 * API 26 — spec §28/§29: event-driven, never polling). Keeping the process
 * alive long enough for these to actually fire in the background is
 * android.services.TriggerMonitorService's job — refreshListeners() below
 * starts/stops it based on whether any binding currently needs one, so it's
 * never a permanent Foreground Service (spec §29), only a conditional one.
 * BOOT_COMPLETED is intentionally not handled here — that one *can* be
 * manifest-declared and belongs in android.receivers.BootCompletedReceiver.
 */
class TriggerRegistry(private val context: Context) {

    private var listener: TriggerListener? = null
    private val bindings = mutableMapOf<String, TriggerBinding>()

    private var broadcastReceiver: BroadcastReceiver? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var lastWifiConnected: Boolean? = null

    fun setListener(listener: TriggerListener) {
        this.listener = listener
    }

    /** Call once at startup with every enabled automation's trigger. */
    fun registerAll(initial: Collection<TriggerBinding>) {
        initial.forEach { bindings[it.automationId] = it }
        refreshListeners()
    }

    fun register(binding: TriggerBinding) {
        bindings[binding.automationId] = binding
        refreshListeners()
    }

    fun unregister(automationId: String) {
        bindings.remove(automationId)
        refreshListeners()
    }

    fun stopAll() {
        bindings.clear()
        refreshListeners()
    }

    private fun bindingsFor(vararg triggers: TriggerType): List<TriggerBinding> =
        bindings.values.filter { it.trigger in triggers }

    private fun refreshListeners() {
        val needsBroadcast = bindingsFor(
            TriggerType.CHARGING_CONNECTED,
            TriggerType.CHARGING_DISCONNECTED,
            TriggerType.BATTERY_LOW,
            TriggerType.BLUETOOTH_CONNECTED,
            TriggerType.BLUETOOTH_DISCONNECTED
        ).isNotEmpty()
        val needsWifi = bindingsFor(TriggerType.WIFI_CONNECTED, TriggerType.WIFI_DISCONNECTED).isNotEmpty()

        if (needsBroadcast && broadcastReceiver == null) startBroadcastReceiver()
        if (!needsBroadcast && broadcastReceiver != null) stopBroadcastReceiver()

        if (needsWifi && networkCallback == null) startWifiCallback()
        if (!needsWifi && networkCallback != null) stopWifiCallback()

        // Idempotent either way: starting an already-started service just
        // redelivers onStartCommand, stopping an already-stopped one is a no-op.
        if (needsBroadcast || needsWifi) {
            TriggerMonitorService.start(context)
        } else {
            TriggerMonitorService.stop(context)
        }
    }

    private fun startBroadcastReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val trigger = when (intent.action) {
                    Intent.ACTION_POWER_CONNECTED -> TriggerType.CHARGING_CONNECTED
                    Intent.ACTION_POWER_DISCONNECTED -> TriggerType.CHARGING_DISCONNECTED
                    Intent.ACTION_BATTERY_LOW -> TriggerType.BATTERY_LOW
                    BluetoothDevice.ACTION_ACL_CONNECTED -> TriggerType.BLUETOOTH_CONNECTED
                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> TriggerType.BLUETOOTH_DISCONNECTED
                    else -> null
                } ?: return

                val deviceName = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    ?.let { runCatching { it.name }.getOrNull() }
                val payload = """{"device":${deviceName?.let { "\"$it\"" } ?: "null"}}"""

                bindingsFor(trigger).forEach { binding ->
                    listener?.onTriggerFired(binding.automationId, trigger, payload)
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        runCatching {
            ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
        }.onSuccess {
            broadcastReceiver = receiver
        }.onFailure {
            android.util.Log.e("TriggerRegistry", "Не удалось зарегистрировать receiver", it)
        }
    }

    private fun stopBroadcastReceiver() {
        broadcastReceiver?.let { runCatching { context.unregisterReceiver(it) } }
        broadcastReceiver = null
    }

    private fun startWifiCallback() {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java) ?: return
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (lastWifiConnected == true) return
                lastWifiConnected = true
                bindingsFor(TriggerType.WIFI_CONNECTED).forEach {
                    listener?.onTriggerFired(it.automationId, TriggerType.WIFI_CONNECTED, "{}")
                }
            }

            override fun onLost(network: Network) {
                if (lastWifiConnected == false) return
                lastWifiConnected = false
                bindingsFor(TriggerType.WIFI_DISCONNECTED).forEach {
                    listener?.onTriggerFired(it.automationId, TriggerType.WIFI_DISCONNECTED, "{}")
                }
            }
        }

        runCatching { connectivityManager.registerNetworkCallback(request, callback) }
            .onSuccess { networkCallback = callback }
    }

    private fun stopWifiCallback() {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        networkCallback?.let { cb -> runCatching { connectivityManager?.unregisterNetworkCallback(cb) } }
        networkCallback = null
        lastWifiConnected = null
    }
}
