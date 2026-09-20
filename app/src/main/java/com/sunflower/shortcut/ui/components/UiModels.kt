package com.sunflower.shortcut.ui.components

import com.sunflower.shortcut.ui.theme.AppTheme

/**
 * UI-facing projection of an installed automation (spec §5).
 * Mapped from data.models.Automation by AutomationRepository — kept separate
 * from the Room entity so the UI layer never depends on persistence types.
 */
data class AutomationUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val enabled: Boolean,
    val actionCount: Int,
    val lastRunLabel: String? = null,
    val lastRunSuccess: Boolean? = null
)

/** One step shown in the Constructor flow graph (spec §18). */
data class FlowStepUi(
    val id: String,
    val emoji: String,
    val title: String,
    val subtitle: String,
    val kind: FlowStepKind,
    val editable: Boolean = false
)

enum class FlowStepKind { TRIGGER, ACTION, CONDITION, DELAY, DYNAMIC, END }

/** Capabilities/permissions an automation declares before install (spec §20/§34). */
data class RequirementUi(
    val label: String,
    val emoji: String,
    val satisfied: Boolean
)

/** One row on the Permissions screen (spec §21). */
data class PermissionUi(
    val label: String,
    val granted: Boolean,
    val reason: String,
    val usedBy: List<String> = emptyList()
)

/**
 * Result of automation.engine.AutomationEngine.analyze(js) — the static-analysis
 * pipeline output (spec §8/§32) shared by ImportJsScreen and AiResultScreen.
 * `errorMessage` non-null means the JS is broken and must not be installable.
 */
data class ScenarioAnalysis(
    val title: String,
    val description: String,
    val steps: List<FlowStepUi>,
    val requirements: List<RequirementUi>,
    val errorMessage: String? = null,
    val errorLine: Int? = null,
    /** Raw event key from the scenario's on(...) call, e.g. "charging" — null
     *  if analysis failed or no trigger was found. Used by automation.triggers
     *  to register the right system listener after install. */
    val triggerKey: String? = null
)

/** One entry in AI generation history (spec §11). */
data class AiHistoryUi(
    val id: String,
    val emoji: String,
    val title: String,
    val dateLabel: String,
    val description: String,
    val prompt: String,
    val jsCode: String,
    val status: AiHistoryStatus
)

enum class AiHistoryStatus { INSTALLED, NOT_INSTALLED, FAILED }

/** One topic in the help section (spec §23). */
data class HelpTopicUi(
    val id: String,
    val title: String,
    val body: String
)

enum class AppLanguage { RU, EN }

/**
 * UI projection of persisted settings (spec §21), read from
 * data.datastore.SettingsDataStore.settingsFlow. MainActivity waits for the
 * first non-null value before drawing, so the theme never flashes.
 */
data class AppSettingsUi(
    val appTheme: AppTheme,
    val language: AppLanguage,
    val animationsEnabled: Boolean,
    val compactInterface: Boolean,
    val notificationsOnRun: Boolean,
    val launchOnBoot: Boolean,
    /** "gemini" | "groq" | "openrouter" | "custom" — matches each AIProvider.id. */
    val aiProviderId: String = "gemini",
    /** Keyed by provider id — BYOK, spec §14: never a developer key baked into the APK. */
    val aiApiKeys: Map<String, String> = emptyMap(),
    val customProviderUrl: String = "",
    val customProviderModel: String = ""
)
