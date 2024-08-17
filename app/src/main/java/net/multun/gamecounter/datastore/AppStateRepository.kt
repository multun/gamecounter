package net.multun.gamecounter.datastore

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.datastore.core.DataStore
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.flow.map
import net.multun.gamecounter.DEFAULT_PALETTE
import javax.inject.Inject

@JvmInline
value class CounterId(val value: Int)

@JvmInline
value class PlayerId(val value: Int)

data class UserAppState(
    val players: ImmutableList<UserPlayer>,
    val counters: ImmutableList<UserCounter>,
)

data class UserCounter(
    val id: CounterId,
    val defaultValue: Int,
    val name: String,
)

data class UserPlayer(
    val id: PlayerId,
    val selectedCounter: CounterId?,
    val counters: PersistentMap<CounterId, Int>,
    val color: Color,
)

const val TAG = "AppStateRepository"

class AppStateRepository @Inject constructor(private val appStateStore: DataStore<AppState>) {
    val appState = appStateStore.data.map { protoAppState ->
        val newAppState = UserAppState(
            players = protoAppState.playerList.map { protoPlayer ->
                val selectedCounter = if (protoPlayer.selectedCounter == -1)
                    null
                else
                    CounterId(protoPlayer.selectedCounter)

                Log.i(TAG, "player with color: ${protoPlayer.color.toULong()}")
                UserPlayer(
                    id = PlayerId(protoPlayer.id),
                    selectedCounter = selectedCounter,
                    color = Color(protoPlayer.color),
                    counters = protoPlayer.countersMap.entries.associate {
                        Pair(CounterId(it.key), it.value)
                    }.toPersistentMap(),
                )
            }.toPersistentList(),
            counters = protoAppState.counterList.map { protoCounter ->
                UserCounter(
                    id = CounterId(protoCounter.id),
                    defaultValue = protoCounter.defaultValue,
                    name = protoCounter.name,
                )
            }.toPersistentList(),
        )
        Log.i(TAG, "new appState: $newAppState")
        newAppState
    }

    suspend fun newGame() {
        appStateStore.updateData { oldState ->
            val defaultCounters = oldState.getDefaultCounters()
            val builder = oldState.toBuilder()
            builder.clearPlayer()
            for (oldPlayer in oldState.playerList) {
                val playerBuilder = oldPlayer.toBuilder()
                playerBuilder.clearCounters()
                playerBuilder.putAllCounters(defaultCounters)
                builder.addPlayer(playerBuilder)
            }
            builder.build()
        }
    }

    suspend fun addCounter(defaultValue: Int, name: String): CounterId {
        var counterId = 0
        appStateStore.updateData { oldState ->
            // allocate an ID
            counterId = (oldState.counterList.maxOfOrNull { it.id } ?: -1) + 1

            // create a new state
            oldState.copy {
                // add a counter
                this.counter.add(counter {
                    this.id = counterId
                    this.defaultValue = defaultValue
                    this.name = name
                })

                // update all players to add the counter
                this.player.clear()
                for (oldPlayer in oldState.playerList) {
                    val newPlayer = oldPlayer.toBuilder()
                    newPlayer.putCounters(counterId, defaultValue)
                    this.player.add(newPlayer.build())
                }
            }
        }

        return CounterId(counterId)
    }

    suspend fun removeCounter(counterId: CounterId) {
        appStateStore.updateData { oldState ->
            // remove the counter itself
            val counterIndex = oldState.counterList.indexOfFirst { it.id == counterId.value }
            if  (counterIndex == -1)
                return@updateData oldState
            val builder = oldState.toBuilder().removeCounter(counterIndex)

            //  remove references to the counter from players
            builder.clearPlayer()
            for (oldPlayer in oldState.playerList) {
                val newPlayer = oldPlayer.toBuilder()
                newPlayer.removeCounters(counterId.value)
                builder.addPlayer(newPlayer)
            }
            builder.build()
        }
    }

    suspend fun addPlayer(): PlayerId {
        var playerId = 0
        appStateStore.updateData { oldState ->
            // allocate the id
            playerId = (oldState.playerList.maxOfOrNull { it.id } ?: -1) + 1

            val usedColors = oldState.playerList.map { Color(it.color) }.toSet()
            val color = DEFAULT_PALETTE.find { !usedColors.contains(it) } ?: DEFAULT_PALETTE[0]

            oldState.copy {
                this.player.add(player {
                    this.id = playerId
                    this.color = color.encode()
                    this.counters.putAll(oldState.getDefaultCounters())
                    this.selectedCounter = if (oldState.counterCount == 0)
                         -1
                    else
                        oldState.counterList[0].id
                })
            }
        }
        return PlayerId(playerId)
    }

    suspend fun removePlayer(playerId: PlayerId) {
        appStateStore.updateData { oldState ->
            val playerIndex = oldState.getPlayerIndex(playerId)
            if (playerIndex == -1)
                return@updateData oldState
            oldState.toBuilder().removePlayer(playerIndex).build()
        }
    }

    suspend fun updatePlayerCounter(playerId: PlayerId, counterId: CounterId, difference: Int) {
        appStateStore.updateData { oldState ->
            val playerIndex = oldState.getPlayerIndex(playerId)
            if (playerIndex == -1)
                return@updateData oldState

            // create a player with an updated counter
            val newPlayer = oldState.getPlayer(playerIndex).toBuilder()
            val oldCounter = newPlayer.getCountersOrThrow(counterId.value)
            newPlayer.putCounters(counterId.value, oldCounter + difference)

            // create a new state with this player
            val newState = oldState.toBuilder()
            newState.setPlayer(playerIndex, newPlayer)
            newState.build()
        }
    }

    suspend fun setPlayerColor(playerId: PlayerId, color: Color) {
        appStateStore.updateData { oldState ->
            val playerIndex = oldState.getPlayerIndex(playerId)
            if (playerIndex == -1)
                return@updateData oldState

            // create a player with an updated color
            val newPlayer = oldState.getPlayer(playerIndex).copy {
                this.color = color.encode()
            }

            // create a new state with this player
            val newState = oldState.toBuilder()
            newState.setPlayer(playerIndex, newPlayer)
            newState.build()
        }
    }

    suspend fun changeCounterSelection(playerId: PlayerId, direction: Int) {
        appStateStore.updateData { oldState ->
            // fetch the old player
            val playerIndex = oldState.getPlayerIndex(playerId)
            if (playerIndex == -1)
                return@updateData oldState
            val oldPlayer = oldState.getPlayer(playerIndex)
            val oldSelectedCounter = oldPlayer.selectedCounter

            // find the index of the previously selected counter
            val counterIndex = oldState.counterList.indexOfFirst { it.id == oldSelectedCounter }
            if (counterIndex == -1)
                return@updateData oldState

            // compute the index of the new selected counter
            val count = oldState.counterCount
            val newCounterIndex = ((counterIndex + direction) + count) % count
            val newCounterId = oldState.getCounter(newCounterIndex).id

            // update the player
            val newPlayer = oldState.getPlayer(playerIndex).copy {
                this.selectedCounter = newCounterId
            }
            val newState = oldState.toBuilder()
            newState.setPlayer(playerIndex, newPlayer)
            newState.build()
        }
    }
}

fun AppState.getDefaultCounters(): Map<Int, Int> {
    return counterList.associate {
        Pair(it.id, it.defaultValue)
    }
}

fun AppState.getPlayerIndex(playerId: PlayerId): Int {
    return playerList.indexOfFirst { it.id == playerId.value }
}

fun Color.encode(): Long {
    assert(colorSpace == ColorSpaces.Srgb)
    return (value shr 32).toLong()
}