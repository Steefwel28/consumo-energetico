@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.steefwel28.consumoenergetico.ui.screens.calculate

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.steefwel28.consumoenergetico.R
import com.steefwel28.consumoenergetico.core.di.AppContainer
import com.steefwel28.consumoenergetico.data.export.ReportData
import com.steefwel28.consumoenergetico.data.export.ReportExporter
import com.steefwel28.consumoenergetico.domain.calculator.AcSchedule
import com.steefwel28.consumoenergetico.domain.calculator.AcUsage
import com.steefwel28.consumoenergetico.domain.calculator.PanamaUtility
import com.steefwel28.consumoenergetico.domain.calculator.TariffComparison
import com.steefwel28.consumoenergetico.domain.calculator.TariffEstimate
import com.steefwel28.consumoenergetico.domain.calculator.tariffComparison
import com.steefwel28.consumoenergetico.domain.units.LocaleNumber
import com.steefwel28.consumoenergetico.ui.AppViewModelProvider
import com.steefwel28.consumoenergetico.ui.components.AppFormat
import com.steefwel28.consumoenergetico.ui.components.InfoHint
import com.steefwel28.consumoenergetico.ui.components.NameplateGuideDialog
import com.steefwel28.consumoenergetico.ui.components.PixelDialog
import com.steefwel28.consumoenergetico.ui.components.PixelModalBottomSheet
import com.steefwel28.consumoenergetico.ui.components.PixelSpringSnappy
import com.steefwel28.consumoenergetico.ui.components.RtcaLabelGuideDialog
import com.steefwel28.consumoenergetico.ui.components.SectionCard
import com.steefwel28.consumoenergetico.ui.components.labelRes
import com.steefwel28.consumoenergetico.ui.components.pixelChargeHaptics
import com.steefwel28.consumoenergetico.ui.components.rememberPixelHaptics
import com.steefwel28.consumoenergetico.ui.illustrations.EducationalSketchCarousel
import com.steefwel28.consumoenergetico.ui.theme.ConsumoEnergeticoTheme
import com.steefwel28.consumoenergetico.ui.theme.LocalExtendedColors
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
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
        onSetBaseKwh = viewModel::setBaseMonthlyKwh,
        onSave = viewModel::save,
        onDismissSaveResult = viewModel::dismissSaveResult,
    )
}

