package com.sunflower.shortcut.ai

import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

/**
 * Google Gemini via the generateContent REST endpoint. [MODEL] is one
 * constant on purpose — Google renames/retires model ids periodically,
 * so this is the one place to update when that happens.
 */
class GeminiProvider : AIProvider {

    override val id: String = "gemini"
    override val displayName: String = "Google Gemini"

    override suspend fun generateCode(prompt: String, apiKey: String): String {
        if (apiKey.isBlank()) throw AIProviderException("Не указан API-ключ Gemini")

        val url = URL("$ENDPOINT_BASE/$MODEL:generateContent?key=${apiKey.trim()}")
        val body = JSONObject().apply {
            put(
                "contents",
                JSONArray().put(
                    JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                )
            )
            put("generationConfig", JSONObject().put("temperature", 0.2))
        }

        val response = AIHttpClient.postJson(url, body, emptyMap())
        return extractText(response)
    }

    private fun extractText(response: JSONObject): String {
        val candidates = response.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            val blockReason = response.optJSONObject("promptFeedback")?.optString("blockReason")
            throw AIProviderException(
                if (!blockReason.isNullOrBlank()) "Gemini отклонил запрос: $blockReason" else "Gemini не вернул ответ"
            )
        }
        val parts = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")
        val text = parts?.let { arr -> (0 until arr.length()).joinToString("") { arr.getJSONObject(it).optString("text") } }
        return text?.takeIf { it.isNotBlank() } ?: throw AIProviderException("Gemini вернул пустой ответ")
    }

    companion object {
        private const val ENDPOINT_BASE = "https://generativelanguage.googleapis.com/v1beta/models"
        private const val MODEL = "gemini-2.0-flash"
    }
}
