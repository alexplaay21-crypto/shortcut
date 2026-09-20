package com.sunflower.shortcut.constructor

/**
 * What kind of editor a constructor value deserves (spec §18). Decided by
 * FlowBuilder from the SDK call, not from the step title — so renaming a
 * friendly title in FlowStepExtractor can't silently change which editor
 * opens.
 */
enum class EditableValueType {
    /** `wait(ms)` — shown and edited as "<число> <единица>", e.g. "5 секунд". */
    DELAY,

    /** First argument of `apps.launch(...)` — an app name. */
    APP,

    /** First argument of `apps.openUrl(...)`. */
    URL,

    /** First argument of `phone.call(...)`. */
    PHONE,

    /** Any other leading string argument, e.g. `notifications.show("...")`. */
    TEXT,

    /** A leading numeric argument; the new value must parse as a number. */
    NUMBER
}

/**
 * One value in the JS text the user is allowed to change from the
 * constructor. The JS source stays the single source of truth (spec §19):
 * this only *describes* where the value lives, ScenarioValuePatcher does the
 * actual replacement of exactly that span.
 *
 * FlowBuilder only creates an [EditableValue] when ScenarioValuePatcher can
 * locate and replace that literal, so the UI never offers a pencil for a
 * value the patcher would refuse. (Bad *input* — e.g. non-numeric text for a
 * [EditableValueType.NUMBER] — is still rejected by the patcher itself.)
 *
 * @property current the value as the editor should prefill it — the bare
 *   argument (`Telegram`), not the step's display subtitle, which joins all
 *   arguments (`Заголовок, Текст`).
 * @property line 1-based source line of the literal being edited.
 * @property column 1-based source column of the literal being edited.
 */
data class EditableValue(
    val type: EditableValueType,
    val current: String,
    val line: Int,
    val column: Int
)
