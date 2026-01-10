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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import kotlinx.collections.immutable.ImmutableList
import net.multun.gamecounter.PaletteColor
import net.multun.gamecounter.R
import net.multun.gamecounter.store.CounterId
import net.multun.gamecounter.ui.GameCounterTopBar
import net.multun.gamecounter.ui.board.GameIconButton
import net.multun.gamecounter.ui.theme.Typography

data class CounterSettingsUIState(
    val id: CounterId,
    val name: String,
    val defaultValue: Int,
    val step: Int,
    val largeStep: Int,
)

sealed class CounterSettingsDialog
data object AddDialog : CounterSettingsDialog()
data class EditDialog(val counter: CounterSettingsUIState) : CounterSettingsDialog()
data class ConfirmDeleteDialog(val counter: CounterSettingsUIState) : CounterSettingsDialog()

@Composable
fun CounterSettingsScreen(
    counters: ImmutableList<CounterSettingsUIState>,
    viewModel: GameCounterSettingsViewModel,
    navController: NavController,
) {
    var dialog by remember { mutableStateOf<CounterSettingsDialog?>(null) }
    Scaffold(
        topBar = {
            GameCounterTopBar(stringResource(R.string.counters_settings), navController)
        },
        floatingActionButton = {
            GameIconButton(
                PaletteColor.Indigo.color,
                onClick = remember { { dialog = AddDialog } }
            ) {
                Icon(Icons.Filled.Add, stringResource(R.string.new_counter))
            }
        }
    ) { contentPadding ->
        CounterSettingsList(
            counters = counters,
            onMoveUp = remember { { viewModel.moveCounterUp(it)} },
            onMoveDown = remember { { viewModel.moveCounterDown(it) } },
            onDialog = remember { { dialog = it } },
            modifier = Modifier.padding(contentPadding),
        )
    }

    val curDialog = dialog
    if (curDialog != null) {
        CounterSettingsDialog(
            curDialog,
            onDelete = remember { { viewModel.deleteCounter(it) } },
            onAddCounter = remember { { name, defaultVal, step, largeStep -> viewModel.addCounter(name, defaultVal, step, largeStep) } },
            onUpdateCounter = remember { { id, name, defaultVal, step, largeStep -> viewModel.updateCounter(id, name, defaultVal, step, largeStep) } },
            onClearDialog = remember { { dialog = null } },
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
    onAddCounter: (String, Int, Int, Int) -> Unit,
    onUpdateCounter: (CounterId, String, Int, Int, Int) -> Unit,
    onClearDialog: () -> Unit,
) {
    when (dialog) {
        AddDialog -> CounterChangeDialog(
            title = stringResource(R.string.new_counter),
            action = stringResource(R.string.add),
            onDismissRequest = onClearDialog,
            onCounterAdded = remember { { name, defaultValue, step, largeStep ->
                onAddCounter(name, defaultValue, step, largeStep)
                onClearDialog()
            } }
        )
        is EditDialog -> CounterChangeDialog(
            title = stringResource(R.string.edit_a_counter),
            action = stringResource(R.string.save),
            initialName = dialog.counter.name,
            initialDefaultValue = dialog.counter.defaultValue,
            initialStep = dialog.counter.step,
            initialLargeStep = dialog.counter.largeStep,
            onDismissRequest = onClearDialog,
            onCounterAdded = remember { { name, defaultValue, step, largeStep ->
                val counterId = dialog.counter.id
                onUpdateCounter(counterId, name, defaultValue, step, largeStep)
                onClearDialog()
            } }
        )
        is ConfirmDeleteDialog -> AlertDialog(
            icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
            text = {
                Text(
                    stringResource(
                        R.string.confirm_delete_counter,
                        dialog.counter.name
                    )
                )
            },
            onDismissRequest = onClearDialog,
            confirmButton = {
                TextButton(onClick = remember { {
                    onDelete(dialog.counter.id)
                    onClearDialog()
                } }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onClearDialog) {
                    Text(stringResource(R.string.cancel))
                }
            })
    }
}


@Composable
fun CounterChangeDialog(
    title: String,
    action: String,
    onDismissRequest: () -> Unit,
    onCounterAdded: (String, Int, Int, Int) -> Unit,
    initialName: String = "",
    initialDefaultValue: Int? = null,
    initialStep: Int? = null,
    initialLargeStep: Int? = null,
) {
    var counterName by remember { mutableStateOf(initialName) }
    var counterDefaultValue by remember { mutableStateOf(initialDefaultValue?.toString() ?: "") }
    var counterStep by remember { mutableStateOf(initialStep?.toString() ?: "1") }
    var counterLargeStep by remember { mutableStateOf(initialLargeStep?.toString() ?: "10") }
    val parsedDefaultValue = counterDefaultValue.toIntOrNull()
    val parsedStep = counterStep.toIntOrNull()
    val parsedLargeStep = counterLargeStep.toIntOrNull()

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
                val stepError = parsedStep == null || parsedStep <= 0
                val largeStepError = parsedLargeStep == null || parsedLargeStep <= 0

                OutlinedTextField(
                    value = counterName,
                    isError = nameError,
                    onValueChange = { counterName = it },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = counterDefaultValue,
                    onValueChange = { counterDefaultValue = it },
                    label = { Text(stringResource(R.string.counter_default_value)) },
                    isError = defaultValueError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = counterStep,
                    onValueChange = { counterStep = it },
                    label = { Text(stringResource(R.string.counter_step)) },
                    isError = stepError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = counterLargeStep,
                    onValueChange = { counterLargeStep = it },
                    label = { Text(stringResource(R.string.counter_big_step)) },
                    isError = largeStepError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = { onDismissRequest() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(
                        enabled = !(nameError || defaultValueError || stepError || largeStepError),
                        onClick = { onCounterAdded(counterName.trim(), parsedDefaultValue!!, parsedStep!!, parsedLargeStep!!) },
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
            Text(
                name,
                style = Typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            Row(horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_a_counter))
                }
                IconButton(enabled = !isFirst, onClick = onMoveUp) {
                    Icon(Icons.Filled.MoveUp, contentDescription = stringResource(R.string.move_up))
                }
                IconButton(enabled = !isLast, onClick = onMoveDown) {
                    Icon(Icons.Filled.MoveDown, contentDescription = stringResource(R.string.move_down))
                }
                IconButton(enabled = !(isFirst && isLast), onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_counter))
                }
            }
        }
    }
}

