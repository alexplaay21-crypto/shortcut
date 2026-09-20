package com.sunflower.shortcut.ui.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sunflower.shortcut.BuildConfig
import com.sunflower.shortcut.ui.theme.ShortcutRadius
import com.sunflower.shortcut.utils.AppConfig
import androidx.compose.foundation.clickable

@Composable
fun AboutScreen(onBack: () -> Unit, onOpenLicenses: () -> Unit) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("О приложении") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
            }
        )

        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("⚡", style = MaterialTheme.typography.displaySmall)
                Text("Shortcut", style = MaterialTheme.typography.titleLarge)
                Text(
                    AppConfig.DEVELOPER_NAME,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Версия ${AppConfig.APP_VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Card(
                shape = ShortcutRadius.card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AboutRow("Связаться с разработчиком") {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:${AppConfig.DEVELOPER_EMAIL}")
                            putExtra(Intent.EXTRA_SUBJECT, "Shortcut — обращение пользователя")
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                    }
                    AboutRow("Документация") {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppConfig.DOCUMENTATION_URL)))
                    }
                    AboutRow("Политика конфиденциальности") {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppConfig.PRIVACY_POLICY_URL)))
                    }
                    AboutRow("Условия использования") {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppConfig.TERMS_URL)))
                    }
                    AboutRow("Открытый исходный код / лицензии", onOpenLicenses)
                }
            }

            Text(
                "Простая автоматизация. Без лишнего.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun AboutRow(title: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    )
}
