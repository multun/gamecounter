package net.multun.gamecounter.ui.main_menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import net.multun.gamecounter.DEFAULT_PALETTE
import net.multun.gamecounter.Screens
import net.multun.gamecounter.toDisplayColor
import net.multun.gamecounter.ui.board.BoardCard


@Composable
fun MainMenuItem(name: String, baseColor: Color, onClick: () -> Unit) {
    BoardCard(
        color = baseColor.toDisplayColor(),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(name, modifier = Modifier.padding(20.dp))
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
                modifier = Modifier.wrapContentSize(),
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
