package com.sunflower.shortcut.ai

/**
 * One AI backend a user can pick in Settings (spec §14). Every
 * implementation is a thin, stateless HTTP client — the API key is
 * supplied per call (BYOK: "не хранить секретные API-ключи разработчика
 * непосредственно в APK") and never stored inside the provider itself.
 *
 * This interface has no dependency on anything else in the app — no
 * Context, no repositories, nothing SDK-related. CodeGenerator (the only
 * caller, in this same package) owns provider selection and reading the
 * chosen key from settings; AIProvider just turns a finished prompt into
 * text. That isolation is exactly what spec §14 asks for: adding a future
 * server-side AI proxy is one more class implementing this interface, not
 * a rewrite of anything that calls it.
 */
interface AIProvider {

    /** Stable id matching the stored provider choice in settings — "gemini", "groq", "openrouter", "custom". */
    val id: String

    /** Shown in the provider picker in Settings. */
    val displayName: String

    /** All current providers are BYOK; a future server-proxy provider might not be. */
    val requiresApiKey: Boolean get() = true

    /**
     * Sends [prompt] — already fully assembled by PromptBuilder, this
     * interface doesn't know or care about SDK docs, examples, or scenario
     * conventions — to the provider's completion endpoint and returns the
     * raw response text (JS possibly wrapped in markdown fences; stripping
     * that is CodeGenerator's job, not every provider's). [apiKey] is
     * empty when [requiresApiKey] is false.
     *
     * Throws [AIProviderException] on any failure — network, auth, rate
     * limit, or the provider's own error payload — with a message safe to
     * surface to the user as-is. Never throws with the API key anywhere in
     * the message.
     */
    suspend fun generateCode(prompt: String, apiKey: String): String
}

class AIProviderException(message: String, cause: Throwable? = null) : Exception(message, cause)
