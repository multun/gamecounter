package net.multun.gamecounter.store

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.flow.map
import net.multun.gamecounter.DefaultSettings
import net.multun.gamecounter.PaletteColor
import net.multun.gamecounter.proto.ProtoGame
import net.multun.gamecounter.proto.copy
import net.multun.gamecounter.proto.counter
import net.multun.gamecounter.proto.player
import javax.inject.Inject

@JvmInline
@Immutable
value class CounterId(val value: Int)

sealed interface CounterUpdate
data class FixedUpdate(val isLarge: Boolean, val sign: Int) : CounterUpdate
data class CustomUpdate(val delta: Int) : CounterUpdate


@JvmInline
@Immutable
value class PlayerId(val value: Int)

data class GameState(
    val isPlayable: Boolean,
    val selectedDice: Int, // either -1 for player order, or dice size
    val alwaysUprightMode: Boolean,
    val players: ImmutableList<Player>,
    val counters: ImmutableList<Counter>,
)

data class Counter(
    val id: CounterId,
    val defaultValue: Int,
    val name: String,
    val smallStep: Int?,
    val largeStep: Int?,
)

data class Player(
    val id: PlayerId,
    val selectedCounter: CounterId?,
    val counters: PersistentMap<CounterId, Int>,
    val color: Color,
    val name: String,
)

