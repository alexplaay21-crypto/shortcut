package com.sunflower.shortcut.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The one place that turns timestamps into the short Russian labels the UI
 * shows. A new SimpleDateFormat is created per call on purpose — it is not
 * thread-safe, and these run on background dispatchers.
 */
object DateFormatter {

    private const val DAY_MS = 24L * 60 * 60 * 1000

    /** "12.09.2026" */
    fun formatDate(epochMs: Long): String =
        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(epochMs))

    /** "14:32" */
    fun formatTime(epochMs: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMs))

    /** "Сегодня, 14:32" / "Вчера, 09:10" / "12.09.2026, 09:10". */
    fun formatDayAndTime(epochMs: Long, nowEpochMs: Long = System.currentTimeMillis()): String {
        val time = formatTime(epochMs)
        // Rounded, not truncated: a calendar day is 23 or 25 hours across a DST change.
        val daysAgo = ((startOfDay(nowEpochMs) - startOfDay(epochMs)) / DAY_MS.toDouble()).roundToInt()
        return when {
            daysAgo <= 0 -> "Сегодня, $time" // includes a slightly-future timestamp from clock skew
            daysAgo == 1 -> "Вчера, $time"
            else -> "${formatDate(epochMs)}, $time"
        }
    }

    private fun startOfDay(epochMs: Long): Long = Calendar.getInstance().apply {
        timeInMillis = epochMs
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
