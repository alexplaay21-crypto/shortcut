package com.sunflower.shortcut.ai

import com.sunflower.shortcut.automation.runtime.JsHostScript
import com.sunflower.shortcut.automation.triggers.TriggerType

/**
 * The only class that needs to know what the Shortcut JS SDK looks like
 * from a *prompting* perspective — module names, trigger keys, output
 * format rules. AIProvider implementations stay completely ignorant of
 * this (spec §31: the AI layer doesn't touch the execution layer, and
 * within the AI layer itself, providers don't know app internals either —
 * PromptBuilder is the one seam that does).
 *
 * [NOT_AN_AUTOMATION] is a sentinel CodeGenerator (not yet generated)
 * checks for before attempting to parse anything as JS — this is how the
 * off-topic-request handling from spec §13 actually gets implemented: the
 * model self-reports rather than CodeGenerator guessing from broken code.
 */
object PromptBuilder {

    const val NOT_AN_AUTOMATION = "NOT_AN_AUTOMATION"

    fun build(userDescription: String): String = buildString {
        appendLine(systemInstructions())
        appendLine()
        appendLine("Доступные модули (вызываются как обычные async-функции, например camera.takePhoto({...})):")
        appendLine(JsHostScript.MODULE_NAMES.joinToString(", "))
        appendLine()
        appendLine("Доступные события для on(\"событие\", ...):")
        appendLine(TriggerType.entries.joinToString(", ") { it.key })
        appendLine()
        appendLine(example())
        appendLine()
        appendLine("Запрос пользователя:")
        appendLine(userDescription.trim())
    }

    private fun systemInstructions(): String = """
        Ты генерируешь JavaScript-сценарий для приложения Shortcut (локальная автоматизация Android).
        Строго следуй правилам:
        1. Верни ТОЛЬКО код на JavaScript — без markdown-разметки, без ```, без пояснений до или после кода.
        2. Сценарий должен содержать РОВНО ОДИН вызов on("событие", async () => { ... }) на верхнем уровне.
        3. Обработчик обязан быть объявлен как async.
        4. Используй только перечисленные ниже модули и wait(миллисекунды) — никакого eval, fetch, window, Reflect, Proxy, localStorage и других обращений за пределы SDK.
        5. Можно объявлять вспомогательные функции (function/const) на верхнем уровне рядом с on(...), но не более того.
        6. Если запрос пользователя не описывает автоматизацию (не указано ни событие, ни действие, или это вопрос не по теме приложения) — верни только строку: $NOT_AN_AUTOMATION
    """.trimIndent()

    private fun example(): String = """
        Пример хорошего результата:
        on("charging", async () => {
            await camera.takePhoto({ camera: "front" });
            await wait(1000);
            await camera.takePhoto({ camera: "front" });
        });
    """.trimIndent()
}
