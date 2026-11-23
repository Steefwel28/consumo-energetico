package com.steefwel28.consumoenergetico.ui.screens.calculate

import com.steefwel28.consumoenergetico.data.local.db.AcUnitEntity
import com.steefwel28.consumoenergetico.domain.calculator.AcComparison
import com.steefwel28.consumoenergetico.domain.calculator.AcConsumptionCalculator
import com.steefwel28.consumoenergetico.domain.calculator.AcPlateConverter
import com.steefwel28.consumoenergetico.domain.calculator.AcSchedule
import com.steefwel28.consumoenergetico.domain.calculator.AcUnitSpec
import com.steefwel28.consumoenergetico.domain.calculator.AcUsage
import com.steefwel28.consumoenergetico.domain.calculator.PanamaTariffEngine
import com.steefwel28.consumoenergetico.domain.calculator.PanamaUtility
import com.steefwel28.consumoenergetico.domain.calculator.TariffEstimate
import com.steefwel28.consumoenergetico.domain.calculator.TariffMode
import com.steefwel28.consumoenergetico.domain.recommender.ElectricalSpec
import com.steefwel28.consumoenergetico.domain.recommender.ElectricalSpecAdvisor
import com.steefwel28.consumoenergetico.domain.units.LocaleNumber
import com.steefwel28.consumoenergetico.domain.units.UnitPreferences
import java.util.Locale
import kotlin.math.roundToInt

/** Cómo se ingresa el costo cuando el usuario activa "Costo Manual". */
enum class ManualCostMode(val label: String) {
    DIRECT_RATE("Tarifa directa"),
    RECEIPT("Desde el recibo"),
}

/** Atajo del conversor de placa. */
enum class PlateConverterMode(val label: String, val unit: String) {
    TONS("Toneladas", "Ton"),
    KILOWATTS("kW térmicos", "kW"),
    AMPERES("Amperios", "A"),
}

/**
 * Estado único de la pantalla Calcular.
 *
 * Los valores derivados son propiedades calculadas: la UI cambia una entrada,
 * el [CalculateViewModel] emite un estado nuevo y kWh/B/. se recalculan solos,
 * sin campos que puedan quedar desincronizados.
 */
