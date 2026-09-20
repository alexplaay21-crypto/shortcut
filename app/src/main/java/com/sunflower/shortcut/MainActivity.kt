package com.sunflower.shortcut

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.sunflower.shortcut.ui.navigation.ShortcutDestinations
import com.sunflower.shortcut.ui.navigation.ShortcutNavHost
import com.sunflower.shortcut.ui.theme.ShortcutTheme

/**
 * Single Activity for the whole app (spec §4/§42 — one UI entry point,
 * everything else is a Composable destination in ui/navigation).
 *
 * Two responsibilities live here on purpose, nothing else:
 *  1. Apply the persisted theme/appearance before first frame.
 *  2. Capture an incoming ".js" file — either opened directly
 *     (ACTION_VIEW, e.g. from a file manager) or shared from another app
 *     (ACTION_SEND, spec §9/§34) — and hand its Uri to the import screen.
 *
 * This activity never parses, validates or executes the JS itself — that
 * pipeline lives entirely in `javascript.parser` / `javascript.analyzer`
 * (spec §8/§32), reached only via navigation.
 */
class MainActivity : ComponentActivity() {

    private var pendingImportUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        pendingImportUri = extractImportUri(intent)

        setContent {
            val app = application as ShortcutApplication
            val settings by app.settingsDataStore.settingsFlow.collectAsState(initial = null)

            // Hold the first frame until settings are read once, so the UI
            // never flashes the wrong theme (spec §21 — Системная/Светлая/Тёмная).
            val loadedSettings = settings ?: return@setContent

            ShortcutTheme(appTheme = loadedSettings.appTheme) {
                val navController = rememberNavController()

                LaunchedEffect(pendingImportUri) {
                    pendingImportUri?.let { uri ->
                        navController.navigate(ShortcutDestinations.importRoute(uri))
                        pendingImportUri = null
                    }
                }

                ShortcutNavHost(
                    navController = navController,
                    automationEngine = app.automationEngine,
                    automationRepository = app.automationRepository,
                    aiHistoryRepository = app.aiHistoryRepository,
                    settingsDataStore = app.settingsDataStore,
                    codeGenerator = app.codeGenerator
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractImportUri(intent)?.let { pendingImportUri = it }
    }

    private fun extractImportUri(intent: Intent?): Uri? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> {
                val importableType = intent.type == "application/javascript" ||
                    intent.type == "text/javascript" ||
                    intent.type == "text/plain" ||
                    intent.type == "application/octet-stream"
                if (!importableType) return null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
            }
            else -> null
        }
    }
}
