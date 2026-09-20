package com.sunflower.shortcut.data.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sunflower.shortcut.automation.logs.LogEntry

/**
 * Room row for one run (see automation.logs.LogEntry). Deliberately has no
 * foreign key to `automations`: entries stay readable after their
 * automation is deleted.
 *
 * The composite index serves the "last run of this automation" subquery in
 * AutomationDao, which runs once per row of the home list.
 */
@Entity(
    tableName = "execution_logs",
    indices = [Index(value = ["automationId", "startedAtEpochMs"])]
)
data class ExecutionLogEntity(
    @PrimaryKey val id: String,
    val automationId: String,
    val automationTitle: String,
    val startedAtEpochMs: Long,
    val triggerReason: String,
    val success: Boolean,
    val durationMs: Long,
    val errorMessage: String?,
    val errorStep: Int?
)

fun ExecutionLogEntity.toLogEntry(): LogEntry = LogEntry(
    id = id,
    automationId = automationId,
    automationTitle = automationTitle,
    startedAtEpochMs = startedAtEpochMs,
    triggerReason = triggerReason,
    success = success,
    durationMs = durationMs,
    errorMessage = errorMessage,
    errorStep = errorStep
)

fun LogEntry.toEntity(): ExecutionLogEntity = ExecutionLogEntity(
    id = id,
    automationId = automationId,
    automationTitle = automationTitle,
    startedAtEpochMs = startedAtEpochMs,
    triggerReason = triggerReason,
    success = success,
    durationMs = durationMs,
    errorMessage = errorMessage,
    errorStep = errorStep
)
