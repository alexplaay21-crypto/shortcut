package com.sunflower.shortcut.sdk.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import androidx.core.content.ContextCompat
import com.sunflower.shortcut.sdk.SdkException
import com.sunflower.shortcut.sdk.SdkModule
import com.sunflower.shortcut.sdk.jsonObjectOf
import com.sunflower.shortcut.sdk.objectArgOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * `camera.takePhoto({camera: "front"|"back"})` — spec §15's own example.
 * Uses the platform Camera2 API directly rather than CameraX to avoid an
 * extra dependency (spec §30); the device/session/reader are opened and
 * torn down within this one call, never kept alive between runs (spec §28/
 * §29 — "камеру включать только при выполнении соответствующего действия").
 */
class CameraModule(private val context: Context) : SdkModule {

    override val name: String = "camera"

    override suspend fun call(method: String, args: JSONArray): String = when (method) {
        "takePhoto" -> takePhoto(args)
        else -> throw SdkException("Метод camera.$method не поддерживается")
    }

    private suspend fun takePhoto(args: JSONArray): String {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            throw SdkException("Нет разрешения на использование камеры")
        }

        val facing = args.objectArgOrNull(0)?.optString("camera", "back") ?: "back"
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: throw SdkException("Камера недоступна на этом устройстве")
        val cameraId = findCameraId(cameraManager, facing)
            ?: throw SdkException("Камера \"$facing\" недоступна на этом устройстве")

        val thread = HandlerThread("ShortcutCamera").apply { start() }
        val handler = Handler(thread.looper)
        var device: CameraDevice? = null
        var session: CameraCaptureSession? = null
        var reader: ImageReader? = null

        try {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val size = bestJpegSize(characteristics)
            reader = ImageReader.newInstance(size.first, size.second, ImageFormat.JPEG, 1)

            device = openCamera(cameraManager, cameraId, handler)
            session = createSession(device, reader, handler)

            val outputFile = createOutputFile()
            capture(device, session, reader, handler, characteristics, outputFile)

            return jsonObjectOf(
                "path" to outputFile.absolutePath,
                "uri" to Uri.fromFile(outputFile).toString()
            ).toString()
        } finally {
            session?.close()
            device?.close()
            reader?.close()
            thread.quitSafely()
        }
    }

    private fun findCameraId(manager: CameraManager, facing: String): String? {
        val wanted = if (facing == "front") {
            CameraCharacteristics.LENS_FACING_FRONT
        } else {
            CameraCharacteristics.LENS_FACING_BACK
        }
        return manager.cameraIdList.firstOrNull { id ->
            manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == wanted
        }
    }

    private fun bestJpegSize(characteristics: CameraCharacteristics): Pair<Int, Int> {
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val sizes = map?.getOutputSizes(ImageFormat.JPEG)
        val best = sizes?.maxByOrNull { it.width.toLong() * it.height }
        return if (best != null) best.width to best.height else 1920 to 1080
    }

    private suspend fun openCamera(manager: CameraManager, cameraId: String, handler: Handler): CameraDevice =
        suspendCancellableCoroutine { cont ->
            try {
                manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                    override fun onOpened(cameraDevice: CameraDevice) {
                        if (cont.isActive) cont.resume(cameraDevice)
                    }
                    override fun onDisconnected(cameraDevice: CameraDevice) {
                        cameraDevice.close()
                        if (cont.isActive) cont.resumeWithException(SdkException("Камера отключена"))
                    }
                    override fun onError(cameraDevice: CameraDevice, error: Int) {
                        cameraDevice.close()
                        if (cont.isActive) cont.resumeWithException(SdkException("Ошибка камеры (код $error)"))
                    }
                }, handler)
            } catch (e: SecurityException) {
                cont.resumeWithException(SdkException("Нет разрешения на использование камеры"))
            }
        }

    @Suppress("DEPRECATION")
    private suspend fun createSession(device: CameraDevice, reader: ImageReader, handler: Handler): CameraCaptureSession =
        suspendCancellableCoroutine { cont ->
            device.createCaptureSession(
                listOf(reader.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        if (cont.isActive) cont.resume(session)
                    }
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        if (cont.isActive) cont.resumeWithException(SdkException("Не удалось настроить сессию камеры"))
                    }
                },
                handler
            )
        }

    private suspend fun capture(
        device: CameraDevice,
        session: CameraCaptureSession,
        reader: ImageReader,
        handler: Handler,
        characteristics: CameraCharacteristics,
        outputFile: File
    ) {
        suspendCancellableCoroutine<Unit> { cont ->
            reader.setOnImageAvailableListener({ r ->
                val image = r.acquireLatestImage()
                try {
                    if (image != null) {
                        val buffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                        FileOutputStream(outputFile).use { it.write(bytes) }
                    }
                    if (cont.isActive) cont.resume(Unit)
                } catch (t: Throwable) {
                    if (cont.isActive) cont.resumeWithException(t)
                } finally {
                    image?.close()
                }
            }, handler)

            val orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
            val request = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(reader.surface)
                set(CaptureRequest.JPEG_ORIENTATION, orientation)
            }.build()

            session.capture(request, null, handler)
        }
    }

    private fun createOutputFile(): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
        if (!dir.exists()) dir.mkdirs()
        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(dir, "shortcut_$name.jpg")
    }
}
