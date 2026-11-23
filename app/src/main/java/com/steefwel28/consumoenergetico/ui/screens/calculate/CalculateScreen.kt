@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.steefwel28.consumoenergetico.ui.screens.calculate

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.steefwel28.consumoenergetico.domain.calculator.TariffComparison
import com.steefwel28.consumoenergetico.domain.calculator.TariffEstimate
import com.steefwel28.consumoenergetico.domain.calculator.utilityOrNull
import com.steefwel28.consumoenergetico.domain.scanner.RtcaLabelReading
import com.steefwel28.consumoenergetico.domain.units.LocaleNumber
import com.steefwel28.consumoenergetico.ui.AppViewModelProvider
import com.steefwel28.consumoenergetico.ui.components.AppFormat
import com.steefwel28.consumoenergetico.ui.components.PixelDialog
import com.steefwel28.consumoenergetico.ui.components.PixelModalBottomSheet
import com.steefwel28.consumoenergetico.ui.components.PixelSpringExpressive
import com.steefwel28.consumoenergetico.ui.components.PixelSpringSnappy
import com.steefwel28.consumoenergetico.ui.components.RtcaScannerSheet
import com.steefwel28.consumoenergetico.ui.components.SectionCard
import com.steefwel28.consumoenergetico.ui.components.rememberPixelHaptics
import com.steefwel28.consumoenergetico.ui.theme.ConsumoEnergeticoTheme
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    onSave: (SaveRequest) -> Unit,
    onDismissSaveResult: () -> Unit,
    onScanApplied: (RtcaLabelReading) -> Unit,
) {
    var showScanner by remember { mutableStateOf(false) }
    var showBillGuide by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var exportError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberPixelHaptics()

    val share: (Boolean) -> Unit = { asPng ->
        val report = state.toReportData()
        if (report == null) {
            exportError = "Completa la etiqueta y la tarifa para generar la ficha."
        } else if (!isExporting) {
            haptics.confirm()
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
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Calcular", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Elige la categoría, compara ENSA y Naturgy y guarda tu equipo por propiedad.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            CategorySelector(
                selected = state.category,
                onSelect = { category ->
                    onUpdate { it.copy(category = category) }
                },
            )
        }

        if (state.category == EquipmentCategory.AIR_CONDITIONER) {
            item {
                ComparisonCard(
                    state = state,
                    onUpdate = onUpdate,
                    onOpenBillGuide = { showBillGuide = true },
                )
            }
            item {
                RtcaConverterCard(
                    state = state,
                    onUpdate = onUpdate,
                    onOpenScanner = { showScanner = true },
                )
            }
            item {
                SaveAndShareCard(
                    state = state,
                    isExporting = isExporting,
                    exportError = exportError,
                    onOpenSave = { showSaveDialog = true },
                    onOpenShare = { showShareSheet = true },
                )
            }
            item {
                RemoteSimulatorCard(state = state, onUpdate = onUpdate)
            }
        } else {
            item { ComingSoonCard(state.category) }
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
    if (showSaveDialog) {
        SaveToProfileDialog(
            state = state,
            onDismiss = { showSaveDialog = false },
            onSave = { request ->
                showSaveDialog = false
                onSave(request)
            },
        )
    }
    if (showShareSheet) {
        ShareSheet(
            chartMode = state.chartMode,
            isExporting = isExporting,
            exportError = exportError,
            onDismiss = { showShareSheet = false },
            onSharePdf = {
                showShareSheet = false
                share(false)
            },
            onSharePng = {
                showShareSheet = false
                share(true)
            },
        )
    }

    state.savedMessage?.let { message ->
        PixelDialog(
            title = "Guardado en tu perfil",
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
private fun CategorySelector(
    selected: EquipmentCategory,
    onSelect: (EquipmentCategory) -> Unit,
) {
    val haptics = rememberPixelHaptics()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EquipmentCategory.entries.forEach { category ->
            val isSelected = category == selected
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0.96f,
                animationSpec = PixelSpringSnappy,
                label = "category-${category.name}",
            )
            FilterChip(
                selected = isSelected,
                onClick = {
                    haptics.tap()
                    onSelect(category)
                },
                label = { Text(category.label) },
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            )
        }
    }
}

@Composable
private fun ComparisonCard(
    state: CalculateUiState,
    onUpdate: ((CalculateUiState) -> CalculateUiState) -> Unit,
    onOpenBillGuide: () -> Unit,
) {
    val haptics = rememberPixelHaptics()
    SectionCard(
        title = "Comparativa de tarifa",
        subtitle = "ENSA, Naturgy o el duelo VS con la diferencia exacta al mes.",
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TariffComparison.entries.forEach { mode ->
                val isSelected = state.chartMode == mode
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.96f,
                    animationSpec = PixelSpringSnappy,
                    label = "tariff-${mode.name}",
                )
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        haptics.tap()
                        onUpdate { current ->
                            current.copy(
                                chartMode = mode,
                                utility = mode.utilityOrNull() ?: current.utility,
                            )
                        }
                    },
                    label = { Text(mode.label) },
                    modifier = Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                )
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
            IconButton(onClick = {
                haptics.tap()
                onOpenBillGuide()
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                    contentDescription = "Guía para ubicar consumo y total en el recibo",
                )
            }
            Switch(
                checked = state.manualCost,
                onCheckedChange = { checked ->
                    haptics.tap()
                    onUpdate { it.copy(manualCost = checked) }
                },
            )
        }

        if (state.manualCost) {
            ManualCostFields(state = state, onUpdate = onUpdate)
        } else {
            Text(
                text = "Consumo base del hogar: ${AppFormat.kwh(state.baseMonthlyKwh)} (se configura en Ajustes).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        when (state.chartMode) {
            TariffComparison.VS -> VsChart(state = state)
            else -> SingleChart(state = state)
        }
    }
}

