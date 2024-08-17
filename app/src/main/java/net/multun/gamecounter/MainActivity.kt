package net.multun.gamecounter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import net.multun.gamecounter.components.BoardScreen
import net.multun.gamecounter.components.CounterSettingsScreen
import net.multun.gamecounter.ui.theme.GamecounterTheme

sealed class Screens(val route: String) {
    data object Board: Screens("board")
    data object CounterSettings: Screens("settings")
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val boardViewModel: BoardViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GamecounterTheme {
                val controller = rememberNavController()
                NavHost(navController = controller, startDestination = Screens.Board.route) {
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