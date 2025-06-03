package net.multun.gamecounter.ui.board

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.multun.gamecounter.store.CounterId
import net.multun.gamecounter.store.PlayerId
import net.multun.gamecounter.store.GameRepository
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds


// the public UI state
sealed interface UIState
data object StartupUI : UIState
data object SetupRequired : UIState
sealed interface BoardUI : UIState {
    val alwaysUprightMode: Boolean
    val players: ImmutableList<CardUIState>
}

data class RollUI(
    val selectedDice: Int,
    override val alwaysUprightMode: Boolean,
    override val players: ImmutableList<RollCardUIState>
) : BoardUI

data class CounterBoardUI(
    override val alwaysUprightMode: Boolean,
    override val players: ImmutableList<CounterCardUIState>
) : BoardUI

data class PlayerNameBoardUI(
    override val alwaysUprightMode: Boolean,
    override val players: ImmutableList<PlayerNameUIState>
) : BoardUI


sealed interface CardUIState {
    val id: PlayerId
    val color: Color
    val name: String
}

data class CounterCardUIState(
    override val id: PlayerId,
    override val color: Color,
    override val name: String,
    val selectedCounter: CounterId,
    val counters: List<CounterUIState>,
) : CardUIState

data class CounterUIState(
    val id: CounterId,
    val name: String,
    val value: Int,
    val combo: Int,
)

data class PlayerNameUIState(
    override val id: PlayerId,
    override val color: Color,
    override val name: String,
) : CardUIState

data class RollCardUIState(
    override val id: PlayerId,
    override val color: Color,
    override val name: String,
    val isOrdinal: Boolean,
    val roll: Int,
) : CardUIState

data class ComboCounterId(val player: PlayerId, val counter: CounterId)

private sealed interface BoardState
data object BoardCounters : BoardState
data object BoardPlayerNames : BoardState
data class BoardRoll(val result: ImmutableList<RollCardUIState>) : BoardState

@HiltViewModel
class BoardViewModel @Inject constructor(private val repository: GameRepository) : ViewModel() {
    // combo timers
    private val comboCounters = MutableStateFlow(persistentMapOf<ComboCounterId, Int>())
    private val comboCountersTimers = UniqueJobPool<ComboCounterId>(viewModelScope)
    private val boardState = MutableStateFlow<BoardState>(BoardCounters)

    fun editPlayerNames() {
        boardState.update { BoardPlayerNames }
    }

    fun roll() {
        viewModelScope.launch {
            val currentState = repository.appState.first()
            val playerCount = currentState.players.size
            val diceSize = currentState.selectedDice
            val order: List<Int>
            val isOrdinal: Boolean
            if (diceSize <= 0) {
                order = (1 .. playerCount).shuffled()
                isOrdinal = true
            } else {
                order = (1 .. playerCount).map { (1..diceSize).random() }
                isOrdinal = false
            }
            val newRollResult = currentState.players.zip(order) {
                    player, playerRoll ->
                RollCardUIState(
                    id = player.id,
                    color = player.color,
                    name = player.name,
                    roll = playerRoll,
                    isOrdinal = isOrdinal,
                )
            }.toPersistentList()
            boardState.update { BoardRoll(newRollResult) }
        }
    }

    fun selectDice(diceSize: Int) {
        viewModelScope.launch {
            repository.selectDice(diceSize)
        }
    }

    fun clearMode() {
        boardState.update { BoardCounters }
    }

    val boardUIState = combine(
        repository.appState,
        comboCounters,
        boardState,
    ) { appState, combos, boardState ->
        if (!appState.isPlayable)
            return@combine SetupRequired

        when (boardState) {
            BoardCounters -> CounterBoardUI(
                alwaysUprightMode = appState.alwaysUprightMode,
                players = appState.players.map { player ->
                    CounterCardUIState(
                        id = player.id,
                        color = player.color,
                        name = player.name,
                        counters = appState.counters.map {
                            CounterUIState(
                                id = it.id,
                                name = it.name,
                                value = player.counters[it.id]!!,
                                combo = combos[ComboCounterId(player.id, it.id)] ?: 0
                            )
                        },
                        selectedCounter = player.selectedCounter ?: appState.counters[0].id
                    )
                }.toPersistentList(),
            )
            BoardPlayerNames -> PlayerNameBoardUI(
                alwaysUprightMode = appState.alwaysUprightMode,
                players = appState.players.map {
                    player -> PlayerNameUIState(player.id, player.color, player.name)
                }.toImmutableList()
            )
            is BoardRoll -> RollUI(
                alwaysUprightMode = appState.alwaysUprightMode,
                players = boardState.result,
                selectedDice = appState.selectedDice,
            )
        }


    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StartupUI,
    )

    fun resetGame() {
        viewModelScope.launch {
            // clear combo counters
            comboCounters.update { it.clear() }

            // stop combo reset timers
            comboCountersTimers.cancelAll()
            repository.resetPlayerCounters()
        }
    }

    fun addPlayer() {
        viewModelScope.launch {
            repository.addPlayers(1)
        }
    }

    fun setUlwaysUprightMode(alwaysUprightMode: Boolean) {
        viewModelScope.launch {
            repository.setAlwaysUprightMode(alwaysUprightMode)
        }
    }

    fun updateCounter(playerId: PlayerId, counterId: CounterId, counterDelta: Int) {
        viewModelScope.launch {
            // update the counter
            repository.updatePlayerCounter(playerId, counterId, counterDelta)

            val counterKey = ComboCounterId(playerId, counterId)
            // update the combo counter
            comboCounters.update {
                val oldCounter = it[counterKey] ?: 0
                it.put(counterKey, oldCounter + counterDelta)
            }

            // start the reset timer
            comboCountersTimers.launch(counterKey) {
                delay(2500.milliseconds)
                // reset the combo counter once the timer expires
                comboCounters.update {
                    it.remove(counterKey)
                }
            }
        }
    }

    fun selectCounter(playerId: PlayerId, counterId: CounterId) {
        viewModelScope.launch {
            repository.selectCounter(playerId, counterId)
        }
    }

    fun setPlayerColor(playerId: PlayerId, color: Color) {
        viewModelScope.launch {
            repository.setPlayerColor(playerId, color)
        }
    }

    fun setPlayerName(playerId: PlayerId, name: String) {
        viewModelScope.launch {
            repository.setPlayerName(playerId, name)
        }
    }
}