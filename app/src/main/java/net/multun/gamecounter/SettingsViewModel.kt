package net.multun.gamecounter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.multun.gamecounter.store.CounterId
import net.multun.gamecounter.store.GameRepository
import javax.inject.Inject

data class CounterUIState(
    val id: CounterId,
    val name: String,
    val defaultValue: Int,
)

data class SettingsUIState(
    val counters: ImmutableList<CounterUIState>,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(private val repository: GameRepository) : ViewModel() {
    val settingsUIState = repository.appState.map { appState ->
        SettingsUIState(
            counters = appState.counters.map {
                CounterUIState(it.id, it.name, it.defaultValue)
            }.toImmutableList(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUIState(counters = persistentListOf()),
    )

    fun addCounter(counterName: String, defaultValue: Int) {
        viewModelScope.launch {
            repository.addCounter(defaultValue, counterName)
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

    fun setCounterName(counterId: CounterId, name: String) {
        viewModelScope.launch {
            repository.setCounterName(counterId, name)
        }
    }

    fun setCounterDefaultValue(counterId: CounterId, defaultValue: Int) {
        viewModelScope.launch {
            repository.setCounterDefaultValue(counterId, defaultValue)
        }
    }
}