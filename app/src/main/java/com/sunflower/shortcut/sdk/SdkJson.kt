package com.sunflower.shortcut.sdk

import org.json.JSONArray
import org.json.JSONObject

/** Encodes a plain Kotlin value as the JSON text SdkModule.call() must return. */
fun jsonOf(value: Any?): String = when (value) {
    null -> "null"
    is JSONObject -> value.toString()
    is JSONArray -> value.toString()
    is String -> JSONObject.quote(value)
    is Boolean, is Int, is Long, is Double, is Float -> value.toString()
    else -> JSONObject.quote(value.toString())
}

/** Builds a JSONObject from Kotlin pairs, handling null values the way
 *  org.json requires (JSONObject.NULL, not a bare Kotlin null). */
fun jsonObjectOf(vararg pairs: Pair<String, Any?>): JSONObject {
    val obj = JSONObject()
    for ((key, value) in pairs) obj.put(key, value ?: JSONObject.NULL)
    return obj
}

// ---- argument-array accessors — every module reads its args through these
// so a missing/wrong-typed argument becomes one clear SdkException instead
// of an unrelated JSONException bubbling up.

fun JSONArray.stringArg(index: Int, name: String): String =
    if (index < length()) optString(index, "") else throw SdkException("Отсутствует аргумент \"$name\"")

fun JSONArray.stringArgOrNull(index: Int): String? =
    if (index < length() && !isNull(index)) optString(index) else null

fun JSONArray.intArg(index: Int, name: String): Int =
    if (index < length()) optInt(index, Int.MIN_VALUE).also {
        if (it == Int.MIN_VALUE) throw SdkException("Аргумент \"$name\" должен быть числом")
    } else throw SdkException("Отсутствует аргумент \"$name\"")

fun JSONArray.objectArgOrNull(index: Int): JSONObject? =
    if (index < length()) optJSONObject(index) else null
