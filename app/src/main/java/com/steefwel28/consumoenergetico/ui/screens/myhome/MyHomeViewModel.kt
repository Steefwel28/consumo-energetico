package com.steefwel28.consumoenergetico.ui.screens.myhome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steefwel28.consumoenergetico.core.di.AppContainer
import com.steefwel28.consumoenergetico.core.signature.CraftedBy
import com.steefwel28.consumoenergetico.data.local.db.AcUnitEntity
import com.steefwel28.consumoenergetico.data.local.db.CustomLocationEntity
import com.steefwel28.consumoenergetico.data.local.db.MaintenanceLogEntity
import com.steefwel28.consumoenergetico.data.local.db.PropertyGroupEntity
import com.steefwel28.consumoenergetico.domain.calculator.AcConsumptionCalculator
import com.steefwel28.consumoenergetico.domain.calculator.AcUnitSpec
import com.steefwel28.consumoenergetico.domain.calculator.AcUsage
import com.steefwel28.consumoenergetico.domain.calculator.PanamaUtility
import com.steefwel28.consumoenergetico.domain.recommender.ElectricalSpecAdvisor
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
            ) { groups, units, locations, logs ->
                MyHomeUiState(
                    groups = groups,
                    units = units,
                    locations = locations,
                    logs = logs,
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
            _state.update { it.copy(message = "El grupo necesita un nombre.") }
            return
        }
        if (!draft.baseKwh.isFinite() || draft.baseKwh < 0.0) {
            _state.update { it.copy(message = "El consumo base del recibo no puede ser negativo.") }
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
                        message = "Grupo guardado.",
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(message = error.message ?: "No se pudo guardar el grupo.") }
            }
        }
    }

    fun saveUnit(draft: UnitDraft) {
        if (draft.btu <= 0) {
            _state.update { it.copy(message = "El BTU/h debe ser mayor que cero.") }
            return
        }
        val location = draft.ubicacion.trim()
        if (location.isEmpty()) {
            _state.update { it.copy(message = "Elige o escribe una ubicación.") }
            return
        }
        val watts = draft.watts
        if (watts != null && (!watts.isFinite() || watts <= 0.0)) {
            _state.update { it.copy(message = "La potencia debe ser mayor que cero W.") }
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
                    seer = draft.seer,
                    esInverter = draft.inverter,
                    voltaje = ElectricalSpecAdvisor.advise(
                        btuPerHour = draft.btu,
                        inverter = draft.inverter,
                    ).voltage,
                    fechaInstalacionMillis = draft.fechaInstalacionMillis,
                    fechaUltimoMantenimientoMillis = draft.fechaUltimoMantenimientoMillis,
                    consumoEstimadoKwhMes = draft.consumoKwhMes ?: estimateMonthlyKwh(draft, watts),
                    groupId = draft.groupId,
                    ubicacion = location,
                )
                container.database.acUnitDao().insert(unit)
                unit
            }.onSuccess { unit ->
                _state.update {
                    it.copy(
                        selectedGroupId = unit.groupId,
                        message = "“${unit.ubicacion}” quedó guardado en tu perfil.",
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(message = error.message ?: "No se pudo guardar el equipo.") }
            }
        }
    }

    /** Registra la limpieza de hoy y reinicia el contador de 90 días. */
    fun registerMaintenance(unitId: Long) {
        viewModelScope.launch {
            runCatching {
                val dao = container.database.acUnitDao()
                val unit = dao.getById(unitId) ?: error("El equipo ya no existe.")
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
                _state.update { it.copy(message = "Mantenimiento registrado hoy.") }
            }.onFailure { error ->
                _state.update { it.copy(message = error.message ?: "No se pudo registrar la limpieza.") }
            }
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(message = null) }
    }

    private fun estimateMonthlyKwh(draft: UnitDraft, watts: Double?): Double {
        val spec = AcUnitSpec(
            btuPerHour = draft.btu.toDouble(),
            nominalWatts = watts ?: (draft.btu / 3.4),
            seer = draft.seer,
            inverter = draft.inverter,
        )
        return AcConsumptionCalculator.monthlyKwh(spec, AcUsage(hoursPerDay = 8.0))
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
