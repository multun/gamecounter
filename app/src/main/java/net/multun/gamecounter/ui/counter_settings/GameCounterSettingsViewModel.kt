package net.multun.gamecounter.ui.counter_settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.multun.gamecounter.store.CounterId
import net.multun.gamecounter.store.GameRepository
import javax.inject.Inject


// the counter settings for the currently running game
@HiltViewModel
class GameCounterSettingsViewModel @Inject constructor(private val repository: GameRepository) : ViewModel() {
    val settingsUIState = repository.appState.map { appState ->
        appState.counters.map {
            CounterSettingsUIState(it.id, it.name, it.defaultValue, it.step, it.largeStep)
        }.toImmutableList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = persistentListOf(),
    )

    fun addCounter(counterName: String, defaultValue: Int, step: Int, largeStep: Int) {
        viewModelScope.launch {
            repository.addCounter(defaultValue, counterName, step, largeStep)
        }
    }

    fun deleteCounter(counterId: CounterId) {
        viewModelScope.launch {
            repository.removeCounter(counterId)
        }
    }

    fun moveCounterUp(counterId: CounterId) {
        viewModelScope.launch {
            repository.moveCounter(counterId, -1)
        }
    }

    fun moveCounterDown(counterId: CounterId) {
        viewModelScope.launch {
            repository.moveCounter(counterId, 1)
        }
    }

    fun updateCounter(counterId: CounterId, name: String, defaultVal: Int, step: Int, largeStep: Int) {
        viewModelScope.launch {
            repository.updateCounter(counterId, name, defaultVal, step, largeStep)
        }
    }
}