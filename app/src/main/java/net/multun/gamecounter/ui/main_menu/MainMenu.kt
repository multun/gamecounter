package net.multun.gamecounter.ui.main_menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import net.multun.gamecounter.DEFAULT_PALETTE
import net.multun.gamecounter.R
import net.multun.gamecounter.Screens
import net.multun.gamecounter.ui.board.GameButton


@Composable
fun MainMenuItem(name: String, baseColor: Color, onClick: () -> Unit) {
    GameButton(baseColor, onClick = onClick) {
        Text(name, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 4.dp,
        modifier = modifier,
    ) {
        Image(
            painterResource(R.drawable.ic_launcher_foreground),
            null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .graphicsLayer(
                    scaleX = 1.5f,
                    scaleY = 1.5f,
                ),
        )
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

            Column(verticalArrangement = Arrangement.spacedBy(50.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AppLogo(Modifier.size(150.dp))

                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.width(IntrinsicSize.Max),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (currentState.canContinue) {
                        MainMenuItem(stringResource(R.string.continue_), DEFAULT_PALETTE[0]) {
                            navController.navigate(Screens.Board.route)
                        }
                    }

                    MainMenuItem(stringResource(R.string.new_game), DEFAULT_PALETTE[2]) {
                        navController.navigate(Screens.NewGameMenu.route)
                    }
                }
            }
       }
    }
}
