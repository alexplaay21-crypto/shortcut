package com.sunflower.shortcut.data.repositories

import android.content.Context
import com.sunflower.shortcut.automation.permissions.AutomationPermissionChecker
import com.sunflower.shortcut.automation.permissions.RequiredPermission
import com.sunflower.shortcut.automation.triggers.TriggerBinding
import com.sunflower.shortcut.automation.triggers.TriggerType
import com.sunflower.shortcut.constructor.FlowBuilder
import com.sunflower.shortcut.data.database.AutomationDao
import com.sunflower.shortcut.data.database.AutomationWithLastRun
import com.sunflower.shortcut.data.models.Automation
import com.sunflower.shortcut.javascript.analyzer.RequirementDetector
import com.sunflower.shortcut.javascript.exporter.ScenarioExporter
import com.sunflower.shortcut.javascript.exporter.ScenarioValuePatcher
import com.sunflower.shortcut.javascript.parser.JsParser
import com.sunflower.shortcut.javascript.validator.ScenarioValidator
import com.sunflower.shortcut.sharing.ScenarioSharer
import com.sunflower.shortcut.ui.components.AutomationUi
import com.sunflower.shortcut.ui.components.FlowStepKind
import com.sunflower.shortcut.ui.components.FlowStepUi
import com.sunflower.shortcut.ui.components.PermissionUi
import com.sunflower.shortcut.ui.components.ScenarioAnalysis
import com.sunflower.shortcut.utils.DateFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Everything the UI and the engine need to know about installed scenarios
 * (spec §5/§18/§33). The stored JS stays the single source of truth: steps
 * are rebuilt from it on demand (FlowBuilder), and constructor edits are
 * written back as a patch to the JS text (ScenarioValuePatcher).
 */
