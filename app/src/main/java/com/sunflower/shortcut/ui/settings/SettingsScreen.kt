package com.sunflower.shortcut.ui.settings

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.sunflower.shortcut.BuildConfig
import com.sunflower.shortcut.data.datastore.SettingsDataStore
import com.sunflower.shortcut.ui.components.AppLanguage
import com.sunflower.shortcut.ui.components.ScreenTitle
import com.sunflower.shortcut.ui.components.SectionHeader
import com.sunflower.shortcut.ui.theme.AppTheme
import com.sunflower.shortcut.ui.theme.ShortcutRadius
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settingsDataStore: SettingsDataStore,
    onOpenPermissions: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenDonate: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by settingsDataStore.settingsFlow.collectAsState(initial = null)
    val current = settings ?: return

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { ScreenTitle(title = "Настройки") }

        item { SectionHeader(title = "Основное") }
        item {
            SettingsGroup {
                var languageMenu by remember { mutableStateOf(false) }
                Box {
                    SettingsRow(
                        title = "Язык",
                        value = if (current.language == AppLanguage.RU) "Русский" else "English",
                        onClick = { languageMenu = true }
                    )
                    DropdownMenu(expanded = languageMenu, onDismissRequest = { languageMenu = false }) {
                        DropdownMenuItem(text = { Text("Русский") }, onClick = {
                            languageMenu = false
                            scope.launch { settingsDataStore.setLanguage(AppLanguage.RU) }
                        })
                        DropdownMenuItem(text = { Text("English") }, onClick = {
                            languageMenu = false
                            scope.launch { settingsDataStore.setLanguage(AppLanguage.EN) }
                        })
                    }
                }

                var themeMenu by remember { mutableStateOf(false) }
                Box {
                    SettingsRow(
                        title = "Тема",
                        value = themeLabel(current.appTheme),
                        onClick = { themeMenu = true }
                    )
                    DropdownMenu(expanded = themeMenu, onDismissRequest = { themeMenu = false }) {
                        AppTheme.entries.forEach { theme ->
                            DropdownMenuItem(text = { Text(themeLabel(theme)) }, onClick = {
                                themeMenu = false
                                scope.launch { settingsDataStore.setTheme(theme) }
                            })
                        }
                    }
                }
            }
        }

        item { SectionHeader(title = "Автоматизация") }
        item {
            SettingsGroup {
                SettingsToggleRow(
                    title = "Уведомления о выполнении",
                    checked = current.notificationsOnRun,
                    onCheckedChange = { scope.launch { settingsDataStore.setNotificationsOnRun(it) } }
                )
                SettingsToggleRow(
                    title = "Запуск после перезагрузки",
                    checked = current.launchOnBoot,
                    onCheckedChange = { scope.launch { settingsDataStore.setLaunchOnBoot(it) } }
                )
            }
        }

        item { SectionHeader(title = "Доступ") }
        item {
            SettingsGroup {
                SettingsRow(title = "Accessibility", value = "Открыть", onClick = onOpenAccessibility)
                SettingsRow(title = "Разрешения", value = "Открыть", onClick = onOpenPermissions)
            }
        }

        item { SectionHeader(title = "Дополнительно") }
        item {
            SettingsGroup {
                SettingsToggleRow(
                    title = "Анимации",
                    checked = current.animationsEnabled,
                    onCheckedChange = { scope.launch { settingsDataStore.setAnimationsEnabled(it) } }
                )
                SettingsToggleRow(
                    title = "Компактный интерфейс",
                    checked = current.compactInterface,
                    onCheckedChange = { scope.launch { settingsDataStore.setCompactInterface(it) } }
                )
                SettingsRow(title = "Помощь", value = null, onClick = onOpenHelp)
                SettingsRow(title = "Поддержать разработчика", value = null, onClick = onOpenDonate)
                SettingsRow(
                    title = "Поделиться Shortcut",
                    value = null,
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Shortcut — локальная автоматизация Android. " +
                                    "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}"
                            )
                        }
                        context.startActivity(Intent.createChooser(intent, "Поделиться Shortcut"))
                    }
                )
                SettingsRow(title = "О приложении", value = null, onClick = onOpenAbout)
            }
        }
    }
}

private fun themeLabel(theme: AppTheme): String = when (theme) {
    AppTheme.SYSTEM -> "Системная"
    AppTheme.LIGHT -> "Светлая"
    AppTheme.DARK -> "Тёмная"
    AppTheme.AMOLED -> "AMOLED"
}

@Composable
private fun SettingsGroup(content: @Composable Column.() -> Unit) {
    Card(
        shape = ShortcutRadius.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun SettingsRow(title: String, value: String?, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = value?.let { v -> { Text(v, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    )
}

@Composable
private fun SettingsToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
        modifier = Modifier.fillMaxWidth()
    )
}
