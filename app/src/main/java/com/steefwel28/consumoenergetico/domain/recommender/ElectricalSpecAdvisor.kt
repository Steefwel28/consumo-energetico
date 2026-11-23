package com.steefwel28.consumoenergetico.domain.recommender

import com.steefwel28.consumoenergetico.core.signature.CraftedBy
import com.steefwel28.consumoenergetico.domain.units.LengthUnit
import com.steefwel28.consumoenergetico.domain.units.UnitConverter

/**
 * Protector de voltaje real de taller en Panamá.
 *
 * [joules] es la energía que absorbe el varistor; [reconnectLabel] el retardo
 * de reconexión impreso por el fabricante.
 */
@CraftedBy("steefwel28")
data class SurgeProtector(
    val brandAndModel: String,
    val joules: Int,
    val reconnectLabel: String,
    val description: String,
)

/** Ficha rápida de instalación eléctrica para el equipo elegido. */
@CraftedBy("steefwel28")
data class ElectricalSpec(
    val voltage: Int,
    val estimatedAmps: Double,
    val breakerAmps: Int,
    val wireGauge: String,
    /** `null` cuando la tubería cabe en el kit estándar de 3 m (10 ft). */
    val installationAlert: String?,
    val isInverter: Boolean,
    val protector: SurgeProtector,
    /** Alternativa comercial de línea directa, cuando existe. */
    val alternativeProtector: SurgeProtector?,
    /** Nota excepcional de 110 V para equipos viejos No-Inverter. */
    val voltageNote: String?,
)

/**
 * Ficha rápida de requisitos eléctricos.
 *
 * Todos los splits modernos de Panamá se instalan en 220 V: la tabla usa ese
 * voltaje en todos los escalones y solo deja 110 V como nota para equipos
 * viejos No-Inverter. La corriente es de operación, sin factor de arranque: el
 * breaker se elige por capacidad del equipo, no por la placa.
 *
 * En Inverter el protector va en línea directa (borneras) y es obligatorio:
 * los picos de la red panameña dañan la tarjeta de potencia. En No-Inverter
 * basta un protector convencional de línea directa.
 */
@CraftedBy("steefwel28")
object ElectricalSpecAdvisor {

    const val LINE_SET_STANDARD_METERS = 3.0
    const val STANDARD_VOLTAGE = 220

    private const val MAX_SUPPORTED_BTU = 60_000

    private data class Bracket(
        val maxBtu: Int,
        val breakerAmps: Int,
        val wireGauge: String,
        val typicalWatts: Double,
    )

    private val brackets = listOf(
        Bracket(9_000, 15, "#12", 850.0),
        Bracket(12_000, 15, "#12", 1_150.0),
        Bracket(18_000, 20, "#10", 1_700.0),
        Bracket(24_000, 30, "#10", 2_300.0),
        Bracket(28_000, 30, "#10", 2_700.0),
        Bracket(36_000, 30, "#8", 3_500.0),
        Bracket(48_000, 30, "#8", 4_200.0),
        Bracket(60_000, 40, "#8", 5_400.0),
    )

    private val inverterProtector = SurgeProtector(
        brandAndModel = "AVTEK SPC-PEBAS-B230",
        joules = 2_100,
        reconnectLabel = "10 s",
        description = "Protector de línea directa (borneras) para Inverter, 220 V, 30 A. " +
            "También se consigue como PBAS-B230-LED.",
    )

    private val inverterAlternative = SurgeProtector(
        brandAndModel = "Forza FVP-6630B",
        joules = 880,
        reconnectLabel = "15 s / 30 s / 3 min ajustable",
        description = "Alternativa de línea directa para Inverter, 220 V, 30 A, con retardo ajustable.",
    )

    private val conventionalProtector = SurgeProtector(
        brandAndModel = "AVTEK SPC-PABB-B230",
        joules = 525,
        reconnectLabel = "3 min",
        description = "Protector convencional de línea directa para equipos No-Inverter, 220 V.",
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
        inverter: Boolean = true,
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
            voltage = STANDARD_VOLTAGE,
            estimatedAmps = watts / STANDARD_VOLTAGE,
            breakerAmps = bracket.breakerAmps,
            wireGauge = bracket.wireGauge,
            installationAlert = alert,
            isInverter = inverter,
            protector = if (inverter) inverterProtector else conventionalProtector,
            alternativeProtector = if (inverter) inverterAlternative else null,
            voltageNote = if (inverter) null else {
                "Solo los equipos viejos No-Inverter pueden venir en 110 V: en ese caso usa un " +
                    "protector de enchufe de 110 V; el split moderno es 220 V."
            },
        )
    }
}
