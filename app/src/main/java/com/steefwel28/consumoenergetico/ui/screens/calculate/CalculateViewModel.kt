package com.steefwel28.consumoenergetico.ui.screens.calculate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steefwel28.consumoenergetico.core.di.AppContainer
import com.steefwel28.consumoenergetico.core.signature.CraftedBy
import com.steefwel28.consumoenergetico.domain.scanner.RtcaLabelReading
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@CraftedBy("steefwel28")
class CalculateViewModel(
    private val container: AppContainer,
) : ViewModel() {

    private val _state = MutableStateFlow(CalculateUiState())
    val state: StateFlow<CalculateUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                container.preferences.utility,
                container.preferences.baseMonthlyKwh,
                container.preferences.unitPreferences,
            ) { utility, baseKwh, units -> Triple(utility, baseKwh, units) }
                .collect { (utility, baseKwh, units) ->
                    _state.update { current ->
                        current.copy(
                            utility = utility,
                            baseMonthlyKwh = baseKwh,
                            unitPreferences = units,
                        )
                    }
                }
        }
    }

    fun update(transform: (CalculateUiState) -> CalculateUiState) {
        _state.update(transform)
    }

    /** Aplica solo los campos que el OCR logró leer; el resto queda como estaba. */
    fun applyScan(reading: RtcaLabelReading) {
        _state.update { current ->
            current.copy(
                btuInput = reading.btuPerHour?.let { whole(it) } ?: current.btuInput,
                wattsInput = reading.nominalWatts?.let { whole(it) } ?: current.wattsInput,
                seerInput = reading.seer?.let { decimal(it) } ?: current.seerInput,
            )
        }
    }

    fun save() {
        val entity = _state.value.toEntity(System.currentTimeMillis()) ?: return
        if (_state.value.isSaving) return

        _state.update { it.copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            runCatching { container.database.acUnitDao().insert(entity) }
                .onSuccess {
                    _state.update { current ->
                        current.copy(
                            isSaving = false,
                            savedMessage = "“${entity.nombreEspacio}” quedó registrado en Mi Casa.",
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { current ->
                        current.copy(
                            isSaving = false,
                            saveError = error.message ?: "No se pudo guardar el equipo.",
                        )
                    }
                }
        }
    }

    fun dismissSaveResult() {
        _state.update { it.copy(savedMessage = null, saveError = null) }
    }

    private fun whole(value: Double): String = value.toLong().toString()

    private fun decimal(value: Double): String =
        String.format(Locale.US, "%.1f", value).trimEnd('0').trimEnd('.')
}
