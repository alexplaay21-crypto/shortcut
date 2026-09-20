package com.sunflower.shortcut.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import com.sunflower.shortcut.ai.CodeGenerator
import com.sunflower.shortcut.automation.engine.AutomationEngine
import com.sunflower.shortcut.data.repositories.AIHistoryRepository
import com.sunflower.shortcut.ui.components.AiHistoryStatus
import com.sunflower.shortcut.ui.components.PrimaryButton
import com.sunflower.shortcut.ui.components.ScreenTitle
import kotlinx.coroutines.launch

@Composable
fun AiAssistantScreen(
    codeGenerator: CodeGenerator,
    automationEngine: AutomationEngine,
    aiHistoryRepository: AIHistoryRepository,
    onResult: (String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenGuide: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var prompt by remember { mutableStateOf("") }
    var generating by remember { mutableStateOf(false) }
    var errorTitle by remember { mutableStateOf<String?>(null) }
    var errorBody by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ScreenTitle(title = "AI-помощник")
            Row {
                IconButton(onClick = onOpenGuide) {
                    Icon(Icons.Filled.HelpOutline, contentDescription = "Как писать промт")
                }
                IconButton(onClick = onOpenHistory) {
                    Icon(Icons.Filled.History, contentDescription = "История")
                }
            }
        }

        Text(
            "Опишите, что должна делать автоматизация",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it; errorTitle = null },
            placeholder = {
                Text("Например: Когда подключается зарядка, сделай фото и отправь его в Telegram.")
            },
            modifier = Modifier.fillMaxWidth().weight(1f),
            isError = errorTitle != null
        )

        if (errorTitle != null) {
            Text(
                text = "❌ ${errorTitle.orEmpty()}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp)
            )
            if (errorBody != null) {
                Text(
                    text = errorBody.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        PrimaryButton(
            text = "Создать сценарий",
            enabled = prompt.isNotBlank(),
            loading = generating,
            modifier = Modifier.padding(top = 16.dp),
            onClick = {
                generating = true
                errorTitle = null
                scope.launch {
                    val generationResult = codeGenerator.generate(prompt)
                    val js = generationResult.getOrNull()
                    if (js == null) {
                        generating = false
                        errorTitle = "Не удалось создать сценарий"
                        errorBody = generationResult.exceptionOrNull()?.message
                            ?: "Запрос не описывает автоматизацию. Опишите, когда " +
                                "должен запускаться сценарий и какое действие выполнить."
                        return@launch
                    }
                    val analysis = automationEngine.analyze(js)
                    generating = false
                    if (analysis.errorMessage != null) {
                        errorTitle = "Не удалось создать сценарий"
                        errorBody = analysis.errorMessage
                        return@launch
                    }
                    val historyId = aiHistoryRepository.add(
                        prompt = prompt,
                        jsCode = js,
                        title = analysis.title,
                        description = analysis.description,
                        status = AiHistoryStatus.NOT_INSTALLED
                    )
                    onResult(historyId)
                }
            }
        )

        Text(
            "AI создаёт JavaScript. Shortcut проверяет и выполняет его локально.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}
