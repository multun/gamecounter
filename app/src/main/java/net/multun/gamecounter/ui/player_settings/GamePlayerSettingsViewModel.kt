package net.multun.gamecounter.ui.player_settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.multun.gamecounter.store.GameRepository
import net.multun.gamecounter.store.PlayerId
import javax.inject.Inject

// the counter settings for the currently running game
@HiltViewModel
class GamePlayerSettingsViewModel @Inject constructor(private val repository: GameRepository) : ViewModel(),
    PlayerSettingsActions {

    val settingsUIState = repository.appState.map { appState ->
        appState.players.map {
            PlayerSettingsUIState(it.id, it.name, it.color)
        }.toImmutableList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = persistentListOf(),
    )

    override fun addPlayer() {
        viewModelScope.launch {
            repository.addPlayers(1)
        }
    }

    override fun deletePlayer(playerId: PlayerId) {
        viewModelScope.launch {
            repository.removePlayer(playerId)
        }
    }

    override fun movePlayerUp(playerId: PlayerId) {
        viewModelScope.launch {
            repository.movePlayer(playerId, -1)
        }
    }

    override fun movePlayerDown(playerId: PlayerId) {
        viewModelScope.launch {
            repository.movePlayer(playerId, 1)
        }
    }

    override fun setPlayerName(playerId: PlayerId, name: String) {
        viewModelScope.launch {
            repository.setPlayerName(playerId, name)
        }
    }
}