package com.sunflower.shortcut.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class OssLibrary(val name: String, val license: String)

// Update as dependencies change — kept as a flat list rather than parsing
// a generated JSON at runtime, since the dependency set is small and stable.
private val libraries = listOf(
    OssLibrary("Jetpack Compose", "Apache License 2.0"),
    OssLibrary("Material Components for Android", "Apache License 2.0"),
    OssLibrary("Kotlin Coroutines", "Apache License 2.0"),
    OssLibrary("AndroidX Room", "Apache License 2.0"),
    OssLibrary("AndroidX DataStore", "Apache License 2.0"),
    OssLibrary("AndroidX WorkManager", "Apache License 2.0"),
    OssLibrary("AndroidX Navigation", "Apache License 2.0")
)

@Composable
fun LicensesScreen(onBack: () -> Unit) {
    Column {
        TopAppBar(
            title = { Text("Лицензии") },
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
            items(libraries) { lib ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(lib.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        lib.license,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