@Composable
private fun ManualCostFields(
    state: CalculateUiState,
    onUpdate: ((CalculateUiState) -> CalculateUiState) -> Unit,
) {
    val haptics = rememberPixelHaptics()
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        ManualCostMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = state.manualMode == mode,
                onClick = {
                    haptics.tap()
                    onUpdate { it.copy(manualMode = mode) }
                },
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

        ManualCostMode.RECEIPT -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
}

@Composable
private fun SingleChart(state: CalculateUiState) {
    val estimate = state.chartEstimate
    if (estimate == null) {
        MissingChartMessage()
        return
    }
    val color = when (state.chartMode) {
        TariffComparison.NATURGY -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    PixelBar(
        label = state.chartMode.label,
        value = estimate.addedCostBalboas,
        maxValue = estimate.addedCostBalboas.coerceAtLeast(0.01),
        color = color,
    )
    Text(
        text = "Tarifa marginal ${AppFormat.balboas(estimate.marginalBalboasPerKwh)}/kWh · " +
            "${AppFormat.kwh(estimate.addedKwh)} del equipo al mes.",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun VsChart(state: CalculateUiState) {
    val ensa = state.ensaEstimate ?: run {
        MissingChartMessage()
        return
    }
    val naturgy = state.naturgyEstimate ?: run {
        MissingChartMessage()
        return
    }
    val maxValue = max(ensa.addedCostBalboas, naturgy.addedCostBalboas).coerceAtLeast(0.01)
    PixelBar(
        label = "ENSA",
        value = ensa.addedCostBalboas,
        maxValue = maxValue,
        color = MaterialTheme.colorScheme.primary,
    )
    PixelBar(
        label = "Naturgy",
        value = naturgy.addedCostBalboas,
        maxValue = maxValue,
        color = MaterialTheme.colorScheme.tertiary,
    )
    state.comparisonDifference?.let { difference ->
        val cheaper = if (ensa.addedCostBalboas <= naturgy.addedCostBalboas) "ENSA" else "Naturgy"
        Text(
            text = if (difference < 0.005) {
                "Diferencia exacta: B/. 0.00 al mes · ambas distribuidoras empatan."
            } else {
                "Diferencia exacta: ${AppFormat.balboas(difference)} al mes · $cheaper sale más barata."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    if (state.manualCost) {
        Text(
            text = "El duelo VS usa los pliegos de ENSA y Naturgy; el Costo Manual aplica al modo simple.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MissingChartMessage() {
    Text(
        text = "Completa BTU/h y potencia nominal mayores que cero para ver la comparativa.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun PixelBar(
    label: String,
    value: Double,
    maxValue: Double,
    color: Color,
) {
    val target = (value / maxValue.coerceAtLeast(0.01)).toFloat().coerceIn(0f, 1f)
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = PixelSpringExpressive,
        label = "bar-$label",
    )
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, style = MaterialTheme.typography.titleMedium)
            Text(
                text = AppFormat.balboas(value),
                style = MaterialTheme.typography.titleMedium,
                color = color,
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp),
        ) {
            val radius = CornerRadius(size.height / 2f)
            drawRoundRect(color = track, cornerRadius = radius, size = size)
            if (progress > 0.001f) {
                drawRoundRect(
                    color = color,
                    cornerRadius = radius,
                    size = Size(size.width * progress, size.height),
                )
            }
        }
    }
}

@Composable
private fun RtcaConverterCard(
    state: CalculateUiState,
    onUpdate: ((CalculateUiState) -> CalculateUiState) -> Unit,
    onOpenScanner: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(0) }
    val haptics = rememberPixelHaptics()

    SectionCard(
        title = "Datos del equipo",
        subtitle = "Etiqueta RTCA y conversor de placa en una sola tarjeta.",
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Etiqueta RTCA", "Conversor de placa").forEachIndexed { index, label ->
                val isSelected = selectedTab == index
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.96f,
                    animationSpec = PixelSpringSnappy,
                    label = "card-tab-$index",
                )
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        haptics.tap()
                        selectedTab = index
                    },
                    label = { Text(label) },
                    modifier = Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                )
            }
        }
        if (selectedTab == 0) {
            EquipmentFields(
                state = state,
                onUpdate = onUpdate,
                onOpenScanner = onOpenScanner,
            )
        } else {
            ConverterFields(state = state, onUpdate = onUpdate)
        }
    }
}

