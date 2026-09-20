package com.sunflower.shortcut.data.repositories

import android.util.Log
import com.sunflower.shortcut.automation.logs.LogEntry
import com.sunflower.shortcut.automation.logs.LogRetentionPolicy
import com.sunflower.shortcut.data.database.ExecutionLogDao
import com.sunflower.shortcut.data.models.toEntity
import com.sunflower.shortcut.data.models.toLogEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Run history (spec §37). Persistence only — *what* to keep is decided by
 * [LogRetentionPolicy], so the same rules apply whether trimming happens
 * right after a run or from LogTrimWorker's periodic sweep.
 */
class LogRepository(private val dao: ExecutionLogDao) {

    /** Two runs finishing together must not trim the table at the same time. */
    private val trimMutex = Mutex()

    /**
     * Never throws (except cancellation): a failed log write must not turn a
     * finished run into a crash of the app-wide scope that launched it.
     */
    suspend fun record(entry: LogEntry) {
        try {
            dao.insert(entry.toEntity())
            trimToRetentionPolicy()
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Log.w(TAG, "Не удалось сохранить запись о запуске", t)
        }
    }

    fun observeRecent(limit: Int = DEFAULT_RECENT_LIMIT): Flow<List<LogEntry>> =
        dao.observeRecent(limit).map { rows -> rows.map { it.toLogEntry() } }

    /**
     * Throws on database errors — LogTrimWorker relies on that to ask
     * WorkManager for a retry.
     */
    suspend fun trimToRetentionPolicy(nowEpochMs: Long = System.currentTimeMillis()) {
        trimMutex.withLock {
            val stored = dao.getAll()
            if (stored.isEmpty()) return

            val keptIds = LogRetentionPolicy
                .apply(stored.map { it.toLogEntry() }, nowEpochMs)
                .mapTo(HashSet<String>()) { it.id }

            stored.asSequence()
                .map { it.id }
                .filter { it !in keptIds }
                .toList()
                .chunked(DELETE_CHUNK_SIZE)
                .forEach { dao.deleteByIds(it) }
        }
    }

    private companion object {
        const val TAG = "LogRepository"
        const val DEFAULT_RECENT_LIMIT = 100

        /** Stays under the 999 bound-variable limit of older SQLite builds. */
        const val DELETE_CHUNK_SIZE = 500
    }
}
