@file:OptIn(ExperimentalMaterial3Api::class)

package net.multun.gamecounter.ui.counter_settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoveDown
import androidx.compose.material.icons.filled.MoveUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.collections.immutable.ImmutableList
import net.multun.gamecounter.store.CounterId
import net.multun.gamecounter.ui.GameCounterTopBar
import net.multun.gamecounter.ui.theme.Typography

sealed class CounterSettingsDialog
data object AddDialog : CounterSettingsDialog()
data class EditDialog(val counter: CounterSettingsUIState) : CounterSettingsDialog()
data class ConfirmDeleteDialog(val counter: CounterSettingsUIState) : CounterSettingsDialog()


@Composable
fun CounterSettingsScreen(
    viewModel: SettingsViewModel,
    navController: NavController,
) {
    var dialog by remember { mutableStateOf<CounterSettingsDialog?>(null) }
    val appState by viewModel.settingsUIState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            GameCounterTopBar("Counter settings", navController)
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { dialog = AddDialog }) {
                Icon(Icons.Filled.Add, "Add a counter")
            }
        }
    ) { contentPadding ->
        CounterSettingsList(
            counters = appState.counters,
            onMoveUp = remember { { viewModel.moveCounterUp(it)} },
            onMoveDown = remember { { viewModel.moveCounterDown(it) } },
            onDialog = { dialog = it },
            modifier = Modifier.padding(contentPadding),
        )
    }

    val curDialog = dialog
    if (curDialog != null) {
        CounterSettingsDialog(
            curDialog,
            onDelete = { viewModel.deleteCounter(it) },
            onAddCounter = { name, defaultVal -> viewModel.addCounter(name, defaultVal) },
            onSetName = { id, name -> viewModel.setCounterName(id, name) },
            onSetDefaultValue = { id, defaultVal -> viewModel.setCounterDefaultValue(id, defaultVal) },
            onClearDialog = { dialog = null },
        )
    }
}

@Composable
fun CounterSettingsList(
    counters: ImmutableList<CounterSettingsUIState>,
    onMoveUp: (CounterId) -> Unit,
    onMoveDown: (CounterId) -> Unit,
    onDialog: (CounterSettingsDialog) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.padding(10.dp),
    ) {
        for (counterIndex in 0 until counters.size) {
            val counter = counters[counterIndex]
            val isFirst = counterIndex == 0
            val isLast = counterIndex == counters.size - 1
            item(counter.id.value) {
                CounterSettingsLine(
                    counter.name,
                    isFirst,
                    isLast,
                    onEdit = { onDialog(EditDialog(counter)) },
                    onMoveUp = remember { { onMoveUp(counter.id) } },
                    onMoveDown = remember { { onMoveDown(counter.id) } },
                    onDelete = { onDialog(ConfirmDeleteDialog(counter)) },
                    modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
                )
            }
        }
    }
}

@Composable
fun CounterSettingsDialog(
    dialog: CounterSettingsDialog,
    onDelete: (CounterId) -> Unit,
    onAddCounter: (String, Int) -> Unit,
    onSetName: (CounterId, String) -> Unit,
    onSetDefaultValue: (CounterId, Int) -> Unit,
    onClearDialog: () -> Unit,
) {
    when (dialog) {
        AddDialog -> CounterChangeDialog(
            title = "Add a counter",
            action = "Add",
            onDismissRequest = onClearDialog,
            onCounterAdded = remember { { name, defaultValue ->
                onAddCounter(name, defaultValue)
                onClearDialog()
            } }
        )
        is EditDialog -> CounterChangeDialog(
            title = "Edit a counter",
            action = "Save",
            initialName = dialog.counter.name,
            initialDefaultValue = dialog.counter.defaultValue,
            onDismissRequest = onClearDialog,
            onCounterAdded = remember { { name, defaultValue ->
                val counterId = dialog.counter.id
                onSetName(counterId, name)
                onSetDefaultValue(counterId, defaultValue)
                onClearDialog()
            } }
        )
        is ConfirmDeleteDialog -> AlertDialog(
            icon = { Icon(Icons.Filled.Delete, contentDescription = "Delete icon") },
            text = { Text("Do you really want to delete counter ${dialog.counter.name}?")},
            onDismissRequest = onClearDialog,
            confirmButton = {
                TextButton(onClick = remember { {
                    onDelete(dialog.counter.id)
                    onClearDialog()
                } }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = onClearDialog) {
                    Text("Cancel")
                }
            })
    }
}


@Composable
fun CounterChangeDialog(
    title: String,
    action: String,
    onDismissRequest: () -> Unit,
    onCounterAdded: (String, Int) -> Unit,
    initialName: String = "",
    initialDefaultValue: Int? = null,
) {
    var counterName by remember { mutableStateOf(initialName) }
    var counterDefaultValue by remember { mutableStateOf(initialDefaultValue?.toString() ?: "") }
    val parsedDefaultValue = counterDefaultValue.toIntOrNull()

    Dialog(onDismissRequest = onDismissRequest) {
        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.width(IntrinsicSize.Min)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(title, style = Typography.bodyLarge)

                val nameError = counterName.isBlank()
                val defaultValueError = parsedDefaultValue == null

                OutlinedTextField(
                    value = counterName,
                    isError = nameError,
                    onValueChange = { counterName = it },
                    label = { Text("Name") },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = counterDefaultValue,
                    onValueChange = { counterDefaultValue = it },
                    label = { Text("Default value") },
                    isError = defaultValueError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = { onDismissRequest() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text("Cancel")
                    }
                    TextButton(
                        enabled = !(nameError || defaultValueError),
                        onClick = { onCounterAdded(counterName.trim(), parsedDefaultValue!!) },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text(action)
                    }
                }
            }
        }
    }
}

@Composable
fun CounterSettingsLine(
    name: String,
    isFirst: Boolean,
    isLast: Boolean,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier
        .height(50.dp)
        .fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, style = Typography.bodyLarge)
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "edit counter")
                }
                IconButton(enabled = !isFirst, onClick = onMoveUp) {
                    Icon(Icons.Filled.MoveUp, contentDescription = "move counter up")
                }
                IconButton(enabled = !isLast, onClick = onMoveDown) {
                    Icon(Icons.Filled.MoveDown, contentDescription = "move counter down")
                }
                IconButton(enabled = !(isFirst && isLast), onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "delete counter")
                }
            }
        }
    }
}

