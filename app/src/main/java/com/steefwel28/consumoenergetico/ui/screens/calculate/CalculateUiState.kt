package com.steefwel28.consumoenergetico.ui.screens.calculate

import com.steefwel28.consumoenergetico.data.local.db.AcUnitEntity
import com.steefwel28.consumoenergetico.data.local.db.CustomLocationEntity
import com.steefwel28.consumoenergetico.data.local.db.PropertyGroupEntity
import com.steefwel28.consumoenergetico.domain.calculator.AcComparison
import com.steefwel28.consumoenergetico.domain.calculator.AcConsumptionCalculator
import com.steefwel28.consumoenergetico.domain.calculator.AcPlateConverter
import com.steefwel28.consumoenergetico.domain.calculator.AcSchedule
import com.steefwel28.consumoenergetico.domain.calculator.AcUnitSpec
import com.steefwel28.consumoenergetico.domain.calculator.AcUsage
import com.steefwel28.consumoenergetico.domain.calculator.PanamaTariffEngine
import com.steefwel28.consumoenergetico.domain.calculator.PanamaUtility
import com.steefwel28.consumoenergetico.domain.calculator.TariffComparison
import com.steefwel28.consumoenergetico.domain.calculator.TariffEstimate
import com.steefwel28.consumoenergetico.domain.calculator.TariffMode
import com.steefwel28.consumoenergetico.domain.recommender.ElectricalSpec
import com.steefwel28.consumoenergetico.domain.recommender.ElectricalSpecAdvisor
import com.steefwel28.consumoenergetico.domain.units.LocaleNumber
import com.steefwel28.consumoenergetico.domain.units.UnitPreferences
import java.time.YearMonth
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Categorías superiores; solo Aires Acondicionados está habilitado en Fase 1. */
enum class EquipmentCategory(val label: String) {
    AIR_CONDITIONER("Aire Acondicionado"),
    REFRIGERATOR("Refrigeradora"),
    FREEZER("Freezer"),
    APPLIANCES("Electrodomésticos"),
}

/** Cómo se ingresa el costo cuando el usuario activa "Costo Manual". */
enum class ManualCostMode(val label: String) {
    DIRECT_RATE("Tarifa directa"),
    RECEIPT("Desde el recibo"),
}

/** Periodo visible en la gráfica; el escalón siempre se decide sobre el mes real. */
enum class UsagePeriod {
    DAY,
    FORTNIGHT,
    MONTH,
}

/** Atajo del conversor de placa. */
enum class PlateConverterMode(val label: String, val unit: String) {
    TONS("Toneladas", "Ton"),
    KILOWATTS("kW térmicos", "kW"),
    AMPERES("Amperios", "A"),
}

/** Ubicaciones base; "Otros" se guarda en Room para reutilizarse. */
object CalculateLocations {
    val base = listOf("Habitación", "Sala", "Cocina", "Comedor")
    const val OTHER = "Otros"
    const val DEFAULT = "Sala"
}

/** Datos que el diálogo jerárquico entrega al guardar el equipo. */
data class SaveRequest(
    val groupId: Long?,
    val newGroupName: String,
    val groupUtility: PanamaUtility,
    val groupBaseKwh: Double,
    val location: String,
    val brandModel: String,
)

/**
 * Estado único de la pantalla Calcular.
 *
 * Los valores derivados son propiedades calculadas: la UI cambia una entrada,
 * el [CalculateViewModel] emite un estado nuevo y kWh/B/. se recalculan solos,
 * sin campos que puedan quedar desincronizados.
 */
