package com.steefwel28.consumoenergetico.ui.screens.settings

import com.steefwel28.consumoenergetico.domain.calculator.PanamaUtility
import com.steefwel28.consumoenergetico.domain.model.GitHubRelease
import com.steefwel28.consumoenergetico.domain.units.UnitPreferences

/**
 * Estado de Ajustes: unidades globales, costo base del recibo, respaldo y
 * verificador de GitHub.
 *
 * Los toques sobre la tarjeta de versión alimentan el easter egg de la firma:
 * al quinto toque se revela `steefwel28`.
 */
data class SettingsUiState(
    val unitPreferences: UnitPreferences = UnitPreferences(),
    val utility: PanamaUtility = PanamaUtility.ENSA,
    val baseMonthlyKwh: Double = 0.0,
    val hasPendingUpdate: Boolean = false,
    val pendingRelease: GitHubRelease? = null,
    val isChecking: Boolean = false,
    val signatureTaps: Int = 0,
    val message: String? = null,
) {

    val signatureUnlocked: Boolean get() = signatureTaps >= SIGNATURE_TAPS

    companion object {
        const val SIGNATURE_TAPS = 5
    }
}
