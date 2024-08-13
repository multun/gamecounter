package net.multun.gamecounter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import net.multun.gamecounter.components.GameCounterUI
import net.multun.gamecounter.ui.theme.GamecounterTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: BoardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GamecounterTheme {
                GameCounterUI(viewModel, modifier = Modifier.fillMaxSize())
            }
        }
    }
}