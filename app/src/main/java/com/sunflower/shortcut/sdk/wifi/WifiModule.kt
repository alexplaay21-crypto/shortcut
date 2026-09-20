package com.sunflower.shortcut.sdk.wifi

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.sunflower.shortcut.sdk.SdkException
import com.sunflower.shortcut.sdk.SdkModule
import com.sunflower.shortcut.sdk.jsonOf
import org.json.JSONArray

/**
 * `wifi.*` — state via WifiManager/ConnectivityManager. There is no
 * `enable()`/`disable()`: WifiManager.setWifiEnabled() has been a
 * documented no-op for regular apps since API 29, so [openSettings]
 * surfaces the quick Wi-Fi panel for the user to flip it themselves,
 * rather than pretending a silent toggle exists.
 */
class WifiModule(private val context: Context) : SdkModule {

    override val name: String = "wifi"

    override suspend fun call(method: String, args: JSONArray): String = when (method) {
        "isEnabled" -> isEnabled()
        "isConnected" -> isConnected()
        "getSSID" -> getSSID()
        "openSettings" -> openSettings()
        else -> throw SdkException("Метод wifi.$method не поддерживается")
    }

    private fun wifiManager(): WifiManager? =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private fun isEnabled(): String = jsonOf(wifiManager()?.isWifiEnabled == true)

    private fun isConnected(): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return jsonOf(false)
        val network = connectivityManager.activeNetwork ?: return jsonOf(false)
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return jsonOf(false)
        return jsonOf(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
    }

    /** SSID has required location permission since Android tied it to
     *  physical-location inference — this is a platform rule, not ours. */
    @Suppress("DEPRECATION")
    private fun getSSID(): String {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) throw SdkException("Для получения имени сети (SSID) нужно разрешение на местоположение")

        val manager = wifiManager() ?: throw SdkException("Wi-Fi недоступен на этом устройстве")
        val ssid = manager.connectionInfo?.ssid?.trim('"')
        if (ssid.isNullOrBlank() || ssid == "<unknown ssid>") {
            throw SdkException("Не удалось определить SSID — устройство не подключено к Wi-Fi")
        }
        return jsonOf(ssid)
    }

    private fun openSettings(): String {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_WIFI)
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS)
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return jsonOf(true)
    }
}
