package com.sunflower.shortcut.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sunflower.shortcut.data.models.ExecutionLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExecutionLogDao {

    @Insert
    suspend fun insert(entry: ExecutionLogEntity)

    @Query("SELECT * FROM execution_logs ORDER BY startedAtEpochMs DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ExecutionLogEntity>>

    /**
     * Everything stored, for LogRetentionPolicy to decide what to keep. The
     * policy caps the table at a few hundred rows, so loading it is cheap.
     */
    @Query("SELECT * FROM execution_logs")
    suspend fun getAll(): List<ExecutionLogEntity>

    /**
     * Callers must pass at most ~500 ids per call: older Android SQLite
     * builds allow only 999 bound variables per statement.
     */
    @Query("DELETE FROM execution_logs WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}
