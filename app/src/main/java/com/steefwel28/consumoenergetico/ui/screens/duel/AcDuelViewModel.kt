package com.steefwel28.consumoenergetico.ui.screens.duel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steefwel28.consumoenergetico.core.di.AppContainer
import com.steefwel28.consumoenergetico.core.signature.CraftedBy
import com.steefwel28.consumoenergetico.domain.calculator.PanamaUtility
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@CraftedBy("steefwel28")
class AcDuelViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val container: AppContainer,
) : ViewModel() {

    private val _state = MutableStateFlow(restoreState())
    val state: StateFlow<AcDuelUiState> = _state.asStateFlow()

    init {
        // La distribuidora global de DataStore siembra la del duelo; luego el
        // selector de la pantalla manda hasta que Ajustes cambie la global.
        viewModelScope.launch {
            container.preferences.utility.collect { utility ->
                _state.update { it.copy(utility = utility) }
            }
        }
    }

    fun update(transform: (AcDuelUiState) -> AcDuelUiState) {
        _state.update(transform)
        persistState(_state.value)
    }

    // --- Muerte de proceso: los campos del duelo sobreviven vía handle. ---

    private fun restoreState(): AcDuelUiState {
        val h = savedStateHandle
        fun text(key: String, fallback: String): String = h[key] ?: fallback
        fun whole(key: String, fallback: Int): Int = h[key] ?: fallback
        fun flag(key: String, fallback: Boolean): Boolean = h[key] ?: fallback
        return AcDuelUiState(
            aliasAInput = text("duel.aliasA", ""),
            btuAInput = text("duel.btuA", "12000"),
            seerAInput = text("duel.seerA", "21"),
            wattsAInput = text("duel.wattsA", ""),
            inverterA = flag("duel.inverterA", true),
            aliasBInput = text("duel.aliasB", ""),
            btuBInput = text("duel.btuB", "12000"),
            seerBInput = text("duel.seerB", "16"),
            wattsBInput = text("duel.wattsB", ""),
            inverterB = flag("duel.inverterB", false),
            hoursPerDay = whole("duel.hours", 8),
            baseKwhInput = text("duel.baseKwh", "350"),
            utility = h.get<String>("duel.utility")
                ?.let { raw -> runCatching { PanamaUtility.valueOf(raw) }.getOrNull() }
                ?: PanamaUtility.ENSA,
            priceDiffInput = text("duel.priceDiff", ""),
        )
    }

    private fun persistState(state: AcDuelUiState) {
        val h = savedStateHandle
        h["duel.aliasA"] = state.aliasAInput
        h["duel.btuA"] = state.btuAInput
        h["duel.seerA"] = state.seerAInput
        h["duel.wattsA"] = state.wattsAInput
        h["duel.inverterA"] = state.inverterA
        h["duel.aliasB"] = state.aliasBInput
        h["duel.btuB"] = state.btuBInput
        h["duel.seerB"] = state.seerBInput
        h["duel.wattsB"] = state.wattsBInput
        h["duel.inverterB"] = state.inverterB
        h["duel.hours"] = state.hoursPerDay
        h["duel.baseKwh"] = state.baseKwhInput
        h["duel.utility"] = state.utility.name
        h["duel.priceDiff"] = state.priceDiffInput
    }
}
