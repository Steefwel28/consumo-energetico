package com.steefwel28.consumoenergetico.ui.screens.myhome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steefwel28.consumoenergetico.R
import com.steefwel28.consumoenergetico.core.di.AppContainer
import com.steefwel28.consumoenergetico.core.signature.CraftedBy
import com.steefwel28.consumoenergetico.data.local.db.AcUnitEntity
import com.steefwel28.consumoenergetico.data.local.db.BillRecordEntity
import com.steefwel28.consumoenergetico.data.local.db.CustomLocationEntity
import com.steefwel28.consumoenergetico.data.local.db.MaintenanceLogEntity
import com.steefwel28.consumoenergetico.data.local.db.PropertyGroupEntity
import com.steefwel28.consumoenergetico.domain.calculator.AcConsumptionCalculator
import com.steefwel28.consumoenergetico.domain.calculator.AcUnitSpec
import com.steefwel28.consumoenergetico.domain.calculator.AcUsage
import com.steefwel28.consumoenergetico.domain.calculator.MAX_BTU_PER_HOUR
import com.steefwel28.consumoenergetico.domain.calculator.PanamaUtility
import com.steefwel28.consumoenergetico.domain.calculator.PowerEstimator
import com.steefwel28.consumoenergetico.domain.recommender.ElectricalSpecAdvisor
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@CraftedBy("steefwel28")
class MyHomeViewModel(
    private val container: AppContainer,
) : ViewModel() {

    private val _state = MutableStateFlow(MyHomeUiState())
    val state: StateFlow<MyHomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { ensureDefaultGroup() }
        viewModelScope.launch {
            combine(
                container.database.propertyGroupDao().observeAll(),
                container.database.acUnitDao().observeUnits(),
                container.database.customLocationDao().observeAll(),
                container.database.maintenanceLogDao().observeAll(),
                container.database.billRecordDao().observeAll(),
            ) { groups, units, locations, logs, bills ->
                MyHomeUiState(
                    groups = groups,
                    units = units,
                    locations = locations,
                    logs = logs,
                    bills = bills,
                    nowMillis = System.currentTimeMillis(),
                )
            }.collect { fresh ->
                _state.update { current ->
                    fresh.copy(
                        selectedGroupId = current.selectedGroupId
                            ?.takeIf { id -> fresh.groups.any { it.id == id } }
                            ?: fresh.groups.firstOrNull()?.id,
                        message = current.message,
                    )
                }
            }
        }
    }

    fun selectGroup(groupId: Long) {
        _state.update { it.copy(selectedGroupId = groupId) }
    }

    fun saveGroup(draft: GroupDraft) {
        val name = draft.nombre.trim()
        if (name.isEmpty()) {
            _state.update {
                it.copy(message = container.appContext.getString(R.string.home_msg_name_required))
            }
            return
        }
        if (!draft.baseKwh.isFinite() || draft.baseKwh < 0.0) {
            _state.update {
                it.copy(message = container.appContext.getString(R.string.home_msg_base_negative))
            }
            return
        }

        viewModelScope.launch {
            runCatching {
                container.database.propertyGroupDao().insert(
                    PropertyGroupEntity(
                        id = draft.id ?: 0L,
                        nombreGrupo = name,
                        distribuidora = draft.utility,
                        consumoBaseKwh = draft.baseKwh.coerceAtMost(100_000.0),
                    ),
                )
            }.onSuccess { savedId ->
                _state.update {
                    it.copy(
                        selectedGroupId = draft.id ?: savedId,
                        message = container.appContext.getString(R.string.home_msg_group_saved),
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        message = error.message
                            ?: container.appContext.getString(R.string.home_msg_group_error),
                    )
                }
            }
        }
    }

    fun saveUnit(draft: UnitDraft) {
        if (draft.btu <= 0 || draft.btu > MAX_BTU_PER_HOUR) {
            _state.update {
                it.copy(message = container.appContext.getString(R.string.home_msg_btu_positive))
            }
            return
        }
        val location = draft.ubicacion.trim()
        if (location.isEmpty()) {
            _state.update {
                it.copy(message = container.appContext.getString(R.string.home_msg_location_required))
            }
            return
        }
        val watts = draft.watts
        if (watts != null && (!watts.isFinite() || watts <= 0.0)) {
            _state.update {
                it.copy(message = container.appContext.getString(R.string.home_msg_watts_positive))
            }
            return
        }

        viewModelScope.launch {
            runCatching {
                if (location !in MyHomeLocations.base) {
                    container.database.customLocationDao()
                        .insert(CustomLocationEntity(nombreUbicacion = location))
                }
                val unit = AcUnitEntity(
                    id = draft.id ?: 0L,
                    nombreEspacio = draft.nombreEspacio.trim().ifBlank { location },
                    marcaModelo = draft.marcaModelo.trim().ifBlank { "Sin marca/modelo" },
                    btu = draft.btu,
                    nominalWatts = watts?.roundToInt(),
                    seer = draft.seer,
                    esInverter = draft.inverter,
                    voltaje = ElectricalSpecAdvisor.advise(
                        btuPerHour = draft.btu,
                        inverter = draft.inverter,
                    ).voltage,
                    fechaInstalacionMillis = draft.fechaInstalacionMillis,
                    fechaUltimoMantenimientoMillis = draft.fechaUltimoMantenimientoMillis,
                    consumoEstimadoKwhMes = draft.consumoKwhMes ?: estimateMonthlyKwh(draft, watts),
                    horasUsoDiario = draft.horasUsoDiario.coerceIn(1.0, 24.0),
                    groupId = draft.groupId,
                    ubicacion = location,
                )
                container.database.acUnitDao().insert(unit)
                unit
            }.onSuccess { unit ->
                _state.update {
                    it.copy(
                        selectedGroupId = unit.groupId,
                        message = container.appContext.getString(R.string.home_msg_unit_saved, unit.ubicacion),
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        message = error.message
                            ?: container.appContext.getString(R.string.home_msg_unit_error),
                    )
                }
            }
        }
    }

    /** Registra la limpieza de hoy y reinicia el contador de 90 días. */
    fun registerMaintenance(unitId: Long) {
        viewModelScope.launch {
            runCatching {
                val dao = container.database.acUnitDao()
                val unit = dao.getById(unitId)
                    ?: error(container.appContext.getString(R.string.home_msg_unit_missing))
                val now = System.currentTimeMillis()
                container.database.maintenanceLogDao().insert(
                    MaintenanceLogEntity(
                        acUnitId = unitId,
                        fechaMillis = now,
                        nota = "Limpieza registrada desde Mi Casa",
                    ),
                )
                dao.insert(unit.copy(fechaUltimoMantenimientoMillis = now))
            }.onSuccess {
                _state.update {
                    it.copy(message = container.appContext.getString(R.string.home_msg_maintenance_saved))
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        message = error.message
                            ?: container.appContext.getString(R.string.home_msg_maintenance_error),
                    )
                }
            }
        }
    }

    /** Guarda una factura real del grupo; misma llave mes/año se reemplaza. */
    fun saveBill(draft: BillDraft) {
        if (draft.realKwh <= 0.0) {
            _state.update {
                it.copy(message = container.appContext.getString(R.string.home_msg_bill_kwh_positive))
            }
            return
        }
        if (draft.paidBalboas < 0.0) {
            _state.update {
                it.copy(message = container.appContext.getString(R.string.home_msg_bill_paid_negative))
            }
            return
        }
        viewModelScope.launch {
            runCatching {
                container.database.billRecordDao().insert(
                    BillRecordEntity(
                        groupId = draft.groupId,
                        year = draft.year,
                        month = draft.month,
                        realKwh = draft.realKwh,
                        paidBalboas = draft.paidBalboas,
                        utility = draft.utility,
                    ),
                )
            }.onSuccess {
                _state.update {
                    it.copy(message = container.appContext.getString(R.string.home_msg_bill_saved))
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        message = error.message
                            ?: container.appContext.getString(R.string.home_msg_bill_error),
                    )
                }
            }
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(message = null) }
    }

    /** Borra el equipo; el Snackbar puede reinsertarlo junto con su historial. */
    fun deleteUnit(unit: AcUnitEntity) {
        viewModelScope.launch {
            runCatching { container.database.acUnitDao().deleteById(unit.id) }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            message = error.message
                                ?: container.appContext.getString(R.string.home_msg_unit_error),
                        )
                    }
                }
        }
    }

    fun restoreUnit(unit: AcUnitEntity, logs: List<MaintenanceLogEntity>) {
        viewModelScope.launch {
            runCatching {
                container.database.acUnitDao().insert(unit)
                if (logs.isNotEmpty()) container.database.maintenanceLogDao().insertAll(logs)
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        message = error.message
                            ?: container.appContext.getString(R.string.home_msg_unit_error),
                    )
                }
            }
        }
    }

    fun deleteBill(bill: BillRecordEntity) {
        viewModelScope.launch {
            runCatching { container.database.billRecordDao().delete(bill.id) }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            message = error.message
                                ?: container.appContext.getString(R.string.home_msg_bill_error),
                        )
                    }
                }
        }
    }

    fun restoreBill(bill: BillRecordEntity) {
        viewModelScope.launch {
            runCatching { container.database.billRecordDao().insert(bill) }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            message = error.message
                                ?: container.appContext.getString(R.string.home_msg_bill_error),
                        )
                    }
                }
        }
    }

    private fun estimateMonthlyKwh(draft: UnitDraft, watts: Double?): Double {
        val spec = AcUnitSpec(
            btuPerHour = draft.btu.toDouble(),
            nominalWatts = watts ?: PowerEstimator.fromBtu(
                btuPerHour = draft.btu.toDouble(),
                seer = draft.seer,
                isInverter = draft.inverter,
            ),
            seer = draft.seer,
            inverter = draft.inverter,
        )
        return AcConsumptionCalculator.monthlyKwh(
            spec,
            AcUsage(hoursPerDay = draft.horasUsoDiario.coerceIn(1.0, 24.0)),
        )
    }

    private suspend fun ensureDefaultGroup() {
        val dao = container.database.propertyGroupDao()
        if (dao.count() == 0) {
            dao.insert(
                PropertyGroupEntity(
                    nombreGrupo = PropertyGroupEntity.DEFAULT_NAME,
                    distribuidora = PanamaUtility.ENSA,
                    consumoBaseKwh = 0.0,
                ),
            )
        }
    }
}
