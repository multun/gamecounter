package net.multun.gamecounter.ui.board


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import net.multun.gamecounter.R
import net.multun.gamecounter.Screens


@Composable
fun BoardScreen(viewModel: BoardViewModel, navController: NavController, modifier: Modifier = Modifier) {
    val boardState = viewModel.boardUIState.collectAsStateWithLifecycle()
    when (val uiState = boardState.value) {
        StartupUI -> {}
        SetupRequired -> {
            if (navController.previousBackStackEntry != null)
                navController.navigateUp()
            else
                navController.navigate(Screens.MainMenu.route)
        }
        is BoardUI -> Board(uiState, viewModel, navController, modifier)
    }
}


enum class ModalState {
    Settings,
    ConfirmGameReset,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Board(boardUI: BoardUI, viewModel: BoardViewModel, navController: NavController, modifier: Modifier = Modifier) {
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
                Player(
                    player,
                    onDelete = remember { { viewModel.removePlayer(player.id) } },
                    onSetColor = remember { { color -> viewModel.setPlayerColor(player.id, color) } },
                    onUpdateCounter = remember { { counterId, delta -> viewModel.updateCounter(player.id, counterId, delta) } },
                    onNextCounter = remember { { viewModel.nextCounter(player.id) } },
                    onPreviousCounter = remember { { viewModel.previousCounter(player.id) } },
                    modifier = slotModifier.wrapContentSize(),
                )
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
                    SettingsItem(Icons.Filled.Add, stringResource(R.string.add_new_player)) {
                        viewModel.addPlayer()
                    }

                    SettingsItem(Icons.Filled.Replay, stringResource(R.string.reset_game)) {
                        modalState = ModalState.ConfirmGameReset
                    }

                    SettingsItem(Icons.Filled.Settings, stringResource(R.string.counter_settings)) {
                        hideBottomSheet()
                        navController.navigate(Screens.CounterSettings.route)
                    }

                    SettingsItem(Icons.AutoMirrored.Filled.ExitToApp,
                        stringResource(R.string.leave_to_main_menu)) {
                        hideBottomSheet()
                        navController.popBackStack(Screens.MainMenu.route, inclusive = false)
                    }

                    SettingsItem(Icons.Filled.Clear, stringResource(R.string.close_menu)) {
                        hideBottomSheet()
                    }
                }
            }
        }

        if (modalState == ModalState.ConfirmGameReset) {
            ConfirmDialog(
                dialogText = stringResource(R.string.confirm_reset_counters),
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
            .padding(30.dp, 4.dp, 8.dp, 4.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = text)
        Spacer(Modifier.size(30.dp))
        Text(text)
    }
}