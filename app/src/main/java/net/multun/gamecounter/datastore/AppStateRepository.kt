package net.multun.gamecounter.datastore

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
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
@Immutable
value class CounterId(val value: Int)

@JvmInline
@Immutable
value class PlayerId(val value: Int)

data class UserAppState(
    val selectedDice: Int, // either -1 for player order, or dice size
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


class AppStateRepository @Inject constructor(private val appStateStore: DataStore<AppState>) {
    val appState = appStateStore.data.map { protoAppState ->
        UserAppState(
            selectedDice = protoAppState.selectedDice,
            players = protoAppState.playerList.map { protoPlayer ->
                val selectedCounter = if (protoPlayer.selectedCounter == -1)
                    null
                else
                    CounterId(protoPlayer.selectedCounter)

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
    }

    suspend fun resetPlayerCounters() {
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
            if (counterIndex == -1)
                return@updateData oldState
            val builder = oldState.toBuilder().removeCounter(counterIndex)

            // find the new default counter, if any
            var newDefaultCounter = -1
            if (builder.counterList.size > 0)
                newDefaultCounter = builder.counterList[0].id

            // remove references to the counter from players
            builder.clearPlayer()
            for (oldPlayer in oldState.playerList) {
                val newPlayer = oldPlayer.toBuilder()
                newPlayer.removeCounters(counterId.value)
                if (newPlayer.selectedCounter == counterId.value)
                    newPlayer.setSelectedCounter(newDefaultCounter)
                builder.addPlayer(newPlayer)
            }
            builder.build()
        }
    }

    suspend fun addPlayers(count: Int) {
        appStateStore.updateData { oldState ->
            // color allocation
            val oldCounters = oldState.getDefaultCounters()
            val usedColors = oldState.playerList.map { Color(it.color) }.toMutableSet()
            fun allocateColor(): Color {
                val unusedColor = DEFAULT_PALETTE.find { !usedColors.contains(it) } ?: DEFAULT_PALETTE[0]
                usedColors.add(unusedColor)
                return unusedColor
            }

            // id allocation
            val newPlayerIdStart = (oldState.playerList.maxOfOrNull { it.id } ?: -1) + 1

            oldState.copy {
                for (newPlayerIndex in 0 until count) {
                    val playerId = newPlayerIdStart + newPlayerIndex

                    this.player.add(player {
                        this.id = playerId
                        this.color = allocateColor().encode()
                        this.counters.putAll(oldCounters)
                        this.selectedCounter = if (oldCounters.isEmpty())
                            -1
                        else
                            oldState.counterList[0].id
                    })
                }
            }
        }
    }

    suspend fun removePlayer(playerId: PlayerId) {
        appStateStore.updateData { oldState ->
            val playerIndex = oldState.getPlayerIndex(playerId)
            if (playerIndex == -1)
                return@updateData oldState
            oldState.toBuilder().removePlayer(playerIndex).build()
        }
    }

    private suspend fun updateCounter(counterId: CounterId, updater: (Counter) -> Counter) {
        appStateStore.updateData { oldState ->
            val counterIndex = oldState.getCounterIndex(counterId)
            if (counterIndex == -1)
                return@updateData oldState

            val newCounter = updater(oldState.getCounter(counterIndex))
            val newState = oldState.toBuilder()
            newState.setCounter(counterIndex, newCounter)
            newState.build()
        }
    }

    private suspend fun updatePlayer(playerId: PlayerId, updater: (AppState, Player) -> Player) {
        appStateStore.updateData { oldState ->
            val playerIndex = oldState.getPlayerIndex(playerId)
            if (playerIndex == -1)
                return@updateData oldState

            val newPlayer = updater(oldState, oldState.getPlayer(playerIndex))
            val newState = oldState.toBuilder()
            newState.setPlayer(playerIndex, newPlayer)
            newState.build()
        }
    }

    suspend fun updatePlayerCounter(playerId: PlayerId, counterId: CounterId, difference: Int) {
        updatePlayer(playerId) {
            _, oldPlayer ->
            oldPlayer.copy {
                val oldCounter = counters[counterId.value]!!
                this.counters.put(counterId.value, oldCounter + difference)
            }
        }
    }

    suspend fun setPlayerColor(playerId: PlayerId, color: Color) {
        updatePlayer(playerId) {
                _, oldPlayer ->
            oldPlayer.copy {
                this.color = color.encode()
            }
        }
    }

    suspend fun changeCounterSelection(playerId: PlayerId, direction: Int) {
        updatePlayer(playerId) {
                oldState, oldPlayer ->
            // find the index of the previously selected counter
            val counterIndex = oldState.counterList.indexOfFirst { it.id == oldPlayer.selectedCounter }
            if (counterIndex == -1)
                return@updatePlayer oldPlayer

            // compute the index of the new selected counter
            val count = oldState.counterCount
            val newCounterIndex = ((counterIndex + direction) + count) % count
            val newCounterId = oldState.getCounter(newCounterIndex).id

            // update the player
            oldPlayer.copy {
                this.selectedCounter = newCounterId
            }
        }
    }

    suspend fun setCounterName(counterId: CounterId, name: String) {
        updateCounter(counterId) {
            it.copy {
                this.name = name
            }
        }
    }

    suspend fun setCounterDefaultValue(counterId: CounterId, defaultValue: Int) {
        updateCounter(counterId) {
            it.copy {
                this.defaultValue = defaultValue
            }
        }
    }

    suspend fun moveCounter(counterId: CounterId, direction: Int) {
        appStateStore.updateData { oldState ->
            // find the current index of the counter
            val counterIndex = oldState.counterList.indexOfFirst { it.id == counterId.value }
            if (counterIndex == -1)
                return@updateData oldState
            val counter = oldState.getCounter(counterIndex)

            var newCounterIndex = counterIndex + direction
            if (newCounterIndex < 0)
                newCounterIndex = 0
            if (newCounterIndex > (oldState.counterCount - 1))
                newCounterIndex = oldState.counterCount - 1

            oldState.toBuilder()
                .removeCounter(counterIndex)
                .addCounter(newCounterIndex, counter)
                .build()
        }
    }

    suspend fun selectDice(diceSize: Int) {
        appStateStore.updateData { oldState ->
            oldState.copy {
                selectedDice = diceSize
            }
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

fun AppState.getCounterIndex(counterId: CounterId): Int {
    return counterList.indexOfFirst { it.id == counterId.value }
}

fun Color.encode(): Long {
    assert(colorSpace == ColorSpaces.Srgb)
    return (value shr 32).toLong()
}