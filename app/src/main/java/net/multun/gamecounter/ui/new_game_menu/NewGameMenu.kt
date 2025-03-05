package net.multun.gamecounter.ui.new_game_menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.sd.lib.compose.wheel_picker.FHorizontalWheelPicker
import com.sd.lib.compose.wheel_picker.rememberFWheelPickerState
import kotlinx.coroutines.launch
import net.multun.gamecounter.DEFAULT_PALETTE
import net.multun.gamecounter.Screens
import net.multun.gamecounter.ui.GameCounterTopBar
import net.multun.gamecounter.ui.board.GameButton
import net.multun.gamecounter.ui.board.GameCard


@Composable
fun NewGameMenu(viewModel: NewGameViewModel, navController: NavController, modifier: Modifier = Modifier) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()
    val currentState = state.value ?: return

    val playerCount = rememberFWheelPickerState(1)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            GameCounterTopBar("New game", navController)
        },
        floatingActionButton = {
            GameButton(baseColor = DEFAULT_PALETTE[6], onClick = {
                if (currentState.needsCounters) {
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
                    viewModel.setupGame(playerCount.currentIndex + 1)
                    navController.navigate(Screens.Board.route)
                }
            }) {
                Text("Start game")
            }
        }
    ) { innerPadding ->
        Box(contentAlignment = Alignment.Center, modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                GameCard(baseColor = DEFAULT_PALETTE[4]) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        Text(
                            "Player count",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.alpha(0.8f),
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
                    baseColor = DEFAULT_PALETTE[7],
                    onClick = {
                        navController.navigate(Screens.CounterSettings.route)
                    }
                ) {
                    Text("Counter settings")
                }

            }
        }
    }
}
