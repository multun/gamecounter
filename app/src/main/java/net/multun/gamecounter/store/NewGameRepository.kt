package net.multun.gamecounter.store

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.map
import net.multun.gamecounter.proto.ProtoNewGame
import net.multun.gamecounter.proto.copy
import net.multun.gamecounter.proto.counter
import javax.inject.Inject


data class NewGameState(
    val playerCount: Int,
    val counters: ImmutableList<Counter>,
)

class NewGameRepository @Inject constructor(private val appStateStore: NewGameStore) {
    val appState = appStateStore.data.map { protoAppState ->
        NewGameState(
            playerCount = protoAppState.playerCount,
            counters = protoAppState.counterList.map { protoCounter ->
                Counter(
                    id = CounterId(protoCounter.id),
                    defaultValue = protoCounter.defaultValue,
                    name = protoCounter.name,
                )
            }.toPersistentList(),
        )
    }

    suspend fun setPlayerCount(playerCount: Int) {
        appStateStore.updateData { oldState ->
            oldState.copy {
                this.playerCount = playerCount
            }
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
            oldState.toBuilder().removeCounter(counterIndex).build()
        }
    }

    suspend fun updateCounter(counterId: CounterId, name: String, defaultValue: Int) {
        appStateStore.updateData { oldState ->
            val counterIndex = oldState.getCounterIndex(counterId)
            if (counterIndex == -1)
                return@updateData oldState

            val newCounter = oldState.getCounter(counterIndex).copy {
                this.name = name
                this.defaultValue = defaultValue
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
}

fun ProtoNewGame.NewGame.getCounterIndex(counterId: CounterId): Int {
    return counterList.indexOfFirst { it.id == counterId.value }
}