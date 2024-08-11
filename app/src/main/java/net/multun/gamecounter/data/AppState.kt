package net.multun.gamecounter.data

import androidx.compose.ui.graphics.Color
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow


@JvmInline
value class CounterId(val value: Int)

@JvmInline
value class PlayerId(val value: Int)

data class PlayerState(
    val id: PlayerId,
    val health: Int,
    val color: Color,
) {
    fun copy(health: Int = this.health, color: Color = this.color): PlayerState {
        return PlayerState(id, health, color)
    }
}

interface AppState {
    var defaultHealth: Int

    fun addPlayer(): PlayerId
    fun removePlayer(playerId: PlayerId)
    fun reset()

    fun watchPlayerOrder(): StateFlow<PersistentList<PlayerId>>
    fun watchPlayer(playerId: PlayerId): StateFlow<PlayerState?>

    fun updatePlayerHealth(playerId: PlayerId, healthDelta: Int)
    fun setPlayerColor(playerId: PlayerId, color: Color)
}

interface DataStore {
    var defaultHealth: Int
    var players: PersistentList<PlayerState>
}