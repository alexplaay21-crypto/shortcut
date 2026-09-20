package com.sunflower.shortcut.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Schema
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sunflower.shortcut.data.repositories.AutomationRepository
import com.sunflower.shortcut.ui.components.AutomationUi
import com.sunflower.shortcut.ui.components.EmptyState
import com.sunflower.shortcut.ui.components.ScenarioCard
import com.sunflower.shortcut.ui.components.ScreenTitle
import com.sunflower.shortcut.ui.components.SectionHeader
import com.sunflower.shortcut.utils.AppConfig
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    automationRepository: AutomationRepository,
    onOpenDetail: (String) -> Unit,
    onOpenConstructor: (String) -> Unit,
    onAddClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val automations by automationRepository.observeAll().collectAsState(initial = emptyList())
    var menuTarget by remember { mutableStateOf<AutomationUi?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить сценарий")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (automations.isEmpty()) {
                EmptyState(
                    emoji = "⚡",
                    title = "Пока нет сценариев",
                    description = "Создайте автоматизацию с помощью AI или импортируйте .js-файл"
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        ScreenTitle(title = "Shortcut", subtitle = AppConfig.DEVELOPER_NAME)
                    }
                    item {
                        SectionHeader(title = "Ваши сценарии  ${automations.size}")
                    }
                    items(automations, key = { it.id }) { automation ->
                        ScenarioCard(
                            automation = automation,
                            onClick = { onOpenDetail(automation.id) },
                            onLongClick = { menuTarget = automation },
                            onToggle = { enabled ->
                                scope.launch { automationRepository.setEnabled(automation.id, enabled) }
                            }
                        )
                    }
                }
            }
        }
    }

    val target = menuTarget
    if (target != null) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { menuTarget = null }, sheetState = sheetState) {
            Text(
                text = target.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            ListItem(
                headlineContent = { Text("Открыть конструкцию") },
                leadingContent = { Icon(Icons.Filled.Schema, contentDescription = null) },
                modifier = Modifier.clickableItem {
                    menuTarget = null
                    onOpenConstructor(target.id)
                }
            )
            ListItem(
                headlineContent = { Text("Открыть код") },
                leadingContent = { Icon(Icons.Filled.Code, contentDescription = null) },
                modifier = Modifier.clickableItem {
                    menuTarget = null
                    onOpenDetail(target.id)
                }
            )
            ListItem(
                headlineContent = { Text("Экспортировать") },
                leadingContent = { Icon(Icons.Filled.Upload, contentDescription = null) },
                modifier = Modifier.clickableItem {
                    menuTarget = null
                    scope.launch { automationRepository.exportAndShare(target.id, context) }
                }
            )
            ListItem(
                headlineContent = { Text("Поделиться") },
                leadingContent = { Icon(Icons.Filled.Share, contentDescription = null) },
                modifier = Modifier.clickableItem {
                    menuTarget = null
                    scope.launch { automationRepository.exportAndShare(target.id, context) }
                }
            )
            ListItem(
                headlineContent = { Text(if (target.enabled) "Выключить" else "Включить") },
                leadingContent = { Icon(Icons.Filled.PowerSettingsNew, contentDescription = null) },
                modifier = Modifier.clickableItem {
                    menuTarget = null
                    scope.launch { automationRepository.setEnabled(target.id, !target.enabled) }
                }
            )
            ListItem(
                headlineContent = { Text("Удалить", color = MaterialTheme.colorScheme.error) },
                leadingContent = {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                modifier = Modifier.clickableItem {
                    menuTarget = null
                    scope.launch { automationRepository.delete(target.id) }
                }
            )
            ListItem(
                headlineContent = { Text("Информация") },
                leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
                modifier = Modifier.clickableItem {
                    menuTarget = null
                    onOpenDetail(target.id)
                }
            )
        }
    }
}

private fun Modifier.clickableItem(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)
