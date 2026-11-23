package com.steefwel28.consumoenergetico.ui.screens.recommend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steefwel28.consumoenergetico.core.di.AppContainer
import com.steefwel28.consumoenergetico.core.signature.CraftedBy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@CraftedBy("steefwel28")
class RecommendViewModel(
    container: AppContainer,
) : ViewModel() {

    private val _state = MutableStateFlow(RecommendUiState())
    val state: StateFlow<RecommendUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            container.preferences.unitPreferences.collect { preferences ->
                _state.update { current -> current.copy(unitPreferences = preferences) }
            }
        }
    }

    fun update(transform: (RecommendUiState) -> RecommendUiState) {
        _state.update(transform)
    }
}
