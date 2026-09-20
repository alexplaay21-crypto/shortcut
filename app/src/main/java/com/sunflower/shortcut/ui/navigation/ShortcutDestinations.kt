package com.sunflower.shortcut.ui.navigation

import android.net.Uri

/**
 * All navigation routes in one place (spec §4: Главная / AI / Настройки as
 * bottom-nav tabs; everything else — add/import/constructor/detail/help/etc —
 * is reached by pushing on top, per the reference mockups).
 */
object ShortcutDestinations {

    // Top-level tabs (bottom navigation)
    const val HOME = "home"
    const val AI = "ai"
    const val SETTINGS = "settings"

    val bottomNavRoutes = listOf(HOME, AI, SETTINGS)

    // Automations flow (spec §5, §6, §7, §8, §9)
    const val ADD_AUTOMATION = "add_automation"
    const val AUTOMATION_DETAIL = "automation_detail/{automationId}"
    const val IMPORT_JS = "import_js?uri={uri}"

    // Constructor + code viewer (spec §18, §19, §33)
    const val CONSTRUCTOR = "constructor/{automationId}"
    const val CODE_VIEW = "code_view/{automationId}"

    // AI flow (spec §10, §11, §12)
    const val AI_CREATE = "ai_create"
    const val AI_RESULT = "ai_result/{historyId}"
    const val AI_HISTORY = "ai_history"
    const val PROMPT_GUIDE = "prompt_guide"

    // Settings sub-screens (spec §21, §22, §23, §24, §25, §26, §27)
    const val PERMISSIONS = "permissions"
    const val ACCESSIBILITY_STATUS = "accessibility_status"
    const val HELP = "help"
    const val DONATE = "donate"
    const val ABOUT = "about"
    const val LICENSES = "licenses"

    fun automationDetailRoute(id: String) = "automation_detail/$id"
    fun constructorRoute(id: String) = "constructor/$id"
    fun codeViewRoute(id: String) = "code_view/$id"
    fun aiResultRoute(historyId: String) = "ai_result/$historyId"

    fun importRoute(uri: Uri): String = "import_js?uri=${Uri.encode(uri.toString())}"
}
