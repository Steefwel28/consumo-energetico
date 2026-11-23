package com.steefwel28.consumoenergetico.ui.screens.calculate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steefwel28.consumoenergetico.core.di.AppContainer
import com.steefwel28.consumoenergetico.core.signature.CraftedBy
import com.steefwel28.consumoenergetico.data.local.db.CustomLocationEntity
import com.steefwel28.consumoenergetico.data.local.db.PropertyGroupEntity
import com.steefwel28.consumoenergetico.domain.calculator.PanamaUtility
import com.steefwel28.consumoenergetico.domain.calculator.tariffComparison
import com.steefwel28.consumoenergetico.domain.scanner.RtcaLabelReading
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@CraftedBy("steefwel28")
class CalculateViewModel(
    private val container: AppContainer,
) : ViewModel() {

    private val _state = MutableStateFlow(CalculateUiState())
    val state: StateFlow<CalculateUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                container.preferences.utility,
                container.preferences.baseMonthlyKwh,
                container.preferences.unitPreferences,
            ) { utility, baseKwh, units -> Triple(utility, baseKwh, units) }
                .collect { (utility, baseKwh, units) ->
                    _state.update { current ->
                        current.copy(
                            utility = utility,
                            baseMonthlyKwh = baseKwh,
                            unitPreferences = units,
                        )
                    }
                }
        }

        viewModelScope.launch {
            ensureDefaultGroup()
            combine(
                container.database.propertyGroupDao().observeAll(),
                container.database.customLocationDao().observeAll(),
            ) { groups, locations -> groups to locations }
                .collect { (groups, locations) ->
                    _state.update { current ->
                        val selected = current.selectedGroupId
                            ?.takeIf { id -> groups.any { it.id == id } }
                            ?: groups.firstOrNull()?.id
                        current.copy(
                            groups = groups,
                            customLocations = locations,
                            selectedGroupId = selected,
                        )
                    }
                }
        }
    }

    fun update(transform: (CalculateUiState) -> CalculateUiState) {
        _state.update(transform)
    }

    /** Aplica solo los campos que el OCR logró leer; el resto queda como estaba. */
    fun applyScan(reading: RtcaLabelReading) {
        _state.update { current ->
            current.copy(
                btuInput = reading.btuPerHour?.let { whole(it) } ?: current.btuInput,
                wattsInput = reading.nominalWatts?.let { whole(it) } ?: current.wattsInput,
                seerInput = reading.seer?.let { decimal(it) } ?: current.seerInput,
            )
        }
    }

    /**
     * Guarda el equipo en el grupo elegido y registra la ubicación nueva.
     * Si el usuario creó un grupo, primero se inserta y su distribuidora y
     * consumo base pasan a la gráfica activa.
     */
    fun save(request: SaveRequest) {
        if (_state.value.isSaving) return
        _state.update { it.copy(isSaving = true, saveError = null) }

        viewModelScope.launch {
            runCatching {
                val dao = container.database.propertyGroupDao()
                val groups = dao.getAll()
                val requestedName = request.newGroupName.trim()
                val group = request.groupId
                    ?.let { id -> groups.firstOrNull { it.id == id } }
                    ?: groups.firstOrNull { it.nombreGrupo.equals(requestedName, ignoreCase = true) }
                    ?: run {
                        val name = requestedName.ifBlank { PropertyGroupEntity.DEFAULT_NAME }
                        val created = PropertyGroupEntity(
                            nombreGrupo = name,
                            distribuidora = request.groupUtility,
                            consumoBaseKwh = request.groupBaseKwh.coerceIn(0.0, 100_000.0),
                        )
                        created.copy(id = dao.insert(created))
                    }

                val location = request.location.trim().ifBlank { CalculateLocations.DEFAULT }
                if (location !in CalculateLocations.base) {
                    container.database.customLocationDao()
                        .insert(CustomLocationEntity(nombreUbicacion = location))
                }

                val entity = _state.value.copy(
                    brandModel = request.brandModel.trim(),
                    spaceName = location,
                ).toEntity(
                    nowMillis = System.currentTimeMillis(),
                    groupId = group.id,
                    ubicacion = location,
                ) ?: error("Completa BTU/h y potencia nominal mayores que cero para guardar.")

                container.database.acUnitDao().insert(entity)
                entity to group
            }.onSuccess { (entity, group) ->
                _state.update { current ->
                    current.copy(
                        isSaving = false,
                        savedMessage = "“${entity.ubicacion}” quedó registrado en “${group.nombreGrupo}”.",
                        selectedGroupId = group.id,
                        selectedLocation = entity.ubicacion,
                        utility = group.distribuidora,
                        baseMonthlyKwh = group.consumoBaseKwh,
                        chartMode = group.distribuidora.tariffComparison(),
                    )
                }
            }.onFailure { error ->
                _state.update { current ->
                    current.copy(
                        isSaving = false,
                        saveError = error.message ?: "No se pudo guardar el equipo.",
                    )
                }
            }
        }
    }

    fun dismissSaveResult() {
        _state.update { it.copy(savedMessage = null, saveError = null) }
    }

    /** Siembra "Mi Casa" en instalaciones nuevas; la migración ya lo hace en v1. */
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

    private fun whole(value: Double): String = value.toLong().toString()

    private fun decimal(value: Double): String =
        String.format(Locale.US, "%.1f", value).trimEnd('0').trimEnd('.')
}
