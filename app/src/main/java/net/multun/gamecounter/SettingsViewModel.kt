package net.multun.gamecounter

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.multun.gamecounter.datastore.CounterId
import net.multun.gamecounter.datastore.AppStateRepository
import javax.inject.Inject

data class CounterUIState(
    val id: CounterId,
    val name: String,
    val defaultValue: Int,
)

data class SettingsUIState(
    val counters: ImmutableList<CounterUIState>,
)

private const val TAG = "SettingsViewModel"

@HiltViewModel
class SettingsViewModel @Inject constructor(repository: AppStateRepository) : ViewModel() {
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

    fun deleteCounter(counterId: CounterId) {

    }

    fun moveCounterUp(counterId: CounterId) {

    }

    fun moveCounterDown(counterId: CounterId) {

    }

    fun setCounterName(counterId: CounterId, name: String) {

    }

    fun setCounterDefaultValue(counterId: CounterId, defaultValue: Int) {

    }
}