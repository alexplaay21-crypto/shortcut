package com.sunflower.shortcut.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sunflower.shortcut.ui.theme.CodeTextStyle
import com.sunflower.shortcut.ui.theme.ShortcutRadius

/**
 * Read-only JS source viewer with line numbers (screenshot "Код").
 * The code is always the ground truth artifact (spec §33) — this just
 * renders it, it never edits it (editing happens via the Constructor,
 * section 19, on the structured representation).
 */
@Composable
fun CodeBlock(
    code: String,
    modifier: Modifier = Modifier
) {
    val lines = remember(code) { code.trimEnd('\n').split("\n") }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, ShortcutRadius.card)
            .padding(vertical = 12.dp)
    ) {
        items(lines.size) { index ->
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                Text(
                    text = (index + 1).toString(),
                    style = CodeTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(28.dp)
                )
                Text(
                    text = lines[index],
                    style = CodeTextStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
    }
}
