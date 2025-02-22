package net.multun.gamecounter.ui.main_menu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import net.multun.gamecounter.Screens


@Composable
fun MainMenu(viewModel: MainMenuViewModel, navController: NavController, modifier: Modifier = Modifier) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()
    val currentState = state.value ?: return

    Scaffold(modifier = modifier) { innerPadding ->
        Box(contentAlignment = Alignment.Center, modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.wrapContentSize()) {
                TextButton(
                    enabled = currentState.canContinue,
                    onClick = { navController.navigate(Screens.Board.route) },
                ) {
                    Text("Continue")
                }

                TextButton(onClick = { navController.navigate(Screens.QuickGameMenu.route) }) {
                    Text("Quick game")
                }
            }
        }
    }
}
