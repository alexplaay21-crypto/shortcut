package com.sunflower.shortcut.sdk.sensors

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.sunflower.shortcut.sdk.SdkException
import com.sunflower.shortcut.sdk.SdkModule
import com.sunflower.shortcut.sdk.jsonObjectOf
import com.sunflower.shortcut.sdk.objectArgOrNull
import com.sunflower.shortcut.sdk.stringArg
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import kotlin.coroutines.resume

/**
 * `sensors.read("light")` etc. — registers a listener, resolves on the
 * first event, unregisters immediately (spec §29: no continuous polling,
 * nothing kept listening between calls). `step_counter` is the one type
 * gated behind a runtime permission (ACTIVITY_RECOGNITION, API 29+) —
 * every other sensor here is a normal, ungated read.
 */
class SensorsModule(private val context: Context) : SdkModule {

    override val name: String = "sensors"

    private val sensorManager: SensorManager?
        get() = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val typeMap = mapOf(
        "accelerometer" to Sensor.TYPE_ACCELEROMETER,
        "gyroscope" to Sensor.TYPE_GYROSCOPE,
        "light" to Sensor.TYPE_LIGHT,
        "proximity" to Sensor.TYPE_PROXIMITY,
        "magnetometer" to Sensor.TYPE_MAGNETIC_FIELD,
        "pressure" to Sensor.TYPE_PRESSURE,
        "gravity" to Sensor.TYPE_GRAVITY,
        "step_counter" to Sensor.TYPE_STEP_COUNTER
    )

    override suspend fun call(method: String, args: JSONArray): String = when (method) {
        "read" -> read(args)
        "list" -> list()
        else -> throw SdkException("Метод sensors.$method не поддерживается")
    }

    private fun list(): String {
        val manager = sensorManager ?: return JSONArray().toString()
        val array = JSONArray()
        typeMap.forEach { (key, type) -> if (manager.getDefaultSensor(type) != null) array.put(key) }
        return array.toString()
    }

    private fun ensurePermissionFor(typeKey: String) {
        if (typeKey == "step_counter" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted) throw SdkException("Для датчика шагов нужно разрешение \"Физическая активность\"")
        }
    }

    private suspend fun read(args: JSONArray): String {
        val typeKey = args.stringArg(0, "type")
        ensurePermissionFor(typeKey)

        val sensorType = typeMap[typeKey] ?: throw SdkException("Неизвестный тип датчика: $typeKey")
        val manager = sensorManager ?: throw SdkException("Датчики недоступны на этом устройстве")
        val sensor = manager.getDefaultSensor(sensorType)
            ?: throw SdkException("Датчик \"$typeKey\" отсутствует на этом устройстве")
        val timeoutMs = args.objectArgOrNull(1)?.optLong("timeoutMs", 5000L) ?: 5000L

        val values = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<FloatArray> { cont ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        manager.unregisterListener(this)
                        if (cont.isActive) cont.resume(event.values.copyOf())
                    }
                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }
                cont.invokeOnCancellation { manager.unregisterListener(listener) }
                manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            }
        } ?: throw SdkException("Не удалось получить данные датчика \"$typeKey\" за отведённое время")

        val valuesArray = JSONArray()
        values.forEach { valuesArray.put(it.toDouble()) }
        return jsonObjectOf("type" to typeKey, "values" to valuesArray).toString()
    }
}
