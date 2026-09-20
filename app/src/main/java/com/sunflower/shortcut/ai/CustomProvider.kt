package com.sunflower.shortcut.ai

import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

/**
 * A provider the user points at anything OpenAI-compatible — a self-hosted
 * model, a provider not in the built-in list. Unlike Gemini/Groq/
 * OpenRouter, endpoint and model aren't fixed constants; they're supplied
 * at construction from the user's own settings (AppSettingsUi will need
 * customEndpointUrl/customModel fields, and CodeGenerator constructs this
 * provider with them when the user selects "Свой провайдер" — both still
 * to be generated).
 */
class CustomProvider(
    private val endpointUrl: String,
    private val model: String
) : AIProvider {

    override val id: String = "custom"
    override val displayName: String = "Свой провайдер"

    /** Many self-hosted servers (LM Studio, llama.cpp, vLLM, text-generation-webui's
     *  OpenAI-compatible mode) need no key at all — unlike the other three
     *  providers, a key here is optional, not required. */
    override val requiresApiKey: Boolean = false

    override suspend fun generateCode(prompt: String, apiKey: String): String {
        if (endpointUrl.isBlank()) throw AIProviderException("Не указан адрес своего AI-провайдера")
        val url = runCatching { URL(endpointUrl) }.getOrNull()
            ?: throw AIProviderException("Некорректный адрес провайдера: $endpointUrl")
        if (url.protocol != "http" && url.protocol != "https") {
            throw AIProviderException("Поддерживаются только http/https адреса")
        }

        val body = JSONObject().apply {
            put("model", model.ifBlank { "default" })
            put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
            put("temperature", 0.2)
        }

        val headers = if (apiKey.isNotBlank()) mapOf("Authorization" to "Bearer ${apiKey.trim()}") else emptyMap()
        val response = AIHttpClient.postJson(url, body, headers)
        return extractText(response)
    }

    private fun extractText(response: JSONObject): String {
        // OpenAI-compatible chat-completions is the most common convention
        // among self-hosted/third-party servers — if a server uses a
        // genuinely different response shape, this fails clearly rather
        // than silently returning something wrong.
        val text = response.optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?.optString("content")
        return text?.takeIf { it.isNotBlank() }
            ?: throw AIProviderException("Провайдер вернул неожиданный формат ответа")
    }
}