class AutomationRepository(
    private val dao: AutomationDao,
    context: Context
) {

    private val permissionChecker = AutomationPermissionChecker(context.applicationContext)

    // ---- observation ---------------------------------------------------

    fun observeAll(): Flow<List<AutomationUi>> =
        dao.observeAll().map { rows -> rows.map { it.toUi() } }

    fun observeById(id: String): Flow<AutomationUi?> =
        dao.observeById(id).map { it?.toUi() }

    /**
     * Steps for the constructor and the detail screen. The query also
     * re-emits when a run is logged, so the code is compared first and the
     * script is only re-parsed when it actually changed.
     */
    fun observeSteps(id: String): Flow<List<FlowStepUi>> =
        dao.observeById(id)
            .map { it?.automation?.jsCode }
            .distinctUntilChanged()
            .map { js ->
                if (js == null) {
                    emptyList<FlowStepUi>()
                } else {
                    FlowBuilder.buildFromSource(js)
                        .map { graph -> graph.toStepUi() }
                        .getOrDefault(emptyList<FlowStepUi>())
                }
            }
            .flowOn(Dispatchers.Default)

    // ---- reads ---------------------------------------------------------

    suspend fun getCode(id: String): String = dao.getCode(id).orEmpty()

    suspend fun getByIdOnce(id: String): Automation? = dao.getByIdOnce(id)

    /**
     * All installed automations that have a known trigger — enabled or not.
     * The enabled flag is enforced when a run starts (AutomationEngine).
     */
    suspend fun getTriggerBindings(): List<TriggerBinding> =
        dao.getTriggerRows().mapNotNull { row ->
            TriggerType.fromKey(row.triggerKey)?.let { TriggerBinding(row.automationId, it) }
        }

    // ---- writes --------------------------------------------------------

    /** Returns the new automation's id. Throws if [analysis] carries an error. */
    suspend fun install(js: String, analysis: ScenarioAnalysis): String {
        require(analysis.errorMessage == null) { analysis.errorMessage ?: "Сценарий содержит ошибки" }

        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        dao.insert(
            Automation(
                id = id,
                title = analysis.title,
                description = analysis.description,
                jsCode = js,
                triggerKey = analysis.triggerKey,
                enabled = true,
                actionCount = analysis.steps.count { it.kind == FlowStepKind.ACTION },
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
        )
        return id
    }

    suspend fun setEnabled(id: String, enabled: Boolean) {
        dao.setEnabled(id, enabled)
    }

    suspend fun delete(id: String) {
        dao.delete(id)
    }

    /**
     * Applies a constructor edit to the stored JS. The patched text is parsed
     * and validated again before it is saved, so an edit can never leave a
     * script in the database that the engine would refuse to run.
     */
    suspend fun updateStepValue(automationId: String, stepId: String, newValue: String): Result<Unit> =
        withContext(Dispatchers.Default) {
            runCatching {
                val automation = dao.getByIdOnce(automationId)
                    ?: throw NoSuchElementException("Автоматизация не найдена")
                val patched = ScenarioValuePatcher.patch(automation.jsCode, stepId, newValue).getOrThrow()
                ScenarioValidator().validate(JsParser().parse(patched))
                dao.updateCode(automationId, patched, System.currentTimeMillis())
            }.onFailure { if (it is CancellationException) throw it }
        }

    // ---- sharing (spec §33: a plain .js file) --------------------------

    suspend fun exportAndShare(id: String, context: Context) {
        val automation = dao.getByIdOnce(id) ?: return
        shareRawJs(automation.jsCode, automation.title, context)
    }

    /** For JS that is not installed (e.g. an AI result the user wants to send on). */
    suspend fun shareRawJs(js: String, title: String, context: Context) {
        ScenarioSharer.shareJs(
            context = context,
            fileName = ScenarioExporter.suggestedFileName(title),
            content = ScenarioExporter.export(js, title)
        )
    }

    // ---- permissions overview (spec §21) -------------------------------

    /**
     * One row per capability with its current state and the scenarios that
     * use it. [context] is what PermissionsScreen passes; the checker itself
     * is built from the application context, which gives the same answers.
     */
    @Suppress("UNUSED_PARAMETER")
    suspend fun getPermissionsOverview(context: Context): List<PermissionUi> =
        withContext(Dispatchers.Default) {
            val usage = permissionUsage(dao.getAllOnce())
            RequiredPermission.entries.map { permission ->
                PermissionUi(
                    label = permission.label,
                    granted = permissionChecker.isGranted(permission),
                    reason = reasonFor(permission),
                    usedBy = usage[permission].orEmpty()
                )
            }
        }

    private fun permissionUsage(automations: List<Automation>): Map<RequiredPermission, List<String>> {
        val usage = mutableMapOf<RequiredPermission, MutableList<String>>()
        for (automation in automations) {
            val validated = runCatching { ScenarioValidator().validate(JsParser().parse(automation.jsCode)) }
                .getOrNull() ?: continue
            for (permission in RequirementDetector.detect(validated)) {
                usage.getOrPut(permission) { mutableListOf() }.add(automation.title)
            }
        }
        return usage.mapValues { (_, titles) -> titles.distinct() }
    }

    private fun reasonFor(permission: RequiredPermission): String = when (permission) {
        RequiredPermission.CAMERA -> "Нужно сценариям, которые делают фото"
        RequiredPermission.LOCATION -> "Нужно сценариям, которым важно ваше местоположение"
        RequiredPermission.MICROPHONE -> "Нужно сценариям, которые записывают звук"
        RequiredPermission.NOTIFICATIONS -> "Нужно, чтобы сценарии могли показывать уведомления"
        RequiredPermission.FILES -> "Нужно сценариям, которые читают или сохраняют файлы"
        RequiredPermission.BLUETOOTH -> "Нужно для реакции на Bluetooth-устройства и работы с ними"
        RequiredPermission.CONTACTS -> "Нужно сценариям, которые ищут контакты"
        RequiredPermission.PHONE -> "Нужно для звонков и состояния телефона"
        RequiredPermission.ACCESSIBILITY -> "Нужно для нажатий и действий в других приложениях"
    }
}

// ---- mapping to the UI layer ---------------------------------------------

private fun AutomationWithLastRun.toUi(): AutomationUi = AutomationUi(
    id = automation.id,
    title = automation.title,
    subtitle = automation.description,
    emoji = triggerEmoji(automation.triggerKey),
    enabled = automation.enabled,
    actionCount = automation.actionCount,
    lastRunLabel = lastRunAtEpochMs?.let { DateFormatter.formatDayAndTime(it) },
    lastRunSuccess = lastRunSuccess
)

/** Same icons FlowStepExtractor gives the trigger step, so a card and its constructor match. */
private fun triggerEmoji(triggerKey: String?): String =
    when (triggerKey?.let { TriggerType.fromKey(it) }) {
        TriggerType.CHARGING_CONNECTED, TriggerType.CHARGING_DISCONNECTED -> "🔌"
        TriggerType.BATTERY_LOW -> "🪫"
        TriggerType.WIFI_CONNECTED, TriggerType.WIFI_DISCONNECTED -> "📶"
        TriggerType.BLUETOOTH_CONNECTED, TriggerType.BLUETOOTH_DISCONNECTED -> "🔷"
        TriggerType.BOOT_COMPLETED -> "🔁"
        else -> "⚡"
    }
