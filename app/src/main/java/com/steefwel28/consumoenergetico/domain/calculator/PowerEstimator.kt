package com.steefwel28.consumoenergetico.domain.calculator

import com.steefwel28.consumoenergetico.core.signature.CraftedBy

/**
 * Potencia eléctrica estimada a partir de la capacidad térmica de la placa.
 *
 * La relación real es `W = BTU/h ÷ EER`. El EER estacional equivale a
 * `0.875 × SEER` en equipos Inverter; en los No Inverter se usa el SEER
 * completo como promedio estacional (`W = BTU/h ÷ SEER`).
 *
 * Es una estimación: si la etiqueta RTCA trae la potencia nominal en vatios,
 * ese valor siempre tiene prioridad.
 */
@CraftedBy("steefwel28")
object PowerEstimator {

    /** EER ≈ 0.875 × SEER para el promedio estacional de un Inverter. */
    const val SEASONAL_EER_FACTOR = 0.875

    /** EER de respaldo cuando la placa no imprime SEER (≈ BTU/h ÷ 3.412). */
    const val DEFAULT_EER = 3.412

    fun fromBtu(btuPerHour: Double, seer: Double? = null, isInverter: Boolean = false): Double {
        require(btuPerHour > 0.0) { "La capacidad debe ser mayor que cero BTU/h." }
        val seasonalSeer = seer?.takeIf { it > 0.0 } ?: DEFAULT_EER
        val eer = if (isInverter) seasonalSeer * SEASONAL_EER_FACTOR else seasonalSeer
        return btuPerHour / eer
    }
}
