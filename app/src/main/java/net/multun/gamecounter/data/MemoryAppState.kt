package net.multun.gamecounter.data

import androidx.compose.ui.graphics.Color
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.multun.gamecounter.DEFAULT_PALETTE
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockDataStore @Inject constructor() : DataStore {
    override var defaultHealth: Int = 42
    override var players: PersistentList<PlayerState> = persistentListOf()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Singleton
    @Binds
    abstract fun bindAppState(repository: MemoryAppState): AppState

    @Singleton
    @Binds
    abstract fun bindDataStore(repository: MockDataStore): DataStore
}

@Singleton
class MemoryAppState @Inject constructor(val dataStore: DataStore) : AppState {
    override var defaultHealth: Int = dataStore.defaultHealth

    private fun usedColors(): Set<Color> {
        val usedColors = mutableSetOf<Color>()
        for (playerStateFlow in _players.values) {
            val player = playerStateFlow.value ?: continue
            usedColors.add(player.color)
        }
        return usedColors
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

    private var _nextPlayerId: Int
    private val _players: MutableMap<PlayerId, MutableStateFlow<PlayerState?>> = mutableMapOf()
    private val _playerOrder: MutableStateFlow<PersistentList<PlayerId>>

    init {
        _nextPlayerId = 0
        val players = dataStore.players
        for (player in players) {
            if (_nextPlayerId <= player.id.value)
                _nextPlayerId = player.id.value + 1
            _players[player.id] = MutableStateFlow(player)
        }
        _playerOrder = MutableStateFlow(players.map { it.id }.toPersistentList())
    }

    private fun allocatePlayerId(): PlayerId {
        return PlayerId(_nextPlayerId++)
    }

    override fun addPlayer(): PlayerId {
        val playerId = allocatePlayerId()
        val color = unusedPaletteColor()
        val player = PlayerState(playerId, defaultHealth, color)
        _players[playerId] = MutableStateFlow(player)
        _playerOrder.update {
            it.add(playerId)
        }
        return playerId
    }

    override fun removePlayer(playerId: PlayerId) {
        val playerState = _players.remove(playerId)!!
        playerState.update { null }
        _playerOrder.update {
            it.remove(playerId)
        }
    }

    override fun reset() {
        for (player in _players.values) {
            player.update {
                it?.copy(health = defaultHealth)
            }
        }
    }

    override fun watchPlayerOrder(): StateFlow<PersistentList<PlayerId>> {
        return _playerOrder.asStateFlow()
    }

    override fun watchPlayer(playerId: PlayerId): StateFlow<PlayerState?> {
        return _players[playerId]!!.asStateFlow()
    }

    override fun updatePlayerHealth(playerId: PlayerId, healthDelta: Int) {
        _players[playerId]!!.update {
            val oldPlayer = it!!
            PlayerState(oldPlayer.id, oldPlayer.health + healthDelta, oldPlayer.color)
        }
    }

    override fun setPlayerColor(playerId: PlayerId, color: Color) {
        _players[playerId]!!.update {
            val oldPlayer = it!!
            PlayerState(oldPlayer.id, oldPlayer.health, color)
        }
    }
}