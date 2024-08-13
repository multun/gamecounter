package net.multun.gamecounter

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.multun.gamecounter.data.AppState
import net.multun.gamecounter.data.CounterId
import net.multun.gamecounter.data.PlayerId
import net.multun.gamecounter.data.Player
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class BoardViewModel @Inject constructor(val appState: AppState) : ViewModel() {
    val playerIds get() = appState.getPlayerOrder()
    private val comboCounters = mutableStateMapOf<PlayerId, PersistentMap<CounterId, Int>>()
    private val comboCountersTimers = mutableMapOf<PlayerId, MutableMap<CounterId, Job>>()

    fun getPlayer(playerId: PlayerId): Player? {
        return appState.getPlayer(playerId)
    }

    fun reset() {
        appState.reset()

        // clear combo counters
        comboCounters.clear()

        // stop combo reset timers
        for (playerComboTimers in comboCountersTimers.values) {
            for (timer in playerComboTimers.values)
                 timer.cancel()
            playerComboTimers.clear()
        }
        comboCountersTimers.clear()
    }

    fun addPlayer() {
        if (canAddPlayer)
            appState.addPlayer()
    }

    fun getCounterCombo(playerId: PlayerId, counterId: CounterId): Int? {
        return comboCounters[playerId]?.get(counterId)
    }

    fun getCounterName(counterId: CounterId): String? {
        return appState.getCounterName(counterId)
    }

    val canAddPlayer get() = playerIds.size < 8

    private fun resetComboCounter(playerId: PlayerId, counterId: CounterId) {
        // reset the counter
        comboCounters.computeIfPresent(playerId) {
            _, playerCounters ->
            playerCounters.remove(counterId)
        }
    }

    private fun resetComboTimer(playerId: PlayerId, counterId: CounterId) {
        // start a job to reset the delta counter
        val playerTimers = comboCountersTimers.computeIfAbsent(playerId) { mutableMapOf() }

        // stop the old timer
        val oldTimer = playerTimers.remove(counterId)
        oldTimer?.cancel()

        // start the a new timer
        playerTimers[counterId] = viewModelScope.launch {
            delay(2500.milliseconds)
            resetComboCounter(playerId, counterId)
        }
    }

    private fun updateComboCounter(playerId: PlayerId, counterId: CounterId, counterDelta: Int) {
        comboCounters.compute(playerId) { _, counters ->
            if (counters == null)
                return@compute persistentMapOf(counterId to counterDelta)
            val oldCounter = counters[counterId] ?: 0
            counters.put(counterId, oldCounter + counterDelta)
        }
    }

    private fun updateCounter(playerId: PlayerId, counterId: CounterId, counterDelta: Int) {
        updateComboCounter(playerId, counterId, counterDelta)
        resetComboTimer(playerId, counterId)
        appState.updatePlayerCounter(playerId, counterId, counterDelta)
    }

    fun incrCount(playerId: PlayerId) {
        updateCounter(playerId, CounterId(0), 1)
    }

    fun decrCount(playerId: PlayerId) {
        updateCounter(playerId, CounterId(0), -1)
    }
}