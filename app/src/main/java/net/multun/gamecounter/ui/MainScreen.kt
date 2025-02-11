package net.multun.gamecounter.ui


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStoreFactory
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.sd.lib.compose.wheel_picker.FHorizontalWheelPicker
import com.sd.lib.compose.wheel_picker.rememberFWheelPickerState
import kotlinx.coroutines.launch
import net.multun.gamecounter.BoardUI
import net.multun.gamecounter.BoardViewModel
import net.multun.gamecounter.CounterBoardUI
import net.multun.gamecounter.RollUI
import net.multun.gamecounter.Screens
import net.multun.gamecounter.SetupUI
import net.multun.gamecounter.StartupUI
import net.multun.gamecounter.store.GameRepository
import net.multun.gamecounter.store.GameSerializer
import java.io.File


@Composable
fun MainScreen(viewModel: BoardViewModel, navController: NavController, modifier: Modifier = Modifier) {
    val boardState = viewModel.boardUIState.collectAsStateWithLifecycle()
    when (val uiState = boardState.value) {
        is BoardUI -> Board(uiState, viewModel, navController, modifier)
        is SetupUI -> Setup(uiState, viewModel, navController, modifier)
        StartupUI -> return
    }
}


enum class ModalState {
    Settings,
    ConfirmGameReset,
    ConfirmLeaveGame,
}

@Composable
fun Setup(setupUI: SetupUI, viewModel: BoardViewModel, navController: NavController, modifier: Modifier = Modifier) {
    val playerCount = rememberFWheelPickerState(1)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.wrapContentSize()) {
                Text("Player count", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(10.dp))
                FHorizontalWheelPicker(
                    modifier = Modifier.height(48.dp),
                    state = playerCount,
                    count = 100,
                ) { index ->
                    Text((index + 1).toString())
                }

                Spacer(Modifier.height(30.dp))

                TextButton(onClick = { navController.navigate(Screens.CounterSettings.route) }) {
                    Text("Counter settings")
                }

                TextButton(
                    onClick = {
                        if (!setupUI.hasCounters) {
                            scope.launch {
                                val result = snackbarHostState
                                    .showSnackbar(
                                        message = "Cannot start game without counter",
                                        actionLabel = "Counter settings",
                                        duration = SnackbarDuration.Long,
                                    )
                                when (result) {
                                    SnackbarResult.ActionPerformed -> {
                                        navController.navigate(Screens.CounterSettings.route)
                                    }
                                    SnackbarResult.Dismissed -> {}
                                }
                            }
                        } else {
                            viewModel.addPlayers(playerCount.currentIndex + 1)
                        }
                    },
                ) {
                    Text("Start game")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Board(boardUI: BoardUI, viewModel: BoardViewModel, navController: NavController, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var modalState by remember { mutableStateOf<ModalState?>(null) }
    val sheetState = rememberModalBottomSheetState()

    fun hideBottomSheet() {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                modalState = null
            }
        }
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            when (boardUI) {
                is RollUI -> RollBottomBar(
                    viewModel = viewModel,
                    initialSelectedDice = remember { boardUI.selectedDice },
                )
                is CounterBoardUI -> CounterBottomBar(
                    onRoll = { viewModel.roll() },
                    onOpenSettings = { modalState = ModalState.Settings },
                )
            }
        },
    ) { innerPadding ->
        val players = boardUI.players
        BoardLayout(
            itemCount = players.size,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) { playerIndex, slotModifier ->
            val player = players[playerIndex]
            key(player.id.value) {
                Player(player, viewModel, modifier = slotModifier.wrapContentSize())
            }
        }

        if (modalState == ModalState.Settings) {
            ModalBottomSheet(
                onDismissRequest = { modalState = null },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SettingsItem(Icons.Filled.Add, "Add new player") {
                        viewModel.addPlayer()
                    }

                    SettingsItem(Icons.Filled.Replay, "Reset game") {
                        modalState = ModalState.ConfirmGameReset
                    }

                    SettingsItem(Icons.Filled.Settings, "Counter settings") {
                        hideBottomSheet()
                        navController.navigate(Screens.CounterSettings.route)
                    }

                    SettingsItem(Icons.Filled.Clear, "Close menu") {
                        hideBottomSheet()
                    }
                }
            }
        }

        if (modalState == ModalState.ConfirmGameReset) {
            ConfirmDialog(
                dialogText = "Are you sure you want to reset all counters to their default value?",
                onDismissRequest = { modalState = null },
                onConfirmation = { modalState = null; viewModel.resetGame() }
            )
        }
    }
}


@Composable
fun SettingsItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(30.dp, 0.dp, 8.dp, 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = text)
        Spacer(Modifier.size(30.dp))
        Text(text)
    }
}


@Preview(showBackground = true)
@Composable
fun BoardPreview() {
    // Define the counter value as a state object
    val viewModel by remember {
        mutableStateOf(BoardViewModel(GameRepository(
            DataStoreFactory.create(serializer = GameSerializer) {
                File.createTempFile("board_preview", ".pb", null)
            }
        )))
    }

    val controller = rememberNavController()
    MainScreen(
        viewModel,
        controller,
        modifier = Modifier.fillMaxSize(),
    )
}