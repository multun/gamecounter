package net.multun.gamecounter.ui.main_menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import net.multun.gamecounter.DEFAULT_PALETTE
import net.multun.gamecounter.Screens
import net.multun.gamecounter.ui.board.GameButton


@Composable
fun MainMenuItem(name: String, baseColor: Color, onClick: () -> Unit) {
    GameButton(baseColor, onClick = onClick) {
        Text(name, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun MainMenu(viewModel: MainMenuViewModel, navController: NavController, modifier: Modifier = Modifier) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()
    val currentState = state.value ?: return

    Scaffold(modifier = modifier) { innerPadding ->
        Box(contentAlignment = Alignment.Center, modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()) {

            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.width(IntrinsicSize.Max),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (currentState.canContinue) {
                    MainMenuItem("Continue", DEFAULT_PALETTE[0]) {
                        navController.navigate(Screens.Board.route)
                    }
                }

                MainMenuItem("New game", DEFAULT_PALETTE[2]) {
                    navController.navigate(Screens.NewGameMenu.route)
                }
            }
       }
    }
}
