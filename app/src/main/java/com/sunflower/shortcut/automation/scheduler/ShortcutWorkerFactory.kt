package com.sunflower.shortcut.automation.scheduler

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.sunflower.shortcut.automation.engine.AutomationEngine
import com.sunflower.shortcut.data.repositories.LogRepository

/**
 * Takes a *provider* rather than a direct [AutomationEngine], because this
 * factory is wired into Configuration.Provider before AutomationEngine has
 * necessarily finished constructing (AutomationEngine's own init creates a
 * WorkManager instance, which triggers WorkManager to read this
 * configuration) — the lambda is only invoked later, when WorkManager
 * actually builds a RetryWorker, by which point construction has finished.
 */
class ShortcutWorkerFactory(
    private val logRepository: LogRepository,
    private val automationEngineProvider: () -> AutomationEngine
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? = when (workerClassName) {
        LogTrimWorker::class.java.name -> LogTrimWorker(appContext, workerParameters, logRepository)
        RetryWorker::class.java.name -> RetryWorker(appContext, workerParameters, automationEngineProvider())
        else -> null // fall back to WorkManager's default factory for anything else
    }
}
