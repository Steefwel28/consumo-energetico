@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.steefwel28.consumoenergetico.ui.screens.calculate

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.steefwel28.consumoenergetico.core.di.AppContainer
import com.steefwel28.consumoenergetico.data.export.ReportData
import com.steefwel28.consumoenergetico.data.export.ReportExporter
import com.steefwel28.consumoenergetico.domain.calculator.AcSchedule
import com.steefwel28.consumoenergetico.domain.calculator.AcUsage
import com.steefwel28.consumoenergetico.domain.calculator.PanamaUtility
import com.steefwel28.consumoenergetico.domain.scanner.RtcaLabelReading
import com.steefwel28.consumoenergetico.ui.AppViewModelProvider
import com.steefwel28.consumoenergetico.ui.components.AppFormat
import com.steefwel28.consumoenergetico.ui.components.PixelDialog
import com.steefwel28.consumoenergetico.ui.components.RtcaScannerSheet
import com.steefwel28.consumoenergetico.ui.components.SectionCard
import com.steefwel28.consumoenergetico.ui.theme.ConsumoEnergeticoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun CalculateRoute(
    container: AppContainer,
    viewModel: CalculateViewModel = viewModel(
        factory = AppViewModelProvider.calculate(container),
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CalculateScreen(
        state = state,
        onUpdate = viewModel::update,
        onSave = viewModel::save,
        onDismissSaveResult = viewModel::dismissSaveResult,
        onScanApplied = viewModel::applyScan,
    )
}

@Composable
fun CalculateScreen(
    state: CalculateUiState,
    onUpdate: ((CalculateUiState) -> CalculateUiState) -> Unit,
    onSave: () -> Unit,
    onDismissSaveResult: () -> Unit,
    onScanApplied: (RtcaLabelReading) -> Unit,
) {
    var showScanner by remember { mutableStateOf(false) }
    var showBillGuide by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var exportError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val share: (Boolean) -> Unit = { asPng ->
        val report = state.toReportData()
        if (report != null && !isExporting) {
            isExporting = true
            exportError = null
            scope.launch {
                runCatching {
                    val file = withContext(Dispatchers.IO) {
                        if (asPng) {
                            ReportExporter.exportPng(context, report)
                        } else {
                            ReportExporter.exportPdf(context, report)
                        }
                    }
                    val chooser = Intent.createChooser(
                        ReportExporter.shareIntent(context, file),
                        "Compartir ficha",
                    )
                    context.startActivity(chooser)
                }.onFailure { error ->
                    exportError = error.message ?: "No se pudo generar la ficha."
                }
                isExporting = false
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 20.dp,
            bottom = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Calcular", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Lee la etiqueta RTCA, simula el control remoto y compara la factura.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            EquipmentCard(
                state = state,
                onUpdate = onUpdate,
                onOpenScanner = { showScanner = true },
            )
        }

        item {
            ConverterCard(state = state, onUpdate = onUpdate)
        }

        item {
            TariffCard(
                state = state,
                onUpdate = onUpdate,
                onOpenBillGuide = { showBillGuide = true },
            )
        }

        item {
            SimulatorCard(state = state, onUpdate = onUpdate)
        }

        item {
            SaveAndShareCard(
                state = state,
                isExporting = isExporting,
                exportError = exportError,
                onSave = onSave,
                onSharePdf = { share(false) },
                onSharePng = { share(true) },
            )
        }

        item {
            Text(
                text = "Los valores son estimaciones educativas; la instalación eléctrica debe validarla un técnico certificado.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (showScanner) {
        RtcaScannerSheet(
            onDismiss = { showScanner = false },
            onApply = onScanApplied,
        )
    }
    if (showBillGuide) {
        BillGuideSheet(onDismiss = { showBillGuide = false })
    }

    state.savedMessage?.let { message ->
        PixelDialog(
            title = "Guardado en Mi Casa",
            text = message,
            confirmLabel = "Entendido",
            onConfirm = onDismissSaveResult,
            onDismissRequest = onDismissSaveResult,
        )
    }
    state.saveError?.let { message ->
        PixelDialog(
            title = "No se pudo guardar",
            text = message,
            confirmLabel = "Cerrar",
            onConfirm = onDismissSaveResult,
            onDismissRequest = onDismissSaveResult,
        )
    }
}

@Composable
private fun EquipmentCard(
    state: CalculateUiState,
    onUpdate: ((CalculateUiState) -> CalculateUiState) -> Unit,
    onOpenScanner: () -> Unit,
) {
    SectionCard(
        title = "Etiqueta RTCA",
        subtitle = "Copia los datos de la etiqueta amarilla o escanéalos.",
    ) {
        OutlinedTextField(
            value = state.spaceName,
            onValueChange = { value -> onUpdate { it.copy(spaceName = value) } },
            label = { Text("Espacio") },
            placeholder = { Text("Sala, recámara, local…") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.brandModel,
            onValueChange = { value -> onUpdate { it.copy(brandModel = value) } },
            label = { Text("Marca y modelo") },
            placeholder = { Text("LG Dual Inverter, Midea…") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberField(
                label = "BTU/h",
                value = state.btuInput,
                onValueChange = { value -> onUpdate { it.copy(btuInput = value) } },
                modifier = Modifier.weight(1f),
            )
            NumberField(
                label = "Potencia W",
                value = state.wattsInput,
                onValueChange = { value -> onUpdate { it.copy(wattsInput = value) } },
                modifier = Modifier.weight(1f),
            )
            NumberField(
                label = "SEER",
                value = state.seerInput,
                onValueChange = { value -> onUpdate { it.copy(seerInput = value) } },
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
                    text = "Modula la potencia y gasta menos que un equipo On/Off.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = state.inverter,
                onCheckedChange = { checked -> onUpdate { it.copy(inverter = checked) } },
            )
        }
        FilledTonalButton(
            onClick = onOpenScanner,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(imageVector = Icons.Rounded.CameraAlt, contentDescription = null)
            Text(text = "Escanear etiqueta amarilla", modifier = Modifier.padding(start = 8.dp))
        }
        if (state.spec == null) {
            Text(
                text = "Completa BTU/h y potencia nominal mayores que cero para calcular.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ConverterCard(
    state: CalculateUiState,
    onUpdate: ((CalculateUiState) -> CalculateUiState) -> Unit,
) {
    SectionCard(
        title = "Conversor de placa",
        subtitle = "Si la placa solo trae Ton, kW térmicos o amperios, llévalo a BTU/h.",
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            PlateConverterMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = state.converterMode == mode,
                    onClick = { onUpdate { it.copy(converterMode = mode) } },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = PlateConverterMode.entries.size,
                    ),
                ) {
                    Text(mode.unit)
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NumberField(
                label = "Valor en ${state.converterMode.unit}",
                value = state.converterInput,
                onValueChange = { value -> onUpdate { it.copy(converterInput = value) } },
                modifier = Modifier.weight(1f),
            )
            if (state.converterMode == PlateConverterMode.AMPERES) {
                Column {
                    FilterChip(
                        selected = state.converterVoltage == 110,
                        onClick = { onUpdate { it.copy(converterVoltage = 110) } },
                        label = { Text("110 V") },
                    )
                    FilterChip(
                        selected = state.converterVoltage == 220,
                        onClick = { onUpdate { it.copy(converterVoltage = 220) } },
                        label = { Text("220 V") },
                    )
                }
            }
        }
        state.converterBtu?.let { btu ->
            Text(
                text = "Equivale a ${AppFormat.btu(btu)}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        OutlinedButton(
            onClick = {
                state.converterBtu?.let { btu ->
                    onUpdate { it.copy(btuInput = btu.roundToInt().toString()) }
                }
            },
            enabled = state.converterBtu != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Usar en la etiqueta")
        }
    }
}

@Composable
private fun TariffCard(
    state: CalculateUiState,
    onUpdate: ((CalculateUiState) -> CalculateUiState) -> Unit,
    onOpenBillGuide: () -> Unit,
) {
    SectionCard(
        title = "Tarifa eléctrica",
        subtitle = "El costo real cambia si tu consumo cruza de bloque.",
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            PanamaUtility.entries.forEachIndexed { index, utility ->
                SegmentedButton(
                    selected = state.utility == utility,
                    onClick = { onUpdate { it.copy(utility = utility) } },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = PanamaUtility.entries.size,
                    ),
                ) {
                    Text(utility.displayName)
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Costo Manual", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Usa la tarifa real de tu factura en vez del pliego precargado.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onOpenBillGuide) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                    contentDescription = "Guía para ubicar consumo y total en el recibo",
                )
            }
            Switch(
                checked = state.manualCost,
                onCheckedChange = { checked -> onUpdate { it.copy(manualCost = checked) } },
            )
        }

        if (state.manualCost) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ManualCostMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = state.manualMode == mode,
                        onClick = { onUpdate { it.copy(manualMode = mode) } },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ManualCostMode.entries.size,
                        ),
                    ) {
                        Text(mode.label)
                    }
                }
            }
            when (state.manualMode) {
                ManualCostMode.DIRECT_RATE -> NumberField(
                    label = "Tarifa en B/. por kWh",
                    value = state.directRateInput,
                    onValueChange = { value -> onUpdate { it.copy(directRateInput = value) } },
                    modifier = Modifier.fillMaxWidth(),
                )

                ManualCostMode.RECEIPT -> Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    NumberField(
                        label = "Total B/.",
                        value = state.receiptTotalInput,
                        onValueChange = { value -> onUpdate { it.copy(receiptTotalInput = value) } },
                        modifier = Modifier.weight(1f),
                    )
                    NumberField(
                        label = "kWh del recibo",
                        value = state.receiptKwhInput,
                        onValueChange = { value -> onUpdate { it.copy(receiptKwhInput = value) } },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (state.tariffMode == null) {
                Text(
                    text = "Completa el total y los kWh del recibo para calcular la tarifa.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        } else {
            Text(
                text = "Consumo base del hogar: ${AppFormat.kwh(state.baseMonthlyKwh)} (se configura en Ajustes).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        state.tariffEstimate?.let { estimate ->
            Text(
                text = "Costo del equipo: ${AppFormat.balboas(estimate.addedCostBalboas)} al mes · " +
                    "tarifa marginal ${AppFormat.balboas(estimate.marginalBalboasPerKwh)}/kWh.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SimulatorCard(
    state: CalculateUiState,
    onUpdate: ((CalculateUiState) -> CalculateUiState) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    SectionCard(
        title = "Simulador de Control Remoto",
        subtitle = "Cada grado y cada filtro sucio cambian la factura en vivo.",
    ) {
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Control remoto") },
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Cara a cara") },
            )
        }

        if (selectedTab == 0) {
            RemoteControl(state = state, onUpdate = onUpdate)
        } else {
            FaceToFace(state = state, onUpdate = onUpdate)
        }
    }
}

@Composable
private fun RemoteControl(
    state: CalculateUiState,
    onUpdate: ((CalculateUiState) -> CalculateUiState) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(
            onClick = {
                onUpdate { it.copy(setpointCelsius = (it.setpointCelsius - 1).coerceAtLeast(18)) }
            },
            enabled = state.setpointCelsius > AcUsage.SIMULATOR_MIN_CELSIUS,
        ) {
            Icon(Icons.Rounded.Remove, contentDescription = "Bajar un grado")
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = AppFormat.temperature(state.setpointCelsius, state.unitPreferences.temperature),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Termostato",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(
            onClick = {
                onUpdate { it.copy(setpointCelsius = (it.setpointCelsius + 1).coerceAtMost(26)) }
            },
            enabled = state.setpointCelsius < AcUsage.SIMULATOR_MAX_CELSIUS,
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Subir un grado")
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricTile(
            label = "Consumo",
            value = state.monthlyKwh?.let(AppFormat::kwh) ?: "—",
            modifier = Modifier.weight(1f),
        )
        MetricTile(
            label = "En tu factura",
            value = state.monthlyCost?.let(AppFormat::balboas) ?: "—",
            modifier = Modifier.weight(1f),
            highlight = true,
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Filtros limpios", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Un filtro sucio sube el consumo cerca de 12 %.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = state.cleanFilters,
            onCheckedChange = { checked -> onUpdate { it.copy(cleanFilters = checked) } },
        )
    }

    Text(text = "Franja horaria", style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AcSchedule.entries.forEach { schedule ->
            FilterChip(
                selected = state.schedule == schedule,
                onClick = { onUpdate { it.copy(schedule = schedule) } },
                label = { Text(schedule.label) },
            )
        }
    }

    Text(
        text = "Horas de uso: ${state.hoursPerDay} h/día",
        style = MaterialTheme.typography.titleMedium,
    )
    Slider(
        value = state.hoursPerDay.toFloat(),
        onValueChange = { value -> onUpdate { it.copy(hoursPerDay = value.roundToInt()) } },
        valueRange = 1f..24f,
        steps = 22,
    )
    if (state.monthlyKwh == null) {
        Text(
            text = "Completa la etiqueta para ver el consumo en vivo.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FaceToFace(
    state: CalculateUiState,
    onUpdate: ((CalculateUiState) -> CalculateUiState) -> Unit,
) {
    Text(text = "Equipo actual", style = MaterialTheme.typography.titleMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        NumberField(
            label = "BTU/h actual",
            value = state.currentBtuInput,
            onValueChange = { value -> onUpdate { it.copy(currentBtuInput = value) } },
            modifier = Modifier.weight(1f),
        )
        NumberField(
            label = "Potencia W",
            value = state.currentWattsInput,
            onValueChange = { value -> onUpdate { it.copy(currentWattsInput = value) } },
            modifier = Modifier.weight(1f),
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NumberField(
            label = "SEER actual",
            value = state.currentSeerInput,
            onValueChange = { value -> onUpdate { it.copy(currentSeerInput = value) } },
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = state.currentInverter,
            onCheckedChange = { checked -> onUpdate { it.copy(currentInverter = checked) } },
        )
        Text(text = "Inverter", style = MaterialTheme.typography.bodyMedium)
    }
    NumberField(
        label = "Inversión del equipo nuevo (B/.)",
        value = state.investmentInput,
        onValueChange = { value -> onUpdate { it.copy(investmentInput = value) } },
        modifier = Modifier.fillMaxWidth(),
    )

    val comparison = state.comparison
    if (comparison == null) {
        Text(
            text = "Completa la etiqueta, el equipo actual y una tarifa válida para comparar.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricTile(
            label = "Equipo actual",
            value = AppFormat.kwh(comparison.currentMonthlyKwh),
            modifier = Modifier.weight(1f),
        )
        MetricTile(
            label = "Equipo nuevo",
            value = AppFormat.kwh(comparison.newMonthlyKwh),
            modifier = Modifier.weight(1f),
            highlight = true,
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricTile(
            label = "Ahorro mensual",
            value = AppFormat.balboas(comparison.monthlySavingsBalboas),
            modifier = Modifier.weight(1f),
            highlight = comparison.monthlySavingsBalboas > 0.0,
        )
        MetricTile(
            label = "Retorno",
            value = comparison.paybackMonths?.let { "${AppFormat.months(it)}" } ?: "No se recupera",
            modifier = Modifier.weight(1f),
        )
    }
    if (comparison.monthlySavingsBalboas <= 0.0) {
        Text(
            text = "Con estos datos el equipo nuevo no ahorra: revisa la potencia, el SEER o la inversión.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun SaveAndShareCard(
    state: CalculateUiState,
    isExporting: Boolean,
    exportError: String?,
    onSave: () -> Unit,
    onSharePdf: () -> Unit,
    onSharePng: () -> Unit,
) {
    SectionCard(
        title = "Guardar y compartir",
        subtitle = "El equipo queda en Mi Casa y la ficha sale lista para WhatsApp.",
    ) {
        Button(
            onClick = onSave,
            enabled = state.canSave && !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Icon(Icons.Rounded.Save, contentDescription = null)
            }
            Text(
                text = if (state.isSaving) "Guardando…" else "Guardar en Mi Casa",
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onSharePdf,
                enabled = state.canSave && !isExporting,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Rounded.PictureAsPdf, contentDescription = null)
                Text(text = "PDF", modifier = Modifier.padding(start = 6.dp))
            }
            OutlinedButton(
                onClick = onSharePng,
                enabled = state.canSave && !isExporting,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Rounded.Image, contentDescription = null)
                Text(text = "PNG", modifier = Modifier.padding(start = 6.dp))
            }
        }
        if (!state.canSave) {
            Text(
                text = "Completa la etiqueta con BTU/h y potencia para guardar o compartir.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        exportError?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun MetricTile(
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
            style = MaterialTheme.typography.titleLarge,
            color = if (highlight) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue -> if (newValue.length <= 12) onValueChange(newValue) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

private fun CalculateUiState.toReportData(): ReportData? {
    val spec = spec ?: return null
    val kwh = monthlyKwh ?: return null
    val cost = monthlyCost ?: return null
    val electrical = electrical ?: return null

    return ReportData(
        spaceName = spaceName.trim().ifBlank { "Mi espacio" },
        brandModel = brandModel.trim().ifBlank { "Sin marca/modelo" },
        btu = spec.btuPerHour.roundToInt(),
        seer = spec.seer,
        inverter = spec.inverter,
        voltage = electrical.voltage,
        monthlyKwh = kwh,
        monthlyBalboas = cost,
        tariffLabel = tariffLabel,
        recommendationText = reportNarrative(),
        electrical = electrical,
        monthlySavingsBalboas = comparison?.monthlySavingsBalboas?.takeIf { it > 0.0 },
    )
}

@Preview(name = "Calcular claro", showBackground = true, heightDp = 2200)
@Preview(
    name = "Calcular oscuro",
    showBackground = true,
    heightDp = 2200,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun CalculateScreenPreview() {
    ConsumoEnergeticoTheme {
        CalculateScreen(
            state = CalculateUiState(),
            onUpdate = {},
            onSave = {},
            onDismissSaveResult = {},
            onScanApplied = {},
        )
    }
}
