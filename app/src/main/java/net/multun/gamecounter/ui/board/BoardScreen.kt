package net.multun.gamecounter.ui.board


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Exposure
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import net.multun.gamecounter.R
import net.multun.gamecounter.Screens
import net.multun.gamecounter.store.PlayerId
import net.multun.gamecounter.ui.theme.Typography


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

@Composable
fun KeepScreenOn() {
    val currentView = LocalView.current
    DisposableEffect(Unit) {
        currentView.keepScreenOn = true
        onDispose {
            currentView.keepScreenOn = false
        }
    }
}


private sealed interface ModalState
data object ModalSettings : ModalState
data object ModalConfirmGameReset : ModalState
data class ModalEditPlayerName(val playerId: PlayerId) : ModalState
data class ModalConfirmRemovePlayer(val playerId: PlayerId) : ModalState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Board(boardUI: BoardUI, viewModel: BoardViewModel, navController: NavController, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var modalState by remember { mutableStateOf<ModalState?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    KeepScreenOn()

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
                    onRoll = remember { { viewModel.roll() } },
                    onClear = remember { { viewModel.clearMode() } },
                    onSelectDice = remember { { viewModel.selectDice(it) } },
                    initialSelectedDice = remember { boardUI.selectedDice },
                )
                is CounterBoardUI -> CounterBottomBar(
                    onRoll = { viewModel.roll() },
                    onOpenSettings = { modalState = ModalSettings },
                )
                is PlayerSettingsBoardUI -> PlayerNamesBottomBar(
                    onClear = remember { { viewModel.clearMode() } },
                )
            }
        },
    ) { innerPadding ->
        val players = boardUI.players
        BoardLayout(
            itemCount = players.size,
            alwaysUprightMode = boardUI.alwaysUprightMode,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) { playerIndex, slotModifier ->
            val player = players[playerIndex]
            key(player.id.value) {
                Player(
                    player,
                    onSetColor = remember { { color -> viewModel.setPlayerColor(player.id, color) } },
                    onDelete = remember { { modalState = ModalConfirmRemovePlayer(player.id) } },
                    onUpdateCounter = remember { { counterId, delta -> viewModel.updateCounter(player.id, counterId, delta) } },
                    onSelectCounter = remember { { counterId -> viewModel.selectCounter(player.id, counterId) } },
                    onEditName = remember { { modalState = ModalEditPlayerName(player.id) } },
                    modifier = slotModifier.wrapContentSize(),
                    onMove = remember { { dir -> viewModel.movePlayer(player.id, dir) }}
                )
            }
        }

        when (val currentModalState = modalState) {
            null -> {}
            is ModalEditPlayerName -> {
                val player = players.find { it.id == currentModalState.playerId }
                val initialName = player?.name ?: ""
                PlayerNameDialog(
                    initialName,
                    onDismissRequest = { modalState = null },
                    onUpdate = { newName ->
                        modalState = null
                        viewModel.setPlayerName(currentModalState.playerId, newName)
                    }
                )
            }
            is ModalConfirmRemovePlayer -> {
                ConfirmDialog(
                    dialogText = stringResource(R.string.confirm_remove_player),
                    onDismissRequest = { modalState = null },
                    onConfirmation = { modalState = null; viewModel.removePlayer(currentModalState.playerId) }
                )
            }
            ModalConfirmGameReset -> ConfirmDialog(
                dialogText = stringResource(R.string.confirm_reset_counters),
                onDismissRequest = { modalState = null },
                onConfirmation = { modalState = null; viewModel.resetGame() }
            )
            ModalSettings -> ModalBottomSheet(
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

                    SettingsItem(Icons.Filled.ManageAccounts, stringResource(R.string.players_settings)) {
                        hideBottomSheet()
                        viewModel.playerSettings()
                    }

                    SettingsItem(Icons.Filled.Exposure, stringResource(R.string.counters_settings)) {
                        hideBottomSheet()
                        navController.navigate(Screens.CounterSettings.route)
                    }

                    SettingsItem({
                        Checkbox(
                            // the default size is huge due to minimumInteractiveComponentSize
                            modifier = Modifier.size(30.dp),
                            checked = boardUI.alwaysUprightMode,
                            onCheckedChange = { viewModel.setUlwaysUprightMode(it) }
                        )
                    }, stringResource(R.string.always_up_tiles)) {
                        viewModel.setUlwaysUprightMode(!boardUI.alwaysUprightMode)
                    }

                    SettingsItem(Icons.Filled.Replay, stringResource(R.string.reset_game)) {
                        modalState = ModalConfirmGameReset
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
    }
}

@Composable
fun PlayerNameDialog(
    initialName: String,
    onDismissRequest: () -> Unit,
    onUpdate: (String) -> Unit,
) {
    var counterName by remember { mutableStateOf(initialName) }
    val onConfirm = { onUpdate(counterName.trim()) }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.width(IntrinsicSize.Min)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(stringResource(R.string.update_player_name), style = Typography.bodyLarge)

                OutlinedTextField(
                    value = counterName,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onConfirm() }),
                    onValueChange = { counterName = it },
                    label = { Text(stringResource(R.string.player_name)) },
                    singleLine = true,
                )

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismissRequest, modifier = Modifier.padding(8.dp)) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(onClick = onConfirm, modifier = Modifier.padding(8.dp)) {
                        Text(stringResource(R.string.confirm))
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    SettingsItem(
        icon = { Icon(icon, contentDescription = null) },
        text,
        onClick = onClick,
    )
}


@Composable
fun SettingsItem(icon: @Composable () -> Unit, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(0.dp, 4.dp, 8.dp, 4.dp)
            .sizeIn(minHeight = 35.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.width(60.dp)) {
            icon()
        }
        Text(text)
    }
}