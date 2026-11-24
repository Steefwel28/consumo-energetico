package com.steefwel28.consumoenergetico.ui.screens.myhome

import com.steefwel28.consumoenergetico.data.local.db.AcUnitEntity
import com.steefwel28.consumoenergetico.data.local.db.BillRecordEntity
import com.steefwel28.consumoenergetico.data.local.db.CustomLocationEntity
import com.steefwel28.consumoenergetico.data.local.db.MaintenanceLogEntity
import com.steefwel28.consumoenergetico.data.local.db.PropertyGroupEntity
import com.steefwel28.consumoenergetico.data.worker.MaintenanceReminderPlanner
import com.steefwel28.consumoenergetico.domain.calculator.AcConsumptionCalculator
import com.steefwel28.consumoenergetico.domain.calculator.PanamaTariffEngine
import com.steefwel28.consumoenergetico.domain.calculator.PanamaUtility
import com.steefwel28.consumoenergetico.domain.calculator.TariffEstimate
import com.steefwel28.consumoenergetico.domain.calculator.TariffMode

/** Ubicaciones que siempre aparecen primero; las personalizadas van después. */
object MyHomeLocations {
    val base = listOf("Habitación", "Sala", "Cocina", "Comedor")
    const val OTHER = "Otros"
    const val DEFAULT = "Sala"
}

/** Datos del formulario de grupo/propiedad. */
data class GroupDraft(
    val id: Long? = null,
    val nombre: String,
    val utility: PanamaUtility,
    val baseKwh: Double,
)

/** Datos del formulario de factura real. */
data class BillDraft(
    val groupId: Long,
    val year: Int,
    val month: Int,
    val realKwh: Double,
    val paidBalboas: Double,
    val utility: PanamaUtility,
)

/** Datos del formulario de equipo; `id` nulo crea uno nuevo. */
data class UnitDraft(
    val id: Long? = null,
    val groupId: Long,
    val nombreEspacio: String,
    val marcaModelo: String,
    val ubicacion: String,
    val btu: Int,
    val watts: Double?,
    val seer: Double?,
    val inverter: Boolean,
    /** Horas diarias configuradas por el usuario al guardar el equipo. */
    val horasUsoDiario: Double = AcConsumptionCalculator.DEFAULT_HOURS_PER_DAY,
    /** Visible y editable; en blanco se estima con las horas configuradas. */
    val consumoKwhMes: Double?,
    val fechaInstalacionMillis: Long,
    val fechaUltimoMantenimientoMillis: Long,
)

/**
 * Estado de Mi Casa / Mi Perfil.
 *
 * El resumen de cada grupo usa el pliego escalonado real de su distribuidora:
 * el consumo de los equipos se suma al recibo base y paga el salto de bloque
 * que realmente produce.
 */
data class MyHomeUiState(
    val groups: List<PropertyGroupEntity> = emptyList(),
    val units: List<AcUnitEntity> = emptyList(),
    val locations: List<CustomLocationEntity> = emptyList(),
    val logs: List<MaintenanceLogEntity> = emptyList(),
    val bills: List<BillRecordEntity> = emptyList(),
    val selectedGroupId: Long? = null,
    val nowMillis: Long = System.currentTimeMillis(),
    val message: String? = null,
) {

    val selectedGroup: PropertyGroupEntity?
        get() = selectedGroupId?.let { id -> groups.firstOrNull { it.id == id } }
            ?: groups.firstOrNull()

    val groupUnits: List<AcUnitEntity>
        get() = selectedGroup?.let { group -> units.filter { it.groupId == group.id } }.orEmpty()

    val knownLocations: List<String>
        get() = (MyHomeLocations.base + locations.map { it.nombreUbicacion }).distinct()

    /** Equipos del grupo activo agrupados por ubicación, en orden familiar. */
    val unitsByLocation: List<Pair<String, List<AcUnitEntity>>>
        get() = groupUnits
            .groupBy { it.ubicacion }
            .entries
            .sortedWith(
                compareBy(
                    { entry -> MyHomeLocations.base.indexOf(entry.key).takeIf { it >= 0 } ?: Int.MAX_VALUE },
                    { entry -> entry.key.lowercase() },
                ),
            )
            .map { it.key to it.value }

    fun summaryFor(group: PropertyGroupEntity): TariffEstimate {
        val addedKwh = units
            .filter { it.groupId == group.id }
            .sumOf { it.consumoEstimadoKwhMes }
        return PanamaTariffEngine.estimate(
            mode = TariffMode.Tiered(group.distribuidora),
            baseKwh = group.consumoBaseKwh,
            addedKwh = addedKwh,
        )
    }

    val summary: TariffEstimate?
        get() = selectedGroup?.let(::summaryFor)

    fun daysUntilMaintenance(unit: AcUnitEntity): Long =
        MaintenanceReminderPlanner.daysUntilDue(unit, nowMillis)

    fun historyFor(unitId: Long): List<MaintenanceLogEntity> =
        logs.filter { it.acUnitId == unitId }.sortedByDescending { it.fechaMillis }

    /** Facturas del grupo activo, de la más antigua a la más reciente. */
    val groupBills: List<BillRecordEntity>
        get() = selectedGroup?.let { group ->
            bills.filter { it.groupId == group.id }
                .sortedWith(compareBy({ it.year }, { it.month }))
        }.orEmpty()

    /** Hasta 12 meses visibles en las barras Pixel. */
    val recentBills: List<BillRecordEntity>
        get() = groupBills.takeLast(12)

    /** Último recibo registrado; alimenta el reparto Climatización/Hogar. */
    val latestBill: BillRecordEntity?
        get() = groupBills.lastOrNull()

    /** kWh de climatización (equipos) frente al resto del hogar del último recibo. */
    val climatizationSplit: Pair<Double, Double>?
        get() {
            val total = latestBill?.realKwh?.takeIf { it > 0.0 } ?: return null
            val climatization = groupUnits.sumOf { it.consumoEstimadoKwhMes }.coerceAtMost(total)
            return climatization to (total - climatization).coerceAtLeast(0.0)
        }

    /** Variación porcentual frente al mes anterior (null si no hay anterior). */
    fun variationPercent(record: BillRecordEntity): Double? {
        val series = groupBills
        val index = series.indexOfFirst { it.id == record.id }
        if (index <= 0) return null
        val previous = series[index - 1].realKwh
        if (previous <= 0.0) return null
        return (record.realKwh - previous) / previous * 100.0
    }
}
