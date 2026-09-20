package com.sunflower.shortcut.ui.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.sunflower.shortcut.ai.CodeGenerator
import com.sunflower.shortcut.automation.engine.AutomationEngine
import com.sunflower.shortcut.data.datastore.SettingsDataStore
import com.sunflower.shortcut.data.repositories.AIHistoryRepository
import com.sunflower.shortcut.data.repositories.AutomationRepository
import com.sunflower.shortcut.ui.about.AboutScreen
import com.sunflower.shortcut.ui.about.LicensesScreen
import com.sunflower.shortcut.ui.ai.AiAssistantScreen
import com.sunflower.shortcut.ui.ai.AiHistoryScreen
import com.sunflower.shortcut.ui.ai.AiResultScreen
import com.sunflower.shortcut.ui.ai.PromptGuideScreen
import com.sunflower.shortcut.ui.automations.AddAutomationScreen
import com.sunflower.shortcut.ui.automations.AutomationDetailScreen
import com.sunflower.shortcut.ui.automations.ImportJsScreen
import com.sunflower.shortcut.ui.automations.WriteCodeScreen
import com.sunflower.shortcut.ui.constructor.CodeViewScreen
import com.sunflower.shortcut.ui.constructor.ConstructorScreen
import com.sunflower.shortcut.ui.donate.DonateScreen
import com.sunflower.shortcut.ui.help.HelpScreen
import com.sunflower.shortcut.ui.home.HomeScreen
import com.sunflower.shortcut.ui.settings.AccessibilityStatusScreen
import com.sunflower.shortcut.ui.settings.PermissionsScreen
import com.sunflower.shortcut.ui.settings.SettingsScreen

private data class BottomTab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomTabs = listOf(
    BottomTab(ShortcutDestinations.HOME, "Главная", Icons.Filled.Home),
    BottomTab(ShortcutDestinations.AI, "AI", Icons.Filled.AutoAwesome),
    BottomTab(ShortcutDestinations.SETTINGS, "Настройки", Icons.Filled.Settings)
)