@Composable
fun CalculateScreen(
    state: CalculateUiState,
    onUpdate: ((CalculateUiState) -> CalculateUiState) -> Unit,
    onSetBaseKwh: (Double) -> Unit,
    onSave: (SaveRequest) -> Unit,
    onDismissSaveResult: () -> Unit,
) {
    var showBillGuide by rememberSaveable { mutableStateOf(false) }
    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    var showShareSheet by rememberSaveable { mutableStateOf(false) }
    var isExporting by rememberSaveable { mutableStateOf(false) }
    var exportError by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberPixelHaptics()
    val listState = rememberLazyListState()

    val share: (Boolean) -> Unit = { asPng ->
        val report = state.toReportData(context)
        if (report == null) {
            exportError = context.getString(R.string.calc_export_incomplete)
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
                        context.getString(R.string.calc_share),
                    )
                    context.startActivity(chooser)
                }.onFailure { error ->
                    exportError = error.message ?: context.getString(R.string.calc_export_error)
                }
                isExporting = false
            }
        }
    }

    // La tarjeta de resultado vive fija arriba; la lista scrollea debajo.
    Column(modifier = Modifier.fillMaxWidth()) {
        ResultCard(
            state = state,
            onUpdate = onUpdate,
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = stringResource(R.string.calc_title), style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = stringResource(R.string.calc_subtitle),
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

        item {
            // Comparativa de las tres distribuidoras para el periodo activo.
            DistributorCompareCard(state = state)
        }
        item {
            BaseConsumptionCard(
                state = state,
                onUpdate = onUpdate,
                onSetBaseKwh = onSetBaseKwh,
            )
        }
        item {
            ResultExtras(
                state = state,
                onUpdate = onUpdate,
                onOpenSave = { showSaveDialog = true },
                onOpenBillGuide = { showBillGuide = true },
            )
        }
        item {
            RtcaConverterCard(
                state = state,
                onUpdate = onUpdate,
                onSeerDone = {
                    scope.launch { listState.animateScrollToItem(CHART_ITEM_INDEX) }
                },
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
            Text(
                text = stringResource(R.string.calc_footer),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        }
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
            title = stringResource(R.string.calc_saved_title),
            text = message,
            confirmLabel = stringResource(R.string.common_understood),
            onConfirm = onDismissSaveResult,
            onDismissRequest = onDismissSaveResult,
        )
    }
    state.saveError?.let { message ->
        PixelDialog(
            title = stringResource(R.string.calc_save_error_title),
            text = message,
            confirmLabel = stringResource(R.string.common_close),
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
        EquipmentCategory.available.forEach { category ->
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
                label = { Text(stringResource(category.labelRes)) },
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            )
        }
    }
}

/**
 * Cabecera anclada y compacta: distribuidora (ENSA / EDEMET / EDECHI, pliego
 * ASEP vigente), monto del periodo activo y selector segmentado Día /
 * Quincena / Mes. El consumo base y los ajustes viven en la lista.
 */
@Composable
private fun ResultCard(
    state: CalculateUiState,
    onUpdate: ((CalculateUiState) -> CalculateUiState) -> Unit,
) {
    val haptics = rememberPixelHaptics()
    val extended = LocalExtendedColors.current
    val cost = state.monthlyCost
    val brandColor = when (state.utility) {
        PanamaUtility.ENSA -> extended.ensa
        PanamaUtility.NATURGY_EDECHI -> extended.edechi
        else -> extended.edemet
    }
    val titleRes = when (state.usagePeriod) {
        UsagePeriod.DAY -> R.string.calc_result_title_day
        UsagePeriod.FORTNIGHT -> R.string.calc_result_title_fortnight
        UsagePeriod.MONTH -> R.string.calc_result_title
    }

    SectionCard(
        title = stringResource(titleRes),
        modifier = Modifier.padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PanamaUtility.entries.forEach { utility ->
                val isSelected = state.utility == utility
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.96f,
                    animationSpec = PixelSpringSnappy,
                    label = "utility-${utility.name}",
                )
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        haptics.tap()
                        onUpdate { it.copy(utility = utility, chartMode = utility.tariffComparison()) }
                    },
                    label = {
                        Text(
                            text = utility.shortName,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        },
                )
            }
        }

        Text(
            text = stringResource(
                R.string.calc_tariff_validity,
                state.tariffValidFrom,
                state.tariffValidTo,
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = state.tariffLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = cost?.let { AppFormat.balboas(it) } ?: "B/. —",
                    style = MaterialTheme.typography.headlineLarge,
                    color = cost?.let { brandColor } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${AppFormat.kwhNumber(state.periodKwh ?: 0.0)} ${periodKwhUnit(state.usagePeriod)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        PeriodSegmented(state = state, onUpdate = onUpdate)
    }
}

/** Unidad de kWh acorde al periodo activo (día / quincena / mes). */
@Composable
private fun periodKwhUnit(period: UsagePeriod): String = when (period) {
    UsagePeriod.DAY -> stringResource(R.string.common_kwh_day)
    UsagePeriod.FORTNIGHT -> stringResource(R.string.common_kwh_fortnight)
    UsagePeriod.MONTH -> stringResource(R.string.common_kwh_month)
}

/** Selector segmentado compacto Día | Quincena | Mes de la cabecera fija. */
@Composable
private fun PeriodSegmented(
    state: CalculateUiState,
    onUpdate: ((CalculateUiState) -> CalculateUiState) -> Unit,
) {
    val haptics = rememberPixelHaptics()
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        UsagePeriod.entries.forEachIndexed { index, period ->
            SegmentedButton(
                selected = state.usagePeriod == period,
                onClick = {
                    haptics.tap()
                    onUpdate { it.copy(usagePeriod = period) }
                },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = UsagePeriod.entries.size,
                ),
            ) {
                Text(
                    text = when (period) {
                        UsagePeriod.DAY -> stringResource(R.string.period_day)
                        UsagePeriod.FORTNIGHT -> stringResource(R.string.period_fortnight)
                        UsagePeriod.MONTH -> stringResource(R.string.period_month_short)
                    },
                    maxLines = 1,
                )
            }
        }
    }
}