@Composable
private fun EquipmentFields(
    state: CalculateUiState,
    onUpdate: ((CalculateUiState) -> CalculateUiState) -> Unit,
    onOpenScanner: () -> Unit,
) {
    val haptics = rememberPixelHaptics()
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
        placeholder = { Text("Hisense ATR362MA") },
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
            onCheckedChange = { checked ->
                haptics.tap()
                onUpdate { it.copy(inverter = checked) }
            },
        )
    }
    FilledTonalButton(
        onClick = {
            haptics.tap()
            onOpenScanner()
        },
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

@Composable
private fun ConverterFields(
    state: CalculateUiState,
    onUpdate: ((CalculateUiState) -> CalculateUiState) -> Unit,
) {
    val haptics = rememberPixelHaptics()
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        PlateConverterMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = state.converterMode == mode,
                onClick = {
                    haptics.tap()
                    onUpdate { it.copy(converterMode = mode) }
                },
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
                    onClick = {
                        haptics.tap()
                        onUpdate { it.copy(converterVoltage = 110) }
                    },
                    label = { Text("110 V") },
                )
                FilterChip(
                    selected = state.converterVoltage == 220,
                    onClick = {
                        haptics.tap()
                        onUpdate { it.copy(converterVoltage = 220) }
                    },
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
            haptics.tap()
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

@Composable
private fun SaveAndShareCard(
    state: CalculateUiState,
    isExporting: Boolean,
    exportError: String?,
    onOpenSave: () -> Unit,
    onOpenShare: () -> Unit,
) {
    val haptics = rememberPixelHaptics()
    SectionCard(
        title = "Guardar y compartir",
        subtitle = "Guarda en tu grupo y comparte la ficha en PDF o PNG.",
    ) {
        Button(
            onClick = {
                haptics.confirm()
                onOpenSave()
            },
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
                text = if (state.isSaving) "Guardando…" else "Guardar en Perfil",
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        FilledTonalButton(
            onClick = {
                haptics.tap()
                onOpenShare()
            },
            enabled = state.canSave && !isExporting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isExporting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            } else {
                Icon(Icons.Rounded.Share, contentDescription = null)
            }
            Text(
                text = if (isExporting) "Generando ficha…" else "Compartir ficha",
                modifier = Modifier.padding(start = 8.dp),
            )
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
private fun RemoteSimulatorCard(
    state: CalculateUiState,
    onUpdate: ((CalculateUiState) -> CalculateUiState) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val haptics = rememberPixelHaptics()
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = PixelSpringSnappy,
        label = "simulator-chevron",
    )

    SectionCard(
        title = "Simulador de Control Remoto",
        subtitle = "Prueba 18 °C–26 °C sin alterar la comparativa principal.",
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable {
                    haptics.tap()
                    expanded = !expanded
                }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (expanded) "Ocultar simulación" else "Abrir simulación",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = null,
                modifier = Modifier.graphicsLayer { rotationZ = rotation },
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = 0.68f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            ) + fadeIn(),
            exit = shrinkVertically(
                animationSpec = spring(
                    dampingRatio = 0.78f,
                    stiffness = Spring.StiffnessMedium,
                ),
            ) + fadeOut(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SimulatorContent(state = state, onUpdate = onUpdate)
            }
        }
    }
}

@Composable
private fun SimulatorContent(
    state: CalculateUiState,
    onUpdate: ((CalculateUiState) -> CalculateUiState) -> Unit,
) {
    var selectedTab by remember { mutableStateOf(0) }
    val haptics = rememberPixelHaptics()

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        listOf("Control remoto", "Cara a cara").forEachIndexed { index, label ->
            SegmentedButton(
                selected = selectedTab == index,
                onClick = {
                    haptics.tap()
                    selectedTab = index
                },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
            ) {
                Text(label)
            }
        }
    }
    if (selectedTab == 0) {
        RemoteControl(state = state, onUpdate = onUpdate)
    } else {
        FaceToFace(state = state, onUpdate = onUpdate)
    }
}

@Composable
private fun RemoteControl(
    state: CalculateUiState,
    onUpdate: ((CalculateUiState) -> CalculateUiState) -> Unit,
) {
    val haptics = rememberPixelHaptics()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(
            onClick = {
                haptics.tap()
                onUpdate {
                    it.copy(
                        simulatedSetpointCelsius =
                            (it.simulatedSetpointCelsius - 1).coerceAtLeast(AcUsage.SIMULATOR_MIN_CELSIUS),
                    )
                }
            },
            enabled = state.simulatedSetpointCelsius > AcUsage.SIMULATOR_MIN_CELSIUS,
        ) {
            Icon(Icons.Rounded.Remove, contentDescription = "Bajar un grado")
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = AppFormat.temperature(
                    state.simulatedSetpointCelsius,
                    state.unitPreferences.temperature,
                ),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Termostato simulado",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(
            onClick = {
                haptics.tap()
                onUpdate {
                    it.copy(
                        simulatedSetpointCelsius =
                            (it.simulatedSetpointCelsius + 1).coerceAtMost(AcUsage.SIMULATOR_MAX_CELSIUS),
                    )
                }
            },
            enabled = state.simulatedSetpointCelsius < AcUsage.SIMULATOR_MAX_CELSIUS,
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Subir un grado")
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricTile(
            label = "Consumo simulado",
            value = state.simulatedMonthlyKwh?.let(AppFormat::kwh) ?: "—",
            modifier = Modifier.weight(1f),
        )
        MetricTile(
            label = "En tu factura",
            value = state.simulatedMonthlyCost?.let(AppFormat::balboas) ?: "—",
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
            checked = state.simulatedCleanFilters,
            onCheckedChange = { checked ->
                haptics.tap()
                onUpdate { it.copy(simulatedCleanFilters = checked) }
            },
        )
    }

    Text(text = "Franja horaria", style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AcSchedule.entries.forEach { schedule ->
            FilterChip(
                selected = state.simulatedSchedule == schedule,
                onClick = {
                    haptics.tap()
                    onUpdate { it.copy(simulatedSchedule = schedule) }
                },
                label = { Text(schedule.label) },
            )
        }
    }

    Text(
        text = "Horas de uso: ${state.simulatedHoursPerDay} h/día",
        style = MaterialTheme.typography.titleMedium,
    )
    Slider(
        value = state.simulatedHoursPerDay.toFloat(),
        onValueChange = { value ->
            val hours = value.roundToInt()
            if (hours != state.simulatedHoursPerDay) haptics.tap()
            onUpdate { it.copy(simulatedHoursPerDay = hours) }
        },
        valueRange = 1f..24f,
        steps = 22,
    )
    Text(
        text = "Esta simulación vive aparte: la gráfica superior conserva su consumo base.",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (state.simulatedMonthlyKwh == null) {
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
    val haptics = rememberPixelHaptics()
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
            onCheckedChange = { checked ->
                haptics.tap()
                onUpdate { it.copy(currentInverter = checked) }
            },
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
            value = comparison.paybackMonths?.let { AppFormat.months(it) } ?: "No se recupera",
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
private fun ComingSoonCard(category: EquipmentCategory) {
    SectionCard(
        title = category.label,
        subtitle = "Este módulo llega en la siguiente fase.",
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Construction,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = "Próximamente", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Estamos calibrando el cálculo para ${category.label.lowercase()} en Panamá.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ShareSheet(
    chartMode: TariffComparison,
    isExporting: Boolean,
    exportError: String?,
    onDismiss: () -> Unit,
    onSharePdf: () -> Unit,
    onSharePng: () -> Unit,
) {
    val haptics = rememberPixelHaptics()
    PixelModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "Compartir ficha", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "La lámina respeta el modo activo: ${chartMode.label}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(
                    onClick = {
                        haptics.confirm()
                        onSharePdf()
                    },
                    enabled = !isExporting,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null)
                    Text(text = "PDF", modifier = Modifier.padding(start = 6.dp))
                }
                FilledTonalButton(
                    onClick = {
                        haptics.confirm()
                        onSharePng()
                    },
                    enabled = !isExporting,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.Image, contentDescription = null)
                    Text(text = "PNG", modifier = Modifier.padding(start = 6.dp))
                }
            }
            if (isExporting) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(text = "Generando la lámina…", style = MaterialTheme.typography.labelMedium)
                }
            }
            exportError?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun SaveToProfileDialog(
    state: CalculateUiState,
    onDismiss: () -> Unit,
    onSave: (SaveRequest) -> Unit,
) {
    val haptics = rememberPixelHaptics()
    val firstGroupId = state.selectedGroupId ?: state.groups.firstOrNull()?.id
    var selectedGroupId by remember { mutableStateOf(firstGroupId) }
    var creatingGroup by remember { mutableStateOf(firstGroupId == null) }
    var newGroupName by remember {
        mutableStateOf(if (state.groups.isEmpty()) "Mi Casa" else "")
    }
    var newGroupUtility by remember { mutableStateOf(state.utility) }
    var newGroupBase by remember {
        mutableStateOf(
            if (state.baseMonthlyKwh > 0.0) AppFormat.kwhNumber(state.baseMonthlyKwh) else "",
        )
    }
    val knownLocations = CalculateLocations.base + state.customLocations.map { it.nombreUbicacion }
    var selectedLocation by remember {
        mutableStateOf(
            state.selectedLocation.takeIf { it.isNotBlank() } ?: CalculateLocations.DEFAULT,
        )
    }
    var useOtherLocation by remember { mutableStateOf(false) }
    var otherLocation by remember { mutableStateOf("") }
    var brandModel by remember { mutableStateOf(state.brandModel) }

    val location = if (useOtherLocation) otherLocation.trim() else selectedLocation
    val groupValid = if (creatingGroup) {
        newGroupName.isNotBlank()
    } else {
        selectedGroupId != null
    }
    val valid = groupValid && location.isNotBlank()
    val selectedGroup = state.groups.firstOrNull { it.id == selectedGroupId }

    PixelDialog(
        title = "Guardar en Perfil",
        confirmLabel = "Guardar",
        confirmEnabled = valid,
        onConfirm = {
            haptics.confirm()
            onSave(
                SaveRequest(
                    groupId = selectedGroupId.takeUnless { creatingGroup },
                    newGroupName = newGroupName,
                    groupUtility = newGroupUtility,
                    groupBaseKwh = LocaleNumber.parse(newGroupBase) ?: 0.0,
                    location = location.ifBlank { CalculateLocations.DEFAULT },
                    brandModel = brandModel,
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
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    state.groups.forEach { group ->
                        FilterChip(
                            selected = !creatingGroup && selectedGroupId == group.id,
                            onClick = {
                                haptics.tap()
                                creatingGroup = false
                                selectedGroupId = group.id
                            },
                            label = { Text(group.nombreGrupo) },
                        )
                    }
                    FilterChip(
                        selected = creatingGroup,
                        onClick = {
                            haptics.tap()
                            creatingGroup = true
                        },
                        label = { Text("Nuevo grupo") },
                    )
                }

                if (creatingGroup) {
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        label = { Text("Nombre del grupo") },
                        placeholder = { Text("Mi Casa, Trabajo, Casa de Mamá") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        PanamaUtility.entries.forEachIndexed { index, utility ->
                            SegmentedButton(
                                selected = newGroupUtility == utility,
                                onClick = {
                                    haptics.tap()
                                    newGroupUtility = utility
                                },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = PanamaUtility.entries.size,
                                ),
                            ) {
                                Text(utility.displayName)
                            }
                        }
                    }
                    NumberField(
                        label = "Consumo base del recibo (kWh/mes)",
                        value = newGroupBase,
                        onValueChange = { newGroupBase = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    selectedGroup?.let { group ->
                        Text(
                            text = "${group.distribuidora.displayName} · " +
                                "base ${AppFormat.kwh(group.consumoBaseKwh)} al mes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Text(text = "Ubicación", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    (knownLocations + CalculateLocations.OTHER).distinct().forEach { name ->
                        val isOther = name == CalculateLocations.OTHER
                        FilterChip(
                            selected = if (isOther) {
                                useOtherLocation
                            } else {
                                !useOtherLocation && selectedLocation == name
                            },
                            onClick = {
                                haptics.tap()
                                if (isOther) {
                                    useOtherLocation = true
                                } else {
                                    useOtherLocation = false
                                    selectedLocation = name
                                }
                            },
                            label = { Text(name) },
                        )
                    }
                }
                if (useOtherLocation) {
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
                    value = brandModel,
                    onValueChange = { brandModel = it },
                    label = { Text("Marca y modelo") },
                    placeholder = { Text("Hisense ATR362MA") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
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
    val electrical = electrical ?: return null
    val ensaCost = ensaEstimate?.addedCostBalboas ?: return null
    val naturgyCost = naturgyEstimate?.addedCostBalboas ?: return null
    val displayCost = primaryMonthlyCost ?: minOf(ensaCost, naturgyCost)

    return ReportData(
        spaceName = spaceName.trim().ifBlank { "Mi espacio" },
        brandModel = brandModel.trim().ifBlank { "Sin marca/modelo" },
        btu = spec.btuPerHour.roundToInt(),
        seer = spec.seer,
        inverter = spec.inverter,
        voltage = electrical.voltage,
        monthlyKwh = kwh,
        comparison = chartMode,
        ensaMonthlyBalboas = ensaCost,
        naturgyMonthlyBalboas = naturgyCost,
        displayMonthlyBalboas = displayCost,
        tariffLabel = if (manualCost) "$reportTariffLabel · Costo Manual" else reportTariffLabel,
        recommendationText = reportNarrative(),
        electrical = electrical,
        monthlySavingsBalboas = comparison?.monthlySavingsBalboas?.takeIf { it > 0.0 },
    )
}

@Preview(name = "Calcular claro", showBackground = true, heightDp = 2600)
@Preview(
    name = "Calcular oscuro",
    showBackground = true,
    heightDp = 2600,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun CalculateScreenPreview() {
    ConsumoEnergeticoTheme {
        CalculateScreen(
            state = CalculateUiState(),
            onUpdate = {},
            onSave = { _ -> },
            onDismissSaveResult = {},
            onScanApplied = {},
        )
    }
}
