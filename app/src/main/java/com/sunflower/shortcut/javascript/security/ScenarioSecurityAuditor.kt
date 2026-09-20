package com.sunflower.shortcut.javascript.security

import com.sunflower.shortcut.automation.permissions.RequiredPermission
import com.sunflower.shortcut.javascript.ast.BinaryExpression
import com.sunflower.shortcut.javascript.ast.MemberExpression
import com.sunflower.shortcut.javascript.ast.NumericLiteral
import com.sunflower.shortcut.javascript.ast.Program
import com.sunflower.shortcut.javascript.ast.StringLiteral
import com.sunflower.shortcut.javascript.ast.walk
import com.sunflower.shortcut.javascript.validator.AllowedGlobals
import java.security.MessageDigest

/**
 * A second, non-fatal pass beyond ScenarioValidator's hard pass/fail gate
 * (spec §32 lists "Static Analyzer" and "Security Analyzer" as separate
 * pipeline stages). Nothing here blocks install — findings are informational,
 * for a future detailed security view or extra install-dialog emphasis on
 * high-risk scenarios. It also closes one real gap ScenarioValidator leaves:
 * that validator only checks *literal* `.foo` access, not computed
 * `obj["foo"]` access to the same dangerous property names.
 */
object ScenarioSecurityAuditor {

    private val HIGH_RISK = setOf(
        RequiredPermission.CAMERA, RequiredPermission.LOCATION,
        RequiredPermission.MICROPHONE, RequiredPermission.ACCESSIBILITY
    )
    private val MEDIUM_RISK = setOf(
        RequiredPermission.CONTACTS, RequiredPermission.PHONE,
        RequiredPermission.BLUETOOTH, RequiredPermission.NOTIFICATIONS
    )

    fun audit(program: Program, source: String, requirements: List<RequiredPermission>): SecurityReport {
        val findings = mutableListOf<SecurityFinding>()

        program.walk { node ->
            when (node) {
                is MemberExpression -> if (node.computed) {
                    val key = node.computedProperty
                    when {
                        key is StringLiteral && key.value in AllowedGlobals.DENIED_PROPERTY_NAMES -> {
                            findings += SecurityFinding(
                                "Обращение к \"${key.value}\" через [] запрещено так же, как через точку",
                                node.line, node.column, SecuritySeverity.WARNING
                            )
                        }
                        key !is StringLiteral && key !is NumericLiteral -> {
                            findings += SecurityFinding(
                                "Динамическое обращение к свойству — невозможно статически проверить безопасность",
                                node.line, node.column, SecuritySeverity.INFO
                            )
                        }
                    }
                }

                is BinaryExpression -> if (node.operator == "+") {
                    val left = node.left
                    val right = node.right
                    if (left is StringLiteral && right is StringLiteral) {
                        val combined = left.value + right.value
                        if (AllowedGlobals.DENIED_IDENTIFIERS.any { combined.contains(it, ignoreCase = true) }) {
                            findings += SecurityFinding(
                                "Похоже на попытку собрать запрещённое имя из частей строки",
                                node.line, node.column, SecuritySeverity.WARNING
                            )
                        }
                    }
                }

                else -> {}
            }
        }

        val risk = when {
            requirements.any { it in HIGH_RISK } -> SecurityRiskLevel.HIGH
            requirements.any { it in MEDIUM_RISK } -> SecurityRiskLevel.MEDIUM
            else -> SecurityRiskLevel.LOW
        }

        return SecurityReport(findings, risk, contentHash(source))
    }

    fun contentHash(source: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(source.trim().toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
