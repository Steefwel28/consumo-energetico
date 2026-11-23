package com.steefwel28.consumoenergetico.domain.calculator

import com.steefwel28.consumoenergetico.core.signature.CraftedBy

/** Distribuidora eléctrica de Panamá con pliego residencial precargado. */
enum class PanamaUtility(val displayName: String) {
    ENSA("ENSA"),
    NATURGY("Naturgy"),
}

/** Vista elegida en la gráfica comparativa y en la ficha exportada. */
enum class TariffComparison(val label: String) {
    ENSA("ENSA"),
    NATURGY("Naturgy"),
    VS("VS"),
}

fun PanamaUtility.tariffComparison(): TariffComparison = when (this) {
    PanamaUtility.ENSA -> TariffComparison.ENSA
    PanamaUtility.NATURGY -> TariffComparison.NATURGY
}

fun TariffComparison.utilityOrNull(): PanamaUtility? = when (this) {
    TariffComparison.ENSA -> PanamaUtility.ENSA
    TariffComparison.NATURGY -> PanamaUtility.NATURGY
    TariffComparison.VS -> null
}

/**
 * Bloque escalonado de un pliego residencial.
 *
 * [fromKwh] se incluye y [upToKwh] se excluye; `null` en el último bloque
 * significa "sin techo". El primer bloque (0–10 kWh) no cobra energía: su
 * costo vive en el cargo fijo de [TariffSchedule].
 */
@CraftedBy("steefwel28")
data class TariffTier(
    val fromKwh: Double,
    val upToKwh: Double?,
    val balboasPerKwh: Double,
)

/** Pliego mensual completo de una distribuidora. */
@CraftedBy("steefwel28")
data class TariffSchedule(
    val utility: PanamaUtility,
    val fixedChargeBalboas: Double,
    val tiers: List<TariffTier>,
)

/**
 * Cómo se valoran los kWh del hogar o local.
 *
 * - [Tiered]: pliego precargado de la distribuidora.
 * - [DirectRate] y [Receipt]: modo "Costo Manual" del usuario.
 */
@CraftedBy("steefwel28")
sealed interface TariffMode {

    data class Tiered(val utility: PanamaUtility) : TariffMode

    data class DirectRate(val balboasPerKwh: Double) : TariffMode {
        init {
            require(balboasPerKwh >= 0.0) { "La tarifa directa no puede ser negativa." }
        }
    }

    /** Tarifa deducida del recibo: `Total B/. ÷ kWh consumidos`. */
    data class Receipt(val totalBalboas: Double, val consumedKwh: Double) : TariffMode {
        init {
            require(totalBalboas >= 0.0) { "El total del recibo no puede ser negativo." }
            require(consumedKwh > 0.0) { "Los kWh del recibo deben ser mayores que cero." }
        }

        val balboasPerKwh: Double get() = totalBalboas / consumedKwh
    }
}

/** Costo del consumo base y del salto que produce el aire acondicionado. */
@CraftedBy("steefwel28")
data class TariffEstimate(
    val baseKwh: Double,
    val addedKwh: Double,
    val baseBillBalboas: Double,
    val totalBillBalboas: Double,
    val addedCostBalboas: Double,
    val marginalBalboasPerKwh: Double,
    val effectiveBalboasPerKwh: Double,
)

/**
 * Motor tarifario de Panamá.
 *
 * Calcula la factura mensual por bloques y, sobre todo, el costo marginal
 * exacto de añadir kWh de aire acondicionado: si el consumo cruza de bloque,
 * [TariffEstimate.addedCostBalboas] cobra cada kWh a su tarifa real y no a un
 * promedio.
 *
 * Los precios son valores de referencia del pliego residencial vigente.
 * shortcut: pliego fijo en código, revisar contra ASEP en cada release.
 */
@CraftedBy("steefwel28")
object PanamaTariffEngine {

    const val FIRST_BLOCK_LIMIT_KWH = 10.0

    private val ENSA_SCHEDULE = TariffSchedule(
        utility = PanamaUtility.ENSA,
        fixedChargeBalboas = 1.20,
        tiers = listOf(
            TariffTier(10.0, 300.0, 0.1685),
            TariffTier(300.0, 750.0, 0.2145),
            TariffTier(750.0, null, 0.2390),
        ),
    )

    private val NATURGY_SCHEDULE = TariffSchedule(
        utility = PanamaUtility.NATURGY,
        fixedChargeBalboas = 1.15,
        tiers = listOf(
            TariffTier(10.0, 300.0, 0.1792),
            TariffTier(300.0, 750.0, 0.2285),
            TariffTier(750.0, null, 0.2540),
        ),
    )

    fun schedule(utility: PanamaUtility): TariffSchedule = when (utility) {
        PanamaUtility.ENSA -> ENSA_SCHEDULE
        PanamaUtility.NATURGY -> NATURGY_SCHEDULE
    }

    /** Factura mensual completa (cargo fijo + energía) para [kwh]. */
    fun bill(mode: TariffMode, kwh: Double): Double {
        require(kwh >= 0.0) { "El consumo no puede ser negativo." }
        return when (mode) {
            is TariffMode.Tiered -> schedule(mode.utility).billFor(kwh)
            is TariffMode.DirectRate -> kwh * mode.balboasPerKwh
            is TariffMode.Receipt -> kwh * mode.balboasPerKwh
        }
    }

    /** Tarifa del siguiente kWh en [kwh]; 0 dentro del bloque cubierto por el cargo fijo. */
    fun marginalRate(mode: TariffMode, kwh: Double): Double = when (mode) {
        is TariffMode.Tiered -> schedule(mode.utility).tierAt(kwh)?.balboasPerKwh ?: 0.0
        is TariffMode.DirectRate -> mode.balboasPerKwh
        is TariffMode.Receipt -> mode.balboasPerKwh
    }

    /**
     * Compara la factura base del hogar con la factura después del aire
     * acondicionado. [addedCostBalboas] es la diferencia exacta entre ambas.
     */
    fun estimate(mode: TariffMode, baseKwh: Double, addedKwh: Double): TariffEstimate {
        require(baseKwh >= 0.0) { "El consumo base no puede ser negativo." }
        require(addedKwh >= 0.0) { "El consumo añadido no puede ser negativo." }

        val baseBill = bill(mode, baseKwh)
        val totalBill = bill(mode, baseKwh + addedKwh)
        val totalKwh = baseKwh + addedKwh

        return TariffEstimate(
            baseKwh = baseKwh,
            addedKwh = addedKwh,
            baseBillBalboas = baseBill,
            totalBillBalboas = totalBill,
            addedCostBalboas = totalBill - baseBill,
            marginalBalboasPerKwh = marginalRate(mode, totalKwh),
            effectiveBalboasPerKwh = if (totalKwh > 0.0) totalBill / totalKwh else 0.0,
        )
    }
}

private fun TariffSchedule.billFor(kwh: Double): Double {
    val energy = tiers.sumOf { tier ->
        val upper = tier.upToKwh?.let { minOf(kwh, it) } ?: kwh
        (upper - tier.fromKwh).coerceAtLeast(0.0) * tier.balboasPerKwh
    }
    return fixedChargeBalboas + energy
}

/** Bloque que contiene el consumo [kwh], es decir, el que paga el próximo kWh. */
private fun TariffSchedule.tierAt(kwh: Double): TariffTier? = tiers.firstOrNull { tier ->
    kwh >= tier.fromKwh && (tier.upToKwh == null || kwh < tier.upToKwh)
}
