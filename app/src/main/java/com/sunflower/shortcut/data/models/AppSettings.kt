package com.sunflower.shortcut.data.models

import com.sunflower.shortcut.ui.components.AppLanguage
import com.sunflower.shortcut.ui.components.AppSettingsUi
import com.sunflower.shortcut.ui.theme.AppTheme

/**
 * What is persisted in settings (spec §21). The default values live here and
 * only here: SettingsDataStore falls back to them for any key that was never
 * written, so a fresh install and a missing key behave identically.
 *
 * Notifications on run start OFF on purpose — a scenario that fires on every
 * charger plug-in would otherwise spam a user who never asked for it (and
 * would need the notifications permission before doing anything useful).
 */
data class AppSettings(
    val appTheme: AppTheme = AppTheme.SYSTEM,
    val language: AppLanguage = AppLanguage.RU,
    val animationsEnabled: Boolean = true,
    val compactInterface: Boolean = false,
    val notificationsOnRun: Boolean = false,
    val launchOnBoot: Boolean = true,
    /** "gemini" | "groq" | "openrouter" | "custom" — matches each AIProvider.id. */
    val aiProviderId: String = "gemini",
    /** BYOK (spec §14): keyed by provider id, never a key baked into the APK. */
    val aiApiKeys: Map<String, String> = emptyMap(),
    val customProviderUrl: String = "",
    val customProviderModel: String = ""
)

fun AppSettings.toUi(): AppSettingsUi = AppSettingsUi(
    appTheme = appTheme,
    language = language,
    animationsEnabled = animationsEnabled,
    compactInterface = compactInterface,
    notificationsOnRun = notificationsOnRun,
    launchOnBoot = launchOnBoot,
    aiProviderId = aiProviderId,
    aiApiKeys = aiApiKeys,
    customProviderUrl = customProviderUrl,
    customProviderModel = customProviderModel
)
