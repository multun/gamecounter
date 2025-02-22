package net.multun.gamecounter.ui.quick_game_menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.multun.gamecounter.store.GameRepository
import javax.inject.Inject


data class QuickGameUI(val needsCounters: Boolean)


@HiltViewModel
class QuickGameViewModel @Inject constructor(private val repository: GameRepository) : ViewModel() {
    val uiState = repository.appState.map { appState ->
        QuickGameUI(
            needsCounters = appState.counters.isEmpty(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    fun setupGame(playerCount: Int) {
        viewModelScope.launch {
            repository.clearPlayers()
            repository.addPlayers(playerCount)
        }
    }
}