/** Consumo base del recibo: chips rápidos + campo con ayuda (i). */
@Composable
private fun BaseConsumptionCard(
    state: CalculateUiState,
    onUpdate: ((CalculateUiState) -> CalculateUiState) -> Unit,
    onSetBaseKwh: (Double) -> Unit,
) {
    val haptics = rememberPixelHaptics()
    val baseInput = state.baseKwhInput.ifBlank {
        if (state.baseMonthlyKwh > 0.0) AppFormat.kwhNumber(state.baseMonthlyKwh) else ""
    }
    SectionCard(title = stringResource(R.string.calc_base_section_title)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                Triple(R.string.calc_base_low, "200", 200.0),
                Triple(R.string.calc_base_medium, "350", 350.0),
                Triple(R.string.calc_base_high, "750", 750.0),
            ).forEach { (labelRes, input, kwh) ->
                FilterChip(
                    selected = state.baseKwhInput == input,
                    onClick = {
                        haptics.tap()
                        onUpdate { it.copy(baseKwhInput = input) }
                        onSetBaseKwh(kwh)
                    },
                    label = {
                        Text(
                            text = stringResource(labelRes),
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        NumberField(
            label = stringResource(R.string.common_base_kwh),
            value = baseInput,
            onValueChange = { text ->
                onUpdate { it.copy(baseKwhInput = text) }
                LocaleNumber.parse(text)?.takeIf { kwh -> kwh >= 0.0 }?.let(onSetBaseKwh)
            },
            placeholder = "350",
            info = stringResource(R.string.calc_base_kwh_info),
        )
    }
}

/** Ajustes opcionales: consumo eléctrico (W), temperatura, horas y costo manual. */
@Composable
private fun ResultExtras(
    state: CalculateUiState,
    onUpdate: ((CalculateUiState) -> CalculateUiState) -> Unit,
    onOpenSave: () -> Unit,
    onOpenBillGuide: () -> Unit,
) {
    val haptics = rememberPixelHaptics()
    SectionCard(title = stringResource(R.string.calc_extras_title)) {
        NumberField(
            label = stringResource(R.string.calc_watts_label),
            value = state.wattsInput,
            onValueChange = { text -> onUpdate { it.copy(wattsInput = text) } },
            placeholder = "1150",
            info = stringResource(R.string.calc_watts_info),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.calc_setpoint, state.setpointCelsius),
                style = MaterialTheme.typography.titleSmall,
            )
            Slider(
                value = state.setpointCelsius.toFloat(),
                onValueChange = { value ->
                    val setpoint = value.roundToInt()
                    if (setpoint != state.setpointCelsius) haptics.tap()
                    onUpdate { it.copy(setpointCelsius = setpoint) }
                },
                valueRange = 16f..30f,
                steps = 13,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        HoursControl(state = state, onUpdate = onUpdate)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = state.manualCost,
                onClick = {
                    haptics.tap()
                    onUpdate { it.copy(manualCost = !it.manualCost) }
                },
                label = {
                    Text(
                        text = stringResource(R.string.calc_manual_chip),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.graphicsLayer {
                            rotationZ = if (state.manualCost) 180f else 0f
                        },
                    )
                },
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = {
                haptics.tap()
                onOpenBillGuide()
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                    contentDescription = stringResource(R.string.calc_bill_guide_cd),
                )
            }
        }
        if (state.manualCost) {
            ManualCostFields(state = state, onUpdate = onUpdate)
        }
        FilledTonalButton(
            onClick = {
                haptics.confirm()
                onOpenSave()
            },
            enabled = state.canSave,
            modifier = Modifier
                .fillMaxWidth()
                .pixelChargeHaptics(),
        ) {
            Icon(imageVector = Icons.Rounded.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.calc_save_profile))
        }
    }
}

