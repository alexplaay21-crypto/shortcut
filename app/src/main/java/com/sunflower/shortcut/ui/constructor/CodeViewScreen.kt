package com.sunflower.shortcut.ui.constructor

import android.content.ClipData
import android.content.ClipboardManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sunflower.shortcut.data.repositories.AutomationRepository
import com.sunflower.shortcut.ui.components.CodeBlock

@Composable
fun CodeViewScreen(
    automationId: String,
    automationRepository: AutomationRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var code by remember { mutableStateOf<String?>(null) }
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(automationId) {
        code = automationRepository.getCode(automationId)
    }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/javascript")
    ) { uri ->
        val js = code
        if (uri != null && js != null) {
            context.contentResolver.openOutputStream(uri)?.use { it.write(js.toByteArray()) }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Код") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
            }
        )

        val currentCode = code
        if (currentCode == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            if (searchOpen) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Поиск по коду") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            val displayedCode = remember(currentCode, query) {
                if (query.isBlank()) {
                    currentCode
                } else {
                    currentCode.lineSequence().filter { it.contains(query, ignoreCase = true) }
                        .joinToString("\n").ifBlank { "// Совпадений не найдено" }
                }
            }

            CodeBlock(
                code = displayedCode,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = { searchOpen = !searchOpen }) {
                    Icon(Icons.Filled.Search, contentDescription = null)
                    Text(" Поиск", style = MaterialTheme.typography.labelLarge)
                }
                TextButton(onClick = {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard?.setPrimaryClip(ClipData.newPlainText("automation.js", currentCode))
                }) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null)
                    Text(" Копировать", style = MaterialTheme.typography.labelLarge)
                }
                TextButton(onClick = { saveLauncher.launch("automation.js") }) {
                    Icon(Icons.Filled.Save, contentDescription = null)
                    Text(" Сохранить", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
