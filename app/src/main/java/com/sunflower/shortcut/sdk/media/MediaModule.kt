package com.sunflower.shortcut.sdk.media

import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.view.KeyEvent
import com.sunflower.shortcut.sdk.SdkException
import com.sunflower.shortcut.sdk.SdkModule
import com.sunflower.shortcut.sdk.intArg
import com.sunflower.shortcut.sdk.jsonObjectOf
import com.sunflower.shortcut.sdk.jsonOf
import org.json.JSONArray

/**
 * `media.*` — controls whatever app currently holds the active media
 * session (the same mechanism a Bluetooth headset's buttons use), plus
 * music-stream volume. Distinct from audio.*, which plays Shortcut's own
 * sound files rather than controlling another app's playback.
 */
class MediaModule(private val context: Context) : SdkModule {

    override val name: String = "media"

    override suspend fun call(method: String, args: JSONArray): String = when (method) {
        "playPause" -> dispatchKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        "play" -> dispatchKey(KeyEvent.KEYCODE_MEDIA_PLAY)
        "pause" -> dispatchKey(KeyEvent.KEYCODE_MEDIA_PAUSE)
        "next" -> dispatchKey(KeyEvent.KEYCODE_MEDIA_NEXT)
        "previous" -> dispatchKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        "stop" -> dispatchKey(KeyEvent.KEYCODE_MEDIA_STOP)
        "getVolume" -> getVolume()
        "setVolume" -> setVolume(args)
        else -> throw SdkException("Метод media.$method не поддерживается")
    }

    private fun audioManager(): AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: throw SdkException("Аудиосистема недоступна на этом устройстве")

    private fun dispatchKey(keyCode: Int): String {
        val manager = audioManager()
        val eventTime = SystemClock.uptimeMillis()
        manager.dispatchMediaKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0))
        manager.dispatchMediaKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0))
        return jsonOf(true)
    }

    private fun getVolume(): String {
        val manager = audioManager()
        return jsonObjectOf(
            "level" to manager.getStreamVolume(AudioManager.STREAM_MUSIC),
            "max" to manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        ).toString()
    }

    private fun setVolume(args: JSONArray): String {
        val manager = audioManager()
        val max = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val level = args.intArg(0, "level").coerceIn(0, max)
        manager.setStreamVolume(AudioManager.STREAM_MUSIC, level, 0)
        return jsonOf(true)
    }
}
