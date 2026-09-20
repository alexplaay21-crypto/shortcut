package com.sunflower.shortcut.automation.execution

data class ExecutionResult(
    val automationId: String,
    val success: Boolean,
    val startedAtEpochMs: Long,
    val durationMs: Long,
    val errorMessage: String? = null,
    val errorStep: Int? = null
)
