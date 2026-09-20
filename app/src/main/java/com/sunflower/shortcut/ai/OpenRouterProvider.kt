package com.sunflower.shortcut.ai

import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

/** OpenRouter — same chat-completions shape as Groq, but one API key
 *  routes to many different underlying models via a "vendor/model" id. */
class OpenRouterProvider : AIProvider {

    override val id: String = "openrouter"
    override val displayName: String = "OpenRouter"

    override suspend fun generateCode(prompt: String, apiKey: String): String {
        if (apiKey.isBlank()) throw AIProviderException("Не указан API-ключ OpenRouter")

        val body = JSONObject().apply {
            put("model", MODEL)
            put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
            put("temperature", 0.2)
        }

        val response = AIHttpClient.postJson(
            URL(ENDPOINT),
            body,
            mapOf(
                "Authorization" to "Bearer ${apiKey.trim()}",
                // Optional per OpenRouter's own docs — not required for the
                // request to succeed, just identifies the app on their side.
                "HTTP-Referer" to "https://bina.co",
                "X-Title" to "Shortcut"
            )
        )
        return extractText(response)
    }

    private fun extractText(response: JSONObject): String {
        val text = response.optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?.optString("content")
        return text?.takeIf { it.isNotBlank() } ?: throw AIProviderException("OpenRouter вернул пустой ответ")
    }

    companion object {
        private const val ENDPOINT = "https://openrouter.ai/api/v1/chat/completions"
        private const val MODEL = "openai/gpt-4o-mini"
    }
}
