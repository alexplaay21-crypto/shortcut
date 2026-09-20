package com.sunflower.shortcut.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sunflower.shortcut.ai.AiHistoryEntry
import com.sunflower.shortcut.ui.components.AiHistoryStatus

/**
 * Room row for one AI generation (spec §11 — history of created scenarios,
 * not a chat log). [status] is stored as the enum's name so adding a status
 * later never needs a type converter or a migration.
 */
@Entity(tableName = "ai_history")
data class AiHistoryEntity(
    @PrimaryKey val id: String,
    val prompt: String,
    val jsCode: String,
    val title: String,
    val description: String,
    val status: String,
    val createdAtEpochMs: Long
)

fun AiHistoryEntity.toEntry(): AiHistoryEntry = AiHistoryEntry(
    id = id,
    prompt = prompt,
    jsCode = jsCode,
    title = title,
    description = description,
    // An unknown name (a row written by a newer build) degrades to the
    // safe "not installed" state instead of crashing the history screen.
    status = AiHistoryStatus.entries.firstOrNull { it.name == status } ?: AiHistoryStatus.NOT_INSTALLED,
    createdAtEpochMs = createdAtEpochMs
)

fun AiHistoryEntry.toEntity(): AiHistoryEntity = AiHistoryEntity(
    id = id,
    prompt = prompt,
    jsCode = jsCode,
    title = title,
    description = description,
    status = status.name,
    createdAtEpochMs = createdAtEpochMs
)
