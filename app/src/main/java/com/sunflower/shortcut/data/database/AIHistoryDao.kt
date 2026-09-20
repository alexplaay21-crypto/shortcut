package com.sunflower.shortcut.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sunflower.shortcut.data.models.AiHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AIHistoryDao {

    @Query("SELECT * FROM ai_history ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<AiHistoryEntity>>

    @Query("SELECT * FROM ai_history WHERE id = :id")
    suspend fun getById(id: String): AiHistoryEntity?

    @Insert
    suspend fun insert(entry: AiHistoryEntity)

    /** [status] is an AiHistoryStatus name — stored as text, mapped by the repository. */
    @Query("UPDATE ai_history SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)
}