data class CalculateUiState(
    val spaceName: String = "Mi espacio",
    val brandModel: String = "",
    val btuInput: String = "12000",
    val wattsInput: String = "1150",
    val seerInput: String = "",
    val inverter: Boolean = true,
    val utility: PanamaUtility = PanamaUtility.ENSA,
    val manualCost: Boolean = false,
    val manualMode: ManualCostMode = ManualCostMode.DIRECT_RATE,
    val directRateInput: String = "0.18",
    val receiptTotalInput: String = "",
    val receiptKwhInput: String = "",
    val baseMonthlyKwh: Double = 0.0,
    val unitPreferences: UnitPreferences = UnitPreferences(),
    val setpointCelsius: Int = 24,
    val cleanFilters: Boolean = true,
    val schedule: AcSchedule = AcSchedule.NIGHT,
    val hoursPerDay: Int = 8,
    val converterMode: PlateConverterMode = PlateConverterMode.TONS,
    val converterInput: String = "1",
    val converterVoltage: Int = 220,
    val currentBtuInput: String = "12000",
    val currentWattsInput: String = "1600",
    val currentSeerInput: String = "",
    val currentInverter: Boolean = false,
    val investmentInput: String = "450",
    val isSaving: Boolean = false,
    val savedMessage: String? = null,
    val saveError: String? = null,
) {

    val btu: Double? get() = btuInput.toDecimal(1.0..60_000.0)
    val nominalWatts: Double? get() = wattsInput.toDecimal(1.0..20_000.0)
    val seer: Double? get() = seerInput.toDecimal(1.0..40.0)
    val directRate: Double? get() = directRateInput.toDecimal(0.0..10.0)?.takeIf { it > 0.0 }
    val receiptTotal: Double? get() = receiptTotalInput.toDecimal(0.0..100_000.0)
    val receiptKwh: Double? get() = receiptKwhInput.toDecimal(0.0..100_000.0)?.takeIf { it > 0.0 }
    val investment: Double? get() = investmentInput.toDecimal(0.0..1_000_000.0)

    val usage: AcUsage
        get() = AcUsage(
            hoursPerDay = hoursPerDay.coerceIn(1, 24).toDouble(),
            setpointCelsius = setpointCelsius.coerceIn(
                AcUsage.SIMULATOR_MIN_CELSIUS,
                AcUsage.SIMULATOR_MAX_CELSIUS,
            ),
            schedule = schedule,
            dirtyFilters = !cleanFilters,
        )

    val spec: AcUnitSpec?
        get() {
            val btu = btu ?: return null
            val watts = nominalWatts ?: return null
            return runCatching { AcUnitSpec(btu, watts, seer, inverter) }.getOrNull()
        }

    val monthlyKwh: Double?
        get() = spec?.let { AcConsumptionCalculator.monthlyKwh(it, usage) }

    val tariffMode: TariffMode?
        get() = when {
            !manualCost -> TariffMode.Tiered(utility)
            manualMode == ManualCostMode.DIRECT_RATE -> directRate?.let(TariffMode::DirectRate)
            else -> {
                val total = receiptTotal
                val kwh = receiptKwh
                if (total != null && kwh != null) TariffMode.Receipt(total, kwh) else null
            }
        }

    val tariffEstimate: TariffEstimate?
        get() {
            val mode = tariffMode ?: return null
            val added = monthlyKwh ?: return null
            return runCatching {
                PanamaTariffEngine.estimate(mode, baseMonthlyKwh.coerceAtLeast(0.0), added)
            }.getOrNull()
        }

    val monthlyCost: Double? get() = tariffEstimate?.addedCostBalboas

    val electrical: ElectricalSpec?
        get() {
            val btu = btu ?: return null
            return runCatching {
                ElectricalSpecAdvisor.advise(
                    btuPerHour = btu.roundToInt(),
                    nominalWatts = nominalWatts,
                )
            }.getOrNull()
        }

    val currentSpec: AcUnitSpec?
        get() {
            val btu = currentBtuInput.toDecimal(1.0..60_000.0) ?: return null
            val watts = currentWattsInput.toDecimal(1.0..20_000.0) ?: return null
            return runCatching {
                AcUnitSpec(btu, watts, currentSeerInput.toDecimal(1.0..40.0), currentInverter)
            }.getOrNull()
        }

    val comparison: AcComparison?
        get() {
            val newSpec = spec ?: return null
            val oldSpec = currentSpec ?: return null
            val rate = tariffEstimate?.marginalBalboasPerKwh ?: return null
            val investment = investment ?: return null
            if (rate <= 0.0) return null
            return runCatching {
                AcConsumptionCalculator.compare(
                    current = oldSpec,
                    currentUsage = usage,
                    new = newSpec,
                    newUsage = usage,
                    investmentBalboas = investment,
                    balboasPerKwh = rate,
                )
            }.getOrNull()
        }

    val converterBtu: Double?
        get() {
            val value = converterInput.toDecimal(0.0001..1_000.0) ?: return null
            return runCatching {
                when (converterMode) {
                    PlateConverterMode.TONS -> AcPlateConverter.tonsToBtuPerHour(value)
                    PlateConverterMode.KILOWATTS -> AcPlateConverter.thermalKilowattsToBtuPerHour(value)
                    PlateConverterMode.AMPERES -> AcPlateConverter.amperesToBtuPerHour(
                        amperes = value,
                        volts = converterVoltage.toDouble(),
                    )
                }
            }.getOrNull()
        }

    val canSave: Boolean get() = spec != null && monthlyKwh != null && electrical != null

    val tariffLabel: String
        get() = when {
            !manualCost -> utility.displayName
            manualMode == ManualCostMode.DIRECT_RATE -> "Tarifa directa"
            else -> "Tarifa del recibo"
        }

    /** Texto breve para la ficha PDF/PNG. */
    fun reportNarrative(): String {
        val spec = spec ?: return ""
        val kwh = monthlyKwh ?: return ""
        val cost = monthlyCost ?: return ""
        val filters = if (cleanFilters) {
            "Mantén los filtros limpios y el termostato en 24 °C: cada grado extra cerca de 18 °C suma casi 6 % al consumo."
        } else {
            "Los filtros sucios fuerzan el compresor y elevan el consumo cerca de 12 %; límpialos cada mes."
        }
        return buildString {
            append("${spaceName.ifBlank { "Tu espacio" }} usa un equipo de ")
            append("${btuText(spec.btuPerHour)} BTU/h ${if (spec.inverter) "Inverter" else "No Inverter"}. ")
            append("Con ${hoursPerDay.coerceIn(1, 24)} h/día y termostato en $setpointCelsius °C ")
            append("consume cerca de ${decimal(kwh)} kWh al mes, unos B/. ${decimal(cost)} en la factura. ")
            append(filters)
        }
    }

    /**
     * Convierte el cálculo en el equipo que se guardará en Mi Casa. El último
     * mantenimiento arranca hoy para que el aviso de 90 días sea exacto.
     */
    fun toEntity(nowMillis: Long): AcUnitEntity? {
        val spec = spec ?: return null
        val kwh = monthlyKwh ?: return null
        val electrical = electrical ?: return null
        return AcUnitEntity(
            nombreEspacio = spaceName.trim().ifBlank { "Mi espacio" },
            marcaModelo = brandModel.trim().ifBlank { "Sin marca/modelo" },
            btu = spec.btuPerHour.roundToInt(),
            seer = spec.seer,
            esInverter = spec.inverter,
            voltaje = electrical.voltage,
            fechaInstalacionMillis = nowMillis,
            fechaUltimoMantenimientoMillis = nowMillis,
            consumoEstimadoKwhMes = kwh,
        )
    }
}

private fun String.toDecimal(range: ClosedFloatingPointRange<Double>): Double? =
    LocaleNumber.parse(this)?.takeIf { it in range }

private fun btuText(value: Double): String = String.format(Locale.US, "%,.0f", value)

private fun decimal(value: Double): String = String.format(Locale.US, "%.1f", value)
