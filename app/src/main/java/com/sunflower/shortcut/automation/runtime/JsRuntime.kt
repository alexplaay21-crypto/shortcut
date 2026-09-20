package com.sunflower.shortcut.automation.runtime

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

/**
 * Runs exactly one scenario's `on(event, handler)` callback to completion and
 * returns. No bundled JS engine (spec §30 — APK size): this uses the
 * Chromium engine already on-device via a headless WebView, which also gives
 * real async/await support for free (spec §15 example scripts use it).
 *
 * A fresh WebView is created per run() call and destroyed in the `finally`
 * block — the runtime never idles between triggers (spec §28: "JS Runtime
 * останавливается / засыпает").
 */
class JsRuntime(
    context: Context,
    private val dispatcher: NativeBridgeDispatcher
) {
    private val appContext = context.applicationContext

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun run(
        script: String,
        triggerEvent: String,
        payloadJson: String = "{}",
        timeoutMs: Long = 30_000L
    ): RunResult = withContext(Dispatchers.Main) {
        val completed = CompletableDeferred<RunResult>()
        val bridgeScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val webView = WebView(appContext).apply {
            settings.javaScriptEnabled = true
            settings.allowContentAccess = false
            settings.allowFileAccess = false
        }

        try {
            val bridge = NativeBridge(
                webView = webView,
                dispatcher = dispatcher,
                scope = bridgeScope,
                onFinished = { result -> if (!completed.isCompleted) completed.complete(result) }
            )
            webView.addJavascriptInterface(bridge, "NativeBridge")

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String?) {
                    view.evaluateJavascript(JsHostScript.BOOTSTRAP, null)
                    view.evaluateJavascript(script, null)
                    view.evaluateJavascript(invocationExpression(triggerEvent, payloadJson), null)
                }
            }
            webView.loadDataWithBaseURL(null, "<!DOCTYPE html><html><head></head><body></body></html>", "text/html", "utf-8", null)

            withTimeoutOrNull(timeoutMs) { completed.await() }
                ?: RunResult(success = false, errorMessage = "Превышено время выполнения сценария")
        } finally {
            bridgeScope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
            webView.stopLoading()
            webView.destroy()
        }
    }

    private fun invocationExpression(triggerEvent: String, payloadJson: String): String =
        "window.__invoke(${JSONObject.quote(triggerEvent)}, ${JSONObject.quote(payloadJson)})" +
            ".then(function(r){ NativeBridge.onComplete(true, JSON.stringify({value:(r===undefined?null:r)})); })" +
            ".catch(function(e){ NativeBridge.onComplete(false, JSON.stringify({error:(e && e.message) ? e.message : String(e)})); });"

    /** Bridge object exposed to JS as `NativeBridge` (spec §17/§20: every
     *  device access goes through this controlled seam, never raw reflection
     *  or native access from the script itself). */
    private class NativeBridge(
        private val webView: WebView,
        private val dispatcher: NativeBridgeDispatcher,
        private val scope: CoroutineScope,
        private val onFinished: (RunResult) -> Unit
    ) {
        @JavascriptInterface
        fun invoke(callId: String, module: String, method: String, argsJson: String) {
            scope.launch {
                val resultJson = try {
                    val valueJson = dispatcher.dispatch(module, method, argsJson)
                    """{"value":$valueJson}"""
                } catch (t: Throwable) {
                    """{"error":${JSONObject.quote(t.message ?: "Ошибка модуля \"$module.$method\"")}}"""
                }
                withContext(Dispatchers.Main) {
                    webView.evaluateJavascript(
                        "window.__resolveNative(${JSONObject.quote(callId)}, ${JSONObject.quote(resultJson)})",
                        null
                    )
                }
            }
        }

        @JavascriptInterface
        fun onComplete(success: Boolean, payloadJson: String) {
            val json = runCatching { JSONObject(payloadJson) }.getOrNull()
            val result = if (success) {
                RunResult(success = true, resultJson = json?.optString("value"))
            } else {
                RunResult(success = false, errorMessage = json?.optString("error") ?: "Неизвестная ошибка")
            }
            onFinished(result)
        }
    }
}
