package com.sunflower.shortcut.javascript.validator

import com.sunflower.shortcut.automation.runtime.JsHostScript

/**
 * Everything a scenario is allowed to reference at the top of an expression
 * or a bare function call. Reused from JsHostScript.MODULE_NAMES rather than
 * duplicated, so the sandboxed JS surface and the validator's allow-list can
 * never silently drift apart (spec §44 — единая реализация).
 */
object AllowedGlobals {

    val SAFE_GLOBAL_CALLEES: Set<String> = setOf(
        "on", "wait",
        "isNaN", "isFinite", "parseInt", "parseFloat",
        "String", "Number", "Boolean", "Array", "Object", "Promise", "Date", "Math", "JSON", "console"
    ) + JsHostScript.MODULE_NAMES

    /**
     * Identifiers that would let a scenario reach outside the controlled SDK
     * surface (spec §20). Deliberately narrow — this bans sandbox-escape and
     * raw-network/raw-storage primitives, not ordinary safe globals like
     * Math/JSON/Date, which stay usable.
     */
    val DENIED_IDENTIFIERS: Set<String> = setOf(
        "eval", "Function",
        "fetch", "XMLHttpRequest", "WebSocket", "Worker", "SharedWorker",
        "require", "importScripts",
        "Reflect", "Proxy",
        "window", "self", "globalThis", "document",
        "localStorage", "sessionStorage", "indexedDB"
    )

    /** Property names commonly used for prototype-pollution / sandbox-escape tricks. */
    val DENIED_PROPERTY_NAMES: Set<String> = setOf("__proto__", "constructor", "prototype")
}
