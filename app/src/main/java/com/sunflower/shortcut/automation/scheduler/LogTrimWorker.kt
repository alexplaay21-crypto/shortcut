package com.sunflower.shortcut.automation.scheduler

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sunflower.shortcut.data.repositories.LogRepository

class LogTrimWorker(
    appContext: Context,
    params: WorkerParameters,
    private val logRepository: LogRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            logRepository.trimToRetentionPolicy()
            Result.success()
        } catch (t: Throwable) {
            Result.retry()
        }
    }
}
