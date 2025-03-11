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
import net.multun.gamecounter.store.NewGameRepository
import javax.inject.Inject

@HiltViewModel
class NewGameCounterSettingsViewModel @Inject constructor(private val repository: NewGameRepository) : ViewModel(), CounterSettingsActions {
    val settingsUIState = repository.appState.map { appState ->
        appState.counters.map {
            CounterSettingsUIState(it.id, it.name, it.defaultValue)
        }.toImmutableList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = persistentListOf(),
    )

    override fun addCounter(counterName: String, defaultValue: Int) {
        viewModelScope.launch {
            repository.addCounter(defaultValue, counterName)
        }
    }

    override fun deleteCounter(counterId: CounterId) {
        viewModelScope.launch {
            repository.removeCounter(counterId)
        }
    }

    override fun moveCounterUp(counterId: CounterId) {
        viewModelScope.launch {
            repository.moveCounter(counterId, -1)
        }
    }

    override fun moveCounterDown(counterId: CounterId) {
        viewModelScope.launch {
            repository.moveCounter(counterId, 1)
        }
    }

    override fun updateCounter(counterId: CounterId, name: String, defaultVal: Int) {
        viewModelScope.launch {
            repository.updateCounter(counterId, name, defaultVal)
        }
    }
}