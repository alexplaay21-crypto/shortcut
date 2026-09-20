package com.sunflower.shortcut.ui.automations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sunflower.shortcut.data.repositories.AutomationRepository
import com.sunflower.shortcut.ui.theme.ShortcutRadius
import kotlinx.coroutines.launch

@Composable
fun AutomationDetailScreen(
    automationId: String,
    automationRepository: AutomationRepository,
    onBack: () -> Unit,
    onOpenConstructor: () -> Unit,
    onOpenCode: () -> Unit,
    onDeleted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val automation by automationRepository.observeById(automationId).collectAsState(initial = null)
    val steps by automationRepository.observeSteps(automationId).collectAsState(initial = emptyList())
    var menuExpanded by remember { mutableStateOf(false) }

    val current = automation
    if (current == null) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxWidth().padding(40.dp),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator() }
        return
    }

    Column {
        TopAppBar(
            title = { Text(current.title) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
            },
            actions = {
                Switch(
                    checked = current.enabled,
                    onCheckedChange = { enabled ->
                        scope.launch { automationRepository.setEnabled(automationId, enabled) }
                    }
                )
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Меню")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Открыть конструкцию") },
                        onClick = { menuExpanded = false; onOpenConstructor() }
                    )
                    DropdownMenuItem(
                        text = { Text("Открыть код") },
                        onClick = { menuExpanded = false; onOpenCode() }
                    )
                    DropdownMenuItem(
                        text = { Text("Экспортировать") },
                        onClick = {
                            menuExpanded = false
                            scope.launch { automationRepository.exportAndShare(automationId, context) }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Поделиться") },
                        onClick = {
                            menuExpanded = false
                            scope.launch { automationRepository.exportAndShare(automationId, context) }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Удалить", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            scope.launch {
                                automationRepository.delete(automationId)
                                onDeleted()
                            }
                        }
                    )
                }
            }
        )

        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Text(current.subtitle, style = MaterialTheme.typography.bodyLarge) }
            item {
                Text(
                    "Действия",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(steps, key = { it.id }) { step ->
                Card(
                    shape = ShortcutRadius.card,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(step.emoji, style = MaterialTheme.typography.titleMedium)
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(step.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                step.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            if (current.lastRunLabel != null) {
                item {
                    Card(
                        shape = ShortcutRadius.card,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Последний запуск", style = MaterialTheme.typography.labelLarge)
                            Text(current.lastRunLabel, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = if (current.lastRunSuccess == true) "Успешно" else "Ошибка",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (current.lastRunSuccess == true) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
