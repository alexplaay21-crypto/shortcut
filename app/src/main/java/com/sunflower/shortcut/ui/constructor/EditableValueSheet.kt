package com.sunflower.shortcut.ui.constructor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sunflower.shortcut.ui.components.FlowStepUi
import com.sunflower.shortcut.ui.components.PrimaryButton
import com.sunflower.shortcut.ui.components.SecondaryButton

/**
 * Generic editor for one editable constructor value. The step's *kind* of
 * value (delay / app / URL / email / phone / plain text) is inferred from
 * its title, since FlowStepUi keeps the UI layer decoupled from AST node
 * types — the JS text itself stays the single source of truth (spec §19:
 * "не превращать весь JS в набор блоков").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditableValueSheet(
    step: FlowStepUi,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val isDelay = step.title.contains("Подожд", ignoreCase = true)
    val parts = remember(step.subtitle) { step.subtitle.trim().split(" ") }

    var textValue by remember { mutableStateOf(step.subtitle) }
    var delayAmount by remember { mutableStateOf(parts.getOrNull(0).orEmpty()) }
    var delayUnit by remember { mutableStateOf(parts.getOrNull(1) ?: "секунда") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(step.title, style = MaterialTheme.typography.titleLarge)

            if (isDelay) {
                Row(modifier = Modifier.padding(top = 16.dp)) {
                    OutlinedTextField(
                        value = delayAmount,
                        onValueChange = { delayAmount = it.filter(Char::isDigit) },
                        label = { Text("Задержка") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = delayUnit,
                        onValueChange = { delayUnit = it },
                        label = { Text("Единица") },
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    )
                }
            } else {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                )
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
                SecondaryButton(text = "Отмена", onClick = onDismiss, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                PrimaryButton(
                    text = "Готово",
                    onClick = {
                        onSave(if (isDelay) "$delayAmount $delayUnit" else textValue)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
