package net.multun.gamecounter

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class BoardUIState(
    val mode: BoardUIMode,
    val players: ImmutableList<PlayerCardUIState>,
)

data class PlayerCounterUIState(
    val id: CounterId,
    val hasMultipleCounters: Boolean,
    val counterValue: Int,
    val counterName: String,
    val combo: Int?,
)

data class PlayerCardUIState(
    val id: PlayerId,
    val color: Color,
    val counter: PlayerCounterUIState?,
    val roll: Int?,
)

private const val TAG = "BoardViewModel"

@HiltViewModel
class BoardViewModel @Inject constructor(private val repository: AppStateRepository) : ViewModel() {
    // combo timers
    private val comboCounters = MutableStateFlow(persistentMapOf<PlayerId, PersistentMap<CounterId, Int>>())
    private val comboCountersTimers = mutableMapOf<PlayerId, MutableMap<CounterId, Job>>()
    private val rollResult = MutableStateFlow<ImmutableList<PlayerCardUIState>?>(null)

    fun roll() {
        viewModelScope.launch {
            val currentState = repository.appState.first()
            val order = (1 .. currentState.players.size).shuffled()
            val newRollResult = currentState.players.zip(order) {
                player, playerRoll ->
                PlayerCardUIState(
                    id = player.id,
                    color = player.color,
                    counter = null,
                    roll = playerRoll,
                )
            }.toPersistentList()
            rollResult.update { newRollResult }
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
            return@combine BoardUIState(
                mode = BoardUIMode.ROLL,
                players = rollResult,
            )
        }

        BoardUIState(
            mode = BoardUIMode.COUNTERS,
            players = appState.players.map { player ->
                PlayerCardUIState(
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
                    roll = null,
                )
            }.toPersistentList(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BoardUIState(mode = BoardUIMode.STARTUP, persistentListOf()),
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