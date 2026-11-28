package com.steefwel28.consumoenergetico.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steefwel28.consumoenergetico.R
import com.steefwel28.consumoenergetico.core.di.AppContainer
import com.steefwel28.consumoenergetico.core.signature.CraftedBy
import com.steefwel28.consumoenergetico.core.updater.UpdateCheckResult
import com.steefwel28.consumoenergetico.data.backup.BackupRestoreResult
import com.steefwel28.consumoenergetico.domain.calculator.PanamaUtility
import com.steefwel28.consumoenergetico.domain.settings.ThemeMode
import com.steefwel28.consumoenergetico.domain.units.TemperatureUnit
import com.steefwel28.consumoenergetico.domain.units.UnitPreferences
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
            }.combine(container.preferences.appearance) { fresh, appearance ->
                fresh.copy(appearance = appearance)
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

    fun setTemperature(unit: TemperatureUnit) = updatePreferences { it.copy(temperature = unit) }

    fun setUtility(utility: PanamaUtility) {
        viewModelScope.launch { container.preferences.setUtility(utility) }
    }

    fun setBaseMonthlyKwh(kwh: Double) {
        if (!kwh.isFinite() || kwh < 0.0) {
            _state.update {
                it.copy(message = container.appContext.getString(R.string.set_msg_base_negative))
            }
            return
        }
        viewModelScope.launch { container.preferences.setBaseMonthlyKwh(kwh.coerceAtMost(100_000.0)) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { container.preferences.setThemeMode(mode) }
    }

    fun setOledPure(enabled: Boolean) {
        viewModelScope.launch { container.preferences.setOledPure(enabled) }
    }

    fun checkForUpdate() {
        if (_state.value.isChecking) return
        _state.update { it.copy(isChecking = true) }
        viewModelScope.launch {
            val message = when (val result = container.releaseChecker.checkForUpdate()) {
                is UpdateCheckResult.UpdateAvailable ->
                    container.appContext.getString(R.string.set_msg_update_available, result.release.version)

                is UpdateCheckResult.UpdateSkipped ->
                    container.appContext.getString(R.string.set_msg_update_skipped, result.release.version)

                UpdateCheckResult.UpToDate -> container.appContext.getString(R.string.set_msg_up_to_date)
                UpdateCheckResult.NoStableRelease -> container.appContext.getString(R.string.set_msg_no_release)
                is UpdateCheckResult.NetworkError ->
                    container.appContext.getString(R.string.set_msg_network_error)
            }
            _state.update { it.copy(isChecking = false, message = message) }
        }
    }

    fun skipPendingVersion() {
        val release = _state.value.pendingRelease ?: return
        viewModelScope.launch {
            container.releaseChecker.skipVersion(release.version)
            _state.update {
                it.copy(message = container.appContext.getString(R.string.set_msg_skipped, release.version))
            }
        }
    }

    /** Texto del `.json` listo para escribir en el `Uri` del selector SAF. */
    suspend fun exportJson(): String =
        runCatching { container.backupManager.exportJson() }
            .getOrElse { error ->
                _state.update {
                    it.copy(
                        message = container.appContext.getString(
                            R.string.set_msg_backup_error,
                            error.message.orEmpty(),
                        ),
                    )
                }
                ""
            }

    suspend fun restoreJson(json: String) {
        val result = container.backupManager.importJson(json)
        val message = when (result) {
            is BackupRestoreResult.Success ->
                container.appContext.getString(
                    R.string.set_msg_restored,
                    result.snapshot.units.size,
                    result.snapshot.groups.size,
                )

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
                        it.copy(
                            message = error.message
                                ?: container.appContext.getString(R.string.set_msg_unit_error),
                        )
                    }
                }
        }
    }
}
