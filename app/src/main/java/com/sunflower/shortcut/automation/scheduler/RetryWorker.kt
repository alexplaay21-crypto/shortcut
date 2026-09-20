package com.sunflower.shortcut.automation.scheduler

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sunflower.shortcut.automation.engine.AutomationEngine

class RetryWorker(
    appContext: Context,
    params: WorkerParameters,
    private val automationEngine: AutomationEngine
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val automationId = inputData.getString(KEY_AUTOMATION_ID) ?: return Result.failure()
        val outcome = automationEngine.retryAutomation(automationId)
        return if (outcome.isSuccess) Result.success() else Result.retry()
    }
}
