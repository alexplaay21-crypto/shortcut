package com.sunflower.shortcut.data.database

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import com.sunflower.shortcut.data.models.Automation
import kotlinx.coroutines.flow.Flow

/**
 * An automation plus the outcome of its most recent run, resolved in SQL.
 *
 * AutomationRepository only receives this DAO (see ShortcutApplication), yet
 * HomeScreen / AutomationDetailScreen show "Последний запуск" — so the last
 * run is read straight from `execution_logs` here. Because both tables are in
 * the query, Room re-emits the Flow whenever either one changes, and the
 * label updates right after a run is logged.
 */
data class AutomationWithLastRun(
    @Embedded val automation: Automation,
    val lastRunAtEpochMs: Long?,
    val lastRunSuccess: Boolean?
)

/** Minimal row for TriggerRegistry — no need to load the JS of every script at startup. */
data class AutomationTriggerRow(
    val automationId: String,
    val triggerKey: String
)

private const val SELECT_WITH_LAST_RUN = """
    SELECT a.*,
        (SELECT l.startedAtEpochMs FROM execution_logs l
            WHERE l.automationId = a.id
            ORDER BY l.startedAtEpochMs DESC LIMIT 1) AS lastRunAtEpochMs,
        (SELECT l.success FROM execution_logs l
            WHERE l.automationId = a.id
            ORDER BY l.startedAtEpochMs DESC LIMIT 1) AS lastRunSuccess
    FROM automations a
"""

@Dao
interface AutomationDao {

    @Query("$SELECT_WITH_LAST_RUN ORDER BY a.createdAtEpochMs DESC")
    fun observeAll(): Flow<List<AutomationWithLastRun>>

    @Query("$SELECT_WITH_LAST_RUN WHERE a.id = :id")
    fun observeById(id: String): Flow<AutomationWithLastRun?>

    @Query("SELECT * FROM automations WHERE id = :id")
    suspend fun getByIdOnce(id: String): Automation?

    /** For the permissions overview, which needs every script's code. */
    @Query("SELECT * FROM automations ORDER BY createdAtEpochMs DESC")
    suspend fun getAllOnce(): List<Automation>

    @Query("SELECT jsCode FROM automations WHERE id = :id")
    suspend fun getCode(id: String): String?

    /**
     * Every installed automation that has a trigger — deliberately NOT only
     * the enabled ones. HomeScreen/DetailScreen flip `enabled` through the
     * repository without re-registering listeners, and
     * AutomationEngine.runAutomation already refuses disabled scenarios, so
     * the enabled flag is enforced at run time instead.
     */
    @Query("SELECT id AS automationId, triggerKey FROM automations WHERE triggerKey IS NOT NULL")
    suspend fun getTriggerRows(): List<AutomationTriggerRow>

    @Insert
    suspend fun insert(automation: Automation)

    @Query("UPDATE automations SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)

    /** Used when the constructor patches a value in the stored JS. */
    @Query("UPDATE automations SET jsCode = :jsCode, updatedAtEpochMs = :updatedAtEpochMs WHERE id = :id")
    suspend fun updateCode(id: String, jsCode: String, updatedAtEpochMs: Long)

    /** Run logs are kept on purpose (LogEntry stays readable after deletion). */
    @Query("DELETE FROM automations WHERE id = :id")
    suspend fun delete(id: String)
}
