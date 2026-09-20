package com.sunflower.shortcut.ai

import com.sunflower.shortcut.ui.components.AiHistoryStatus
import com.sunflower.shortcut.ui.components.AiHistoryUi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Domain model for one AI generation (spec §11: "хранить историю созданных
 * сценариев", explicitly not a full chat log). Kept separate from
 * ui.components.AiHistoryUi the same way automation.logs.LogEntry is kept
 * separate from its own presentation — this is what
 * data.repositories.AIHistoryRepository (not yet generated) persists;
 * [toUi] is how a stored entry becomes what AiHistoryScreen/AiResultScreen
 * actually render, formatting the raw timestamp and picking an emoji only
 * at the presentation boundary.
 */
data class AiHistoryEntry(
    val id: String,
    val prompt: String,
    val jsCode: String,
    val title: String,
    val description: String,
    val status: AiHistoryStatus,
    val createdAtEpochMs: Long
)

fun AiHistoryEntry.toUi(): AiHistoryUi = AiHistoryUi(
    id = id,
    emoji = emojiFor(title),
    title = title,
    dateLabel = formatDate(createdAtEpochMs),
    description = description,
    prompt = prompt,
    jsCode = jsCode,
    status = status
)

private fun formatDate(epochMs: Long): String =
    SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(epochMs))

/** No stored icon field — picked from the title the same lightweight way
 *  javascript.analyzer.FlowStepExtractor picks trigger emoji, so a history
 *  entry visually matches what the constructor already shows for it. */
private fun emojiFor(title: String): String = when {
    title.contains("зарядк", ignoreCase = true) -> "🔌"
    title.contains("батаре", ignoreCase = true) -> "🪫"
    title.contains("wi-fi", ignoreCase = true) || title.contains("wifi", ignoreCase = true) -> "📶"
    title.contains("bluetooth", ignoreCase = true) -> "🔷"
    title.contains("включен", ignoreCase = true) -> "🔁"
    else -> "✨"
}
