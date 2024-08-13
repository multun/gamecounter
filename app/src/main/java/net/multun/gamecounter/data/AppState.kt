package net.multun.gamecounter.data

import androidx.compose.ui.graphics.Color
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap


@JvmInline
value class CounterId(val value: Int)

@JvmInline
value class PlayerId(val value: Int)

data class Counter(
    val id: CounterId,
    val defaultValue: Int,
    val name: String,
)

data class Player(
    val id: PlayerId,
    val selectedCounter: CounterId,
    val counters: PersistentMap<CounterId, Int>,
    val color: Color,
) {
    fun copy(
        selectedCounter: CounterId = this.selectedCounter,
        counters: PersistentMap<CounterId, Int> = this.counters,
        color: Color = this.color,
    ): Player {
        return Player(id, selectedCounter, counters, color)
    }
}

interface AppState {
    fun reset()

    fun addCounter(defaultValue: Int, name: String): CounterId
    fun removeCounter(counterId: CounterId)
    fun getCounterName(counterId: CounterId): String?

    fun addPlayer(): PlayerId
    fun removePlayer(playerId: PlayerId)

    fun getPlayerOrder(): List<PlayerId>
    fun getPlayer(playerId: PlayerId): Player?

    fun updatePlayerCounter(playerId: PlayerId, counterId: CounterId, difference: Int)
    fun setPlayerColor(playerId: PlayerId, color: Color)
    fun setPlayerSelectedCounter(playerId: PlayerId, selectedCounter: CounterId)
}


data class AppStateSnapshot(
    val counters: PersistentList<Counter>,
    val players: PersistentList<Player>,
)

interface AppStateStorage {
    fun load(): AppStateSnapshot
    fun save(appState: AppStateSnapshot)
}