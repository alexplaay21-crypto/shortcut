package com.sunflower.shortcut.sdk.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.sunflower.shortcut.sdk.SdkException
import com.sunflower.shortcut.sdk.SdkModule
import com.sunflower.shortcut.sdk.jsonOf
import com.sunflower.shortcut.sdk.objectArgOrNull
import com.sunflower.shortcut.sdk.stringArg
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * `audio.play(source, {wait, volume})` / `audio.stop()`. [source] is either
 * an http(s) URL or a path inside the app's own sandboxed storage (same
 * resolution rule as files.*). One shared MediaPlayer per module instance —
 * a second concurrent play() stops whatever is currently playing, which
 * matches how a single device speaker actually behaves; there is no
 * per-automation isolation for audio the way there is for the JS runtime.
 */
class AudioModule(private val context: Context) : SdkModule {

    override val name: String = "audio"

    @Volatile
    private var currentPlayer: MediaPlayer? = null

    override suspend fun call(method: String, args: JSONArray): String = when (method) {
        "play" -> play(args)
        "stop" -> stop()
        else -> throw SdkException("Метод audio.$method не поддерживается")
    }

    private suspend fun play(args: JSONArray): String {
        val source = args.stringArg(0, "source")
        val options = args.objectArgOrNull(1)
        val wait = options?.optBoolean("wait", true) ?: true
        val volume = (options?.optDouble("volume", 1.0) ?: 1.0).toFloat().coerceIn(0f, 1f)

        releaseCurrent()
        val player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }
        currentPlayer = player

        try {
            setSource(player, source)
            player.setVolume(volume, volume)

            if (wait) {
                suspendCancellableCoroutine<Unit> { cont ->
                    player.setOnCompletionListener { if (cont.isActive) cont.resume(Unit) }
                    player.setOnErrorListener { _, what, extra ->
                        if (cont.isActive) {
                            cont.resumeWithException(SdkException("Ошибка воспроизведения (код $what/$extra)"))
                        }
                        true
                    }
                    player.setOnPreparedListener { it.start() }
                    cont.invokeOnCancellation { runCatching { player.release() } }
                    player.prepareAsync()
                }
            } else {
                player.setOnPreparedListener { it.start() }
                player.prepareAsync()
            }
            return jsonOf(true)
        } catch (t: Throwable) {
            releaseIfCurrent(player)
            throw if (t is SdkException) t else SdkException("Не удалось воспроизвести \"$source\": ${t.message}")
        } finally {
            if (wait) releaseIfCurrent(player)
        }
    }

    private fun setSource(player: MediaPlayer, source: String) {
        if (source.startsWith("http://") || source.startsWith("https://")) {
            player.setDataSource(source)
        } else {
            player.setDataSource(resolveLocal(source).absolutePath)
        }
    }

    private fun resolveLocal(path: String): File {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        val requested = File(path)
        val target = if (requested.isAbsolute) requested else File(base, path)
        if (!target.exists()) throw SdkException("Аудиофайл \"$path\" не найден")
        return target
    }

    private fun stop(): String {
        releaseCurrent()
        return jsonOf(true)
    }

    private fun releaseCurrent() {
        currentPlayer?.let { player ->
            runCatching { if (player.isPlaying) player.stop() }
            runCatching { player.release() }
        }
        currentPlayer = null
    }

    private fun releaseIfCurrent(player: MediaPlayer) {
        if (currentPlayer === player) {
            runCatching { player.release() }
            currentPlayer = null
        }
    }
}
