package net.multun.gamecounter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import dagger.hilt.android.AndroidEntryPoint
import net.multun.gamecounter.ui.AboutScreen
import net.multun.gamecounter.ui.board.BoardScreen
import net.multun.gamecounter.ui.board.BoardViewModel
import net.multun.gamecounter.ui.counter_settings.CounterSettingsScreen
import net.multun.gamecounter.ui.counter_settings.GameCounterSettingsViewModel
import net.multun.gamecounter.ui.counter_settings.NewGameCounterSettingsViewModel
import net.multun.gamecounter.ui.main_menu.MainMenu
import net.multun.gamecounter.ui.main_menu.MainMenuViewModel
import net.multun.gamecounter.ui.new_game_menu.NewGameMenu
import net.multun.gamecounter.ui.new_game_menu.NewGameViewModel
import net.multun.gamecounter.ui.theme.GamecounterTheme

sealed class Screens(val route: String) {
    data object MainMenu: Screens("main_menu")
    data object NewGameMenu: Screens("new_game_menu")
    data object Board: Screens("board")
    // the counter settings of the currently running game
    data object CounterSettings: Screens("counter_settings")
    // the counter settings of the not yet started game
    data object NewGameCounterSettings: Screens("new_game_counter_settings")
    data object About: Screens("about")
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val boardViewModel: BoardViewModel by viewModels()
    private val gameCounterSettingsViewModel: GameCounterSettingsViewModel by viewModels()
    private val newGameCounterSettingsViewModel: NewGameCounterSettingsViewModel by viewModels()
    private val mainMenuViewModel: MainMenuViewModel by viewModels()
    private val newGameViewModel: NewGameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // the default splash screen does a weird flashing animation in dark mode
        installSplashScreen().setOnExitAnimationListener { splashScreenViewProvider ->
            val height = splashScreenViewProvider.view.width.toFloat()
            splashScreenViewProvider.view
                .animate()
                .translationY(-height)
                .alpha(0f)
                .setDuration(400)
                .withEndAction { splashScreenViewProvider.remove() }
                .start()
        }

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
                            val counters by gameCounterSettingsViewModel.settingsUIState.collectAsStateWithLifecycle()
                            CounterSettingsScreen(counters, gameCounterSettingsViewModel, controller)
                        }
                        composable(route = Screens.NewGameCounterSettings.route) {
                            val counters by newGameCounterSettingsViewModel.settingsUIState.collectAsStateWithLifecycle()
                            CounterSettingsScreen(counters, newGameCounterSettingsViewModel, controller)
                        }
                        composable(route = Screens.About.route) {
                            AboutScreen(controller)
                        }
                    }
                }
            }
        }
    }
}