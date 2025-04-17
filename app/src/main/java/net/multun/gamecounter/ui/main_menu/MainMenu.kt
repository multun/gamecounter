package net.multun.gamecounter.ui.main_menu

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import net.multun.gamecounter.PaletteColor
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
fun AppLogo(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 4.dp,
        modifier = modifier,
    ) {
        val logo = painterResource(R.drawable.ic_launcher_foreground)
        val bgColor = colorResource(R.color.ic_launcher_background)
        Canvas(modifier = Modifier.fillMaxSize()) {
            scale(1.5f, pivot = Offset(size.width / 2, size.height / 2)) {
                with(logo){
                    drawRect(color = bgColor)
                    draw(size = this@Canvas.size)
                }
            }
        }
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
                        MainMenuItem(stringResource(R.string.continue_), PaletteColor.Red.color) {
                            navController.navigate(Screens.Board.route)
                        }
                    }

                    MainMenuItem(stringResource(R.string.new_game), PaletteColor.Purple.color) {
                        navController.navigate(Screens.NewGameMenu.route)
                    }
                }
            }

            IconButton(
                onClick = { navController.navigate(Screens.About.route) },
                modifier = Modifier.padding(10.dp).align(Alignment.BottomEnd)
            ) {
                Icon(Icons.Filled.Info, stringResource(R.string.about))
            }
       }
    }
}