@Composable
fun ShortcutNavHost(
    navController: NavHostController,
    automationEngine: AutomationEngine,
    automationRepository: AutomationRepository,
    aiHistoryRepository: AIHistoryRepository,
    settingsDataStore: SettingsDataStore,
    codeGenerator: CodeGenerator
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in ShortcutDestinations.bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = ShortcutDestinations.HOME,
            modifier = androidx.compose.ui.Modifier.padding(padding)
        ) {
            composable(ShortcutDestinations.HOME) {
                HomeScreen(
                    automationRepository = automationRepository,
                    onOpenDetail = { id ->
                        navController.navigate(ShortcutDestinations.automationDetailRoute(id))
                    },
                    onOpenConstructor = { id ->
                        navController.navigate(ShortcutDestinations.constructorRoute(id))
                    },
                    onAddClick = { navController.navigate(ShortcutDestinations.ADD_AUTOMATION) }
                )
            }

            composable(ShortcutDestinations.ADD_AUTOMATION) {
                AddAutomationScreen(
                    onCreateWithAi = {
                        navController.popBackStack()
                        navController.navigate(ShortcutDestinations.AI)
                    },
                    onImportJs = { uri ->
                        navController.popBackStack()
                        navController.navigate(ShortcutDestinations.importRoute(uri))
                    },
                    onWriteManually = {
                        navController.navigate("write_code")
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("write_code") {
                WriteCodeScreen(
                    automationEngine = automationEngine,
                    automationRepository = automationRepository,
                    onInstalled = { id ->
                        navController.popBackStack(ShortcutDestinations.HOME, inclusive = false)
                        navController.navigate(ShortcutDestinations.automationDetailRoute(id))
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = ShortcutDestinations.IMPORT_JS,
                arguments = listOf(navArgument("uri") { type = NavType.StringType })
            ) { entry ->
                val uriArg = entry.arguments?.getString("uri").orEmpty()
                ImportJsScreen(
                    uri = Uri.parse(Uri.decode(uriArg)),
                    automationEngine = automationEngine,
                    automationRepository = automationRepository,
                    onInstalled = { id ->
                        navController.popBackStack(ShortcutDestinations.HOME, inclusive = false)
                        navController.navigate(ShortcutDestinations.automationDetailRoute(id))
                    },
                    onCancel = { navController.popBackStack() }
                )
            }

            composable(
                route = ShortcutDestinations.AUTOMATION_DETAIL,
                arguments = listOf(navArgument("automationId") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("automationId").orEmpty()
                AutomationDetailScreen(
                    automationId = id,
                    automationRepository = automationRepository,
                    onBack = { navController.popBackStack() },
                    onOpenConstructor = { navController.navigate(ShortcutDestinations.constructorRoute(id)) },
                    onOpenCode = { navController.navigate(ShortcutDestinations.codeViewRoute(id)) },
                    onDeleted = { navController.popBackStack(ShortcutDestinations.HOME, inclusive = false) }
                )
            }

            composable(
                route = ShortcutDestinations.CONSTRUCTOR,
                arguments = listOf(navArgument("automationId") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("automationId").orEmpty()
                ConstructorScreen(
                    automationId = id,
                    automationRepository = automationRepository,
                    onBack = { navController.popBackStack() },
                    onOpenCode = { navController.navigate(ShortcutDestinations.codeViewRoute(id)) }
                )
            }

            composable(
                route = ShortcutDestinations.CODE_VIEW,
                arguments = listOf(navArgument("automationId") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("automationId").orEmpty()
                CodeViewScreen(
                    automationId = id,
                    automationRepository = automationRepository,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(ShortcutDestinations.AI) {
                AiAssistantScreen(
                    codeGenerator = codeGenerator,
                    automationEngine = automationEngine,
                    aiHistoryRepository = aiHistoryRepository,
                    onResult = { historyId ->
                        navController.navigate(ShortcutDestinations.aiResultRoute(historyId))
                    },
                    onOpenHistory = { navController.navigate(ShortcutDestinations.AI_HISTORY) },
                    onOpenGuide = { navController.navigate(ShortcutDestinations.PROMPT_GUIDE) }
                )
            }

            composable(
                route = ShortcutDestinations.AI_RESULT,
                arguments = listOf(navArgument("historyId") { type = NavType.StringType })
            ) { entry ->
                val historyId = entry.arguments?.getString("historyId").orEmpty()
                AiResultScreen(
                    historyId = historyId,
                    aiHistoryRepository = aiHistoryRepository,
                    automationEngine = automationEngine,
                    automationRepository = automationRepository,
                    onBack = { navController.popBackStack() },
                    onInstalled = { id ->
                        navController.popBackStack(ShortcutDestinations.AI, inclusive = false)
                        navController.navigate(ShortcutDestinations.automationDetailRoute(id))
                    }
                )
            }

            composable(ShortcutDestinations.AI_HISTORY) {
                AiHistoryScreen(
                    aiHistoryRepository = aiHistoryRepository,
                    onOpenEntry = { id -> navController.navigate(ShortcutDestinations.aiResultRoute(id)) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(ShortcutDestinations.PROMPT_GUIDE) {
                PromptGuideScreen(onBack = { navController.popBackStack() })
            }

            composable(ShortcutDestinations.SETTINGS) {
                SettingsScreen(
                    settingsDataStore = settingsDataStore,
                    onOpenPermissions = { navController.navigate(ShortcutDestinations.PERMISSIONS) },
                    onOpenAccessibility = { navController.navigate(ShortcutDestinations.ACCESSIBILITY_STATUS) },
                    onOpenHelp = { navController.navigate(ShortcutDestinations.HELP) },
                    onOpenDonate = { navController.navigate(ShortcutDestinations.DONATE) },
                    onOpenAbout = { navController.navigate(ShortcutDestinations.ABOUT) }
                )
            }

            composable(ShortcutDestinations.PERMISSIONS) {
                PermissionsScreen(
                    automationRepository = automationRepository,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(ShortcutDestinations.ACCESSIBILITY_STATUS) {
                AccessibilityStatusScreen(onBack = { navController.popBackStack() })
            }

            composable(ShortcutDestinations.HELP) {
                HelpScreen(onBack = { navController.popBackStack() })
            }

            composable(ShortcutDestinations.DONATE) {
                DonateScreen(onBack = { navController.popBackStack() })
            }

            composable(ShortcutDestinations.ABOUT) {
                AboutScreen(
                    onBack = { navController.popBackStack() },
                    onOpenLicenses = { navController.navigate(ShortcutDestinations.LICENSES) }
                )
            }

            composable(ShortcutDestinations.LICENSES) {
                LicensesScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
