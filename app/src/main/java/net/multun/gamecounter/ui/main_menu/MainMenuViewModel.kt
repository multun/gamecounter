package net.multun.gamecounter.ui.main_menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.multun.gamecounter.store.GameRepository
import javax.inject.Inject


data class MainMenuUI(val canContinue: Boolean)


@HiltViewModel
class MainMenuViewModel @Inject constructor(private val repository: GameRepository) : ViewModel() {
    val uiState = repository.appState.map { appState ->
        MainMenuUI(
            canContinue = appState.isPlayable,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )
}