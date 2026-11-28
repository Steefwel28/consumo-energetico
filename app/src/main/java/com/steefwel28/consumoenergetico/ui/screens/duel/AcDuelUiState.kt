package com.steefwel28.consumoenergetico.ui.screens.duel

import com.steefwel28.consumoenergetico.domain.calculator.AcConsumptionCalculator
import com.steefwel28.consumoenergetico.domain.calculator.AcSchedule
import com.steefwel28.consumoenergetico.domain.calculator.AcUnitSpec
import com.steefwel28.consumoenergetico.domain.calculator.AcUsage
import com.steefwel28.consumoenergetico.domain.calculator.MAX_BTU_PER_HOUR
import com.steefwel28.consumoenergetico.domain.calculator.PanamaTariffEngine
import com.steefwel28.consumoenergetico.domain.calculator.PanamaUtility
import com.steefwel28.consumoenergetico.domain.calculator.PowerEstimator
import com.steefwel28.consumoenergetico.domain.calculator.TariffMode
import com.steefwel28.consumoenergetico.domain.units.LocaleNumber
import kotlin.math.abs

/** Ganador del duelo según el gasto mensual en balboas. */
enum class DuelWinner { A, B, TIE }

/**
 * Estado del Duelo AC vs. AC: dos equipos que comparten horas de uso diario,
 * consumo base del hogar y distribuidora (ENSA / EDEMET / EDECHI, pliego ASEP
 * vigente). Lógica pura y probable sin Android.
 */
data class AcDuelUiState(
    val aliasAInput: String = "",
    val btuAInput: String = "12000",
    val seerAInput: String = "21",
    val wattsAInput: String = "",
    val inverterA: Boolean = true,
    val aliasBInput: String = "",
    val btuBInput: String = "12000",
    val seerBInput: String = "16",
    val wattsBInput: String = "",
    val inverterB: Boolean = false,
    val hoursPerDay: Int = 8,
    val baseKwhInput: String = "350",
    val utility: PanamaUtility = PanamaUtility.ENSA,
    val priceDiffInput: String = "",
) {

    val canCompare: Boolean get() = kwhA != null && kwhB != null

    /** kWh/mes de cada equipo con setpoint neutro (24 °C) y ciclo mixto. */
    val kwhA: Double? get() = monthlyKwh(btuAInput, seerAInput, wattsAInput, inverterA)

    val kwhB: Double? get() = monthlyKwh(btuBInput, seerBInput, wattsBInput, inverterB)

    /** Gasto mensual añadido en B/. sobre el consumo base, escalón ASEP real. */
    val monthlyCostA: Double? get() = kwhA?.let(::addedCost)

    val monthlyCostB: Double? get() = kwhB?.let(::addedCost)

    val winner: DuelWinner?
        get() {
            val costA = monthlyCostA ?: return null
            val costB = monthlyCostB ?: return null
            return when {
                abs(costA - costB) < 0.005 -> DuelWinner.TIE
                costA < costB -> DuelWinner.A
                else -> DuelWinner.B
            }
        }

    /** % de ahorro directo del ganador frente al perdedor (sobre el gasto). */
    val savingsPercent: Double?
        get() {
            val costA = monthlyCostA ?: return null
            val costB = monthlyCostB ?: return null
            val expensive = maxOf(costA, costB)
            if (expensive <= 0.0) return null
            return (expensive - minOf(costA, costB)) / expensive * 100.0
        }

    val monthlySavingsBalboas: Double?
        get() {
            val costA = monthlyCostA ?: return null
            val costB = monthlyCostB ?: return null
            val savings = abs(costA - costB)
            return savings.takeIf { it >= 0.005 }
        }

    val annualSavingsBalboas: Double? get() = monthlySavingsBalboas?.times(12.0)

    /** Diferencia de precio en tienda (B/.), opcional. */
    val priceDiff: Double? get() = priceDiffInput.toDecimal(0.0..1_000_000.0)

    /** Meses para recuperar la diferencia de precio con el ahorro mensual. */
    val paybackMonths: Double?
        get() {
            val diff = priceDiff?.takeIf { it > 0.0 } ?: return null
            val savings = monthlySavingsBalboas?.takeIf { it > 0.0 } ?: return null
            return diff / savings
        }

    val tariffValidFrom: String get() = PanamaTariffEngine.VALID_FROM

    val tariffValidTo: String get() = PanamaTariffEngine.VALID_TO

    private val baseMonthlyKwh: Double get() = baseKwhInput.toDecimal(0.0..100_000.0) ?: 0.0

    private fun addedCost(monthlyKwh: Double): Double? = runCatching {
        PanamaTariffEngine
            .estimate(TariffMode.Tiered(utility), baseMonthlyKwh, monthlyKwh)
            .addedCostBalboas
    }.getOrNull()

    private fun monthlyKwh(
        btuInput: String,
        seerInput: String,
        wattsInput: String,
        inverter: Boolean,
    ): Double? {
        val btu = btuInput.toDecimal(1.0..MAX_BTU_PER_HOUR) ?: return null
        val seer = seerInput.toDecimal(1.0..40.0)
        val watts = wattsInput.toDecimal(1.0..20_000.0)
            ?: seer?.let {
                PowerEstimator.fromBtu(btu, it, inverter).takeIf { w -> w in 1.0..20_000.0 }
            }
            ?: return null
        val spec = runCatching {
            AcUnitSpec(btuPerHour = btu, nominalWatts = watts, seer = seer, inverter = inverter)
        }.getOrNull() ?: return null
        val usage = AcUsage(hoursPerDay = hoursPerDay.toDouble(), schedule = AcSchedule.MIXED)
        return runCatching { AcConsumptionCalculator.monthlyKwh(spec, usage) }.getOrNull()
    }
}

private fun String.toDecimal(range: ClosedRange<Double>): Double? =
    LocaleNumber.parse(this)?.takeIf { it in range }
