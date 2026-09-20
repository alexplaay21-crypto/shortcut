package com.sunflower.shortcut.automation.execution

import android.content.Context
import com.sunflower.shortcut.automation.logs.LogEntry
import com.sunflower.shortcut.automation.runtime.JsRuntime
import com.sunflower.shortcut.automation.runtime.NativeBridgeDispatcher
import com.sunflower.shortcut.automation.triggers.TriggerType
import com.sunflower.shortcut.data.repositories.LogRepository
import java.util.UUID

/**
 * One call to [execute] = one JS Runtime lifecycle (spec §28) = one log
 * entry, success or failure. Exceptions from JsRuntime are already caught
 * inside it and turned into a failed RunResult, so a crashing scenario can
 * never propagate past this class into AutomationEngine (spec §37: "Ошибка
 * одного JS не должна ломать Automation Engine").
 */
class ExecutionCoordinator(
    private val context: Context,
    private val logRepository: LogRepository
) {
    suspend fun execute(
        automationId: String,
        automationTitle: String,
        js: String,
        trigger: TriggerType,
        payloadJson: String,
        dispatcher: NativeBridgeDispatcher
    ): ExecutionResult {
        val startedAt = System.currentTimeMillis()
        val runtime = JsRuntime(context, dispatcher)

        val runResult = runCatching {
            runtime.run(script = js, triggerEvent = trigger.key, payloadJson = payloadJson)
        }.getOrElse { t ->
            com.sunflower.shortcut.automation.runtime.RunResult(
                success = false,
                errorMessage = t.message ?: "Неизвестная ошибка выполнения"
            )
        }

        val durationMs = System.currentTimeMillis() - startedAt
        val executionResult = ExecutionResult(
            automationId = automationId,
            success = runResult.success,
            startedAtEpochMs = startedAt,
            durationMs = durationMs,
            errorMessage = runResult.errorMessage
        )

        logRepository.record(
            LogEntry(
                id = UUID.randomUUID().toString(),
                automationId = automationId,
                automationTitle = automationTitle,
                startedAtEpochMs = startedAt,
                triggerReason = trigger.label,
                success = runResult.success,
                durationMs = durationMs,
                errorMessage = runResult.errorMessage
            )
        )

        return executionResult
    }
}
