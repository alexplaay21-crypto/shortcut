package com.sunflower.shortcut.ui.automations

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.sunflower.shortcut.automation.engine.AutomationEngine
import com.sunflower.shortcut.data.repositories.AutomationRepository
import com.sunflower.shortcut.ui.components.InstallWarningDialog
import com.sunflower.shortcut.ui.components.PrimaryButton
import com.sunflower.shortcut.ui.components.ScenarioAnalysis
import kotlinx.coroutines.launch

private const val TEMPLATE = "on(\"charging\", async () => {\n    \n});\n"

@Composable
fun WriteCodeScreen(
    automationEngine: AutomationEngine,
    automationRepository: AutomationRepository,
    onInstalled: (String) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf(TEMPLATE) }
    var checking by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pendingAnalysis by remember { mutableStateOf<ScenarioAnalysis?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Написать код", style = MaterialTheme.typography.titleLarge)
        Text(
            "Обычный JavaScript с Shortcut SDK — on(), camera.*, apps.*, ui.* и другие модули.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        OutlinedTextField(
            value = code,
            onValueChange = { code = it; errorMessage = null },
            modifier = Modifier.fillMaxWidth().weight(1f),
            textStyle = com.sunflower.shortcut.ui.theme.CodeTextStyle,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            isError = errorMessage != null
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        PrimaryButton(
            text = "Проверить и установить",
            loading = checking,
            modifier = Modifier.padding(top = 16.dp),
            onClick = {
                checking = true
                errorMessage = null
                scope.launch {
                    val analysis = automationEngine.analyze(code)
                    checking = false
                    if (analysis.errorMessage != null) {
                        errorMessage = analysis.errorMessage
                    } else {
                        pendingAnalysis = analysis
                    }
                }
            }
        )
    }

    val analysis = pendingAnalysis
    if (analysis != null) {
        InstallWarningDialog(
            scenarioTitle = analysis.title,
            requirements = analysis.requirements,
            onViewConstructor = { pendingAnalysis = null },
            onDismiss = { pendingAnalysis = null },
            onInstall = {
                pendingAnalysis = null
                scope.launch {
                    val result = automationEngine.install(js = code, analysis = analysis)
                    result.getOrNull()?.let(onInstalled)
                }
            }
        )
    }
}
