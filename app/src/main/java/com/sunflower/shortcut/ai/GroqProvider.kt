package com.sunflower.shortcut.ai

import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

/** Groq's OpenAI-compatible chat completions endpoint — fast inference. */
class GroqProvider : AIProvider {

    override val id: String = "groq"
    override val displayName: String = "Groq"

    override suspend fun generateCode(prompt: String, apiKey: String): String {
        if (apiKey.isBlank()) throw AIProviderException("Не указан API-ключ Groq")

        val body = JSONObject().apply {
            put("model", MODEL)
            put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
            put("temperature", 0.2)
        }

        val response = AIHttpClient.postJson(
            URL(ENDPOINT),
            body,
            mapOf("Authorization" to "Bearer ${apiKey.trim()}")
        )
        return extractText(response)
    }

    private fun extractText(response: JSONObject): String {
        val text = response.optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?.optString("content")
        return text?.takeIf { it.isNotBlank() } ?: throw AIProviderException("Groq вернул пустой ответ")
    }

    companion object {
        private const val ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"
        private const val MODEL = "llama-3.3-70b-versatile"
    }
}
