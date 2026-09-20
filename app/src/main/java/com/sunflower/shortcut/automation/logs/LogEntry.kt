package com.sunflower.shortcut.automation.logs

/**
 * One run record. Persisted by data.repositories.LogRepository (Room) and
 * mapped for display on AutomationDetailScreen ("Последний запуск") and any
 * future full log screen. Kept intentionally flat — no automation object
 * reference — so old entries stay readable even after the automation that
 * produced them is deleted.
 */
data class LogEntry(
    val id: String,
    val automationId: String,
    val automationTitle: String,
    val startedAtEpochMs: Long,
    val triggerReason: String,
    val success: Boolean,
    val durationMs: Long,
    val errorMessage: String? = null,
    val errorStep: Int? = null
)
