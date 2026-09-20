package com.sunflower.shortcut.ai

import com.sunflower.shortcut.data.datastore.SettingsDataStore
import com.sunflower.shortcut.ui.components.AppSettingsUi
import kotlinx.coroutines.flow.first

/**
 * The one entry point ui/ai's AiAssistantScreen and AiResultScreen already
 * call. Resolves the user's selected provider + key from settings, builds
 * the prompt, calls the provider, and turns its two "not really code"
 * outcomes (the NOT_AN_AUTOMATION sentinel, spec §13; markdown-fenced code
 * a model wrapped despite being told not to) into either a clean failure or
 * clean JS text. What it deliberately does NOT do is validate that JS —
 * the caller runs it through AutomationEngine.analyze() exactly like an
 * imported file, so the AI layer never gets to skip that gate (spec §31).
 */
class CodeGenerator(private val settingsDataStore: SettingsDataStore) {

    suspend fun generate(userDescription: String): Result<String> {
        if (userDescription.isBlank()) {
            return Result.failure(IllegalArgumentException("Опишите, что должна делать автоматизация"))
        }

        val settings = settingsDataStore.settingsFlow.first()
            ?: return Result.failure(IllegalStateException("Настройки ещё не загружены"))

        val provider = resolveProvider(settings)
        val apiKey = settings.aiApiKeys[provider.id].orEmpty()
        if (provider.requiresApiKey && apiKey.isBlank()) {
            return Result.failure(IllegalStateException("Укажите API-ключ для ${provider.displayName} в настройках"))
        }

        val prompt = PromptBuilder.build(userDescription)

        val rawResponse = try {
            provider.generateCode(prompt, apiKey)
        } catch (e: AIProviderException) {
            return Result.failure(e)
        } catch (t: Throwable) {
            return Result.failure(AIProviderException(t.message ?: "Не удалось обратиться к AI-провайдеру", t))
        }

        val trimmed = rawResponse.trim()
        if (trimmed == PromptBuilder.NOT_AN_AUTOMATION || trimmed.contains(PromptBuilder.NOT_AN_AUTOMATION)) {
            return Result.failure(
                IllegalArgumentException(
                    "Этот запрос не относится к созданию сценариев автоматизации. " +
                        "Опишите, когда должен запускаться сценарий и какое действие выполнить."
                )
            )
        }

        val code = stripMarkdownFences(trimmed)
        if (code.isBlank()) {
            return Result.failure(IllegalStateException("AI вернул пустой ответ"))
        }

        return Result.success(code)
    }

    private fun resolveProvider(settings: AppSettingsUi): AIProvider = when (settings.aiProviderId) {
        "groq" -> GroqProvider()
        "openrouter" -> OpenRouterProvider()
        "custom" -> CustomProvider(settings.customProviderUrl, settings.customProviderModel)
        else -> GeminiProvider()
    }

    /** Defensive: PromptBuilder asks for bare code, but models wrap it in
     *  ```js fences anyway often enough that stripping here is worth it. */
    private fun stripMarkdownFences(text: String): String {
        val fenceRegex = Regex("^```[a-zA-Z]*\\n([\\s\\S]*?)\\n```$")
        val match = fenceRegex.find(text)
        return (match?.groupValues?.get(1) ?: text).trim()
    }
}
