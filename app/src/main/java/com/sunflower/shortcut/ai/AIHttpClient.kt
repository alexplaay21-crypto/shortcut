package com.sunflower.shortcut.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Plain HttpURLConnection, no OkHttp/Retrofit — same choice as
 * sdk.network.NetworkModule, for the same reason (spec §30). Every
 * provider's HTTP shape differs (auth header, body, response parsing) but
 * the connection plumbing itself doesn't, so it lives here once.
 */
internal object AIHttpClient {

    suspend fun postJson(url: URL, body: JSONObject, headers: Map<String, String>): JSONObject =
        withContext(Dispatchers.IO) {
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 30_000
            connection.readTimeout = 60_000
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }

            try {
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val responseText = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

                if (status !in 200..299) {
                    throw AIProviderException(errorMessageFor(status, responseText))
                }
                runCatching { JSONObject(responseText) }
                    .getOrElse { throw AIProviderException("Некорректный ответ от AI-провайдера") }
            } catch (e: IOException) {
                throw AIProviderException("Ошибка сети при обращении к AI-провайдеру: ${e.message.orEmpty()}".trim())
            } finally {
                connection.disconnect()
            }
        }

    private fun errorMessageFor(status: Int, body: String): String {
        val detail = runCatching {
            JSONObject(body).let { it.optJSONObject("error")?.optString("message") ?: it.optString("message") }
        }.getOrNull()?.takeIf { it.isNotBlank() }

        return when (status) {
            401, 403 -> "Неверный или отклонённый API-ключ"
            429 -> "Превышен лимит запросов — попробуйте позже"
            in 500..599 -> "AI-провайдер временно недоступен"
            else -> detail ?: "Ошибка AI-провайдера (код $status)"
        }
    }
}
