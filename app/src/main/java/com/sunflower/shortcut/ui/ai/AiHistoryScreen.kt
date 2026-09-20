package com.sunflower.shortcut.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sunflower.shortcut.data.repositories.AIHistoryRepository
import com.sunflower.shortcut.ui.components.EmptyState
import com.sunflower.shortcut.ui.components.AiHistoryStatus
import com.sunflower.shortcut.ui.theme.ShortcutRadius

@Composable
fun AiHistoryScreen(
    aiHistoryRepository: AIHistoryRepository,
    onOpenEntry: (String) -> Unit,
    onBack: () -> Unit
) {
    val history by aiHistoryRepository.observeAll().collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("История AI") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
            }
        )

        if (history.isEmpty()) {
            EmptyState(
                emoji = "✨",
                title = "История пуста",
                description = "Здесь появятся сценарии, созданные с помощью AI"
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(history, key = { it.id }) { entry ->
                    Card(
                        onClick = { onOpenEntry(entry.id) },
                        shape = ShortcutRadius.card,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                            Text("${entry.emoji} ${entry.title}", style = MaterialTheme.typography.titleMedium)
                            Text(
                                entry.dateLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                entry.description,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                            Row(modifier = Modifier.padding(top = 8.dp)) {
                                Text(
                                    text = when (entry.status) {
                                        AiHistoryStatus.INSTALLED -> "✓ Установлен"
                                        AiHistoryStatus.NOT_INSTALLED -> "Не установлен"
                                        AiHistoryStatus.FAILED -> "Ошибка"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when (entry.status) {
                                        AiHistoryStatus.INSTALLED -> MaterialTheme.colorScheme.tertiary
                                        AiHistoryStatus.FAILED -> MaterialTheme.colorScheme.error
                                        AiHistoryStatus.NOT_INSTALLED -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