/** Comparativa simultánea de las tres distribuidoras para el periodo activo. */
@Composable
private fun DistributorCompareCard(state: CalculateUiState) {
    val haptics = rememberPixelHaptics()
    val extended = LocalExtendedColors.current
    var open by rememberSaveable { mutableStateOf(false) }
    val chevron by animateFloatAsState(
        targetValue = if (open) 180f else 0f,
        animationSpec = PixelSpringSnappy,
        label = "compare-chevron",
    )
    val quotes = state.distributorQuotes
    val cheapest = quotes?.minByOrNull { it.costBalboas }
    val priciest = quotes?.maxByOrNull { it.costBalboas }

    SectionCard(
        title = stringResource(R.string.calc_compare_title),
        subtitle = periodLabelText(LocalContext.current, state.usagePeriod, state.daysInMonth),
    ) {
        FilledTonalButton(
            onClick = {
                haptics.tap()
                open = !open
            },
            modifier = Modifier
                .fillMaxWidth()
                .pixelChargeHaptics(),
        ) {
            Icon(imageVector = Icons.AutoMirrored.Rounded.CompareArrows, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.calc_compare_button),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = null,
                modifier = Modifier.graphicsLayer { rotationZ = chevron },
            )
        }
        AnimatedVisibility(visible = open) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (quotes == null) {
                    Text(
                        text = stringResource(R.string.calc_compare_empty),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    quotes.forEach { quote ->
                        val color = when (quote.utility) {
                            PanamaUtility.ENSA -> extended.ensa
                            PanamaUtility.NATURGY -> extended.edemet
                            PanamaUtility.NATURGY_EDECHI -> extended.edechi
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(color)
                            )
                            Text(
                                text = quote.utility.shortName,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f),
                            )
                            if (cheapest != null && quote.utility == cheapest.utility) {
                                Text(
                                    text = stringResource(R.string.calc_compare_cheapest),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Text(
                                text = AppFormat.balboas(quote.costBalboas),
                                style = MaterialTheme.typography.titleMedium,
                                color = color,
                            )
                        }
                    }
                    if (cheapest != null && priciest != null) {
                        Text(
                            text = stringResource(
                                R.string.calc_compare_diff,
                                AppFormat.balboas(priciest.costBalboas - cheapest.costBalboas),
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
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
                Text(stringResource(mode.labelRes))
            }
        }
    }
    when (state.manualMode) {
        ManualCostMode.DIRECT_RATE -> NumberField(
            label = stringResource(R.string.calc_direct_rate),
            value = state.directRateInput,
            onValueChange = { value -> onUpdate { it.copy(directRateInput = value) } },
            modifier = Modifier.fillMaxWidth(),
        )

        ManualCostMode.RECEIPT -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberField(
                label = stringResource(R.string.calc_receipt_total),
                value = state.receiptTotalInput,
                onValueChange = { value -> onUpdate { it.copy(receiptTotalInput = value) } },
                modifier = Modifier.weight(1f),
            )
            NumberField(
                label = stringResource(R.string.calc_receipt_kwh),
                value = state.receiptKwhInput,
                onValueChange = { value -> onUpdate { it.copy(receiptKwhInput = value) } },
                modifier = Modifier.weight(1f),
            )
        }
    }
    if (state.tariffMode == null) {
        Text(
            text = stringResource(R.string.calc_receipt_incomplete),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun HoursControl(
    state: CalculateUiState,
    onUpdate: ((CalculateUiState) -> CalculateUiState) -> Unit,
) {
    val haptics = rememberPixelHaptics()
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = stringResource(R.string.calculate_hours, state.hoursPerDay),
            style = MaterialTheme.typography.titleSmall,
        )
        Slider(
            value = state.hoursPerDay.toFloat(),
            onValueChange = { value ->
                val hours = value.roundToInt()
                if (hours != state.hoursPerDay) haptics.tap()
                onUpdate { it.copy(hoursPerDay = hours) }
            },
            valueRange = 1f..24f,
            steps = 22,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun periodLabelText(context: Context, period: UsagePeriod, daysInMonth: Int): String {
    val month = YearMonth.now().month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    return when (period) {
        UsagePeriod.DAY -> context.getString(R.string.period_day)
        UsagePeriod.FORTNIGHT -> context.getString(R.string.period_fortnight)
        UsagePeriod.MONTH -> context.getString(R.string.period_month, month, daysInMonth)
    }
}

@Composable
private fun RtcaConverterCard(
    state: CalculateUiState,
    onUpdate: ((CalculateUiState) -> CalculateUiState) -> Unit,
    onSeerDone: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showGuide by remember { mutableStateOf(false) }
    val haptics = rememberPixelHaptics()
    val isAc = state.category == EquipmentCategory.AIR_CONDITIONER

    SectionCard(
        title = stringResource(R.string.calc_equipment_title),
        subtitle = stringResource(R.string.calc_equipment_subtitle),
    ) {
        if (isAc) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    stringResource(R.string.calc_tab_label),
                    stringResource(R.string.calc_tab_converter),
                ).forEachIndexed { index, label ->
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
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = {
                        haptics.tap()
                        showGuide = true
                    },
                    modifier = Modifier.pixelChargeHaptics(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = stringResource(R.string.calc_guide_cd),
                    )
                }
            }
            if (showGuide) {
                if (selectedTab == 1) {
                    NameplateGuideDialog(onDismiss = { showGuide = false })
                } else {
                    RtcaLabelGuideDialog(onDismiss = { showGuide = false })
                }
            }
        }
        if (isAc && selectedTab == 1) {
            ConverterFields(state = state, onUpdate = onUpdate)
        } else {
            EquipmentFields(
                state = state,
                onUpdate = onUpdate,
                onSeerDone = onSeerDone,
            )
        }
    }
}

/** Etiquetas de ejemplo translúcidas según la categoría activa. */
private data class EquipmentPlaceholders(
    val brand: String,
    val btu: String,
    val watts: String,
    val seer: String,
)

private fun placeholdersFor(category: EquipmentCategory): EquipmentPlaceholders = when (category) {
    EquipmentCategory.AIR_CONDITIONER -> EquipmentPlaceholders(
        brand = "Hisense ATR122HA",
        btu = "12000",
        watts = "1134",
        seer = "21",
    )

    EquipmentCategory.REFRIGERATOR,
    EquipmentCategory.FREEZER,
    EquipmentCategory.APPLIANCES,
    -> EquipmentPlaceholders(
        brand = "Hisense RT-33",
        btu = "",
        watts = "180",
        seer = "",
    )
}

@Composable
private fun EquipmentFields(
    state: CalculateUiState,
    onUpdate: ((CalculateUiState) -> CalculateUiState) -> Unit,
    onSeerDone: () -> Unit,
) {
    val haptics = rememberPixelHaptics()
    val keyboard = LocalSoftwareKeyboardController.current
    val btuFocus = remember { FocusRequester() }
    val wattsFocus = remember { FocusRequester() }
    val seerFocus = remember { FocusRequester() }
    val hints = placeholdersFor(state.category)
    var identifyExpanded by remember { mutableStateOf(false) }
    val identifyRotation by animateFloatAsState(
        targetValue = if (identifyExpanded) 180f else 0f,
        animationSpec = PixelSpringSnappy,
        label = "identify-chevron",
    )

    if (state.category == EquipmentCategory.AIR_CONDITIONER) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            listOf(9_000, 12_000, 18_000, 24_000, 36_000).forEach { btu ->
                FilterChip(
                    selected = state.btu?.roundToInt() == btu,
                    onClick = {
                        haptics.tap()
                        onUpdate { it.copy(btuInput = btu.toString()) }
                    },
                    label = {
                        Text(if (btu % 1_000 == 0) "${btu / 1_000}k" else btu.toString())
                    },
                )
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        NumberField(
            label = stringResource(R.string.common_btu),
            value = state.btuInput,
            onValueChange = { value -> onUpdate { it.copy(btuInput = value) } },
            placeholder = hints.btu,
            imeAction = ImeAction.Next,
            onImeAction = { wattsFocus.requestFocus() },
            modifier = Modifier
                .weight(1f)
                .focusRequester(btuFocus),
        )
        NumberField(
            label = stringResource(R.string.common_watts),
            value = state.wattsInput,
            onValueChange = { value -> onUpdate { it.copy(wattsInput = value) } },
            placeholder = hints.watts,
            imeAction = ImeAction.Next,
            onImeAction = { seerFocus.requestFocus() },
            modifier = Modifier
                .weight(1f)
                .focusRequester(wattsFocus),
        )
        NumberField(
            label = stringResource(R.string.common_seer),
            value = state.seerInput,
            onValueChange = { value -> onUpdate { it.copy(seerInput = value) } },
            placeholder = hints.seer,
            imeAction = ImeAction.Done,
            onImeAction = {
                keyboard?.hide()
                onSeerDone()
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(seerFocus),
        )
    }
    if (state.wattsEstimatedBySeer) {
        Text(
            text = stringResource(R.string.calc_watts_estimated),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.inverter), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.calc_inverter_hint),
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

    // Espacio y marca quedan plegados: cotizar rápido sin ruido visual.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                haptics.tap()
                identifyExpanded = !identifyExpanded
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.calc_identify_equipment),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Rounded.ExpandMore,
            contentDescription = null,
            modifier = Modifier.graphicsLayer { rotationZ = identifyRotation },
        )
    }
    if (identifyExpanded) {
        OutlinedTextField(
            value = state.spaceName,
            onValueChange = { value -> onUpdate { it.copy(spaceName = value) } },
            label = { Text(stringResource(R.string.calc_space)) },
            placeholder = { Text(stringResource(R.string.calc_space_placeholder)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.brandModel,
            onValueChange = { value -> onUpdate { it.copy(brandModel = value) } },
            label = { Text(stringResource(R.string.common_brand_model)) },
            placeholder = { Text(hints.brand) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    if (state.spec == null) {
        Text(
            text = stringResource(R.string.calc_missing_spec),
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
            label = stringResource(R.string.calc_converter_value, state.converterMode.unit),
            value = state.converterInput,
            onValueChange = { value -> onUpdate { it.copy(converterInput = value) } },
            modifier = Modifier.weight(1f),
        )
    }
    state.converterBtu?.let { btu ->
        Text(
            text = stringResource(R.string.calc_converter_result, AppFormat.btu(btu)),
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
        Text(stringResource(R.string.calc_converter_use))
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
        title = stringResource(R.string.calc_save_share_title),
        subtitle = stringResource(R.string.calc_save_share_subtitle),
    ) {
        Button(
            onClick = {
                haptics.confirm()
                onOpenSave()
            },
            enabled = state.canSave && !state.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .pixelChargeHaptics(),
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
                text = if (state.isSaving) {
                    stringResource(R.string.calc_saving)
                } else {
                    stringResource(R.string.calc_save_profile)
                },
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        FilledTonalButton(
            onClick = {
                haptics.tap()
                onOpenShare()
            },
            enabled = state.canSave && !isExporting,
            modifier = Modifier
                .fillMaxWidth()
                .pixelChargeHaptics(),
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
                text = if (isExporting) {
                    stringResource(R.string.calc_generating)
                } else {
                    stringResource(R.string.calc_share)
                },
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        if (!state.canSave) {
            Text(
                text = stringResource(R.string.calc_save_hint),
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

/**
 * Hoja del simulador abierta desde el menú superior `☰`, fuera del scroll de
 * Calcular. Reutiliza el mismo estado para que el consumo en vivo responda.
 */
@Composable
fun RemoteSimulatorSheet(
    state: CalculateUiState,
    onUpdate: ((CalculateUiState) -> CalculateUiState) -> Unit,
    onDismiss: () -> Unit,
) {
    PixelModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.calc_simulator_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.calc_simulator_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SimulatorContent(state = state, onUpdate = onUpdate)
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
        listOf(
            stringResource(R.string.calc_sim_remote),
            stringResource(R.string.calc_sim_face),
        ).forEachIndexed { index, label ->
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
            Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.calc_sim_lower))
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
                text = stringResource(R.string.calc_sim_thermostat),
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
            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.calc_sim_raise))
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricTile(
            label = stringResource(R.string.calc_sim_consumption),
            value = state.simulatedMonthlyKwh?.let(AppFormat::kwh) ?: "—",
            modifier = Modifier.weight(1f),
        )
        MetricTile(
            label = stringResource(R.string.calc_sim_bill),
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
            Text(text = stringResource(R.string.calc_sim_filters), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.calc_sim_filters_hint),
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

    Text(text = stringResource(R.string.calc_sim_schedule), style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AcSchedule.entries.forEach { schedule ->
            FilterChip(
                selected = state.simulatedSchedule == schedule,
                onClick = {
                    haptics.tap()
                    onUpdate { it.copy(simulatedSchedule = schedule) }
                },
                label = { Text(stringResource(schedule.labelRes)) },
            )
        }
    }

    Text(
        text = stringResource(R.string.calculate_hours, state.simulatedHoursPerDay),
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
        text = stringResource(R.string.calc_sim_note),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (state.simulatedMonthlyKwh == null) {
        Text(
            text = stringResource(R.string.calc_sim_missing),
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
    Text(text = stringResource(R.string.calc_face_current), style = MaterialTheme.typography.titleMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        NumberField(
            label = stringResource(R.string.calc_face_btu),
            value = state.currentBtuInput,
            onValueChange = { value -> onUpdate { it.copy(currentBtuInput = value) } },
            modifier = Modifier.weight(1f),
        )
        NumberField(
            label = stringResource(R.string.common_watts),
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
            label = stringResource(R.string.calc_face_seer),
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
        Text(text = stringResource(R.string.inverter), style = MaterialTheme.typography.bodyMedium)
    }
    NumberField(
        label = stringResource(R.string.calc_face_investment),
        value = state.investmentInput,
        onValueChange = { value -> onUpdate { it.copy(investmentInput = value) } },
        modifier = Modifier.fillMaxWidth(),
    )

    val comparison = state.comparison
    if (comparison == null) {
        Text(
            text = stringResource(R.string.calc_face_missing),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricTile(
            label = stringResource(R.string.calc_face_current),
            value = AppFormat.kwh(comparison.currentMonthlyKwh),
            modifier = Modifier.weight(1f),
        )
        MetricTile(
            label = stringResource(R.string.calc_face_new),
            value = AppFormat.kwh(comparison.newMonthlyKwh),
            modifier = Modifier.weight(1f),
            highlight = true,
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricTile(
            label = stringResource(R.string.calc_face_savings),
            value = AppFormat.balboas(comparison.monthlySavingsBalboas),
            modifier = Modifier.weight(1f),
            highlight = comparison.monthlySavingsBalboas > 0.0,
        )
        MetricTile(
            label = stringResource(R.string.calc_face_payback),
            value = comparison.paybackMonths?.let { stringResource(R.string.months_format, it) }
                ?: stringResource(R.string.calc_face_no_payback),
            modifier = Modifier.weight(1f),
        )
    }
    if (comparison.monthlySavingsBalboas <= 0.0) {
        Text(
            text = stringResource(R.string.calc_face_no_savings),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
fun CapsulesSheet(onDismiss: () -> Unit) {
    PixelModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.common_capsules_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Text(
                text = stringResource(R.string.calc_capsules_swipe),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            EducationalSketchCarousel(
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 4.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
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
            Text(text = stringResource(R.string.calc_share), style = MaterialTheme.typography.headlineSmall)
            Text(
                text = stringResource(R.string.calc_share_mode, chartMode.label),
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
                    Text(text = stringResource(R.string.calc_share_generating), style = MaterialTheme.typography.labelMedium)
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
    var selectedGroupId by rememberSaveable { mutableStateOf(firstGroupId) }
    var creatingGroup by rememberSaveable { mutableStateOf(firstGroupId == null) }
    var newGroupName by rememberSaveable {
        mutableStateOf(if (state.groups.isEmpty()) "Mi Casa" else "")
    }
    var newGroupUtility by rememberSaveable { mutableStateOf(state.utility) }
    var newGroupBase by rememberSaveable {
        mutableStateOf(
            if (state.baseMonthlyKwh > 0.0) AppFormat.kwhNumber(state.baseMonthlyKwh) else "",
        )
    }
    val knownLocations = CalculateLocations.base + state.customLocations.map { it.nombreUbicacion }
    var selectedLocation by rememberSaveable {
        mutableStateOf(
            state.selectedLocation.takeIf { it.isNotBlank() } ?: CalculateLocations.DEFAULT,
        )
    }
    var useOtherLocation by rememberSaveable { mutableStateOf(false) }
    var otherLocation by rememberSaveable { mutableStateOf("") }
    var brandModel by rememberSaveable { mutableStateOf(state.brandModel) }

    val location = if (useOtherLocation) otherLocation.trim() else selectedLocation
    val groupValid = if (creatingGroup) {
        newGroupName.isNotBlank()
    } else {
        selectedGroupId != null
    }
    val valid = groupValid && location.isNotBlank()
    val selectedGroup = state.groups.firstOrNull { it.id == selectedGroupId }

    PixelDialog(
        title = stringResource(R.string.calc_save_profile),
        confirmLabel = stringResource(R.string.common_save),
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
        dismissLabel = stringResource(R.string.common_cancel),
        content = {
            Column(
                modifier = Modifier
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(text = stringResource(R.string.common_group_property), style = MaterialTheme.typography.titleMedium)
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
                        label = { Text(stringResource(R.string.common_new_group)) },
                    )
                }

                if (creatingGroup) {
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        label = { Text(stringResource(R.string.home_group_name)) },
                        placeholder = { Text(stringResource(R.string.home_group_placeholder)) },
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
                                Text(utility.shortName)
                            }
                        }
                    }
                    NumberField(
                        label = stringResource(R.string.common_base_kwh),
                        value = newGroupBase,
                        onValueChange = { newGroupBase = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    selectedGroup?.let { group ->
                        Text(
                            text = stringResource(
                                R.string.calc_group_info,
                                group.distribuidora.displayName,
                                AppFormat.kwh(group.consumoBaseKwh),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Text(text = stringResource(R.string.common_location), style = MaterialTheme.typography.titleMedium)
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
                        label = { Text(stringResource(R.string.common_new_location)) },
                        placeholder = { Text(stringResource(R.string.common_new_location_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                OutlinedTextField(
                    value = brandModel,
                    onValueChange = { brandModel = it },
                    label = { Text(stringResource(R.string.common_brand_model)) },
                    placeholder = { Text(stringResource(R.string.common_brand_placeholder)) },
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
    placeholder: String = "",
    info: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue -> if (newValue.length <= 12) onValueChange(newValue) },
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                info?.let { hint ->
                    InfoHint(
                        text = hint,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        },
        placeholder = if (placeholder.isBlank()) null else {
            { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions(
            onNext = { onImeAction() },
            onDone = { onImeAction() },
            onGo = { onImeAction() },
        ),
        modifier = modifier,
    )
}

private const val CHART_ITEM_INDEX = 2

private fun CalculateUiState.toReportData(context: Context): ReportData? {
    val spec = spec ?: return null
    val kwh = periodKwh ?: return null
    val electrical = electrical ?: return null
    val ensa = ensaEstimate ?: return null
    val naturgy = naturgySideEstimate ?: return null
    val ensaCost = ensa.addedCostBalboas
    val naturgyCost = naturgy.addedCostBalboas
    val displayCost = primaryMonthlyCost ?: minOf(ensaCost, naturgyCost)
    val narrative = context.getString(
        if (cleanFilters) R.string.report_narrative_clean else R.string.report_narrative_dirty,
        spaceName.ifBlank { context.getString(R.string.report_default_space) },
        String.format(Locale.US, "%,.0f", spec.btuPerHour),
        if (spec.inverter) context.getString(R.string.inverter) else context.getString(R.string.no_inverter),
        hoursPerDay.coerceIn(1, 24),
        AppFormat.decimal(kwh),
        AppFormat.balboas(displayCost),
        reportTariffLabel,
    )

    return ReportData(
        spaceName = spaceName.trim().ifBlank { context.getString(R.string.report_default_space) },
        brandModel = brandModel.trim().ifBlank { context.getString(R.string.report_no_brand) },
        btu = spec.btuPerHour.roundToInt(),
        seer = spec.seer,
        inverter = spec.inverter,
        voltage = electrical.voltage,
        monthlyKwh = kwh,
        comparison = chartMode,
        ensaMonthlyBalboas = ensaCost,
        naturgyMonthlyBalboas = naturgyCost,
        displayMonthlyBalboas = displayCost,
        tariffLabel = if (manualCost) "$reportTariffLabel · ${context.getString(R.string.manual_cost)}" else reportTariffLabel,
        recommendationText = narrative,
        electrical = electrical,
        monthlySavingsBalboas = comparison?.monthlySavingsBalboas
            ?.times(periodFactor)
            ?.takeIf { it > 0.0 },
        periodLabel = periodLabelText(context, usagePeriod, daysInMonth),
        ensaMarginalBalboasPerKwh = ensa.marginalBalboasPerKwh,
        naturgyMarginalBalboasPerKwh = naturgy.marginalBalboasPerKwh,
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
            onSetBaseKwh = {},
            onSave = { _ -> },
            onDismissSaveResult = {},
        )
    }
}
