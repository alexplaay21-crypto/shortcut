package com.sunflower.shortcut.javascript.security

enum class SecuritySeverity { INFO, WARNING }

data class SecurityFinding(
    val message: String,
    val line: Int?,
    val column: Int?,
    val severity: SecuritySeverity
)

/** Coarse classification used to give higher-risk scenarios more visual
 *  weight in the install dialog — never blocks install by itself. */
enum class SecurityRiskLevel { LOW, MEDIUM, HIGH }

data class SecurityReport(
    val findings: List<SecurityFinding>,
    val riskLevel: SecurityRiskLevel,
    /** SHA-256 of the trimmed source — lets a future re-import/update flow
     *  detect that an automation's underlying code changed since approval. */
    val contentHash: String
)
