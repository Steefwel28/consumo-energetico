package com.steefwel28.consumoenergetico.ui.screens.calculate

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steefwel28.consumoenergetico.R
import com.steefwel28.consumoenergetico.core.di.AppContainer
import com.steefwel28.consumoenergetico.core.signature.CraftedBy
import com.steefwel28.consumoenergetico.data.local.db.CustomLocationEntity
import com.steefwel28.consumoenergetico.data.local.db.PropertyGroupEntity
import com.steefwel28.consumoenergetico.domain.calculator.AcConsumptionCalculator
import com.steefwel28.consumoenergetico.domain.calculator.AcSchedule
import com.steefwel28.consumoenergetico.domain.calculator.PanamaUtility
import com.steefwel28.consumoenergetico.domain.calculator.TariffComparison
import com.steefwel28.consumoenergetico.domain.calculator.tariffComparison
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@CraftedBy("steefwel28")
class CalculateViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val container: AppContainer,
) : ViewModel() {

    private val _state = MutableStateFlow(restoreState())
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
        persistState(_state.value)
    }

    /** Chips rápidos de consumo base: actualiza la vista y lo persiste en DataStore. */
    fun setBaseMonthlyKwh(kwh: Double) {
        _state.update { it.copy(baseMonthlyKwh = kwh) }
        viewModelScope.launch { container.preferences.setBaseMonthlyKwh(kwh) }
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
                ) ?: error(
                    container.appContext.getString(R.string.calc_msg_save_incomplete),
                )

                container.database.acUnitDao().insert(entity)
                entity to group
            }.onSuccess { (entity, group) ->
                _state.update { current ->
                    current.copy(
                        isSaving = false,
                        savedMessage = container.appContext.getString(
                            R.string.calc_msg_saved,
                            entity.ubicacion,
                            group.nombreGrupo,
                        ),
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
                        saveError = error.message
                            ?: container.appContext.getString(R.string.calc_msg_save_error),
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

    // --- Rotación y muerte de proceso: los campos editados sobreviven vía
    // SavedStateHandle. Distribuidora y consumo base llegan de DataStore/Room.

    private fun restoreState(): CalculateUiState {
        val h = savedStateHandle
        fun text(key: String, fallback: String): String = h[key] ?: fallback
        fun whole(key: String, fallback: Int): Int = h[key] ?: fallback
        fun flag(key: String, fallback: Boolean): Boolean = h[key] ?: fallback
        return CalculateUiState(
            category = enum("calc.category", EquipmentCategory.AIR_CONDITIONER),
            spaceName = text("calc.spaceName", "Mi espacio"),
            brandModel = text("calc.brandModel", ""),
            btuInput = text("calc.btu", ""),
            wattsInput = text("calc.watts", ""),
            seerInput = text("calc.seer", ""),
            inverter = flag("calc.inverter", true),
            chartMode = enum("calc.chartMode", TariffComparison.VS),
            manualCost = flag("calc.manualCost", false),
            manualMode = enum("calc.manualMode", ManualCostMode.DIRECT_RATE),
            directRateInput = text("calc.directRate", "0.18"),
            receiptTotalInput = text("calc.receiptTotal", ""),
            receiptKwhInput = text("calc.receiptKwh", ""),
            baseKwhInput = text("calc.baseKwhInput", ""),
            usagePeriod = enum("calc.period", UsagePeriod.MONTH),
            setpointCelsius = whole("calc.setpoint", 24),
            cleanFilters = flag("calc.cleanFilters", true),
            schedule = enum("calc.schedule", AcSchedule.NIGHT),
            hoursPerDay = whole("calc.hours", AcConsumptionCalculator.DEFAULT_HOURS_PER_DAY.toInt()),
            simulatedSetpointCelsius = whole("calc.simSetpoint", 24),
            simulatedCleanFilters = flag("calc.simCleanFilters", true),
            simulatedSchedule = enum("calc.simSchedule", AcSchedule.NIGHT),
            simulatedHoursPerDay = whole("calc.simHours", 8),
            converterMode = enum("calc.converterMode", PlateConverterMode.TONS),
            converterInput = text("calc.converterInput", "1"),
            currentBtuInput = text("calc.currentBtu", "12000"),
            currentWattsInput = text("calc.currentWatts", "1600"),
            currentSeerInput = text("calc.currentSeer", ""),
            currentInverter = flag("calc.currentInverter", false),
            investmentInput = text("calc.investment", "450"),
            selectedLocation = text("calc.location", CalculateLocations.DEFAULT),
        )
    }

    private fun persistState(state: CalculateUiState) {
        val h = savedStateHandle
        h["calc.category"] = state.category.name
        h["calc.spaceName"] = state.spaceName
        h["calc.brandModel"] = state.brandModel
        h["calc.btu"] = state.btuInput
        h["calc.watts"] = state.wattsInput
        h["calc.seer"] = state.seerInput
        h["calc.inverter"] = state.inverter
        h["calc.chartMode"] = state.chartMode.name
        h["calc.manualCost"] = state.manualCost
        h["calc.manualMode"] = state.manualMode.name
        h["calc.directRate"] = state.directRateInput
        h["calc.receiptTotal"] = state.receiptTotalInput
        h["calc.receiptKwh"] = state.receiptKwhInput
        h["calc.baseKwhInput"] = state.baseKwhInput
        h["calc.period"] = state.usagePeriod.name
        h["calc.setpoint"] = state.setpointCelsius
        h["calc.cleanFilters"] = state.cleanFilters
        h["calc.schedule"] = state.schedule.name
        h["calc.hours"] = state.hoursPerDay
        h["calc.simSetpoint"] = state.simulatedSetpointCelsius
        h["calc.simCleanFilters"] = state.simulatedCleanFilters
        h["calc.simSchedule"] = state.simulatedSchedule.name
        h["calc.simHours"] = state.simulatedHoursPerDay
        h["calc.converterMode"] = state.converterMode.name
        h["calc.converterInput"] = state.converterInput
        h["calc.currentBtu"] = state.currentBtuInput
        h["calc.currentWatts"] = state.currentWattsInput
        h["calc.currentSeer"] = state.currentSeerInput
        h["calc.currentInverter"] = state.currentInverter
        h["calc.investment"] = state.investmentInput
        h["calc.location"] = state.selectedLocation
    }

    private inline fun <reified T : Enum<T>> enum(key: String, default: T): T =
        savedStateHandle.get<String>(key)
            ?.let { raw -> runCatching { enumValueOf<T>(raw) }.getOrNull() }
            ?: default
}
