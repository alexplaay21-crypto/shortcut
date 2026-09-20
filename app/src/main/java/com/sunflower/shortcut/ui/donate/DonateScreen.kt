package com.sunflower.shortcut.ui.donate

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sunflower.shortcut.BuildConfig
import com.sunflower.shortcut.ui.components.PrimaryButton
import com.sunflower.shortcut.utils.AppConfig

@Composable
fun DonateScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Поддержать разработчика") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
            }
        )

        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("❤️", style = MaterialTheme.typography.displaySmall)
            Text(
                AppConfig.DEVELOPER_NAME,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                "Shortcut бесплатный и останется бесплатным.\n\n" +
                    "Если приложение оказалось полезным, вы можете добровольно " +
                    "поддержать дальнейшую разработку. Донат не разблокирует " +
                    "никакие функции — все основные возможности приложения " +
                    "остаются доступны всем.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp, bottom = 24.dp)
            )

            PrimaryButton(
                text = "Поддержать проект",
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppConfig.DONATE_URL)))
                }
            )

            TextButton(
                onClick = {
                    val marketUri = Uri.parse("market://details?id=${BuildConfig.APPLICATION_ID}")
                    context.startActivity(Intent(Intent.ACTION_VIEW, marketUri))
                },
                modifier = Modifier.padding(top = 8.dp)
            ) { Text("Оставить отзыв") }

            Text(
                "Спасибо за вашу поддержку!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
