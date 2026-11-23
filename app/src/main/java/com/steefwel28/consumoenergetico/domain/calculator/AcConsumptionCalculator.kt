package com.steefwel28.consumoenergetico.domain.calculator

import com.steefwel28.consumoenergetico.core.signature.CraftedBy

/**
 * Datos de la etiqueta amarilla RTCA 23.01.78:20.
 *
 * [seer] es el REEE/SEER impreso (`BTU/hW`); si la etiqueta no lo trae se
 * deduce de `BTU/h ÷ W`, que es exactamente el índice de eficiencia.
 */
/** Techo comercial real que acepta la app: equipos de hasta 5 toneladas. */
const val MAX_BTU_PER_HOUR = 60_000.0

@CraftedBy("steefwel28")
data class AcUnitSpec(
    val btuPerHour: Double,
    val nominalWatts: Double,
    val seer: Double? = null,
    val inverter: Boolean = false,
) {
    init {
        require(btuPerHour in 1.0..MAX_BTU_PER_HOUR) {
            "La capacidad debe estar entre 1 y $MAX_BTU_PER_HOUR BTU/h."
        }
        require(nominalWatts > 0.0) { "La potencia nominal debe ser mayor que cero W." }
        require(seer == null || seer > 0.0) { "El SEER debe ser mayor que cero." }
    }

    val seerBtuPerWatt: Double get() = seer ?: (btuPerHour / nominalWatts)
}

/**
 * Conversor de placa cuando el técnico solo tiene `Ton`, `kW` térmicos o
 * amperios. Las tres salidas están en BTU/h, la unidad de la etiqueta RTCA.
 */
@CraftedBy("steefwel28")
object AcPlateConverter {

    const val BTUS_PER_TON = 12_000.0
    const val BTUS_PER_THERMAL_KW = 3_412.142

    fun tonsToBtuPerHour(tons: Double): Double {
        require(tons > 0.0) { "Las toneladas deben ser mayores que cero." }
        return tons * BTUS_PER_TON
    }

    fun thermalKilowattsToBtuPerHour(kilowatts: Double): Double {
        require(kilowatts > 0.0) { "Los kW térmicos deben ser mayores que cero." }
        return kilowatts * BTUS_PER_THERMAL_KW
    }

    /**
     * Estima la capacidad térmica desde la corriente de placa.
     *
     * shortcut: asume factor de potencia y eficiencia 1.0, ajustar cuando se
     * lean las tres corrientes del compresor.
     */
    fun amperesToBtuPerHour(amperes: Double, volts: Double, powerFactor: Double = 1.0): Double {
        require(amperes > 0.0) { "Los amperios deben ser mayores que cero." }
        require(volts > 0.0) { "El voltaje debe ser mayor que cero." }
        require(powerFactor in 0.0..1.0) { "El factor de potencia debe estar entre 0 y 1." }
        return amperes * volts * powerFactor * BTUS_PER_THERMAL_KW / 1000.0
    }
}

/** Franja de uso; el sol y la madrugada cambian la carga térmica real. */
enum class AcSchedule(val label: String, val multiplier: Double) {
    NIGHT("Noche/Madrugada", 0.92),
    SUNNY_DAY("Día con sol", 1.10),
    MIXED("Mixto 24/7", 1.00),
}

/**
 * Hábitos de uso ingresados en el Simulador de Control Remoto.
 *
 * El simulador ofrece [SIMULATOR_MIN_CELSIUS] a [SIMULATOR_MAX_CELSIUS];
 * el rango del motor es más amplio para tolerar entradas manuales.
 */
@CraftedBy("steefwel28")
data class AcUsage(
    val hoursPerDay: Double,
    val setpointCelsius: Int = AcConsumptionCalculator.NEUTRAL_SETPOINT_CELSIUS,
    val schedule: AcSchedule = AcSchedule.NIGHT,
    val dirtyFilters: Boolean = false,
) {
    init {
        require(hoursPerDay in 0.0..24.0) { "Las horas por día deben estar entre 0 y 24." }
        require(setpointCelsius in MIN_SETPOINT_CELSIUS..MAX_SETPOINT_CELSIUS) {
            "El termostato debe estar entre $MIN_SETPOINT_CELSIUS °C y $MAX_SETPOINT_CELSIUS °C."
        }
    }

    companion object {
        const val MIN_SETPOINT_CELSIUS = 16
        const val MAX_SETPOINT_CELSIUS = 30
        const val SIMULATOR_MIN_CELSIUS = 18
        const val SIMULATOR_MAX_CELSIUS = 26
    }
}

