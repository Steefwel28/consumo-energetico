@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.steefwel28.consumoenergetico.ui.screens.myhome

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.steefwel28.consumoenergetico.core.di.AppContainer
import com.steefwel28.consumoenergetico.data.local.db.AcUnitEntity
import com.steefwel28.consumoenergetico.data.local.db.MaintenanceLogEntity
import com.steefwel28.consumoenergetico.data.local.db.PropertyGroupEntity
import com.steefwel28.consumoenergetico.domain.calculator.PanamaUtility
import com.steefwel28.consumoenergetico.domain.calculator.TariffEstimate
import com.steefwel28.consumoenergetico.domain.units.LocaleNumber
import com.steefwel28.consumoenergetico.ui.AppViewModelProvider
import com.steefwel28.consumoenergetico.ui.components.AppFormat
import com.steefwel28.consumoenergetico.ui.components.PixelDialog
import com.steefwel28.consumoenergetico.ui.components.PixelSpringSnappy
import com.steefwel28.consumoenergetico.ui.components.SectionCard
import com.steefwel28.consumoenergetico.ui.components.rememberPixelHaptics
import com.steefwel28.consumoenergetico.ui.theme.ConsumoEnergeticoTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun MyHomeRoute(
    container: AppContainer,
    viewModel: MyHomeViewModel = viewModel(factory = AppViewModelProvider.myHome(container)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MyHomeScreen(
        state = state,
        onSelectGroup = viewModel::selectGroup,
        onSaveGroup = viewModel::saveGroup,
        onSaveUnit = viewModel::saveUnit,
        onRegisterMaintenance = viewModel::registerMaintenance,
        onDismissMessage = viewModel::dismissMessage,
    )
}

@Composable
fun MyHomeScreen(
    state: MyHomeUiState,
    onSelectGroup: (Long) -> Unit,
    onSaveGroup: (GroupDraft) -> Unit,
    onSaveUnit: (UnitDraft) -> Unit,
    onRegisterMaintenance: (Long) -> Unit,
    onDismissMessage: () -> Unit,
) {
    var showNewGroup by remember { mutableStateOf(false) }
    var editingGroup by remember { mutableStateOf<PropertyGroupEntity?>(null) }
    var showNewUnit by remember { mutableStateOf(false) }
    var editingUnit by remember { mutableStateOf<AcUnitEntity?>(null) }
    var historyUnit by remember { mutableStateOf<AcUnitEntity?>(null) }
    val haptics = rememberPixelHaptics()
    val group = state.selectedGroup

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Mi Casa", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Cada propiedad con su recibo, sus equipos y su salto de bloque tarifario.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            GroupsCard(
                state = state,
                onSelectGroup = { id ->
                    haptics.tap()
                    onSelectGroup(id)
                },
                onNewGroup = {
                    haptics.tap()
                    showNewGroup = true
                },
            )
        }

        if (group == null) {
            item {
                SectionCard(
                    title = "Todavía no hay grupos",
                    subtitle = "Crea “Mi Casa” o “Trabajo” para empezar.",
                ) {
                    Button(onClick = { showNewGroup = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Text(text = "Crear grupo", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        } else {
            item {
                GroupSummaryCard(
                    group = group,
                    estimate = state.summary,
                    onEdit = {
                        haptics.tap()
                        editingGroup = group
                    },
                )
            }

            item {
                NotificationsCard()
            }

            item {
                Button(
                    onClick = {
                        haptics.confirm()
                        showNewUnit = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text(text = "Añadir equipo a ${group.nombreGrupo}", modifier = Modifier.padding(start = 8.dp))
                }
            }

            if (state.groupUnits.isEmpty()) {
                item {
                    SectionCard(
                        title = "Sin equipos todavía",
                        subtitle = "Guarda un aire desde Calcular o agrégalo aquí con su consumo mensual.",
                    ) {}
                }
            } else {
                state.unitsByLocation.forEach { (location, units) ->
                    item(key = "location-$location") {
                        SectionCard(
                            title = location,
                            subtitle = if (units.size == 1) {
                                "1 equipo en esta ubicación"
                            } else {
                                "${units.size} equipos en esta ubicación"
                            },
                        ) {
                            units.forEach { unit ->
                                UnitCard(
                                    unit = unit,
                                    days = state.daysUntilMaintenance(unit),
                                    onEdit = {
                                        haptics.tap()
                                        editingUnit = unit
                                    },
                                    onHistory = {
                                        haptics.tap()
                                        historyUnit = unit
                                    },
                                    onMaintenance = {
                                        haptics.confirm()
                                        onRegisterMaintenance(unit.id)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "El mantenimiento se avisa cada 90 días; registrar la limpieza reinicia el contador.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (showNewGroup || editingGroup != null) {
        GroupDialog(
            group = editingGroup,
            onDismiss = {
                showNewGroup = false
                editingGroup = null
            },
            onSave = { draft ->
                onSaveGroup(draft)
                showNewGroup = false
                editingGroup = null
            },
        )
    }

    if ((showNewUnit || editingUnit != null) && group != null) {
        UnitDialog(
            unit = editingUnit,
            groups = state.groups,
            locations = state.knownLocations,
            initialGroupId = group.id,
            onDismiss = {
                showNewUnit = false
                editingUnit = null
            },
            onSave = { draft ->
                onSaveUnit(draft)
                showNewUnit = false
                editingUnit = null
            },
        )
    }

    historyUnit?.let { unit ->
        HistoryDialog(
            unit = unit,
            logs = state.historyFor(unit.id),
            onDismiss = { historyUnit = null },
        )
    }

    state.message?.let { message ->
        PixelDialog(
            title = "Mi Casa",
            text = message,
            confirmLabel = "Entendido",
            onConfirm = onDismissMessage,
            onDismissRequest = onDismissMessage,
        )
    }
}

@Composable
private fun GroupsCard(
    state: MyHomeUiState,
    onSelectGroup: (Long) -> Unit,
    onNewGroup: () -> Unit,
) {
    SectionCard(
        title = "Grupos y propiedades",
        subtitle = "Cada grupo guarda su distribuidora y el consumo base de su recibo.",
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.groups.forEach { group ->
                val selected = group.id == state.selectedGroup?.id
                val scale by animateFloatAsState(
                    targetValue = if (selected) 1f else 0.96f,
                    animationSpec = PixelSpringSnappy,
                    label = "group-${group.id}",
                )
                FilterChip(
                    selected = selected,
                    onClick = { onSelectGroup(group.id) },
                    label = { Text(group.nombreGrupo) },
                    modifier = Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                )
            }
            FilterChip(
                selected = false,
                onClick = onNewGroup,
                label = { Text("Nuevo grupo") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.padding(2.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun GroupSummaryCard(
    group: PropertyGroupEntity,
    estimate: TariffEstimate?,
    onEdit: () -> Unit,
) {
    SectionCard(
        title = "Resumen de ${group.nombreGrupo}",
        subtitle = "${group.distribuidora.displayName} · recibo base ${AppFormat.kwh(group.consumoBaseKwh)}",
    ) {
        if (estimate == null) {
            Text(
                text = "Agrega equipos para ver el consumo combinado y su salto de bloque.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HomeMetric(
                    label = "Consumo total",
                    value = AppFormat.kwh(estimate.baseKwh + estimate.addedKwh),
                    modifier = Modifier.weight(1f),
                )
                HomeMetric(
                    label = "Factura estimada",
                    value = AppFormat.balboas(estimate.totalBillBalboas),
                    modifier = Modifier.weight(1f),
                    highlight = true,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HomeMetric(
                    label = "Aires del grupo",
                    value = AppFormat.kwh(estimate.addedKwh),
                    modifier = Modifier.weight(1f),
                )
                HomeMetric(
                    label = "Salto a la factura",
                    value = AppFormat.balboas(estimate.addedCostBalboas),
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = "El próximo kWh paga ${AppFormat.balboas(estimate.marginalBalboasPerKwh)}: " +
                    "así se siente el escalón de ${group.distribuidora.displayName}.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Edit, contentDescription = null)
            Text(text = "Editar grupo", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun NotificationsCard() {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { result -> granted = result }
    val haptics = rememberPixelHaptics()

    if (granted) return

    SectionCard(
        title = "Recordatorios de mantenimiento",
        subtitle = "Android 13 o superior necesita tu permiso para avisarte cada 3 meses.",
    ) {
        Button(
            onClick = {
                haptics.confirm()
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Activar avisos")
        }
    }
}

@Composable
private fun UnitCard(
    unit: AcUnitEntity,
    days: Long,
    onEdit: () -> Unit,
    onHistory: () -> Unit,
    onMaintenance: () -> Unit,
) {
    val overdue = days < 0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = unit.marcaModelo, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${AppFormat.btu(unit.btu.toDouble())} · " +
                        "${if (unit.esInverter) "Inverter" else "No Inverter"} · ${unit.voltaje} V",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Consumo estimado: ${AppFormat.kwh(unit.consumoEstimadoKwhMes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = maintenanceText(days),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (overdue) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Rounded.Edit, contentDescription = "Editar ${unit.nombreEspacio}")
            }
            IconButton(onClick = onHistory) {
                Icon(Icons.Rounded.History, contentDescription = "Historial de ${unit.nombreEspacio}")
            }
        }
        OutlinedButton(onClick = onMaintenance, modifier = Modifier.fillMaxWidth()) {
            Text("Registrar mantenimiento hoy")
        }
    }
}

@Composable
private fun HomeMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (highlight) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = if (highlight) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun GroupDialog(
    group: PropertyGroupEntity?,
    onDismiss: () -> Unit,
    onSave: (GroupDraft) -> Unit,
) {
    val haptics = rememberPixelHaptics()
    var name by remember { mutableStateOf(group?.nombreGrupo.orEmpty()) }
    var utility by remember { mutableStateOf(group?.distribuidora ?: PanamaUtility.ENSA) }
    var base by remember {
        mutableStateOf(group?.let { AppFormat.kwhNumber(it.consumoBaseKwh) }.orEmpty())
    }

    PixelDialog(
        title = if (group == null) "Nuevo grupo" else "Editar grupo",
        confirmLabel = "Guardar",
        confirmEnabled = name.isNotBlank(),
        onConfirm = {
            haptics.confirm()
            onSave(
                GroupDraft(
                    id = group?.id,
                    nombre = name,
                    utility = utility,
                    baseKwh = LocaleNumber.parse(base) ?: 0.0,
                ),
            )
        },
        onDismissRequest = onDismiss,
        dismissLabel = "Cancelar",
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del grupo") },
                    placeholder = { Text("Mi Casa, Trabajo, Casa de Mamá") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    PanamaUtility.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = utility == option,
                            onClick = {
                                haptics.tap()
                                utility = option
                            },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = PanamaUtility.entries.size,
                            ),
                        ) { Text(option.displayName) }
                    }
                }
                OutlinedTextField(
                    value = base,
                    onValueChange = { base = it },
                    label = { Text("Consumo base del recibo (kWh/mes)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
}

@Composable
private fun UnitDialog(
    unit: AcUnitEntity?,
    groups: List<PropertyGroupEntity>,
    locations: List<String>,
    initialGroupId: Long,
    onDismiss: () -> Unit,
    onSave: (UnitDraft) -> Unit,
) {
    val haptics = rememberPixelHaptics()
    var groupId by remember { mutableStateOf(unit?.groupId ?: initialGroupId) }
    var location by remember { mutableStateOf(unit?.ubicacion ?: MyHomeLocations.DEFAULT) }
    var useOther by remember { mutableStateOf(false) }
    var otherLocation by remember { mutableStateOf("") }
    var spaceName by remember { mutableStateOf(unit?.nombreEspacio.orEmpty()) }
    var brandModel by remember { mutableStateOf(unit?.marcaModelo?.takeUnless { it == "Sin marca/modelo" }.orEmpty()) }
    var btuInput by remember { mutableStateOf((unit?.btu ?: 12_000).toString()) }
    var wattsInput by remember { mutableStateOf("") }
    var seerInput by remember { mutableStateOf(unit?.seer?.let { AppFormat.decimal(it) }.orEmpty()) }
    var inverter by remember { mutableStateOf(unit?.esInverter ?: true) }
    var consumoInput by remember {
        mutableStateOf(unit?.let { AppFormat.kwhNumber(it.consumoEstimadoKwhMes) }.orEmpty())
    }

    val btu = btuInput.toIntOrNull()
    val chosenLocation = if (useOther) otherLocation.trim() else location
    val valid = btu != null && btu > 0 && chosenLocation.isNotBlank() &&
        groups.any { it.id == groupId }

    PixelDialog(
        title = if (unit == null) "Nuevo equipo" else "Editar equipo",
        confirmLabel = "Guardar",
        confirmEnabled = valid,
        onConfirm = {
            haptics.confirm()
            onSave(
                UnitDraft(
                    id = unit?.id,
                    groupId = groupId,
                    nombreEspacio = spaceName,
                    marcaModelo = brandModel,
                    ubicacion = chosenLocation,
                    btu = btu ?: 0,
                    watts = LocaleNumber.parse(wattsInput),
                    seer = LocaleNumber.parse(seerInput),
                    inverter = inverter,
                    consumoKwhMes = LocaleNumber.parse(consumoInput),
                    fechaInstalacionMillis = unit?.fechaInstalacionMillis
                        ?: System.currentTimeMillis(),
                    fechaUltimoMantenimientoMillis = unit?.fechaUltimoMantenimientoMillis
                        ?: System.currentTimeMillis(),
                ),
            )
        },
        onDismissRequest = onDismiss,
        dismissLabel = "Cancelar",
        content = {
            Column(
                modifier = Modifier
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(text = "Grupo / propiedad", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    groups.forEach { group ->
                        FilterChip(
                            selected = groupId == group.id,
                            onClick = {
                                haptics.tap()
                                groupId = group.id
                            },
                            label = { Text(group.nombreGrupo) },
                        )
                    }
                }

                Text(text = "Ubicación", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (locations + MyHomeLocations.OTHER).distinct().forEach { name ->
                        val isOther = name == MyHomeLocations.OTHER
                        FilterChip(
                            selected = if (isOther) useOther else !useOther && location == name,
                            onClick = {
                                haptics.tap()
                                if (isOther) {
                                    useOther = true
                                } else {
                                    useOther = false
                                    location = name
                                }
                            },
                            label = { Text(name) },
                        )
                    }
                }
                if (useOther) {
                    OutlinedTextField(
                        value = otherLocation,
                        onValueChange = { otherLocation = it },
                        label = { Text("Nueva ubicación") },
                        placeholder = { Text("Balcón, Oficina, Garaje…") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                OutlinedTextField(
                    value = spaceName,
                    onValueChange = { spaceName = it },
                    label = { Text("Nombre del espacio (opcional)") },
                    placeholder = { Text("Recámara principal") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = brandModel,
                    onValueChange = { brandModel = it },
                    label = { Text("Marca y modelo") },
                    placeholder = { Text("Hisense ATR362MA") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = btuInput,
                        onValueChange = { btuInput = it },
                        label = { Text("BTU/h") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = wattsInput,
                        onValueChange = { wattsInput = it },
                        label = { Text("Potencia W") },
                        placeholder = { Text("1150") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = seerInput,
                        onValueChange = { seerInput = it },
                        label = { Text("SEER") },
                        placeholder = { Text("12.5") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = consumoInput,
                        onValueChange = { consumoInput = it },
                        label = { Text("kWh/mes") },
                        placeholder = { Text("Se estima si lo dejas vacío") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Inverter", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Define el protector de línea directa que necesita.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = inverter,
                        onCheckedChange = { checked ->
                            haptics.tap()
                            inverter = checked
                        },
                    )
                }
            }
        },
    )
}

@Composable
private fun HistoryDialog(
    unit: AcUnitEntity,
    logs: List<MaintenanceLogEntity>,
    onDismiss: () -> Unit,
) {
    PixelDialog(
        title = "Historial de ${unit.nombreEspacio}",
        confirmLabel = "Cerrar",
        onConfirm = onDismiss,
        onDismissRequest = onDismiss,
        content = {
            Column(
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (logs.isEmpty()) {
                    Text(
                        text = "Aún no hay limpiezas registradas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    logs.forEach { log ->
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = formatDate(log.fechaMillis),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = log.nota.ifBlank { "Limpieza registrada" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
    )
}

private fun maintenanceText(days: Long): String = when {
    days > 1 -> "Faltan $days días para el mantenimiento (90 días)"
    days == 1L -> "Falta 1 día para el mantenimiento"
    days == 0L -> "El mantenimiento vence hoy"
    days == -1L -> "Mantenimiento vencido hace 1 día"
    else -> "Mantenimiento vencido hace ${-days} días"
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("es-PA")).format(Date(millis))

@Preview(name = "Mi Casa claro", showBackground = true, heightDp = 900)
@Preview(
    name = "Mi Casa oscuro",
    showBackground = true,
    heightDp = 900,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun MyHomeScreenPreview() {
    ConsumoEnergeticoTheme {
        MyHomeScreen(
            state = MyHomeUiState(),
            onSelectGroup = {},
            onSaveGroup = {},
            onSaveUnit = {},
            onRegisterMaintenance = {},
            onDismissMessage = {},
        )
    }
}
