package com.sunflower.shortcut.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.sunflower.shortcut.data.repositories.AIHistoryRepository
import com.sunflower.shortcut.data.repositories.AutomationRepository
import com.sunflower.shortcut.ui.components.AiHistoryStatus
import com.sunflower.shortcut.ui.components.AiHistoryUi
import com.sunflower.shortcut.ui.components.CodeBlock
import com.sunflower.shortcut.ui.components.InstallWarningDialog
import com.sunflower.shortcut.ui.components.PrimaryButton
import com.sunflower.shortcut.ui.components.SecondaryButton
import kotlinx.coroutines.launch

@Composable
fun AiResultScreen(
    historyId: String,
    aiHistoryRepository: AIHistoryRepository,
    automationEngine: AutomationEngine,
    automationRepository: AutomationRepository,
    onBack: () -> Unit,
    onInstalled: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var entry by remember { mutableStateOf<AiHistoryUi?>(null) }
    var showWarning by remember { mutableStateOf(false) }
    var installing by remember { mutableStateOf(false) }

    LaunchedEffect(historyId) {
        entry = aiHistoryRepository.getById(historyId)
    }

    val current = entry
    if (current == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Сценарий готов", style = MaterialTheme.typography.titleLarge)
                Text(
                    current.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            item { Text("Код", style = MaterialTheme.typography.labelLarge) }
            item { CodeBlock(code = current.jsCode) }
            item {
                Text(
                    "Описание:",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(current.description, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (current.status == AiHistoryStatus.INSTALLED) {
                Text(
                    "✓ Установлено",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary
                )
            } else {
                PrimaryButton(
                    text = "💾 Установить",
                    loading = installing,
                    onClick = { showWarning = true }
                )
            }
            SecondaryButton(text = "📤 Экспортировать", onClick = {
                scope.launch { automationRepository.shareRawJs(current.jsCode, current.title, context) }
            })
        }
    }

    if (showWarning) {
        var requirements by remember { mutableStateOf(emptyList<com.sunflower.shortcut.ui.components.RequirementUi>()) }
        LaunchedEffect(current.id) {
            requirements = automationEngine.analyze(current.jsCode).requirements
        }
        InstallWarningDialog(
            scenarioTitle = current.title,
            requirements = requirements,
            onViewConstructor = { showWarning = false },
            onDismiss = { showWarning = false },
            onInstall = {
                showWarning = false
                installing = true
                scope.launch {
                    val analysis = automationEngine.analyze(current.jsCode)
                    val result = automationEngine.install(js = current.jsCode, analysis = analysis)
                    installing = false
                    result.getOrNull()?.let { id ->
                        aiHistoryRepository.updateStatus(historyId, AiHistoryStatus.INSTALLED)
                        onInstalled(id)
                    }
                }
            }
        )
    }
}
