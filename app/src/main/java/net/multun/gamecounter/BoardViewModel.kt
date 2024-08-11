package net.multun.gamecounter

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.StateFlow
import net.multun.gamecounter.data.AppState
import net.multun.gamecounter.data.PlayerId
import net.multun.gamecounter.data.PlayerState
import javax.inject.Inject

data class PlayerUIState(val count: Int, val color: Color) {
    fun applyCountDelta(delta: Int): PlayerUIState {
        return PlayerUIState(count + delta, color)
    }
}

@HiltViewModel
class BoardViewModel @Inject constructor(val appState: AppState) : ViewModel() {
    val playerIds: StateFlow<ImmutableList<PlayerId>> = appState.watchPlayerOrder()

    fun watchPlayer(playerId: PlayerId): StateFlow<PlayerState?> {
        return appState.watchPlayer(playerId)
    }

    fun reset() {
        appState.reset()
    }
    fun addPlayer() {
        appState.addPlayer()
    }

    private fun updateCounter(playerId: PlayerId, counterDelta: Int) {
        appState.updatePlayerHealth(playerId, counterDelta)
    }

    fun incrCount(playerId: PlayerId) {
        updateCounter(playerId, 1)
    }

    fun decrCount(playerId: PlayerId) {
        updateCounter(playerId, -1)
    }
}