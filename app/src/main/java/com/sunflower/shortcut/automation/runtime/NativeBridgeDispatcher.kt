package com.sunflower.shortcut.automation.runtime

/**
 * The one seam between "JS said `camera.takePhoto({...})`" and "a real
 * Android API ran". JsRuntime never talks to SDK modules directly — it
 * only knows this interface, so the runtime and the SDK modules can be
 * built and evolve independently (spec §31: SDK modules live under
 * com.sunflower.shortcut.sdk packages, added there without JsRuntime changing).
 *
 * [module] is the JS namespace ("camera", "apps", "ui", ...), [method] the
 * called function name, [argsJson] the JSON-encoded argument array from the
 * JS call site. Implementations return a JSON-encoded result value (or throw
 * — JsRuntime turns a thrown exception into a rejected JS Promise).
 */
fun interface NativeBridgeDispatcher {
    suspend fun dispatch(module: String, method: String, argsJson: String): String
}
