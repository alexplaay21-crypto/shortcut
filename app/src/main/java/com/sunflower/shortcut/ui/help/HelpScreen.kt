package com.sunflower.shortcut.ui.help

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sunflower.shortcut.ui.components.HelpTopicUi
import com.sunflower.shortcut.ui.theme.ShortcutRadius

private val topics = listOf(
    HelpTopicUi("1", "Что такое Shortcut", "Shortcut — локальная система автоматизации Android. Сценарии — обычные JavaScript-файлы, которые выполняются на устройстве через Automation Engine, без обязательного сервера."),
    HelpTopicUi("2", "Как создать сценарий", "Нажмите «+» на главном экране и выберите способ: с помощью AI, импорт .js-файла или ручное написание кода."),
    HelpTopicUi("3", "Как создать сценарий через AI", "Откройте раздел AI, опишите обычным языком, что и когда должно происходить. AI сгенерирует JavaScript, который нужно будет проверить и установить."),
    HelpTopicUi("4", "Как импортировать JS", "Выберите «Импортировать JS» и укажите .js-файл в памяти телефона. Shortcut проверит синтаксис и покажет конструкцию перед установкой."),
    HelpTopicUi("5", "Где находятся файлы сценариев", "Код каждого установленного сценария хранится внутри Shortcut и доступен на экране «Код» — оттуда его можно скопировать или сохранить как .js-файл."),
    HelpTopicUi("6", "Как экспортировать сценарий", "В меню сценария выберите «Экспортировать» — файл можно сохранить или отправить через системное меню Android."),
    HelpTopicUi("7", "Как поделиться сценарием", "В меню сценария выберите «Поделиться» — откроется стандартный Android Share Sheet: Telegram, почта, Google Drive и другие приложения."),
    HelpTopicUi("8", "Как работает конструктор", "Конструктор превращает JavaScript в наглядную схему: триггер, действия, условия, задержки. Часть значений можно менять прямо в конструкторе."),
    HelpTopicUi("9", "Как включить Accessibility", "Настройки → Accessibility Service → «Открыть настройки Android» и включите Shortcut в списке специальных возможностей. Нужен только сценариям, которые взаимодействуют с интерфейсом других приложений."),
    HelpTopicUi("10", "Как выдавать разрешения", "Разрешения запрашиваются только тогда, когда они реально нужны конкретному сценарию. Полный список — в Настройки → Разрешения."),
    HelpTopicUi("11", "Как отключить сценарий", "Переключите тумблер рядом со сценарием на главном экране или откройте его меню и выберите «Отключить»."),
    HelpTopicUi("12", "Как удалить сценарий", "Долгое нажатие на сценарий → «Удалить». Действие необратимо."),
    HelpTopicUi("13", "Как написать хороший prompt для AI", "Опишите: что должен делать сценарий, когда он запускается, какие приложения использовать и что делать при ошибке. Подробнее — в разделе «Как правильно писать запросы»."),
    HelpTopicUi("14", "Безопасность сценариев", "JavaScript выполняется только через контролируемый Shortcut SDK — без доступа к системным API напрямую. Перед установкой любого сценария показывается список используемых возможностей."),
    HelpTopicUi("15", "Ограничения Android", "Некоторые функции ограничены политиками Android (например, фоновые задачи и Accessibility). Shortcut работает строго в рамках этих ограничений и не пытается их обходить.")
)

@Composable
fun HelpScreen(onBack: () -> Unit) {
    var expandedId by remember { mutableStateOf<String?>(null) }

    Column {
        TopAppBar(
            title = { Text("Как пользоваться Shortcut") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
            }
        )

        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(topics, key = { it.id }) { topic ->
                val expanded = expandedId == topic.id
                Card(
                    onClick = { expandedId = if (expanded) null else topic.id },
                    shape = ShortcutRadius.card,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                            .padding(14.dp)
                    ) {
                        Text(topic.title, style = MaterialTheme.typography.titleMedium)
                        if (expanded) {
                            Text(
                                topic.body,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
