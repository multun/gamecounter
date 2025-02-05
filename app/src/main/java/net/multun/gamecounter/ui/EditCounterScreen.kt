package net.multun.gamecounter.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import net.multun.gamecounter.SettingsViewModel
import net.multun.gamecounter.datastore.CounterId

@Composable
fun EditCounterScreen(
    counterId: CounterId,
    viewModel: SettingsViewModel,
    navController: NavController,
) {
    val appState by viewModel.settingsUIState.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        GameCounterTopBar("Edit counter", navController)
    }) { contentPadding ->
        Column(modifier = Modifier
            .padding(contentPadding)
            .padding(10.dp)) {
            Text("TODO")
        }
    }
}