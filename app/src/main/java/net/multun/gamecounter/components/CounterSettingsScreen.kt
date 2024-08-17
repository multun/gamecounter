@file:OptIn(ExperimentalMaterial3Api::class)

package net.multun.gamecounter.components

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.unit.em
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import net.multun.gamecounter.CounterUIState
import net.multun.gamecounter.SettingsViewModel

sealed class CounterSettingsDialog
data object AddDialog : CounterSettingsDialog()
data class EditDialog(val counter: CounterUIState) : CounterSettingsDialog()
data class ConfirmDeleteDialog(val counter: CounterUIState) : CounterSettingsDialog()


@OptIn(ExperimentalFoundationApi::class)
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
        LazyColumn(
            modifier = Modifier
                .padding(contentPadding)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            for (counterIndex in 0 until appState.counters.size) {
                val counter = appState.counters[counterIndex]
                val isFirst = counterIndex == 0
                val isLast = counterIndex == appState.counters.size - 1
                item(counter.id.value) {
                    CounterSettingsLine(
                        counter.name,
                        isFirst,
                        isLast,
                        onEdit = { dialog = EditDialog(counter) },
                        onMoveUp = { viewModel.moveCounterUp(counter.id) },
                        onMoveDown = { viewModel.moveCounterDown(counter.id) },
                        onDelete = { dialog = ConfirmDeleteDialog(counter) },
                        modifier = Modifier.animateItemPlacement(),
                    )
                }
            }
        }
    }

    when (val curDialog = dialog) {
        AddDialog -> CounterChangeDialog(
            title = "Add a counter",
            action = "Add",
            onDismissRequest = { dialog = null },
            onCounterAdded = { name, defaultValue ->
                viewModel.addCounter(name, defaultValue)
                dialog = null
            }
        )
        is EditDialog -> CounterChangeDialog(
            title = "Edit a counter",
            action = "Save",
            initialName = curDialog.counter.name,
            initialDefaultValue = curDialog.counter.defaultValue,
            onDismissRequest = { dialog = null },
            onCounterAdded = { name, defaultValue ->
                val counterId = curDialog.counter.id
                viewModel.setCounterName(counterId, name)
                viewModel.setCounterDefaultValue(counterId, defaultValue)
                dialog = null
            }
        )
        is ConfirmDeleteDialog -> AlertDialog(
            icon = { Icon(Icons.Filled.Delete, contentDescription = "Delete icon") },
            text = { Text("Do you really want to delete counter ${curDialog.counter.name}?")},
            onDismissRequest = { dialog = null },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCounter(curDialog.counter.id)
                    dialog = null
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { dialog = null }) {
                    Text("Cancel")
                }
            })
        null -> {}
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
                Text(title, fontSize = 4.em)

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
            Text(name, fontSize = 4.em)
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

