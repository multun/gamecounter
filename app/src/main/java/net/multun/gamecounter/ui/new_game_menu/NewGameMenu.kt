package net.multun.gamecounter.ui.new_game_menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.sd.lib.compose.wheel_picker.FHorizontalWheelPicker
import com.sd.lib.compose.wheel_picker.rememberFWheelPickerState
import net.multun.gamecounter.PaletteColor
import net.multun.gamecounter.R
import net.multun.gamecounter.Screens
import net.multun.gamecounter.ui.GameCounterTopBar
import net.multun.gamecounter.ui.board.GameButton
import net.multun.gamecounter.ui.board.GameCard


@Composable
fun NewGameMenu(viewModel: NewGameViewModel, navController: NavController, modifier: Modifier = Modifier) {
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

    Scaffold(
        modifier = modifier,
        topBar = {
            GameCounterTopBar(stringResource(R.string.new_game), navController)
        },
        floatingActionButton = {
            GameButton(baseColor = PaletteColor.Indigo.color, onClick = {
                viewModel.startGame()
                navController.navigate(Screens.Board.route)
            }) {
                Text(stringResource(R.string.start_game))
            }
        }
    ) { innerPadding ->
        Box(contentAlignment = Alignment.Center, modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                GameCard(baseColor = PaletteColor.Green.color) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {

                        Text(
                            stringResource(R.string.player_count),
                            style = MaterialTheme.typography.bodyLarge,
                        )

                        FHorizontalWheelPicker(
                            modifier = Modifier.height(48.dp),
                            state = playerCount,
                            count = 100,
                        ) { index ->
                            Text((index + 1).toString())
                        }
                    }
                }

                GameButton(
                    baseColor = PaletteColor.Red.color,
                    onClick = {
                        navController.navigate(Screens.NewGameCounterSettings.route)
                    }
                ) {
                    Text(stringResource(R.string.counters_settings))
                }
            }
        }
    }
}
