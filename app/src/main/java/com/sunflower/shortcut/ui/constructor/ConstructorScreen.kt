package com.sunflower.shortcut.ui.constructor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sunflower.shortcut.data.repositories.AutomationRepository
import com.sunflower.shortcut.ui.components.FlowStepKind
import com.sunflower.shortcut.ui.components.FlowStepUi
import com.sunflower.shortcut.ui.components.PrimaryButton
import com.sunflower.shortcut.ui.theme.ShortcutRadius
import kotlinx.coroutines.launch

@Composable
fun ConstructorScreen(
    automationId: String,
    automationRepository: AutomationRepository,
    onBack: () -> Unit,
    onOpenCode: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val steps by automationRepository.observeSteps(automationId).collectAsState(initial = emptyList())
    var editMode by remember { mutableStateOf(false) }
    var editingStep by remember { mutableStateOf<FlowStepUi?>(null) }

    Column {
        TopAppBar(
            title = { Text("Конструкция") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
            },
            actions = {
                IconButton(onClick = onOpenCode) {
                    Icon(Icons.Filled.Code, contentDescription = "Открыть код")
                }
            }
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(steps, key = { it.id }) { step ->
                ConstructorStepCard(
                    step = step,
                    editMode = editMode,
                    onClick = { if (editMode && step.editable) editingStep = step }
                )
            }
        }

        PrimaryButton(
            text = if (editMode) "Готово" else "✏️ Редактировать",
            modifier = Modifier.padding(20.dp),
            onClick = { editMode = !editMode }
        )
    }

    val target = editingStep
    if (target != null) {
        EditableValueSheet(
            step = target,
            onDismiss = { editingStep = null },
            onSave = { newValue ->
                editingStep = null
                scope.launch { automationRepository.updateStepValue(automationId, target.id, newValue) }
            }
        )
    }
}

@Composable
private fun ConstructorStepCard(
    step: FlowStepUi,
    editMode: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = ShortcutRadius.card,
        colors = CardDefaults.cardColors(
            containerColor = if (step.kind == FlowStepKind.DYNAMIC) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(step.emoji, style = MaterialTheme.typography.titleMedium)
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(step.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    step.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (editMode && step.editable) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "Изменить",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
