package com.steefwel28.consumoenergetico.domain.recommender

import com.steefwel28.consumoenergetico.core.signature.CraftedBy
import com.steefwel28.consumoenergetico.domain.units.LengthUnit
import com.steefwel28.consumoenergetico.domain.units.UnitConverter

/** Ficha rápida de instalación eléctrica para el equipo elegido. */
@CraftedBy("steefwel28")
data class ElectricalSpec(
    val voltage: Int,
    val estimatedAmps: Double,
    val breakerAmps: Int,
    val wireGauge: String,
    /** `null` cuando la tubería cabe en el kit estándar de 3 m (10 ft). */
    val installationAlert: String?,
    val surgeProtectorJoules: Int,
    val reconnectDelayMinutes: IntRange,
)

/**
 * Ficha rápida de requisitos eléctricos.
 *
 * La tabla por capacidad sigue la práctica de instalación en Panamá:
 * 9,000–12,000 BTU/h van en 110 V con breaker de 15 A y cable #12; desde
 * 18,000 BTU/h se usa 220 V. La corriente es de operación, sin factor de
 * arranque: el breaker se elige por capacidad del equipo, no por la placa.
 *
 * El protector de voltaje se recomienda siempre y es obligatorio en Inverter:
 * los picos de la red panameña dañan la tarjeta de potencia.
 */
@CraftedBy("steefwel28")
object ElectricalSpecAdvisor {

    const val LINE_SET_STANDARD_METERS = 3.0

    private const val MAX_SUPPORTED_BTU = 36_000

    private data class Bracket(
        val maxBtu: Int,
        val voltage: Int,
        val breakerAmps: Int,
        val wireGauge: String,
        val typicalWatts: Double,
        val surgeJoules: Int,
    )

    private val brackets = listOf(
        Bracket(9_000, 110, 15, "#12", 850.0, 900),
        Bracket(12_000, 110, 15, "#12", 1_150.0, 900),
        Bracket(18_000, 220, 20, "#10", 1_700.0, 1_200),
        Bracket(24_000, 220, 30, "#10", 2_300.0, 1_400),
        Bracket(36_000, 220, 30, "#8", 3_500.0, 1_400),
    )

    /**
     * [btuPerHour] es la capacidad comercial elegida; [nominalWatts] sale de
     * la etiqueta RTCA y, si falta, se usa el consumo típico del escalón.
     * [pipeDistanceMeters] se recibe ya convertida a metros.
     */
    fun advise(
        btuPerHour: Int,
        pipeDistanceMeters: Double = 0.0,
        nominalWatts: Double? = null,
    ): ElectricalSpec {
        require(btuPerHour in 1..MAX_SUPPORTED_BTU) {
            "La ficha cubre de 1 a $MAX_SUPPORTED_BTU BTU/h; sobre eso se requiere evaluación técnica."
        }
        require(pipeDistanceMeters >= 0.0) { "La tubería no puede ser negativa." }
        require(nominalWatts == null || nominalWatts > 0.0) {
            "La potencia nominal debe ser mayor que cero W."
        }

        val bracket = brackets.first { btuPerHour <= it.maxBtu }
        val watts = nominalWatts ?: bracket.typicalWatts
        val alert = if (pipeDistanceMeters > LINE_SET_STANDARD_METERS) {
            "La tubería de ${UnitConverter.formatLength(pipeDistanceMeters, LengthUnit.METERS)} " +
                "supera el kit estándar de 3 m (10 ft): requerirá tubería de cobre adicional " +
                "y posible ajuste de gas refrigerante."
        } else {
            null
        }

        return ElectricalSpec(
            voltage = bracket.voltage,
            estimatedAmps = watts / bracket.voltage,
            breakerAmps = bracket.breakerAmps,
            wireGauge = bracket.wireGauge,
            installationAlert = alert,
            surgeProtectorJoules = bracket.surgeJoules,
            reconnectDelayMinutes = 3..5,
        )
    }
}
