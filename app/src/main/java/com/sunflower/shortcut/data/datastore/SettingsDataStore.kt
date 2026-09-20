package com.sunflower.shortcut.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sunflower.shortcut.data.models.AppSettings
import com.sunflower.shortcut.data.models.toUi
import com.sunflower.shortcut.ui.components.AppLanguage
import com.sunflower.shortcut.ui.components.AppSettingsUi
import com.sunflower.shortcut.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

/** One DataStore per process — the delegate must live at top level. */
private val Context.settingsPreferences: DataStore<Preferences> by preferencesDataStore(name = "shortcut_settings")

private object Keys {
    val THEME = stringPreferencesKey("app_theme")
    val LANGUAGE = stringPreferencesKey("language")
    val ANIMATIONS = booleanPreferencesKey("animations_enabled")
    val COMPACT = booleanPreferencesKey("compact_interface")
    val NOTIFY_ON_RUN = booleanPreferencesKey("notifications_on_run")
    val LAUNCH_ON_BOOT = booleanPreferencesKey("launch_on_boot")
    val AI_PROVIDER = stringPreferencesKey("ai_provider_id")
    val CUSTOM_URL = stringPreferencesKey("custom_provider_url")
    val CUSTOM_MODEL = stringPreferencesKey("custom_provider_model")

    /** API keys are stored one key per provider: "ai_api_key_<providerId>". */
    const val AI_KEY_PREFIX = "ai_api_key_"
}

/**
 * Persisted app settings (spec §21). [settingsFlow] always emits — a fresh
 * install yields the defaults from [AppSettings] — so MainActivity's
 * "wait for the first value" gate is released as soon as the file is read.
 *
 * API keys are plain preferences in app-private storage. They must be kept
 * out of cloud backup / device transfer in the manifest rules.
 */
class SettingsDataStore(context: Context) {

    private val dataStore = context.applicationContext.settingsPreferences

    val settingsFlow: Flow<AppSettingsUi> = dataStore.data
        .catch { error ->
            // An unreadable file must not brick the app — fall back to defaults.
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> preferences.toAppSettings().toUi() }
        .distinctUntilChanged()

    suspend fun setTheme(theme: AppTheme) {
        dataStore.edit { it[Keys.THEME] = theme.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun setAnimationsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.ANIMATIONS] = enabled }
    }

    suspend fun setCompactInterface(enabled: Boolean) {
        dataStore.edit { it[Keys.COMPACT] = enabled }
    }

    suspend fun setNotificationsOnRun(enabled: Boolean) {
        dataStore.edit { it[Keys.NOTIFY_ON_RUN] = enabled }
    }

    suspend fun setLaunchOnBoot(enabled: Boolean) {
        dataStore.edit { it[Keys.LAUNCH_ON_BOOT] = enabled }
    }

    /** [providerId] is an AIProvider.id: "gemini" | "groq" | "openrouter" | "custom". */
    suspend fun setAiProvider(providerId: String) {
        dataStore.edit { it[Keys.AI_PROVIDER] = providerId }
    }

    /** A blank [apiKey] removes the stored key for that provider. */
    suspend fun setApiKey(providerId: String, apiKey: String) {
        val key = stringPreferencesKey(Keys.AI_KEY_PREFIX + providerId)
        dataStore.edit { preferences ->
            val trimmed = apiKey.trim()
            if (trimmed.isEmpty()) preferences.remove(key) else preferences[key] = trimmed
        }
    }

    suspend fun setCustomProvider(url: String, model: String) {
        dataStore.edit {
            it[Keys.CUSTOM_URL] = url.trim()
            it[Keys.CUSTOM_MODEL] = model.trim()
        }
    }

    private fun Preferences.toAppSettings(): AppSettings {
        val defaults = AppSettings()
        val apiKeys = asMap().entries.mapNotNull { (key, value) ->
            if (key.name.startsWith(Keys.AI_KEY_PREFIX) && value is String) {
                key.name.removePrefix(Keys.AI_KEY_PREFIX) to value
            } else {
                null
            }
        }.toMap()

        return AppSettings(
            appTheme = enumOrDefault(this[Keys.THEME], defaults.appTheme),
            language = enumOrDefault(this[Keys.LANGUAGE], defaults.language),
            animationsEnabled = this[Keys.ANIMATIONS] ?: defaults.animationsEnabled,
            compactInterface = this[Keys.COMPACT] ?: defaults.compactInterface,
            notificationsOnRun = this[Keys.NOTIFY_ON_RUN] ?: defaults.notificationsOnRun,
            launchOnBoot = this[Keys.LAUNCH_ON_BOOT] ?: defaults.launchOnBoot,
            aiProviderId = this[Keys.AI_PROVIDER] ?: defaults.aiProviderId,
            aiApiKeys = apiKeys,
            customProviderUrl = this[Keys.CUSTOM_URL] ?: defaults.customProviderUrl,
            customProviderModel = this[Keys.CUSTOM_MODEL] ?: defaults.customProviderModel
        )
    }

    /** A stored name that no longer matches any entry falls back to the default. */
    private inline fun <reified E : Enum<E>> enumOrDefault(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default
}
