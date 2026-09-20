package com.sunflower.shortcut.sdk

import com.sunflower.shortcut.automation.runtime.NativeBridgeDispatcher
import org.json.JSONArray

/**
 * Aggregates whatever modules currently exist — an unknown module name (one
 * not yet built, e.g. "camera" today) fails with a clear message rather
 * than the generic "SDK-модули ещё не инициализированы" AutomationEngine
 * shows when no dispatcher is set at all, so partial SDK coverage during
 * development degrades honestly instead of silently.
 */
class SdkModuleDispatcher(modules: List<SdkModule>) : NativeBridgeDispatcher {

    private val modulesByName = modules.associateBy { it.name }

    override suspend fun dispatch(module: String, method: String, argsJson: String): String {
        val target = modulesByName[module]
            ?: throw SdkException("Модуль \"$module\" пока не поддерживается")

        val args = try {
            JSONArray(argsJson)
        } catch (e: Exception) {
            throw SdkException("Некорректные аргументы вызова $module.$method")
        }

        return target.call(method, args)
    }
}
