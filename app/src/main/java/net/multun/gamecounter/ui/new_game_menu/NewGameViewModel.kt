package net.multun.gamecounter.ui.new_game_menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.multun.gamecounter.proto.counter
import net.multun.gamecounter.store.GameRepository
import net.multun.gamecounter.store.NewGameRepository
import javax.inject.Inject


data class NewGameUI(
    val playerCount: Int,
    val needsCounters: Boolean,
)

@HiltViewModel
class NewGameViewModel @Inject constructor(
    private val currentGame: GameRepository,
    private val newGame: NewGameRepository,
) : ViewModel() {
    val uiState = newGame.appState.map { appState ->
        NewGameUI(
            playerCount = appState.playerCount,
            needsCounters = appState.counters.isEmpty(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    fun setPlayerCount(playerCount: Int) {
        viewModelScope.launch {
            newGame.setPlayerCount(playerCount)
        }
    }

    fun startGame() {
        viewModelScope.launch {
            val newGameSettings = newGame.appState.first()
            currentGame.startGame(
                playerCount = newGameSettings.playerCount,
                counters = newGameSettings.counters.map {
                    counter {
                        this.id = it.id.value
                        this.name = it.name
                        this.defaultValue = it.defaultValue
                    }
                },
            )
        }
    }
}