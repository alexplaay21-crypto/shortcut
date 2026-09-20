package com.sunflower.shortcut.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Mandatory warning shown before installing any external ".js" scenario or
 * AI-generated scenario (spec §7). Lists the declared requirements
 * (spec §20) so the user knows exactly what the scenario can touch.
 */
@Composable
fun InstallWarningDialog(
    scenarioTitle: String,
    requirements: List<RequirementUi>,
    onViewConstructor: () -> Unit,
    onDismiss: () -> Unit,
    onInstall: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("⚠️ Внимание") },
        text = {
            Column {
                Text(
                    "Сценарий «$scenarioTitle» может получать доступ к функциям " +
                        "устройства и другим приложениям.\n\n" +
                        "Устанавливайте только сценарии, которым доверяете. " +
                        "Перед установкой вы можете посмотреть, какие действия он выполняет."
                )
                if (requirements.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Этот сценарий использует:", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    requirements.forEach { req ->
                        Text(
                            text = "${if (req.satisfied) "✓" else "✗"} ${req.emoji} ${req.label}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (req.satisfied) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onInstall) { Text("Установить") }
        },
        dismissButton = {
            Column {
                TextButton(onClick = onViewConstructor) { Text("Посмотреть конструкцию") }
                TextButton(onClick = onDismiss) { Text("Отмена") }
            }
        }
    )
}
