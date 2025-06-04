package net.multun.gamecounter.ui.new_game_menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.multun.gamecounter.proto.counter
import net.multun.gamecounter.store.CounterId
import net.multun.gamecounter.store.GameRepository
import net.multun.gamecounter.store.NewGameRepository
import net.multun.gamecounter.store.makeDefaultCounter
import net.multun.gamecounter.ui.counter_settings.CounterSettingsUIState
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
            val counters = if (newGameSettings.counters.isEmpty())
                listOf(makeDefaultCounter())
            else
                newGameSettings.counters.map {
                    counter {
                        this.id = it.id.value
                        this.name = it.name
                        this.defaultValue = it.defaultValue
                    }
                }
            currentGame.startGame(
                playerCount = newGameSettings.playerCount,
                counters = counters,
            )
        }
    }

    val counterSettingsUI = newGame.appState.map { appState ->
        appState.counters.map {
            CounterSettingsUIState(it.id, it.name, it.defaultValue)
        }.toImmutableList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = persistentListOf(),
    )

    fun addCounter(counterName: String, defaultValue: Int) {
        viewModelScope.launch {
            newGame.addCounter(defaultValue, counterName)
        }
    }

    fun deleteCounter(counterId: CounterId) {
        viewModelScope.launch {
            newGame.removeCounter(counterId)
        }
    }

    fun moveCounterUp(counterId: CounterId) {
        viewModelScope.launch {
            newGame.moveCounter(counterId, -1)
        }
    }

    fun moveCounterDown(counterId: CounterId) {
        viewModelScope.launch {
            newGame.moveCounter(counterId, 1)
        }
    }

    fun updateCounter(counterId: CounterId, name: String, defaultVal: Int) {
        viewModelScope.launch {
            newGame.updateCounter(counterId, name, defaultVal)
        }
    }
}