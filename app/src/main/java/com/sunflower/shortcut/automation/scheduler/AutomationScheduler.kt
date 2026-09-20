package com.sunflower.shortcut.automation.scheduler

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

private const val LOG_TRIM_WORK_NAME = "log_trim_periodic"
private const val RETRY_WORK_PREFIX = "automation_retry_"
const val KEY_AUTOMATION_ID = "automation_id"

/**
 * The only two WorkManager use cases the spec actually calls for (§2/§28):
 * housekeeping (trimming old logs, §36) and retrying a failed run past the
 * current process's lifetime. Everything trigger-related stays event-driven
 * through automation.triggers — WorkManager is not used for polling.
 */
class AutomationScheduler(private val workManager: WorkManager) {

    fun schedulePeriodicLogTrim() {
        val request = PeriodicWorkRequestBuilder<LogTrimWorker>(24, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()
        workManager.enqueueUniquePeriodicWork(
            LOG_TRIM_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun scheduleRetry(automationId: String, delay: Long, delayUnit: TimeUnit = TimeUnit.SECONDS) {
        val request = OneTimeWorkRequestBuilder<RetryWorker>()
            .setInputData(workDataOf(KEY_AUTOMATION_ID to automationId))
            .setInitialDelay(delay, delayUnit)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
            .build()
        workManager.enqueueUniqueWork(
            RETRY_WORK_PREFIX + automationId,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelRetry(automationId: String) {
        workManager.cancelUniqueWork(RETRY_WORK_PREFIX + automationId)
    }
}
