package com.sunflower.shortcut.data.repositories

import com.sunflower.shortcut.ai.AiHistoryEntry
import com.sunflower.shortcut.ai.toUi
import com.sunflower.shortcut.data.database.AIHistoryDao
import com.sunflower.shortcut.data.models.toEntity
import com.sunflower.shortcut.data.models.toEntry
import com.sunflower.shortcut.ui.components.AiHistoryStatus
import com.sunflower.shortcut.ui.components.AiHistoryUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * History of AI generations (spec §11). Hands the UI ready-to-render
 * [AiHistoryUi] (date formatting and emoji happen in AiHistoryEntry.toUi()),
 * so the screens never touch Room types.
 */
class AIHistoryRepository(private val dao: AIHistoryDao) {

    fun observeAll(): Flow<List<AiHistoryUi>> =
        dao.observeAll().map { rows -> rows.map { it.toEntry().toUi() } }

    suspend fun getById(id: String): AiHistoryUi? =
        dao.getById(id)?.toEntry()?.toUi()

    /** Returns the new entry's id, which AiAssistantScreen passes on to the result screen. */
    suspend fun add(
        prompt: String,
        jsCode: String,
        title: String,
        description: String,
        status: AiHistoryStatus
    ): String {
        val entry = AiHistoryEntry(
            id = UUID.randomUUID().toString(),
            prompt = prompt,
            jsCode = jsCode,
            title = title,
            description = description,
            status = status,
            createdAtEpochMs = System.currentTimeMillis()
        )
        dao.insert(entry.toEntity())
        return entry.id
    }

    suspend fun updateStatus(id: String, status: AiHistoryStatus) {
        dao.updateStatus(id, status.name)
    }
}
