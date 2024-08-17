@file:OptIn(ExperimentalMaterial3Api::class)

package net.multun.gamecounter.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import net.multun.gamecounter.CounterUIState
import net.multun.gamecounter.Screens
import net.multun.gamecounter.SettingsViewModel
import net.multun.gamecounter.datastore.CounterId

sealed class CounterSettingsDialog
data object AddDialog : CounterSettingsDialog()
data class EditDialog(val counterId: CounterId) : CounterSettingsDialog()


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
        Column(modifier = Modifier
            .padding(contentPadding)
            .padding(10.dp)) {
            for (counter in appState.counters) {
                key(counter.id) {
                    CounterCard(counter, viewModel, onEditCounter = {
                        dialog = EditDialog(counter.id)
                    })
                }
            }
        }
    }

    when (val curDialog = dialog) {
        AddDialog -> AddCounterDialog(
            onDismissRequest = { dialog = null },
            onConfirmation = { dialog = null },
        )

        is EditDialog -> EditCounterDialog(
            curDialog.counterId,
            onDismissRequest = { dialog = null },
            onConfirmation = { dialog = null },
        )
        null -> {}
    }
}

@Composable
fun AddCounterDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
) {
    AlertDialog(
        title = {
            Text(text = "dialogTitle")
        },
        text = {
            Text(text = "dialogText")
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation()
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text("Dismiss")
            }
        }
    )
}

@Composable
fun EditCounterDialog(
    counterId: CounterId,
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
) {
    AlertDialog(
        title = {
            Text(text = "dialogTitle")
        },
        text = {
            Text(text = "dialogText")
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation()
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text("Dismiss")
            }
        }
    )
}


@Composable
fun CounterCard(
    counter: CounterUIState,
    viewModel: SettingsViewModel,
    onEditCounter: () -> Unit,
) {
    Card(modifier = Modifier
        .height(50.dp)
        .fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(counter.name, fontSize = 4.em)
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { onEditCounter() }) {
                    Icon(Icons.Filled.Edit, contentDescription = "edit counter")
                }
                IconButton(onClick = { viewModel.moveCounterUp(counter.id) }) {
                    Icon(Icons.Filled.MoveUp, contentDescription = "move counter up")
                }
                IconButton(onClick = { viewModel.moveCounterDown(counter.id) }) {
                    Icon(Icons.Filled.MoveDown, contentDescription = "move counter down")
                }
                IconButton(onClick = { viewModel.deleteCounter(counter.id) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "delete counter")
                }
            }
        }
    }
}

