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
    val players: ImmutableList<CardUIState>
}
data class RollUI(val selectedDice: Int, override val players: ImmutableList<RollCardUIState>) :
    BoardUI
data class CounterBoardUI(override val players: ImmutableList<CounterCardUIState>) : BoardUI
data class PlayerNameBoardUI(override val players: ImmutableList<PlayerNameUIState>) : BoardUI


data class PlayerCounterUIState(
    val id: CounterId,
    val hasMultipleCounters: Boolean,
    val counterValue: Int,
    val counterName: String,
    val combo: Int?,
)

sealed interface CardUIState {
    val id: PlayerId
    val color: Color
    val name: String
}

data class CounterCardUIState(
    override val id: PlayerId,
    override val color: Color,
    override val name: String,
    val counter: PlayerCounterUIState?,
) : CardUIState

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
                players = appState.players.map { player ->
                    CounterCardUIState(
                        id = player.id,
                        color = player.color,
                        name = player.name,
                        counter = player.selectedCounter?.let {
                                counterId ->
                            PlayerCounterUIState(
                                id = player.selectedCounter,
                                hasMultipleCounters = appState.counters.size > 1,
                                counterValue = player.counters[counterId]!!,
                                counterName = appState.counters.find { it.id == counterId }!!.name,
                                combo = combos[ComboCounterId(player.id, counterId)],
                            )
                        },
                    )
                }.toPersistentList(),
            )
            BoardPlayerNames -> PlayerNameBoardUI(
                players = appState.players.map {
                    player -> PlayerNameUIState(player.id, player.color, player.name)
                }.toImmutableList()
            )
            is BoardRoll -> RollUI(
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
        addPlayers(1)
    }

    fun addPlayers(count: Int) {
        viewModelScope.launch {
            repository.addPlayers(count)
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

    fun nextCounter(playerId: PlayerId) {
        viewModelScope.launch {
            repository.changeCounterSelection(playerId, 1)
        }
    }

    fun previousCounter(playerId: PlayerId) {
        viewModelScope.launch {
            repository.changeCounterSelection(playerId, -1)
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

    fun removePlayer(playerId: PlayerId) {
        viewModelScope.launch {
            repository.removePlayer(playerId)
        }
    }
}