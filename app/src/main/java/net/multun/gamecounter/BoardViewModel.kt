package net.multun.gamecounter

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.multun.gamecounter.datastore.CounterId
import net.multun.gamecounter.datastore.PlayerId
import net.multun.gamecounter.datastore.AppStateRepository
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

enum class BoardUIMode {
    STARTUP,
    COUNTERS,
    ROLL,
}

sealed interface UIState
sealed interface BoardUI : UIState {
    val players: ImmutableList<CardUIState>
}
data object StartupUI : UIState
data class SetupUI(val hasCounters: Boolean, val hasPlayers: Boolean) : UIState
data class RollUI(val selectedDice: Int, override val players: ImmutableList<RollCardUIState>) : BoardUI
data class CounterBoardUI(override val players: ImmutableList<CounterCardUIState>) : BoardUI

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
}

data class CounterCardUIState(
    override val id: PlayerId,
    override val color: Color,
    val counter: PlayerCounterUIState?,
) : CardUIState

data class RollCardUIState(
    override val id: PlayerId,
    override val color: Color,
    val roll: Int,
) : CardUIState

private const val TAG = "BoardViewModel"

@HiltViewModel
class BoardViewModel @Inject constructor(private val repository: AppStateRepository) : ViewModel() {
    // combo timers
    private val comboCounters = MutableStateFlow(persistentMapOf<PlayerId, PersistentMap<CounterId, Int>>())
    private val comboCountersTimers = mutableMapOf<PlayerId, MutableMap<CounterId, Job>>()
    private val rollResult = MutableStateFlow<ImmutableList<RollCardUIState>?>(null)

    fun roll() {
        viewModelScope.launch {
            val currentState = repository.appState.first()
            val playerCount = currentState.players.size
            val diceSize = currentState.selectedDice
            val order = if (diceSize <= 0)
                (1 .. playerCount).shuffled()
            else
                (1 .. playerCount).map { (1..diceSize).random() }
            val newRollResult = currentState.players.zip(order) {
                    player, playerRoll ->
                RollCardUIState(
                    id = player.id,
                    color = player.color,
                    roll = playerRoll,
                )
            }.toPersistentList()
            rollResult.update { newRollResult }        }
    }

    fun selectDice(diceSize: Int) {
        viewModelScope.launch {
            repository.selectDice(diceSize)
        }
    }

    fun clearRoll() {
        rollResult.update { null }
    }

    val boardUIState = combine(
        repository.appState,
        comboCounters,
        rollResult,
    ) { appState, combos, rollResult ->
        if (rollResult != null) {
            return@combine RollUI(
                players = rollResult,
                selectedDice = appState.selectedDice,
            )
        }

        if (appState.counters.isEmpty() || appState.players.isEmpty())
            return@combine SetupUI(
                hasCounters = appState.counters.isNotEmpty(),
                hasPlayers = appState.players.isNotEmpty(),
            )

        CounterBoardUI(
            players = appState.players.map { player ->
                CounterCardUIState(
                    id = player.id,
                    color = player.color,
                    counter = player.selectedCounter?.let {
                            counterId ->
                        PlayerCounterUIState(
                            id = player.selectedCounter,
                            hasMultipleCounters = appState.counters.size > 1,
                            counterValue = player.counters[counterId]!!,
                            counterName = appState.counters.find { it.id == counterId }!!.name,
                            combo = combos[player.id]?.get(counterId),
                        )
                    },
                )
            }.toPersistentList(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StartupUI,
    )

    fun newGame() {
        viewModelScope.launch {
            // clear combo counters
            comboCounters.update { it.clear() }

            // stop combo reset timers
            for (playerComboTimers in comboCountersTimers.values) {
                for (timer in playerComboTimers.values)
                    timer.cancel()
                playerComboTimers.clear()
            }
            comboCountersTimers.clear()
            repository.newGame()
        }
    }

    fun addPlayer() {
        Log.i(TAG, "adding new player")
        viewModelScope.launch {
            repository.addPlayer()
        }
    }

    fun updateCounter(playerId: PlayerId, counterId: CounterId, counterDelta: Int) {
        viewModelScope.launch {
            // update the counter
            repository.updatePlayerCounter(playerId, counterId, counterDelta)

            // update the combo counter
            comboCounters.update {
                val currentPlayerCounters = it[playerId]
                val newPlayerCounters = if (currentPlayerCounters == null) {
                    persistentMapOf(counterId to counterDelta)
                } else {
                    val oldCounter = currentPlayerCounters[counterId] ?: 0
                    currentPlayerCounters.put(counterId, oldCounter + counterDelta)
                }
                it.put(playerId, newPlayerCounters)
            }

            // stop the old combo timer
            comboCountersTimers[playerId]?.remove(counterId)?.cancel()

            // start a job to reset the combo counter
            val playerTimers = comboCountersTimers.computeIfAbsent(playerId) { mutableMapOf() }
            playerTimers[counterId] = viewModelScope.launch {
                delay(2500.milliseconds)
                // reset the combo counter once the timer expires
                comboCounters.update {
                    val currentPlayerCounters = it[playerId] ?: return@update it
                    it.put(playerId, currentPlayerCounters.remove(counterId))
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

    fun removePlayer(playerId: PlayerId) {
        viewModelScope.launch {
            repository.removePlayer(playerId)
        }
    }
}