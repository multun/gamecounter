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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.multun.gamecounter.datastore.CounterId
import net.multun.gamecounter.datastore.PlayerId
import net.multun.gamecounter.datastore.AppStateRepository
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

data class BoardUIState(
    val canAddPlayer: Boolean,
    val hasMultipleCounters: Boolean,
    val players: ImmutableList<PlayerCardUIState>,
)

data class PlayerCounterUIState(
    val id: CounterId,
    val counterValue: Int,
    val counterName: String,
    val combo: Int?,
)

data class PlayerCardUIState(
    val id: PlayerId,
    val color: Color,
    val counter: PlayerCounterUIState?,
    val rollResult: Int?,
)

private const val TAG = "BoardViewModel"

@HiltViewModel
class BoardViewModel @Inject constructor(private val repository: AppStateRepository) : ViewModel() {
    // combo timers
    private val comboCounters = MutableStateFlow(persistentMapOf<PlayerId, PersistentMap<CounterId, Int>>())
    private val comboCountersTimers = mutableMapOf<PlayerId, MutableMap<CounterId, Job>>()

    val boardUIState = combine(
        repository.appState,
        comboCounters,
    ) { appState, combos ->
        BoardUIState(
            canAddPlayer = appState.players.size < 8,
            hasMultipleCounters = appState.counters.size > 1,
            players = appState.players.map { player ->
                PlayerCardUIState(
                    id = player.id,
                    color = player.color,
                    counter = player.selectedCounter?.let {
                            counterId ->
                        PlayerCounterUIState(
                            id = player.selectedCounter,
                            counterValue = player.counters[counterId]!!,
                            counterName = appState.counters.find { it.id == counterId }!!.name,
                            combo = combos[player.id]?.get(counterId),
                        )
                    },
                    rollResult = null,
                )
            }.toPersistentList(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BoardUIState(canAddPlayer = false, hasMultipleCounters = false, persistentListOf()),
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

        // update the counter
        viewModelScope.launch {
            repository.updatePlayerCounter(playerId, counterId, counterDelta)
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