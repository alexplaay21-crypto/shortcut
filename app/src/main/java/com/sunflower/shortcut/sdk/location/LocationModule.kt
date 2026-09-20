package com.sunflower.shortcut.sdk.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.sunflower.shortcut.sdk.SdkException
import com.sunflower.shortcut.sdk.SdkModule
import com.sunflower.shortcut.sdk.jsonObjectOf
import com.sunflower.shortcut.sdk.objectArgOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import kotlin.coroutines.resume

/**
 * `const loc = await location.current()` (spec §15). Uses the platform
 * LocationManager directly rather than Play Services' FusedLocationProvider
 * — no extra dependency, and it works identically on non-GMS devices
 * (spec §30's minimal-dependency rule applies here the same way it did to
 * choosing Camera2 over CameraX).
 */
class LocationModule(private val context: Context) : SdkModule {

    override val name: String = "location"

    override suspend fun call(method: String, args: JSONArray): String = when (method) {
        "current" -> current(args)
        else -> throw SdkException("Метод location.$method не поддерживается")
    }

    @SuppressLint("MissingPermission") // checked explicitly below before any location API call
    private suspend fun current(args: JSONArray): String {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) throw SdkException("Нет разрешения на использование местоположения")

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: throw SdkException("Служба геолокации недоступна на этом устройстве")

        val timeoutMs = args.objectArgOrNull(0)?.optLong("timeoutMs", 10_000L) ?: 10_000L
        val provider = bestProvider(locationManager, hasFine)
            ?: throw SdkException("Нет доступного источника геолокации — включите GPS или геолокацию по сети")

        val location = withTimeoutOrNull(timeoutMs) { requestSingleUpdate(locationManager, provider) }
            ?: lastKnown(locationManager, hasFine)
            ?: throw SdkException("Не удалось определить местоположение")

        return jsonObjectOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "accuracy" to location.accuracy,
            "timestamp" to location.time
        ).toString()
    }

    private fun bestProvider(manager: LocationManager, hasFine: Boolean): String? {
        val candidates = buildList {
            if (hasFine) add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
        }
        return candidates.firstOrNull { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
    }

    private fun lastKnown(manager: LocationManager, hasFine: Boolean): Location? {
        val providers = if (hasFine) {
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        } else {
            listOf(LocationManager.NETWORK_PROVIDER)
        }
        return providers
            .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleUpdate(manager: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { cont ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    manager.removeUpdates(this)
                    if (cont.isActive) cont.resume(location)
                }
                @Deprecated("Deprecated in Java", ReplaceWith(""))
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                override fun onProviderEnabled(provider: String) = Unit
                override fun onProviderDisabled(provider: String) = Unit
            }
            cont.invokeOnCancellation { manager.removeUpdates(listener) }
            manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        }
}
