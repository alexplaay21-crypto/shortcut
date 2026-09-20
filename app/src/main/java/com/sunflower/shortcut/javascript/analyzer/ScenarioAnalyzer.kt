package com.sunflower.shortcut.javascript.analyzer

import com.sunflower.shortcut.automation.permissions.RequiredPermission
import com.sunflower.shortcut.automation.triggers.TriggerType
import com.sunflower.shortcut.javascript.ast.Program
import com.sunflower.shortcut.javascript.validator.ScenarioValidationException
import com.sunflower.shortcut.javascript.validator.ScenarioValidator
import com.sunflower.shortcut.ui.components.FlowStepKind
import com.sunflower.shortcut.ui.components.RequirementUi
import com.sunflower.shortcut.ui.components.ScenarioAnalysis

/**
 * End of the spec §32 pipeline: AST → Static Analyzer → Security Analyzer →
 * API Analyzer → Permission Analyzer → Flow Builder, collapsed into one
 * class since each stage here is small (ScenarioValidator does the
 * structural+security stages, FlowStepExtractor/RequirementDetector do the
 * API/permission/flow stages). AutomationEngine.analyze() is the only
 * caller and already treats a returned [ScenarioAnalysis] with
 * `errorMessage != null` as a syntax/validation failure — this class never
 * throws, so that contract holds for validation errors the same way it
 * already does for parser errors.
 */
class ScenarioAnalyzer {

    private val validator = ScenarioValidator()

    /**
     * [isGranted] lets the caller (AutomationEngine, which has a real
     * AutomationPermissionChecker + Context) report actual current grant
     * status per requirement. Defaults to "not granted" so a caller that
     * doesn't wire this up gets a conservative, attention-drawing result
     * rather than a falsely reassuring one.
     */
    fun analyze(program: Program, isGranted: (RequiredPermission) -> Boolean = { false }): ScenarioAnalysis {
        val validated = try {
            validator.validate(program)
        } catch (e: ScenarioValidationException) {
            return ScenarioAnalysis(
                title = "Сценарий",
                description = "",
                steps = emptyList(),
                requirements = emptyList(),
                errorMessage = e.message ?: "Ошибка проверки сценария",
                errorLine = e.line
            )
        }

        val steps = FlowStepExtractor.extract(validated)
        val requirements = RequirementDetector.detect(validated).map { permission ->
            RequirementUi(label = permission.label, emoji = permission.emoji, satisfied = isGranted(permission))
        }

        return ScenarioAnalysis(
            title = titleFor(validated.triggerKey),
            description = descriptionFor(validated.triggerKey, steps),
            steps = steps,
            requirements = requirements,
            triggerKey = validated.triggerKey
        )
    }

    private fun titleFor(triggerKey: String): String = TriggerType.fromKey(triggerKey)?.label ?: "Сценарий"

    private fun descriptionFor(triggerKey: String, steps: List<com.sunflower.shortcut.ui.components.FlowStepUi>): String {
        val actionCount = steps.count { it.kind == FlowStepKind.ACTION }
        val trigger = titleFor(triggerKey)
        return if (actionCount == 0) {
            "$trigger — без описанных действий"
        } else {
            "$trigger: выполняется $actionCount ${actionWord(actionCount)}"
        }
    }

    private fun actionWord(count: Int): String {
        val mod100 = count % 100
        val mod10 = count % 10
        return when {
            mod100 in 11..14 -> "действий"
            mod10 == 1 -> "действие"
            mod10 in 2..4 -> "действия"
            else -> "действий"
        }
    }
}
