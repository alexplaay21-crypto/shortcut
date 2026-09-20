package com.sunflower.shortcut.sdk.network

import com.sunflower.shortcut.sdk.SdkException
import com.sunflower.shortcut.sdk.SdkModule
import com.sunflower.shortcut.sdk.jsonObjectOf
import com.sunflower.shortcut.sdk.objectArgOrNull
import com.sunflower.shortcut.sdk.stringArg
import com.sunflower.shortcut.sdk.stringArgOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * `network.*` — plain `HttpURLConnection` rather than OkHttp/Retrofit
 * (spec §30: no dependency not already justified elsewhere). All calls run
 * on Dispatchers.IO; every response comes back as `{status, body}` so the
 * script decides how to parse it (JSON.parse, etc. — already whitelisted
 * globals per javascript.validator.AllowedGlobals).
 */
class NetworkModule : SdkModule {

    override val name: String = "network"

    override suspend fun call(method: String, args: JSONArray): String = when (method) {
        "get" -> simpleRequest(args, "GET", hasBody = false)
        "post" -> simpleRequest(args, "POST", hasBody = true)
        "put" -> simpleRequest(args, "PUT", hasBody = true)
        "delete" -> simpleRequest(args, "DELETE", hasBody = false)
        "send" -> send(args)
        else -> throw SdkException("Метод network.$method не поддерживается")
    }

    private suspend fun simpleRequest(args: JSONArray, method: String, hasBody: Boolean): String {
        val url = args.stringArg(0, "url")
        val body = if (hasBody) args.stringArgOrNull(1) else null
        val options = args.objectArgOrNull(if (hasBody) 2 else 1)
        return performRequest(url, method, body, options)
    }

    private suspend fun send(args: JSONArray): String {
        val options = args.objectArgOrNull(0) ?: throw SdkException("network.send требует объект с параметрами")
        val url = options.optString("url").takeIf { it.isNotBlank() } ?: throw SdkException("Не указан url")
        val method = options.optString("method", "GET").uppercase()
        val body = options.optString("body").takeIf { it.isNotBlank() }
        return performRequest(url, method, body, options)
    }

    private suspend fun performRequest(
        urlString: String,
        method: String,
        body: String?,
        options: JSONObject?
    ): String = withContext(Dispatchers.IO) {
        val url = runCatching { URL(urlString) }.getOrNull()
            ?: throw SdkException("Некорректный URL: $urlString")
        if (url.protocol != "http" && url.protocol != "https") {
            throw SdkException("Поддерживаются только http/https ссылки")
        }

        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000

        options?.optJSONObject("headers")?.let { headers ->
            headers.keys().forEach { key -> connection.setRequestProperty(key, headers.optString(key)) }
        }

        try {
            if (body != null) {
                connection.doOutput = true
                if (connection.getRequestProperty("Content-Type") == null) {
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""

            jsonObjectOf("status" to status, "body" to responseBody).toString()
        } catch (e: IOException) {
            throw SdkException("Ошибка сети: ${e.message ?: urlString}")
        } finally {
            connection.disconnect()
        }
    }
}
