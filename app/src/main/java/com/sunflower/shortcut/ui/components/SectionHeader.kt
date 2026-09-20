package com.sunflower.shortcut.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Small uppercase-ish section label above a group of cards (e.g. "Ваши сценарии"). */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        trailing?.invoke()
    }
}

/** Big screen title used at the top of top-level tabs (spec §3 "большие аккуратные заголовки"). */
@Composable
fun ScreenTitle(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Column(modifier = modifier.padding(horizontal = 4.dp)) {
        Text(text = title, style = MaterialTheme.typography.displaySmall)
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
