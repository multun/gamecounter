package net.multun.gamecounter.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.toMutableStateMap
import androidx.compose.ui.graphics.Color
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.multun.gamecounter.DEFAULT_PALETTE
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockAppStateStorage @Inject constructor() : AppStateStorage {
    override fun load(): AppStateSnapshot {
        return AppStateSnapshot(
            players = persistentListOf(Player(
                id = PlayerId(0),
                color = DEFAULT_PALETTE[0],
                selectedCounter = CounterId(0),
                counters = persistentMapOf(
                    CounterId(0) to 42
                ),
            )),
            counters = persistentListOf(Counter(
                id = CounterId(0),
                defaultValue = 10,
                name = "hp",
            )),
        )
    }

    override fun save(appState: AppStateSnapshot) {
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Singleton
    @Binds
    abstract fun bindAppState(repository: MemoryAppState): AppState

    @Singleton
    @Binds
    abstract fun bindAppStateStorage(repository: MockAppStateStorage): AppStateStorage
}

@Singleton
class MemoryAppState @Inject constructor(appStateStorage: AppStateStorage) : AppState {
    private var _nextPlayerId: Int
    private var _nextCounterId: Int

    private val _players: SnapshotStateMap<PlayerId, Player>
    private val _counters: SnapshotStateMap<CounterId, Counter>
    private val _playerOrder: SnapshotStateList<PlayerId>
    private val _counterOrder: SnapshotStateList<CounterId>

    private fun usedColors(): Set<Color> {
        return _players.values.map { it.color }.toSet()
    }

    private fun unusedPaletteColor(): Color {
        val usedColors = usedColors()
        for (color in DEFAULT_PALETTE) {
            if (color in usedColors)
                continue
            return color
        }
        return Color.LightGray
    }

    init {
        val initialState = appStateStorage.load()

        _nextPlayerId = 0
        for (player in initialState.players) {
            if (_nextPlayerId <= player.id.value)
                _nextPlayerId = player.id.value + 1
        }

        _players = initialState.players.map { Pair(it.id, it) }.toMutableStateMap()
        _playerOrder = initialState.players.map { it.id }.toMutableStateList()

        _nextCounterId = 0
        for (counter in initialState.counters) {
            if (_nextCounterId <= counter.id.value)
                _nextCounterId = counter.id.value + 1
        }
        _counters = initialState.counters.map { Pair(it.id, it) }.toMutableStateMap()
        _counterOrder = initialState.counters.map { it.id }.toMutableStateList()
    }

    private fun allocatePlayerId(): PlayerId {
        return PlayerId(_nextPlayerId++)
    }

    private fun allocateCounterId(): CounterId {
        return CounterId(_nextCounterId++)
    }

    private fun getDefaultCounters(): PersistentMap<CounterId, Int> {
        return _counters.values.associate { Pair(it.id, it.defaultValue) }.toPersistentMap()
    }

    override fun reset() {
        Snapshot.withMutableSnapshot {
            val defaultCounters = getDefaultCounters()
            for (playerId in _players.keys) {
                val newPlayer = _players[playerId]!!.copy(counters = defaultCounters)
                _players[playerId] = newPlayer
            }
        }
    }
    override fun addPlayer(): PlayerId {
        val playerId = allocatePlayerId()
        Snapshot.withMutableSnapshot {
            val color = unusedPaletteColor()
            val player = Player(playerId, _counterOrder[0], getDefaultCounters(), color)
            _players[playerId] = player
            _playerOrder.add(playerId)
        }
        return playerId
    }

    override fun removePlayer(playerId: PlayerId) {
        Snapshot.withMutableSnapshot {
            _players.remove(playerId)
            _playerOrder.remove(playerId)
        }
    }

    override fun getPlayerOrder(): List<PlayerId> {
        return _playerOrder
    }

    override fun getPlayer(playerId: PlayerId): Player? {
        return _players[playerId]
    }

    override fun addCounter(defaultValue: Int, name: String): CounterId {
        val counterId = allocateCounterId()
        val counter = Counter(counterId, defaultValue, name)
        _counters[counterId] = counter
        return counterId
    }

    override fun removeCounter(counterId: CounterId) {
        _counters.remove(counterId)
    }

    override fun getCounterName(counterId: CounterId): String? {
        return _counters[counterId]?.name
    }

    override fun updatePlayerCounter(playerId: PlayerId, counterId: CounterId, difference: Int) {
        _players.computeIfPresent(playerId) { _, player ->
            val oldCounters = player.counters
            val newCounters = oldCounters.mutate {
                it.compute(counterId) {_, counterValue ->
                    val oldValue = counterValue ?: _counters[counterId]!!.defaultValue
                    oldValue + difference
                }
            }
            player.copy(counters = newCounters)
        }
    }

    override fun setPlayerColor(playerId: PlayerId, color: Color) {
        _players.computeIfPresent(playerId) { _, player ->
            player.copy(color = color)
        }
    }

    override fun setPlayerSelectedCounter(playerId: PlayerId, selectedCounter: CounterId) {
        _players.computeIfPresent(playerId) { _, player ->
            player.copy(selectedCounter = selectedCounter)
        }
    }
}