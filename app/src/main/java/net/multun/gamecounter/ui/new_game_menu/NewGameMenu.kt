package net.multun.gamecounter.ui.new_game_menu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Exposure
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sd.lib.compose.wheel_picker.FHorizontalWheelPicker
import com.sd.lib.compose.wheel_picker.rememberFWheelPickerState
import net.multun.gamecounter.R
import net.multun.gamecounter.Screens
import net.multun.gamecounter.ui.GameCounterTopBar
import net.multun.gamecounter.ui.counter_settings.AddDialog
import net.multun.gamecounter.ui.counter_settings.CounterSettingsDialog
import net.multun.gamecounter.ui.counter_settings.CounterSettingsList


enum class NewGameTabs(
    val route: String,
    val labelResource: Int,
    val icon: ImageVector,
) {
    PLAYERS("players", R.string.players, Icons.Default.ManageAccounts),
    COUNTERS("counters", R.string.counters, Icons.Default.Exposure),
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewGameMenu(viewModel: NewGameViewModel, navController: NavController, modifier: Modifier = Modifier) {
    val tabNavController = rememberNavController()
    val startDestination = NewGameTabs.PLAYERS
    var selectedDestination by rememberSaveable { mutableIntStateOf(startDestination.ordinal) }
    var dialog by remember { mutableStateOf<CounterSettingsDialog?>(null) }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            BottomAppBar(
                actions = {
                    AnimatedVisibility(selectedDestination == NewGameTabs.COUNTERS.ordinal) {
                        Button(
                            onClick = { dialog = AddDialog },
                            colors = ButtonDefaults.textButtonColors()
                        ) {
                            Icon(Icons.Filled.Add, null)
                            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                            Text(stringResource(R.string.add_counter))
                        }
                    }
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = {
                            viewModel.startGame()
                            navController.navigate(Screens.Board.route)
                        },
                        icon = { Icon(Icons.Filled.Done, null) },
                        text = { Text(text = stringResource(R.string.start_game)) },
                        containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                    )
                },
            )
        },
        topBar = {
            GameCounterTopBar(stringResource(R.string.new_game), navController)
        },
    ) { contentPadding ->
        Column(modifier = Modifier.padding(contentPadding)) {
            PrimaryTabRow(selectedTabIndex = selectedDestination) {
                NewGameTabs.entries.forEachIndexed { index, destination ->
                    Tab(
                        selected = selectedDestination == index,
                        onClick = {
                            tabNavController.navigate(route = destination.route)
                            selectedDestination = index
                        },
                        text = {
                            Text(
                                text = stringResource(destination.labelResource),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
            NavHost(
                tabNavController,
                startDestination = startDestination.route
            ) {
                NewGameTabs.entries.forEach { destination ->
                    composable(destination.route) {
                        when (destination) {
                            NewGameTabs.PLAYERS -> PlayerSetupScreen(viewModel)
                            NewGameTabs.COUNTERS -> {
                                val counters by viewModel.counterSettingsUI.collectAsStateWithLifecycle()
                                CounterSettingsList(
                                    counters = counters,
                                    onMoveUp = remember { { viewModel.moveCounterUp(it)} },
                                    onMoveDown = remember { { viewModel.moveCounterDown(it) } },
                                    onDialog = remember { { dialog = it } },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val curDialog = dialog
    if (curDialog != null) {
        CounterSettingsDialog(
            curDialog,
            onDelete = remember { { viewModel.deleteCounter(it) } },
            onAddCounter = remember { { name, defaultVal -> viewModel.addCounter(name, defaultVal) } },
            onUpdateCounter = remember { { id, name, defaultVal -> viewModel.updateCounter(id, name, defaultVal) } },
            onClearDialog = remember { { dialog = null } },
        )
    }
}



@Composable
fun PlayerSetupScreen(
    viewModel: NewGameViewModel,
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()
    val currentState = state.value ?: return

    // initialize the player count to either 2 or whatever was saved before
    val playerCount = rememberFWheelPickerState(remember {
        if (currentState.playerCount == 0)
            return@remember 1 // 2 players by default
        currentState.playerCount - 1
    })

    // when the index is changed, save to disk
    LaunchedEffect(playerCount) {
        snapshotFlow { playerCount.currentIndex }
            .collect {
                viewModel.setPlayerCount(playerCount.currentIndex + 1)
            }
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                FHorizontalWheelPicker(
                    modifier = Modifier.height(48.dp),
                    state = playerCount,
                    count = 100,
                ) { index ->
                    Text((index + 1).toString())
                }
            }
        }
    }
}