data class CalculateUiState(
    val category: EquipmentCategory = EquipmentCategory.AIR_CONDITIONER,
    val spaceName: String = "Mi espacio",
    val brandModel: String = "",
    val btuInput: String = "",
    val wattsInput: String = "",
    val seerInput: String = "",
    val inverter: Boolean = true,
    val utility: PanamaUtility = PanamaUtility.ENSA,
    val chartMode: TariffComparison = TariffComparison.ENSA,
    val manualCost: Boolean = false,
    val manualMode: ManualCostMode = ManualCostMode.DIRECT_RATE,
    val directRateInput: String = "0.18",
    val receiptTotalInput: String = "",
    val receiptKwhInput: String = "",
    val baseMonthlyKwh: Double = 0.0,
    val unitPreferences: UnitPreferences = UnitPreferences(),
    val usagePeriod: UsagePeriod = UsagePeriod.MONTH,
    val setpointCelsius: Int = 24,
    val cleanFilters: Boolean = true,
    val schedule: AcSchedule = AcSchedule.NIGHT,
    val hoursPerDay: Int = AcConsumptionCalculator.DEFAULT_HOURS_PER_DAY.toInt(),
    val simulatedSetpointCelsius: Int = 24,
    val simulatedCleanFilters: Boolean = true,
    val simulatedSchedule: AcSchedule = AcSchedule.NIGHT,
    val simulatedHoursPerDay: Int = 8,
    val converterMode: PlateConverterMode = PlateConverterMode.TONS,
    val converterInput: String = "1",
    val converterVoltage: Int = 220,
    val currentBtuInput: String = "12000",
    val currentWattsInput: String = "1600",
    val currentSeerInput: String = "",
    val currentInverter: Boolean = false,
    val investmentInput: String = "450",
    val groups: List<PropertyGroupEntity> = emptyList(),
    val customLocations: List<CustomLocationEntity> = emptyList(),
    val selectedGroupId: Long? = null,
    val selectedLocation: String = CalculateLocations.DEFAULT,
    val isSaving: Boolean = false,
    val savedMessage: String? = null,
    val saveError: String? = null,
) {

    val btu: Double? get() = btuInput.toDecimal(1.0..60_000.0)
    val seer: Double? get() = seerInput.toDecimal(1.0..40.0)

    /** Sin potencia en placa se estima con BTU/h y SEER: W = BTU / (SEER × 0.293). */
    val wattsEstimatedBySeer: Boolean
        get() = wattsInput.isBlank() && btu != null && seer != null

    val nominalWatts: Double?
        get() = wattsInput.toDecimal(1.0..20_000.0)
            ?: if (wattsEstimatedBySeer) {
                (btu!! / (seer!! * 0.293)).takeIf { it in 1.0..20_000.0 }
            } else {
                null
            }
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

    /** Días reales del mes en curso (28, 29 en bisiestos, 30 o 31). */
    val daysInMonth: Int
        get() = YearMonth.now().lengthOfMonth()

    val periodDays: Int
        get() = when (usagePeriod) {
            UsagePeriod.DAY -> 1
            UsagePeriod.FORTNIGHT -> 15
            UsagePeriod.MONTH -> daysInMonth
        }

    val periodFactor: Double
        get() = periodDays.toDouble() / daysInMonth.toDouble()

    val dailyKwh: Double?
        get() = spec?.let { AcConsumptionCalculator.dailyKwh(it, usage) }

    /** Proyección mensual real; decide el salto de bloque tarifario. */
    val monthlyKwh: Double?
        get() = dailyKwh?.times(daysInMonth)

    /** Consumo que se pinta y se exporta para el periodo elegido. */
    val periodKwh: Double?
        get() = monthlyKwh?.times(periodFactor)

    /** Uso del simulador; vive aparte para no mover la gráfica principal. */
    val simulatedUsage: AcUsage
        get() = AcUsage(
            hoursPerDay = simulatedHoursPerDay.coerceIn(1, 24).toDouble(),
            setpointCelsius = simulatedSetpointCelsius.coerceIn(
                AcUsage.SIMULATOR_MIN_CELSIUS,
                AcUsage.SIMULATOR_MAX_CELSIUS,
            ),
            schedule = simulatedSchedule,
            dirtyFilters = !simulatedCleanFilters,
        )

    val simulatedMonthlyKwh: Double?
        get() = spec?.let { AcConsumptionCalculator.monthlyKwh(it, simulatedUsage, daysInMonth.toDouble()) }

    val simulatedTariffEstimate: TariffEstimate?
        get() {
            val mode = tariffMode ?: return null
            return estimateFor(mode, simulatedMonthlyKwh)
        }

    val simulatedMonthlyCost: Double? get() = simulatedTariffEstimate?.addedCostBalboas

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

    /** Elige el bloque con el mes real y luego reparte kWh/B/. al periodo. */
    private fun estimateFor(mode: TariffMode, monthlyAdded: Double? = monthlyKwh): TariffEstimate? {
        val added = monthlyAdded ?: return null
        val monthly = runCatching {
            PanamaTariffEngine.estimate(mode, baseMonthlyKwh.coerceAtLeast(0.0), added)
        }.getOrNull() ?: return null
        return monthly.scaledToPeriod(periodFactor)
    }

    val tariffEstimate: TariffEstimate?
        get() = tariffMode?.let(::estimateFor)

    /** Pliego ENSA, independiente del modo manual, para el duelo VS. */
    val ensaEstimate: TariffEstimate?
        get() = estimateFor(TariffMode.Tiered(PanamaUtility.ENSA))

    /** Pliego Naturgy, independiente del modo manual, para el duelo VS. */
    val naturgyEstimate: TariffEstimate?
        get() = estimateFor(TariffMode.Tiered(PanamaUtility.NATURGY))

    /** Estimación de la vista activa; en VS se dibujan las dos por separado. */
    val chartEstimate: TariffEstimate?
        get() = when (chartMode) {
            TariffComparison.ENSA -> if (manualCost) tariffEstimate else ensaEstimate
            TariffComparison.NATURGY -> if (manualCost) tariffEstimate else naturgyEstimate
            TariffComparison.VS -> null
        }

    val comparisonDifference: Double?
        get() {
            val ensa = ensaEstimate?.addedCostBalboas ?: return null
            val naturgy = naturgyEstimate?.addedCostBalboas ?: return null
            return abs(ensa - naturgy)
        }

    /** Costo de la vista activa; en VS, el menor de los dos pliegos. */
    val primaryMonthlyCost: Double?
        get() = when (chartMode) {
            TariffComparison.ENSA, TariffComparison.NATURGY -> chartEstimate?.addedCostBalboas
            TariffComparison.VS -> listOfNotNull(
                ensaEstimate?.addedCostBalboas,
                naturgyEstimate?.addedCostBalboas,
            ).minOrNull()
        }

    val monthlyCost: Double? get() = tariffEstimate?.addedCostBalboas

    val electrical: ElectricalSpec?
        get() {
            val btu = btu ?: return null
            return runCatching {
                ElectricalSpecAdvisor.advise(
                    btuPerHour = btu.roundToInt(),
                    nominalWatts = nominalWatts,
                    inverter = inverter,
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

    val reportTariffLabel: String
        get() = when (chartMode) {
            TariffComparison.ENSA -> "ENSA"
            TariffComparison.NATURGY -> "Naturgy"
            TariffComparison.VS -> "ENSA vs. Naturgy"
        }

    /** Texto breve para la ficha PDF/PNG. */
    fun reportNarrative(): String {
        val spec = spec ?: return ""
        val kwh = monthlyKwh ?: return ""
        val cost = primaryMonthlyCost ?: return ""
        val filters = if (cleanFilters) {
            "Mantén los filtros limpios y el termostato en 24 °C: cada grado extra cerca de 18 °C suma casi 6 % al consumo."
        } else {
            "Los filtros sucios fuerzan el compresor y elevan el consumo cerca de 12 %; límpialos cada mes."
        }
        return buildString {
            append("${spaceName.ifBlank { "Tu espacio" }} usa un equipo de ")
            append("${btuText(spec.btuPerHour)} BTU/h ${if (spec.inverter) "Inverter" else "No Inverter"}. ")
            append("Con ${hoursPerDay.coerceIn(1, 24)} h/día y termostato en $setpointCelsius °C ")
            append("consume cerca de ${decimal(kwh)} kWh al mes, unos B/. ${decimal(cost)} en la factura ")
            append("con $reportTariffLabel. ")
            append(filters)
        }
    }

    /**
     * Convierte el cálculo en el equipo que se guardará en el grupo elegido.
     * El último mantenimiento arranca hoy para que el aviso de 90 días sea exacto.
     */
    fun toEntity(
        nowMillis: Long,
        groupId: Long = PropertyGroupEntity.DEFAULT_ID,
        ubicacion: String = CalculateLocations.DEFAULT,
    ): AcUnitEntity? {
        val spec = spec ?: return null
        val kwh = monthlyKwh ?: return null
        val electrical = electrical ?: return null
        return AcUnitEntity(
            nombreEspacio = spaceName.trim().ifBlank { ubicacion.ifBlank { "Mi espacio" } },
            marcaModelo = brandModel.trim().ifBlank { "Sin marca/modelo" },
            btu = spec.btuPerHour.roundToInt(),
            seer = spec.seer,
            esInverter = spec.inverter,
            voltaje = electrical.voltage,
            fechaInstalacionMillis = nowMillis,
            fechaUltimoMantenimientoMillis = nowMillis,
            consumoEstimadoKwhMes = kwh,
            horasUsoDiario = hoursPerDay.coerceIn(1, 24).toDouble(),
            groupId = groupId,
            ubicacion = ubicacion.trim().ifBlank { CalculateLocations.DEFAULT },
        )
    }
}

/** Reparte el costo mensual del bloque al periodo visible sin promediar la tarifa. */
private fun TariffEstimate.scaledToPeriod(factor: Double): TariffEstimate =
    if (factor == 1.0) {
        this
    } else {
        copy(
            addedKwh = addedKwh * factor,
            totalBillBalboas = baseBillBalboas + addedCostBalboas * factor,
            addedCostBalboas = addedCostBalboas * factor,
        )
    }

private fun String.toDecimal(range: ClosedFloatingPointRange<Double>): Double? =
    LocaleNumber.parse(this)?.takeIf { it in range }

private fun btuText(value: Double): String = String.format(Locale.US, "%,.0f", value)

private fun decimal(value: Double): String = String.format(Locale.US, "%.1f", value)
