package net.multun.gamecounter.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoveDown
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.multun.gamecounter.BoardViewModel
import net.multun.gamecounter.data.CounterId
import net.multun.gamecounter.data.MemoryAppState
import net.multun.gamecounter.data.MockAppStateStorage


@Composable
fun BoardLayout(
    slots: Int,
    modifier: Modifier = Modifier,
    padding: Dp = 8.dp,
    callback: @Composable (Int, Modifier) -> Unit,
) {
    val playersPerRow: Int
    val rowCount: Int
    if (slots == 2) {
        playersPerRow = 1
        rowCount = 2
    } else {
        playersPerRow = 2
        rowCount = (slots + 1) / 2
    }

    Column(modifier = modifier.padding(padding / 2)) {
        for (rowIndex in 0 until rowCount) {
            val rowOffset = rowIndex * playersPerRow
            val remainingSlots = slots - rowOffset
            val rowSlots = if (remainingSlots > playersPerRow) playersPerRow else remainingSlots
            Row(modifier = Modifier
                .padding(padding / 2)
                .fillMaxSize()
                .weight(1f), horizontalArrangement = Arrangement.spacedBy(padding)) {
                for (colIndex in 0 until rowSlots) {
                    val slotIndex = rowOffset + colIndex
                    val orientation = when {
                        slots == 2 && slotIndex == 0 -> Rotation.ROT_180
                        rowSlots == 1 || slots == 2 -> Rotation.ROT_0
                        colIndex == 0 -> Rotation.ROT_90
                        else -> Rotation.ROT_270
                    }

                    val slotModifier = Modifier
                        .weight(1f)
                        .rotateLayout(orientation)
                    callback(slotIndex, slotModifier)
                }
            }
        }
    }
}


@Composable
fun PlayerCounterBoard(
    viewModel: BoardViewModel,
    modifier: Modifier = Modifier,
) {
    val players = viewModel.playerIds
    BoardLayout(slots = players.size, modifier = modifier) {
        slotIndex, slotModifier ->
        val playerId = players[slotIndex]
        key(playerId) {
            PlayerCounter(
                viewModel,
                playerId,
                modifier = slotModifier,
            )
        }
    }
}

@Composable
fun ConfirmDialog(
    dialogText: String,
    dialogTitle: String? = null,
    icon: @Composable () -> Unit = {},
    onDismissRequest: () -> Unit = {},
    onConfirmation: () -> Unit = {},
) {
    AlertDialog(
        icon = icon,
        title = dialogTitle?.let {{
            Text(text = it)
        }},
        text = {
            Text(text = dialogText)
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onConfirmation) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}



@Composable
fun GameCounterUI(viewModel: BoardViewModel, modifier: Modifier = Modifier) {
    var showConfirmNewGame by rememberSaveable { mutableStateOf(false) }
    if (showConfirmNewGame) {
        ConfirmDialog(
            dialogText = "Start a new game?",
            onDismissRequest = { showConfirmNewGame = false },
            onConfirmation = { showConfirmNewGame = false; viewModel.reset() }
        )
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(enabled = viewModel.canAddPlayer, onClick = { viewModel.addPlayer() }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add a new player")
                    }

                    IconButton(enabled = viewModel.canAddPlayer, onClick = { viewModel.addPlayer() }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove a player")
                    }

                    IconButton(onClick = { }) {
                        Icon(Icons.Filled.Casino, contentDescription = "Roll a dice")
                    }

                    IconButton(onClick = { }) {
                        Icon(Icons.Filled.MoveDown, contentDescription = "Reset")
                    }

                    IconButton(onClick = { }) {
                        Icon(Icons.Filled.Palette, contentDescription = "Assign player colors")
                    }

                    IconButton(onClick = { showConfirmNewGame = true }) {
                        Icon(Icons.Filled.Replay, contentDescription = "Reset")
                    }

                    IconButton(onClick = { }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { innerPadding ->
        PlayerCounterBoard(viewModel,
            Modifier
                .padding(innerPadding)
                .fillMaxSize())
    }
}

@Preview(showBackground = true)
@Composable
fun BoardPreview() {
    // Define the counter value as a state object
    val viewModel by remember { mutableStateOf(BoardViewModel(MemoryAppState(MockAppStateStorage()))) }
    GameCounterUI(
        viewModel,
        modifier = Modifier.fillMaxSize(),
    )
}