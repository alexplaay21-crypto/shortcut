package com.sunflower.shortcut.sdk.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.sunflower.shortcut.sdk.SdkException
import com.sunflower.shortcut.sdk.SdkModule
import com.sunflower.shortcut.sdk.jsonObjectOf
import com.sunflower.shortcut.sdk.jsonOf
import com.sunflower.shortcut.sdk.stringArg
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import kotlin.coroutines.resume

/**
 * `bluetooth.*` — adapter state + paired-device queries. Per-device
 * connection checks (`isDeviceConnected`) go through the documented
 * BluetoothProfile.ServiceListener proxy (A2DP + HEADSET profiles), not
 * reflection on the hidden BluetoothDevice.isConnected() — slower, but
 * stays on supported public API, matching spec §20's own "no reflection"
 * rule applied to our own native code too, not just user scripts.
 */
class BluetoothModule(private val context: Context) : SdkModule {

    override val name: String = "bluetooth"

    override suspend fun call(method: String, args: JSONArray): String = when (method) {
        "isEnabled" -> jsonOf(adapter()?.isEnabled == true)
        "getName" -> jsonOf(adapter()?.name)
        "getPairedDevices" -> getPairedDevices()
        "isDeviceConnected" -> isDeviceConnected(args)
        else -> throw SdkException("Метод bluetooth.$method не поддерживается")
    }

    private fun ensurePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted) throw SdkException("Нет разрешения на использование Bluetooth")
        }
    }

    private fun adapter(): BluetoothAdapter? {
        ensurePermission()
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return manager?.adapter
    }

    private fun getPairedDevices(): String {
        val bonded = adapter()?.bondedDevices.orEmpty()
        val array = JSONArray()
        bonded.forEach { device -> array.put(jsonObjectOf("name" to device.name, "address" to device.address)) }
        return array.toString()
    }

    private suspend fun isDeviceConnected(args: JSONArray): String {
        val nameOrAddress = args.stringArg(0, "device")
        val bonded = adapter()?.bondedDevices.orEmpty()
        val device = bonded.firstOrNull {
            it.name?.equals(nameOrAddress, ignoreCase = true) == true ||
                it.address.equals(nameOrAddress, ignoreCase = true)
        } ?: throw SdkException("Устройство \"$nameOrAddress\" не найдено среди сопряжённых")

        val connected = listOf(BluetoothProfile.A2DP, BluetoothProfile.HEADSET)
            .any { profile -> isConnectedOnProfile(device, profile) }
        return jsonOf(connected)
    }

    private suspend fun isConnectedOnProfile(device: BluetoothDevice, profile: Int): Boolean {
        val bluetoothAdapter = adapter() ?: return false
        return suspendCancellableCoroutine { cont ->
            val started = bluetoothAdapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profileId: Int, proxy: BluetoothProfile) {
                    val connected = runCatching { proxy.connectedDevices.any { it.address == device.address } }
                        .getOrDefault(false)
                    bluetoothAdapter.closeProfileProxy(profileId, proxy)
                    if (cont.isActive) cont.resume(connected)
                }
                override fun onServiceDisconnected(profileId: Int) {
                    if (cont.isActive) cont.resume(false)
                }
            }, profile)
            if (!started && cont.isActive) cont.resume(false)
        }
    }
}
