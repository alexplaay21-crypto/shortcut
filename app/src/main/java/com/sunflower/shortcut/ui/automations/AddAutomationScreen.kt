package com.sunflower.shortcut.ui.automations

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sunflower.shortcut.ui.theme.ShortcutRadius

/**
 * "Импортировать JS" launches Android's document picker directly (SAF —
 * ACTION_OPEN_DOCUMENT) and only calls [onImportJs] once the user has
 * actually picked a file, handing its [Uri] straight to ImportJsScreen —
 * spec §8, no automatic install of anything the user didn't pick.
 */
@Composable
fun AddAutomationScreen(
    onCreateWithAi: () -> Unit,
    onImportJs: (Uri) -> Unit,
    onWriteManually: () -> Unit,
    onBack: () -> Unit
) {
    val pickJsFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(onImportJs) }

    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Новый сценарий", style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.Close, contentDescription = "Закрыть")
            }
        }

        Column(
            modifier = Modifier.padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AddOptionCard(
                icon = Icons.Filled.AutoAwesome,
                title = "Создать с AI",
                subtitle = "Опишите, что должна делать автоматизация",
                onClick = onCreateWithAi
            )
            AddOptionCard(
                icon = Icons.Filled.UploadFile,
                title = "Импортировать JS",
                subtitle = "Выбрать .js файл",
                onClick = { pickJsFile.launch(arrayOf("application/javascript", "text/javascript", "text/plain", "*/*")) }
            )
            AddOptionCard(
                icon = Icons.Filled.Code,
                title = "Написать код",
                subtitle = "Создать JavaScript вручную",
                onClick = onWriteManually
            )
        }
    }
}

@Composable
private fun AddOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = ShortcutRadius.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
