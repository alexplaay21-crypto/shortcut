package com.sunflower.shortcut.sdk

import org.json.JSONArray

/**
 * One JS namespace (`android`, `apps`, `camera`, ...). [call] receives the
 * raw JSON arguments array exactly as the script passed them and must
 * return a JSON-encoded result *text* (e.g. "42", "\"ok\"", "{...}", "null")
 * — see SdkJson.kt for helpers that build this consistently. Throwing
 * [SdkException] (or anything) turns into a rejected Promise in the script,
 * caught by whatever try/catch the scenario author wrote (spec §37).
 */
interface SdkModule {
    val name: String
    suspend fun call(method: String, args: JSONArray): String
}
