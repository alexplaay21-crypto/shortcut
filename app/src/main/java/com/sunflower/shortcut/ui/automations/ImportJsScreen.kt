package com.sunflower.shortcut.ui.automations

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sunflower.shortcut.automation.engine.AutomationEngine
import com.sunflower.shortcut.data.repositories.AutomationRepository
import com.sunflower.shortcut.ui.components.CodeBlock
import com.sunflower.shortcut.ui.components.InstallWarningDialog
import com.sunflower.shortcut.ui.components.PrimaryButton
import com.sunflower.shortcut.ui.components.ScenarioAnalysis
import com.sunflower.shortcut.ui.components.SecondaryButton
import kotlinx.coroutines.launch

private sealed interface ImportState {
    data object Loading : ImportState
    data class Ready(val js: String, val analysis: ScenarioAnalysis) : ImportState
    data class Failed(val message: String) : ImportState
}

@Composable
fun ImportJsScreen(
    uri: Uri,
    automationEngine: AutomationEngine,
    automationRepository: AutomationRepository,
    onInstalled: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<ImportState>(ImportState.Loading) }
    var showWarning by remember { mutableStateOf(false) }
    var installing by remember { mutableStateOf(false) }

    LaunchedEffect(uri) {
        state = try {
            val js = context.contentResolver.openInputStream(uri)?.use {
                it.readBytes().toString(Charsets.UTF_8)
            } ?: throw IllegalStateException("Не удалось прочитать файл")
            val analysis = automationEngine.analyze(js)
            if (analysis.errorMessage != null) {
                ImportState.Failed(analysis.errorMessage)
            } else {
                ImportState.Ready(js, analysis)
            }
        } catch (t: Throwable) {
            ImportState.Failed(t.message ?: "Не удалось прочитать файл")
        }
    }

    when (val s = state) {
        is ImportState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is ImportState.Failed -> {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Text("❌ Невозможно установить сценарий", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Ошибка JavaScript:",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    text = s.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )
                SecondaryButton(text = "Назад", onClick = onCancel)
            }
        }

        is ImportState.Ready -> {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(s.analysis.title, style = MaterialTheme.typography.titleLarge)
                        Text(
                            s.analysis.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )
                    }
                    item { Text("Код", style = MaterialTheme.typography.labelLarge) }
                    item { CodeBlock(code = s.js) }
                }
                Column(modifier = Modifier.padding(20.dp)) {
                    PrimaryButton(
                        text = "Установить",
                        loading = installing,
                        onClick = { showWarning = true }
                    )
                }
            }

            if (showWarning) {
                InstallWarningDialog(
                    scenarioTitle = s.analysis.title,
                    requirements = s.analysis.requirements,
                    onViewConstructor = { showWarning = false },
                    onDismiss = { showWarning = false },
                    onInstall = {
                        showWarning = false
                        installing = true
                        scope.launch {
                            val result = automationEngine.install(js = s.js, analysis = s.analysis)
                            installing = false
                            result.getOrNull()?.let(onInstalled)
                        }
                    }
                )
            }
        }
    }
}
