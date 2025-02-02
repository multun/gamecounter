package net.multun.gamecounter.components


import android.app.Activity
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.datastore.core.DataStoreFactory
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import net.multun.gamecounter.BoardUIMode
import net.multun.gamecounter.BoardViewModel
import net.multun.gamecounter.PlayerCardUIState
import net.multun.gamecounter.Screens
import net.multun.gamecounter.datastore.AppStateRepository
import net.multun.gamecounter.datastore.AppStateSerializer
import net.multun.gamecounter.toDisplayColor
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

enum class BoardDialog {
    ConfirmNewGame,
}

@Composable
fun BoardScreen(viewModel: BoardViewModel, navController: NavController, modifier: Modifier = Modifier) {
    var dialog by remember { mutableStateOf<BoardDialog?>(null) }

    when (dialog) {
        BoardDialog.ConfirmNewGame -> ConfirmDialog(
            dialogText = "Start a new game?",
            onDismissRequest = { dialog = null },
            onConfirmation = { dialog = null; viewModel.newGame() }
        )
        null -> {}
    }

    val boardState by viewModel.boardUIState.collectAsStateWithLifecycle()
    val mode by remember { derivedStateOf { boardState.mode } }
    Scaffold(
        modifier = modifier,
        bottomBar = {
            when (mode) {
                BoardUIMode.STARTUP -> {
                    Text("starting up...")
                }
                BoardUIMode.COUNTERS -> BottomAppBar(
                    actions = {
                        IconButton(onClick = { viewModel.addPlayer() }) {
                            Icon(Icons.Filled.Add, contentDescription = "Add a new player")
                        }

                        IconButton(onClick = { viewModel.roll() }) {
                            Icon(Icons.Filled.Casino, contentDescription = "Roll a dice")
                        }

                        IconButton(onClick = { dialog = BoardDialog.ConfirmNewGame }) {
                            Icon(Icons.Filled.Replay, contentDescription = "New game")
                        }

                        IconButton(onClick = { navController.navigate(Screens.CounterSettings.route) }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    },
                )
                BoardUIMode.ROLL -> BottomAppBar(
                    actions = {},
                    floatingActionButton = {
                        FloatingActionButton(onClick = { viewModel.clearRoll() }) {
                            Icon(Icons.Filled.Clear, "Clear roll")
                        }
                    }
                )
            }

        },
    ) { innerPadding ->
        val players = boardState.players
        BoardLayout(
            itemCount = players.size,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) { playerIndex, slotModifier ->
            val player = players[playerIndex]
            key(player.id.value) {
                Player(
                    player,
                    viewModel,
                    modifier = slotModifier
                        .wrapContentSize()
                        .size(210.dp, 170.dp)
                )
            }
        }
    }
}

@Composable
fun Player(
    player: PlayerCardUIState,
    viewModel: BoardViewModel,
    modifier: Modifier = Modifier,
) {
    var isEditing by rememberSaveable { mutableStateOf(false) }
    PlayerCard(
        color = player.color.toDisplayColor(),
        modifier = modifier
    ) {
        if (isEditing) {
            PlayerSettings(
                currentPlayerColor = player.color,
                onExit = { isEditing = false },
                onDelete = remember { { viewModel.removePlayer(player.id) } },
                onSetColor = remember { { color -> viewModel.setPlayerColor(player.id, color) } },
            )
            return@PlayerCard
        }

        if (player.roll != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "${player.roll}", fontSize = 10.em)
            }
            return@PlayerCard
        }

        if (player.counter == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "no counter", fontSize = 5.em)
            }
            return@PlayerCard
        }

        val counter = player.counter
        PlayerCounter(
            modifier = Modifier.fillMaxSize(),
            counter = counter,
            onIncrement = remember(counter.id.value) { { viewModel.updateCounter(player.id, counter.id, 1) } },
            onDecrement = remember(counter.id.value) { { viewModel.updateCounter(player.id, counter.id, -1) } },
            onNextCounter = remember { { viewModel.nextCounter(player.id) } },
            onPreviousCounter = remember { { viewModel.previousCounter(player.id) } },
            onEdit = { isEditing = true }
        )
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