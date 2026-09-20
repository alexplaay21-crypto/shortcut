package com.sunflower.shortcut.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sunflower.shortcut.ui.theme.ShortcutRadius

private data class PromptExample(val bad: String, val good: String)

private val checklist = listOf(
    "Что должен делать сценарий",
    "Когда он должен запускаться",
    "Какие приложения использовать",
    "Какие данные передавать",
    "Какие условия использовать",
    "Что должно произойти при ошибке"
)

private val examples = listOf(
    PromptExample(
        bad = "Сделай что-нибудь с Telegram",
        good = "Когда я подключаю зарядку, открой Telegram, открой Избранное и отправь " +
            "сообщение «Телефон заряжается»."
    ),
    PromptExample(
        bad = "Сфоткай меня",
        good = "Когда подключается зарядка, сделай фото на фронтальную камеру, подожди " +
            "1 секунду и сделай ещё одно."
    ),
    PromptExample(
        bad = "Уведоми меня о батарее",
        good = "Когда заряд батареи ниже 20%, покажи уведомление «Низкий заряд»."
    )
)

@Composable
fun PromptGuideScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Как правильно писать запросы") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
            }
        )

        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Опишите в запросе:", style = MaterialTheme.typography.labelLarge)
            }
            items(checklist) { point ->
                Text("•  $point", style = MaterialTheme.typography.bodyMedium)
            }
            item {
                Text(
                    "Примеры",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(examples) { example ->
                Card(
                    shape = ShortcutRadius.card,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(
                            "Плохо",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(example.bad, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Хорошо",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                        Text(example.good, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
