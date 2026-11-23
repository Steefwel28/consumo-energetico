package com.steefwel28.consumoenergetico.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steefwel28.consumoenergetico.core.di.AppContainer
import com.steefwel28.consumoenergetico.core.signature.CraftedBy
import com.steefwel28.consumoenergetico.core.updater.UpdateCheckResult
import com.steefwel28.consumoenergetico.data.backup.BackupRestoreResult
import com.steefwel28.consumoenergetico.domain.calculator.PanamaUtility
import com.steefwel28.consumoenergetico.domain.units.LengthUnit
import com.steefwel28.consumoenergetico.domain.units.ShortLengthUnit
import com.steefwel28.consumoenergetico.domain.units.TemperatureUnit
import com.steefwel28.consumoenergetico.domain.units.UnitPreferences
import com.steefwel28.consumoenergetico.domain.units.VolumeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Ajustes y respaldo. El respaldo se serializa aquí y la pantalla solo se
 * encarga del selector SAF, para que el mismo código sirva a archivo y a
 * `content://`.
 */
@CraftedBy("steefwel28")
class SettingsViewModel(
    private val container: AppContainer,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                container.preferences.unitPreferences,
                container.preferences.utility,
                container.preferences.baseMonthlyKwh,
                container.releaseChecker.hasPendingUpdate,
                container.releaseChecker.pendingRelease,
            ) { preferences, utility, baseKwh, pending, release ->
                SettingsUiState(
                    unitPreferences = preferences,
                    utility = utility,
                    baseMonthlyKwh = baseKwh,
                    hasPendingUpdate = pending,
                    pendingRelease = release,
                )
            }.collect { fresh ->
                _state.update { current ->
                    fresh.copy(
                        signatureTaps = current.signatureTaps,
                        isChecking = current.isChecking,
                        message = current.message,
                    )
                }
            }
        }
    }

    fun setLength(unit: LengthUnit) = updatePreferences { it.copy(length = unit) }

    fun setShortLength(unit: ShortLengthUnit) = updatePreferences { it.copy(shortLength = unit) }

    fun setVolume(unit: VolumeUnit) = updatePreferences { it.copy(volume = unit) }

    fun setTemperature(unit: TemperatureUnit) = updatePreferences { it.copy(temperature = unit) }

    fun setUtility(utility: PanamaUtility) {
        viewModelScope.launch { container.preferences.setUtility(utility) }
    }

    fun setBaseMonthlyKwh(kwh: Double) {
        if (!kwh.isFinite() || kwh < 0.0) {
            _state.update { it.copy(message = "El consumo base no puede ser negativo.") }
            return
        }
        viewModelScope.launch { container.preferences.setBaseMonthlyKwh(kwh.coerceAtMost(100_000.0)) }
    }

    fun checkForUpdate() {
        if (_state.value.isChecking) return
        _state.update { it.copy(isChecking = true) }
        viewModelScope.launch {
            val message = when (val result = container.releaseChecker.checkForUpdate()) {
                is UpdateCheckResult.UpdateAvailable ->
                    "Hay una versión nueva: ${result.release.version}."

                is UpdateCheckResult.UpdateSkipped ->
                    "La versión ${result.release.version} está omitida. Búscala de nuevo cuando quieras."

                UpdateCheckResult.UpToDate -> "Ya tienes la última versión."
                UpdateCheckResult.NoStableRelease -> "El repositorio no tiene releases estables todavía."
                is UpdateCheckResult.NetworkError ->
                    "No se pudo consultar GitHub: revisa tu conexión."
            }
            _state.update { it.copy(isChecking = false, message = message) }
        }
    }

    fun skipPendingVersion() {
        val release = _state.value.pendingRelease ?: return
        viewModelScope.launch {
            container.releaseChecker.skipVersion(release.version)
            _state.update { it.copy(message = "Omitiste la versión ${release.version}.") }
        }
    }

    /** Texto del `.json` listo para escribir en el `Uri` del selector SAF. */
    suspend fun exportJson(): String =
        runCatching { container.backupManager.exportJson() }
            .getOrElse { error ->
                _state.update { it.copy(message = "No se pudo preparar el respaldo: ${error.message}") }
                ""
            }

    suspend fun restoreJson(json: String) {
        val result = container.backupManager.importJson(json)
        val message = when (result) {
            is BackupRestoreResult.Success ->
                "Respaldo restaurado: ${result.snapshot.units.size} equipos y " +
                    "${result.snapshot.groups.size} grupos."

            is BackupRestoreResult.Invalid -> result.reason
        }
        _state.update { it.copy(message = message) }
    }

    fun reportFileProblem(reason: String) {
        _state.update { it.copy(message = reason) }
    }

    fun onVersionTapped() {
        _state.update { it.copy(signatureTaps = it.signatureTaps + 1) }
    }

    fun dismissMessage() {
        _state.update { it.copy(message = null) }
    }

    fun dismissSignature() {
        _state.update { it.copy(signatureTaps = 0) }
    }

    private fun updatePreferences(transform: (UnitPreferences) -> UnitPreferences) {
        val updated = transform(_state.value.unitPreferences)
        viewModelScope.launch {
            runCatching { container.preferences.updateUnitPreferences(updated) }
                .onFailure { error ->
                    _state.update {
                        it.copy(message = error.message ?: "No se pudo guardar la unidad.")
                    }
                }
        }
    }
}
