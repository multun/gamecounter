package net.multun.gamecounter.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.datastore.core.DataStoreFactory
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import net.multun.gamecounter.BoardViewModel
import net.multun.gamecounter.Screens
import net.multun.gamecounter.datastore.AppStateRepository
import net.multun.gamecounter.datastore.AppStateSerializer
import java.io.File


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
fun BoardScreen(viewModel: BoardViewModel, navController: NavController, modifier: Modifier = Modifier) {
    var showConfirmNewGame by rememberSaveable { mutableStateOf(false) }
    if (showConfirmNewGame) {
        ConfirmDialog(
            dialogText = "Start a new game?",
            onDismissRequest = { showConfirmNewGame = false },
            onConfirmation = { showConfirmNewGame = false; viewModel.newGame() }
        )
    }

    val boardState by viewModel.boardUIState.collectAsStateWithLifecycle()
    Scaffold(
        modifier = modifier,
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(enabled = boardState.canAddPlayer, onClick = { viewModel.addPlayer() }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add a new player")
                    }

                    IconButton(onClick = { }) {
                        Icon(Icons.Filled.Casino, contentDescription = "Roll a dice")
                    }

                    IconButton(onClick = { showConfirmNewGame = true }) {
                        Icon(Icons.Filled.Replay, contentDescription = "New game")
                    }

                    IconButton(onClick = { navController.navigate(Screens.CounterSettings.route) }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { innerPadding ->
        val players = boardState.players
        BoardLayout(
            slots = players.size,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) { slotIndex, slotModifier ->
            val player = players[slotIndex]
            key(player.id) {
                var isEditing by remember { mutableStateOf(false) }
                PlayerCard(
                    color = player.color,
                    modifier = slotModifier
                        .wrapContentSize()
                        .size(210.dp, 170.dp)
                ) {
                    if (isEditing) {
                        PlayerSettings(
                            currentPlayerColor = player.color,
                            onExit = { isEditing = false },
                            onDelete = { viewModel.removePlayer(player.id) },
                            onSetColor = { color -> viewModel.setPlayerColor(player.id, color) },
                        )
                    } else {
                        if (player.counter == null) {
                            Text(text = "no counter", fontSize = 5.em)
                            return@PlayerCard
                        }

                        val counter = player.counter
                        PlayerCounter(
                            modifier = Modifier.fillMaxSize(),
                            counter = counter,
                            hasMultipleCounters = boardState.hasMultipleCounters,
                            onIncrement = { viewModel.updateCounter(player.id, counter.id, 1) },
                            onDecrement = { viewModel.updateCounter(player.id, counter.id, -1) },
                            onNextCounter = { viewModel.nextCounter(player.id) },
                            onPreviousCounter = { viewModel.previousCounter(player.id) },
                            onEdit = { isEditing = true }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BoardPreview() {
    // Define the counter value as a state object
    val viewModel by remember {
        mutableStateOf(BoardViewModel(AppStateRepository(
            DataStoreFactory.create(serializer = AppStateSerializer) {
                File.createTempFile("board_preview", ".pb", null)
            }
        )))
    }
    val controller = rememberNavController()
    BoardScreen(
        viewModel,
        controller,
        modifier = Modifier.fillMaxSize(),
    )
}