/** Comparador Cara a Cara: equipo actual contra equipo nuevo. */
@CraftedBy("steefwel28")
data class AcComparison(
    val currentMonthlyKwh: Double,
    val newMonthlyKwh: Double,
    val monthlySavingsBalboas: Double,
    val annualSavingsBalboas: Double,
    /** `null` cuando el equipo nuevo no ahorra nada. */
    val paybackMonths: Double?,
)

/**
 * Consumo mensual de un aire acondicionado y comparador de equipos.
 *
 * Inverter: modula la entrega de frío y se valoriza con el SEER de la
 * etiqueta. No Inverter: cicla On/Off sobre la potencia nominal `W`.
 */
@CraftedBy("steefwel28")
object AcConsumptionCalculator {

    const val NEUTRAL_SETPOINT_CELSIUS = 24
    const val PENALTY_PER_DEGREE_BELOW_NEUTRAL = 0.06
    const val DIRTY_FILTER_PENALTY = 0.12

    // El recibo panameño es mensual; 30 días sigue siendo el respaldo neutro.
    const val DAYS_PER_MONTH = 30.0
    const val DEFAULT_HOURS_PER_DAY = 8.0

    // Carga promedio de un split bien dimensionado en clima tropical.
    private const val INVERTER_PART_LOAD = 0.55
    private const val NON_INVERTER_DUTY_CYCLE = 0.75

    /** Energía de un día completo con los hábitos indicados. */
    fun dailyKwh(unit: AcUnitSpec, usage: AcUsage): Double {
        val load = loadFactor(usage)
        val filter = if (usage.dirtyFilters) 1.0 + DIRTY_FILTER_PENALTY else 1.0

        val daily = if (unit.inverter) {
            val deliveredBtuPerHour = unit.btuPerHour * INVERTER_PART_LOAD * load
            deliveredBtuPerHour * usage.hoursPerDay / unit.seerBtuPerWatt / 1000.0
        } else {
            val duty = (NON_INVERTER_DUTY_CYCLE * load).coerceIn(0.0, 1.0)
            unit.nominalWatts / 1000.0 * usage.hoursPerDay * duty
        }

        return daily * filter
    }

    /** Energía mensual en kWh para el equipo y los hábitos indicados. */
    fun monthlyKwh(
        unit: AcUnitSpec,
        usage: AcUsage,
        daysInMonth: Double = DAYS_PER_MONTH,
    ): Double {
        require(daysInMonth > 0.0) { "El mes debe tener al menos un día." }
        return dailyKwh(unit, usage) * daysInMonth
    }

    /**
     * Compara el equipo actual con el nuevo y devuelve ahorro mensual, anual
     * y meses de retorno de la inversión.
     *
     * [balboasPerKwh] es el costo marginal del kWh (pliego o Costo Manual).
     */
    fun compare(
        current: AcUnitSpec,
        currentUsage: AcUsage,
        new: AcUnitSpec,
        newUsage: AcUsage = currentUsage,
        investmentBalboas: Double,
        balboasPerKwh: Double,
    ): AcComparison {
        require(investmentBalboas >= 0.0) { "La inversión no puede ser negativa." }
        require(balboasPerKwh >= 0.0) { "El costo del kWh no puede ser negativo." }

        val currentMonthlyKwh = monthlyKwh(current, currentUsage)
        val newMonthlyKwh = monthlyKwh(new, newUsage)
        val monthlySavings = (currentMonthlyKwh - newMonthlyKwh) * balboasPerKwh

        return AcComparison(
            currentMonthlyKwh = currentMonthlyKwh,
            newMonthlyKwh = newMonthlyKwh,
            monthlySavingsBalboas = monthlySavings,
            annualSavingsBalboas = monthlySavings * 12.0,
            paybackMonths = if (monthlySavings > 0.0) investmentBalboas / monthlySavings else null,
        )
    }

    /** 6 % más de carga por cada grado bajo 24 °C en el clima húmedo de Panamá. */
    private fun loadFactor(usage: AcUsage): Double {
        val degrees = NEUTRAL_SETPOINT_CELSIUS - usage.setpointCelsius
        val temperature = 1.0 + PENALTY_PER_DEGREE_BELOW_NEUTRAL * degrees
        return temperature * usage.schedule.multiplier
    }
}