class GameRepository @Inject constructor(private val appStateStore: GameStore) {
    val appState = appStateStore.data.map { protoAppState ->
        GameState(
            isPlayable = protoAppState.counterCount != 0,
            selectedDice = protoAppState.selectedDice,
            alwaysUprightMode = protoAppState.alwaysUprightMode,
            players = protoAppState.playerList.map { protoPlayer ->
                val selectedCounter = if (protoPlayer.selectedCounter == -1)
                    null
                else
                    CounterId(protoPlayer.selectedCounter)

                Player(
                    id = PlayerId(protoPlayer.id),
                    selectedCounter = selectedCounter,
                    name = protoPlayer.name,
                    color = Color(protoPlayer.color),
                    counters = protoPlayer.countersMap.entries.associate {
                        Pair(CounterId(it.key), it.value)
                    }.toPersistentMap(),
                )
            }.toPersistentList(),
            counters = protoAppState.counterList.map { protoCounter ->
                Counter(
                    id = CounterId(protoCounter.id),
                    defaultValue = protoCounter.defaultValue,
                    name = protoCounter.name,
                    smallStep = if (protoCounter.step == 0) null else protoCounter.step,
                    largeStep = if (protoCounter.largeStep == 0) null else protoCounter.largeStep,
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

    suspend fun addCounter(defaultValue: Int, name: String, step: Int?, largeStep: Int?): CounterId {
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
                    this.step = step ?: 0
                    this.largeStep = largeStep ?: 0
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
            val builder = oldState.toBuilder()
            builder.addPlayers(count)
            builder.build()
        }
    }

    suspend fun startGame(playerCount: Int, counters: List<ProtoGame.Counter>) {
        appStateStore.updateData {
            val builder = ProtoGame.Game.newBuilder()
            builder.addAllCounter(counters)
            builder.addPlayers(playerCount)
            builder.build()
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

    private suspend fun updatePlayer(playerId: PlayerId, updater: (ProtoGame.Game, ProtoGame.Player) -> ProtoGame.Player) {
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

    suspend fun updatePlayerCounter(playerId: PlayerId, counterId: CounterId, counterUpdate: CounterUpdate): Int {
        var updateSize = 0
        updatePlayer(playerId) {
            game, oldPlayer ->
            val counterIndex = game.getCounterIndex(counterId)
            if (counterIndex == -1)
                return@updatePlayer oldPlayer
            val counter = game.getCounter(counterIndex)
            updateSize = when (counterUpdate) {
                is FixedUpdate -> {
                    val step = if (!counterUpdate.isLarge) {
                        if (counter.step == 0) DefaultSettings.DEFAULT_SMALL_STEP else counter.step
                    } else {
                        if (counter.largeStep == 0) DefaultSettings.DEFAULT_LARGE_STEP else counter.largeStep
                    }
                    step * counterUpdate.sign
                }
                is CustomUpdate -> counterUpdate.delta
            }

            oldPlayer.copy {
                val oldCounter = counters[counterId.value]!!
                this.counters.put(counterId.value, oldCounter + updateSize)
            }
        }
        return updateSize
    }

    suspend fun setPlayerColor(playerId: PlayerId, color: Color) {
        updatePlayer(playerId) {
                _, oldPlayer ->
            oldPlayer.copy {
                this.color = color.encode()
            }
        }
    }

    suspend fun setPlayerName(playerId: PlayerId, name: String) {
        updatePlayer(playerId) {
                _, oldPlayer ->
            oldPlayer.copy {
                this.name = name
            }
        }
    }

    suspend fun selectCounter(playerId: PlayerId, counterId: CounterId) {
        updatePlayer(playerId) {
                _, oldPlayer ->
            oldPlayer.copy {
                this.selectedCounter = counterId.value
            }
        }
    }

    suspend fun updateCounter(counterId: CounterId, name: String, defaultValue: Int, step: Int?, largeStep: Int?) {
        appStateStore.updateData { oldState ->
            val counterIndex = oldState.getCounterIndex(counterId)
            if (counterIndex == -1)
                return@updateData oldState

            val newCounter = oldState.getCounter(counterIndex).copy {
                this.name = name
                this.defaultValue = defaultValue
                this.step = step ?: 0
                this.largeStep = largeStep ?: 0
            }
            val newState = oldState.toBuilder()
            newState.setCounter(counterIndex, newCounter)
            newState.build()
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

    suspend fun movePlayer(playerId: PlayerId, direction: Int) {
        appStateStore.updateData { oldState ->
            // find the current index of the counter
            val playerIndex = oldState.playerList.indexOfFirst { it.id == playerId.value }
            if (playerIndex == -1)
                return@updateData oldState
            val player = oldState.getPlayer(playerIndex)

            var newPlayerIndex = playerIndex + direction
            if (newPlayerIndex < 0)
                newPlayerIndex = oldState.playerCount - 1
            if (newPlayerIndex > (oldState.playerCount - 1))
                newPlayerIndex = 0

            oldState.toBuilder()
                .removePlayer(playerIndex)
                .addPlayer(newPlayerIndex, player)
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

    suspend fun setAlwaysUprightMode(alwaysUprightMode: Boolean) {
        appStateStore.updateData { oldState ->
            oldState.copy {
                this.alwaysUprightMode = alwaysUprightMode
            }
        }
    }
}

fun ProtoGame.GameOrBuilder.getDefaultCounters(): Map<Int, Int> {
    return counterList.associate {
        Pair(it.id, it.defaultValue)
    }
}

fun ProtoGame.GameOrBuilder.getPlayerIndex(playerId: PlayerId): Int {
    return playerList.indexOfFirst { it.id == playerId.value }
}

fun ProtoGame.GameOrBuilder.getCounterIndex(counterId: CounterId): Int {
    return counterList.indexOfFirst { it.id == counterId.value }
}

fun Color.encode(): Long {
    assert(colorSpace == ColorSpaces.Srgb)
    return (value shr 32).toLong()
}

fun ProtoGame.Game.Builder.addPlayers(playerCount: Int) {
    // color allocation
    val oldCounters = this.getDefaultCounters()
    val usedColors = this.playerList.map { Color(it.color) }.toMutableList()
    fun allocateColor(): Color {
        val newColor = PaletteColor.allocate(usedColors).color
        usedColors.add(newColor)
        return newColor
    }

    // id allocation
    val newPlayerIdStart = (this.playerList.maxOfOrNull { it.id } ?: -1) + 1

    for (newPlayerIndex in 0 until playerCount) {
        val playerId = newPlayerIdStart + newPlayerIndex

        this.addPlayer(player {
            this.id = playerId
            this.color = allocateColor().encode()
            this.counters.putAll(oldCounters)
            this.selectedCounter = if (oldCounters.isEmpty())
                -1
            else
                this@addPlayers.counterList[0].id
        })
    }
}
