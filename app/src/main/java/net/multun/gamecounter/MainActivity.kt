package net.multun.gamecounter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import net.multun.gamecounter.ui.board.BoardScreen
import net.multun.gamecounter.ui.board.BoardViewModel
import net.multun.gamecounter.ui.counter_settings.CounterSettingsScreen
import net.multun.gamecounter.ui.counter_settings.SettingsViewModel
import net.multun.gamecounter.ui.main_menu.MainMenu
import net.multun.gamecounter.ui.main_menu.MainMenuViewModel
import net.multun.gamecounter.ui.new_game_menu.NewGameMenu
import net.multun.gamecounter.ui.new_game_menu.NewGameViewModel
import net.multun.gamecounter.ui.theme.GamecounterTheme

sealed class Screens(val route: String) {
    data object MainMenu: Screens("main_menu")
    data object NewGameMenu: Screens("new_game_menu")
    data object Board: Screens("board")
    data object CounterSettings: Screens("counter_settings")
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val boardViewModel: BoardViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val mainMenuViewModel: MainMenuViewModel by viewModels()
    private val newGameViewModel: NewGameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GamecounterTheme {
                // the surface is used to provide a sane background during navigation transitions
                // without it, navigation in dark mode will flash a light background
                Surface {
                    val controller = rememberNavController()
                    NavHost(navController = controller, startDestination = Screens.MainMenu.route) {
                        composable(route = Screens.MainMenu.route) {
                            MainMenu(mainMenuViewModel, controller)
                        }
                        composable(route = Screens.NewGameMenu.route) {
                            NewGameMenu(newGameViewModel, controller)
                        }
                        composable(route = Screens.Board.route) {
                            BoardScreen(boardViewModel, controller, modifier = Modifier.fillMaxSize())
                        }
                        composable(route = Screens.CounterSettings.route) {
                            CounterSettingsScreen(settingsViewModel, controller)
                        }
                    }
                }
            }
        }
    }
}