package com.sunflower.shortcut.automation.engine

import android.content.Context
import androidx.work.WorkManager
import com.sunflower.shortcut.automation.execution.ExecutionCoordinator
import com.sunflower.shortcut.automation.execution.ExecutionResult
import com.sunflower.shortcut.automation.permissions.AutomationPermissionChecker
import com.sunflower.shortcut.automation.runtime.NativeBridgeDispatcher
import com.sunflower.shortcut.automation.scheduler.AutomationScheduler
import com.sunflower.shortcut.automation.triggers.TriggerBinding
import com.sunflower.shortcut.automation.triggers.TriggerListener
import com.sunflower.shortcut.automation.triggers.TriggerRegistry
import com.sunflower.shortcut.automation.triggers.TriggerType
import com.sunflower.shortcut.data.repositories.AutomationRepository
import com.sunflower.shortcut.data.repositories.LogRepository
import com.sunflower.shortcut.javascript.analyzer.ScenarioAnalyzer
import com.sunflower.shortcut.javascript.parser.JsParser
import com.sunflower.shortcut.javascript.parser.JsSyntaxException
import com.sunflower.shortcut.ui.components.ScenarioAnalysis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Single entry point the UI layer talks to (spec §31: "AI Layer не должен
 * быть связан напрямую с Android execution layer" — this class *is* the
 * execution layer boundary). It orchestrates, but does not itself implement,
 * each of these (all in their own package):
 *
 *  - parsing/validation/analysis → javascript.parser / javascript.validator / javascript.analyzer (done)
 *  - persistence                  → data.repositories.AutomationRepository (not yet generated)
 *  - JS execution                 → automation.runtime.JsRuntime (done)
 *  - trigger listening            → automation.triggers.TriggerRegistry (done)
 *  - capability checks            → automation.permissions.AutomationPermissionChecker (done)
 *
 * SDK module implementations (camera.*, apps.*, ...) are supplied later via
 * [setNativeBridgeDispatcher] once the sdk/ package exists — until then,
 * analyze()/install() work fully, but actually *running* an automation fails
 * with a clear error rather than silently doing nothing.
 */
class AutomationEngine(
    private val context: Context,
    private val automationRepository: AutomationRepository,
    private val logRepository: LogRepository,
    private val scope: CoroutineScope
) {
    private val permissionChecker = AutomationPermissionChecker(context)
    private val triggerRegistry = TriggerRegistry(context)
    private val executionCoordinator = ExecutionCoordinator(context, logRepository)
    private val scheduler = AutomationScheduler(WorkManager.getInstance(context))
    private val scenarioAnalyzer = ScenarioAnalyzer()

    private var nativeBridgeDispatcher: NativeBridgeDispatcher? = null

    init {
        triggerRegistry.setListener(TriggerListener { automationId, trigger, payloadJson ->
            scope.launch { runAutomation(automationId, trigger, payloadJson) }
        })
        // Cheap to enqueue — WorkManager itself decides when it actually runs,
        // ExistingPeriodicWorkPolicy.KEEP makes repeated calls a no-op.
        scheduler.schedulePeriodicLogTrim()
    }

    /** Wired in once the sdk/ package's module aggregator exists. */
    fun setNativeBridgeDispatcher(dispatcher: NativeBridgeDispatcher) {
        nativeBridgeDispatcher = dispatcher
    }

    fun permissions(): AutomationPermissionChecker = permissionChecker

    /**
     * Restores trigger listeners for every currently-enabled automation.
     * Call once on process start after a real trigger source exists to call
     * it from (spec §40 — boot restores engine state without a heavy UI or
     * AI warm-up); this class does not call itself.
     */
    suspend fun start() {
        triggerRegistry.registerAll(automationRepository.getTriggerBindings())
    }

    suspend fun analyze(js: String): ScenarioAnalysis = withContext(Dispatchers.Default) {
        try {
            val ast = JsParser().parse(js)
            scenarioAnalyzer.analyze(ast, isGranted = permissionChecker::isGranted)
        } catch (e: JsSyntaxException) {
            ScenarioAnalysis(
                title = "Сценарий",
                description = "",
                steps = emptyList(),
                requirements = emptyList(),
                errorMessage = e.message ?: "Ошибка синтаксиса JavaScript",
                errorLine = e.line
            )
        } catch (t: Throwable) {
            ScenarioAnalysis(
                title = "Сценарий",
                description = "",
                steps = emptyList(),
                requirements = emptyList(),
                errorMessage = t.message ?: "Не удалось проанализировать сценарий"
            )
        }
    }

    suspend fun install(js: String, analysis: ScenarioAnalysis): Result<String> {
        if (analysis.errorMessage != null) {
            return Result.failure(IllegalStateException(analysis.errorMessage))
        }

        val newId = runCatching { automationRepository.install(js, analysis) }
            .getOrElse { return Result.failure(it) }

        analysis.triggerKey
            ?.let { TriggerType.fromKey(it) }
            ?.let { triggerRegistry.register(TriggerBinding(newId, it)) }

        return Result.success(newId)
    }

    suspend fun runAutomation(
        automationId: String,
        trigger: TriggerType,
        payloadJson: String = "{}"
    ): Result<ExecutionResult> {
        val dispatcher = nativeBridgeDispatcher
            ?: return Result.failure(IllegalStateException("SDK-модули ещё не инициализированы"))

        val automation = automationRepository.getByIdOnce(automationId)
            ?: return Result.failure(NoSuchElementException("Автоматизация не найдена"))

        if (!automation.enabled) {
            return Result.failure(IllegalStateException("Автоматизация отключена"))
        }

        val js = automationRepository.getCode(automationId)
        val result = executionCoordinator.execute(
            automationId = automationId,
            automationTitle = automation.title,
            js = js,
            trigger = trigger,
            payloadJson = payloadJson,
            dispatcher = dispatcher
        )
        return Result.success(result)
    }

    suspend fun retryAutomation(automationId: String): Result<ExecutionResult> =
        runAutomation(automationId, TriggerType.MANUAL)

    fun uninstall(automationId: String) {
        triggerRegistry.unregister(automationId)
        scheduler.cancelRetry(automationId)
    }
}
