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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.steefwel28.consumoenergetico.R
import com.steefwel28.consumoenergetico.core.di.AppContainer
import com.steefwel28.consumoenergetico.data.local.db.AcUnitEntity
import com.steefwel28.consumoenergetico.data.local.db.BillRecordEntity
import com.steefwel28.consumoenergetico.data.local.db.MaintenanceLogEntity
import com.steefwel28.consumoenergetico.data.local.db.PropertyGroupEntity
import com.steefwel28.consumoenergetico.domain.calculator.AcConsumptionCalculator
import com.steefwel28.consumoenergetico.domain.calculator.AcUnitSpec
import com.steefwel28.consumoenergetico.domain.calculator.AcUsage
import com.steefwel28.consumoenergetico.domain.calculator.MAX_BTU_PER_HOUR
import com.steefwel28.consumoenergetico.domain.calculator.PanamaUtility
import com.steefwel28.consumoenergetico.domain.calculator.PowerEstimator
import com.steefwel28.consumoenergetico.domain.calculator.TariffEstimate
import com.steefwel28.consumoenergetico.domain.units.LocaleNumber
import com.steefwel28.consumoenergetico.ui.AppViewModelProvider
import com.steefwel28.consumoenergetico.ui.components.AppFormat
import com.steefwel28.consumoenergetico.ui.components.InfoHint
import com.steefwel28.consumoenergetico.ui.components.PixelDialog
import com.steefwel28.consumoenergetico.ui.components.PixelSpringSnappy
import com.steefwel28.consumoenergetico.ui.components.SectionCard
import com.steefwel28.consumoenergetico.ui.components.pixelChargeHaptics
import com.steefwel28.consumoenergetico.ui.components.rememberPixelHaptics
import com.steefwel28.consumoenergetico.ui.theme.ConsumoEnergeticoTheme
import java.text.SimpleDateFormat
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

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
        onSaveBill = viewModel::saveBill,
        onDeleteUnit = viewModel::deleteUnit,
        onRestoreUnit = viewModel::restoreUnit,
        onDeleteBill = viewModel::deleteBill,
        onRestoreBill = viewModel::restoreBill,
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
    onSaveBill: (BillDraft) -> Unit,
    onDeleteUnit: (AcUnitEntity) -> Unit,
    onRestoreUnit: (AcUnitEntity, List<MaintenanceLogEntity>) -> Unit,
    onDeleteBill: (BillRecordEntity) -> Unit,
    onRestoreBill: (BillRecordEntity) -> Unit,
    onRegisterMaintenance: (Long) -> Unit,
    onDismissMessage: () -> Unit,
) {
    var showNewGroup by rememberSaveable { mutableStateOf(false) }
    var editingGroup by remember { mutableStateOf<PropertyGroupEntity?>(null) }
    var showNewUnit by rememberSaveable { mutableStateOf(false) }
    var editingUnit by remember { mutableStateOf<AcUnitEntity?>(null) }
    var historyUnit by remember { mutableStateOf<AcUnitEntity?>(null) }
    var showBillDialog by rememberSaveable { mutableStateOf(false) }
    val haptics = rememberPixelHaptics()
    val group = state.selectedGroup
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedUnitMessage = stringResource(R.string.home_msg_unit_deleted)
    val deletedBillMessage = stringResource(R.string.home_msg_bill_deleted)
    val undoLabel = stringResource(R.string.home_undo)

    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = stringResource(R.string.home_title), style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = stringResource(R.string.home_subtitle),
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
                    title = stringResource(R.string.home_no_groups_title),
                    subtitle = stringResource(R.string.home_no_groups_subtitle),
                ) {
                    Button(
                        onClick = { showNewGroup = true },
                        modifier = Modifier.fillMaxWidth().pixelChargeHaptics(),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Text(text = stringResource(R.string.home_create_group), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        } else {
            item {
                GroupSummaryCard(
                    state = state,
                    group = group,
                    estimate = state.summary,
                    onEdit = {
                        haptics.tap()
                        editingGroup = group
                    },
                    onRegisterBill = {
                        haptics.tap()
                        showBillDialog = true
                    },
                    onDeleteBill = { bill ->
                        haptics.tap()
                        onDeleteBill(bill)
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = deletedBillMessage,
                                actionLabel = undoLabel,
                            )
                            if (result == SnackbarResult.ActionPerformed) onRestoreBill(bill)
                        }
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
                    modifier = Modifier.fillMaxWidth().pixelChargeHaptics(),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text(
                        text = stringResource(R.string.home_add_unit, group.nombreGrupo),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            if (state.groupUnits.isEmpty()) {
                item {
                    SectionCard(
                        title = stringResource(R.string.home_no_units_title),
                        subtitle = stringResource(R.string.home_no_units_subtitle),
                    ) {}
                }
            } else {
                state.unitsByLocation.forEach { (location, units) ->
                    item(key = "location-$location") {
                        SectionCard(
                            title = location,
                            subtitle = if (units.size == 1) {
                                stringResource(R.string.home_units_one)
                            } else {
                                stringResource(R.string.home_units_many, units.size)
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
                text = stringResource(R.string.home_footer),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
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
            onDelete = { unitToDelete ->
                val logs = state.historyFor(unitToDelete.id)
                onDeleteUnit(unitToDelete)
                showNewUnit = false
                editingUnit = null
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = deletedUnitMessage,
                        actionLabel = undoLabel,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        onRestoreUnit(unitToDelete, logs)
                    }
                }
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

    if (showBillDialog && group != null) {
        BillDialog(
            group = group,
            onDismiss = { showBillDialog = false },
            onSave = { draft ->
                onSaveBill(draft)
                showBillDialog = false
            },
        )
    }

    state.message?.let { message ->
        PixelDialog(
            title = stringResource(R.string.home_title),
            text = message,
            confirmLabel = stringResource(R.string.common_understood),
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
        title = stringResource(R.string.home_groups_title),
        subtitle = stringResource(R.string.home_groups_subtitle),
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
                label = { Text(stringResource(R.string.common_new_group)) },
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
    state: MyHomeUiState,
    group: PropertyGroupEntity,
    estimate: TariffEstimate?,
    onEdit: () -> Unit,
    onRegisterBill: () -> Unit,
    onDeleteBill: (BillRecordEntity) -> Unit,
) {
    SectionCard(
        title = stringResource(R.string.home_summary_title, group.nombreGrupo),
        subtitle = stringResource(
            R.string.home_summary_subtitle,
            group.distribuidora.displayName,
            AppFormat.kwh(group.consumoBaseKwh),
        ),
    ) {
        if (estimate == null) {
            Text(
                text = stringResource(R.string.home_add_units_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HomeMetric(
                    label = stringResource(R.string.home_total),
                    value = AppFormat.kwh(estimate.baseKwh + estimate.addedKwh),
                    modifier = Modifier.weight(1f),
                )
                HomeMetric(
                    label = stringResource(R.string.home_bill),
                    value = AppFormat.balboas(estimate.totalBillBalboas),
                    modifier = Modifier.weight(1f),
                    highlight = true,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HomeMetric(
                    label = stringResource(R.string.home_group_acs),
                    value = AppFormat.kwh(estimate.addedKwh),
                    modifier = Modifier.weight(1f),
                )
                HomeMetric(
                    label = stringResource(R.string.home_jump),
                    value = AppFormat.balboas(estimate.addedCostBalboas),
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = stringResource(
                    R.string.home_marginal,
                    AppFormat.balboas(estimate.marginalBalboasPerKwh),
                    group.distribuidora.displayName,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        state.climatizationSplit?.let { (climatization, rest) ->
            ClimatizationBar(climatization = climatization, rest = rest)
        }

        if (state.recentBills.isNotEmpty()) {
            BillMonthBars(state = state, onDeleteBill = onDeleteBill)
        }

        FilledTonalButton(
            onClick = onRegisterBill,
            modifier = Modifier.fillMaxWidth().pixelChargeHaptics(),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Text(
                text = stringResource(R.string.home_register_bill),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Edit, contentDescription = null)
            Text(text = stringResource(R.string.home_edit_group), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

/** Reparto del último recibo real: climatización contra el resto del hogar. */
@Composable
private fun ClimatizationBar(climatization: Double, rest: Double) {
    val total = (climatization + rest).coerceAtLeast(0.01)
    val climatizationShare = climatization / total
    val restShare = 1.0 - climatizationShare
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(
                R.string.home_climatization_split,
                (climatizationShare * 100).roundToInt(),
                (restShare * 100).roundToInt(),
            ),
            style = MaterialTheme.typography.titleSmall,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Box(
                modifier = Modifier
                    .weight(climatizationShare.toFloat().coerceAtLeast(0.001f))
                    .height(14.dp)
                    .background(MaterialTheme.colorScheme.primary),
            )
            Box(
                modifier = Modifier
                    .weight(restShare.toFloat().coerceAtLeast(0.001f))
                    .height(14.dp)
                    .background(MaterialTheme.colorScheme.tertiary),
            )
        }
    }
}

/** Barras Pixel deslizables con el histórico real; toca un mes para el detalle. */
@Composable
private fun BillMonthBars(
    state: MyHomeUiState,
    onDeleteBill: (BillRecordEntity) -> Unit,
) {
    val haptics = rememberPixelHaptics()
    var showKwh by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf<BillRecordEntity?>(null) }
    val bills = state.recentBills
    val maxKwh = bills.maxOf { it.realKwh }.coerceAtLeast(0.01)
    val maxCost = bills.maxOf { it.paidBalboas }.coerceAtLeast(0.01)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.home_bill_history_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = showKwh,
                onClick = {
                    haptics.tap()
                    showKwh = true
                },
                label = { Text(stringResource(R.string.home_bill_kwh_tab)) },
            )
            FilterChip(
                selected = !showKwh,
                onClick = {
                    haptics.tap()
                    showKwh = false
                },
                label = { Text(stringResource(R.string.home_bill_cost_tab)) },
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        bills.forEach { bill ->
            val value = if (showKwh) bill.realKwh else bill.paidBalboas
            val maxValue = if (showKwh) maxKwh else maxCost
            val fraction by animateFloatAsState(
                targetValue = (value / maxValue).toFloat().coerceIn(0f, 1f),
                animationSpec = PixelSpringSnappy,
                label = "bill-bar-${bill.id}",
            )
            val isSelected = selected?.id == bill.id
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.clickable {
                    haptics.tap()
                    selected = bill
                },
            ) {
                Text(
                    text = if (showKwh) AppFormat.kwhNumber(value) else AppFormat.balboas(value),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(96.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction.coerceAtLeast(0.04f))
                            .clip(RoundedCornerShape(17.dp))
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                },
                            ),
                    )
                }
                Text(
                    text = monthLabel(bill.year, bill.month),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }

    selected?.let { bill ->
        val variation = state.variationPercent(bill)
        val variationText = when {
            variation == null -> stringResource(R.string.home_bill_no_previous)
            abs(variation) < 0.05 -> stringResource(R.string.home_bill_variation_tie)
            variation > 0 -> stringResource(R.string.home_bill_variation_up, variation)
            else -> stringResource(R.string.home_bill_variation_down, abs(variation))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = monthLabel(bill.year, bill.month) + " · " + AppFormat.kwh(bill.realKwh),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = variationText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            bill.effectiveBalboasPerKwh?.let { rate ->
                Text(
                    text = stringResource(
                        R.string.home_bill_effective,
                        String.format(Locale.US, "B/. %.4f/kWh", rate),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(
                onClick = {
                    haptics.confirm()
                    selected = null
                    onDeleteBill(bill)
                },
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = stringResource(R.string.home_delete_bill),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

private fun monthLabel(year: Int, month: Int): String {
    val name = Month.of(month).getDisplayName(TextStyle.SHORT, Locale.getDefault())
    return "$name ${year % 100}"
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
        title = stringResource(R.string.home_notifications_title),
        subtitle = stringResource(R.string.home_notifications_subtitle),
    ) {
        Button(
            onClick = {
                haptics.confirm()
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            modifier = Modifier.fillMaxWidth().pixelChargeHaptics(),
        ) {
            Text(stringResource(R.string.home_enable_notifications))
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
                        "${stringResource(if (unit.esInverter) R.string.inverter else R.string.no_inverter)} · " +
                        "${unit.voltaje} V",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        R.string.home_unit_consumption,
                        AppFormat.kwh(unit.consumoEstimadoKwhMes),
                        AppFormat.decimal(unit.horasUsoDiario),
                    ),
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
                Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.home_edit_unit_cd, unit.nombreEspacio))
            }
            IconButton(onClick = onHistory) {
                Icon(Icons.Rounded.History, contentDescription = stringResource(R.string.home_history_cd, unit.nombreEspacio))
            }
        }
        OutlinedButton(onClick = onMaintenance, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.home_register_maintenance))
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
        title = stringResource(if (group == null) R.string.common_new_group else R.string.home_edit_group),
        confirmLabel = stringResource(R.string.common_save),
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
        dismissLabel = stringResource(R.string.common_cancel),
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.home_group_name)) },
                    placeholder = { Text(stringResource(R.string.home_group_placeholder)) },
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
                        ) { Text(option.shortName) }
                    }
                }
                OutlinedTextField(
                    value = base,
                    onValueChange = { base = it },
                    label = { Text(stringResource(R.string.common_base_kwh)) },
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
    onDelete: (AcUnitEntity) -> Unit,
) {
    val haptics = rememberPixelHaptics()
    var groupId by remember { mutableStateOf(unit?.groupId ?: initialGroupId) }
    var location by remember { mutableStateOf(unit?.ubicacion ?: MyHomeLocations.DEFAULT) }
    var useOther by remember { mutableStateOf(false) }
    var otherLocation by remember { mutableStateOf("") }
    var spaceName by remember { mutableStateOf(unit?.nombreEspacio.orEmpty()) }
    var brandModel by remember { mutableStateOf(unit?.marcaModelo?.takeUnless { it == "Sin marca/modelo" }.orEmpty()) }
    var btuInput by remember { mutableStateOf((unit?.btu ?: 12_000).toString()) }
    var wattsInput by remember { mutableStateOf(unit?.nominalWatts?.toString().orEmpty()) }
    var seerInput by remember { mutableStateOf(unit?.seer?.let { AppFormat.decimal(it) }.orEmpty()) }
    var inverter by remember { mutableStateOf(unit?.esInverter ?: true) }
    var consumoInput by remember {
        mutableStateOf(unit?.let { AppFormat.kwhNumber(it.consumoEstimadoKwhMes) }.orEmpty())
    }
    var hoursInput by remember {
        mutableStateOf(AppFormat.kwhNumber(unit?.horasUsoDiario ?: 8.0))
    }
    // Si el consumo guardado se aparta >5 % de la estimación, el usuario lo
    // escribió a mano y no debe sobrescribirse al cambiar BTU u horas.
    var consumoEdited by remember(unit?.id) {
        mutableStateOf(
            unit != null && unit.consumoEstimadoKwhMes > 0.0 && run {
                val estimate = estimatedMonthlyKwh(
                    unit.btu,
                    unit.nominalWatts?.toDouble(),
                    unit.seer,
                    unit.esInverter,
                    unit.horasUsoDiario,
                ) ?: return@run false
                abs(unit.consumoEstimadoKwhMes - estimate) / unit.consumoEstimadoKwhMes > 0.05
            },
        )
    }

    val btuValue = LocaleNumber.parse(btuInput)?.takeIf { it in 1.0..MAX_BTU_PER_HOUR }
    val btu = btuValue?.roundToInt()
    val btuError = btuInput.isNotBlank() && btuValue == null
    val chosenLocation = if (useOther) otherLocation.trim() else location
    val valid = btu != null && chosenLocation.isNotBlank() &&
        groups.any { it.id == groupId }

    LaunchedEffect(btuInput, wattsInput, seerInput, inverter, hoursInput) {
        if (consumoEdited || btu == null) return@LaunchedEffect
        val estimate = estimatedMonthlyKwh(
            btu = btu,
            watts = LocaleNumber.parse(wattsInput),
            seer = LocaleNumber.parse(seerInput),
            inverter = inverter,
            hours = (LocaleNumber.parse(hoursInput) ?: 8.0).coerceIn(1.0, 24.0),
        )
        if (estimate != null) consumoInput = AppFormat.kwhNumber(estimate)
    }

    PixelDialog(
        title = stringResource(if (unit == null) R.string.home_new_unit else R.string.home_edit_unit),
        confirmLabel = stringResource(R.string.common_save),
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
                    horasUsoDiario = (LocaleNumber.parse(hoursInput) ?: 8.0).coerceIn(1.0, 24.0),
                    consumoKwhMes = LocaleNumber.parse(consumoInput),
                    fechaInstalacionMillis = unit?.fechaInstalacionMillis
                        ?: System.currentTimeMillis(),
                    fechaUltimoMantenimientoMillis = unit?.fechaUltimoMantenimientoMillis
                        ?: System.currentTimeMillis(),
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

                Text(text = stringResource(R.string.common_location), style = MaterialTheme.typography.titleMedium)
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
                        label = { Text(stringResource(R.string.common_new_location)) },
                        placeholder = { Text(stringResource(R.string.common_new_location_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                OutlinedTextField(
                    value = spaceName,
                    onValueChange = { spaceName = it },
                    label = { Text(stringResource(R.string.home_space_name)) },
                    placeholder = { Text(stringResource(R.string.home_space_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = brandModel,
                    onValueChange = { brandModel = it },
                    label = { Text(stringResource(R.string.common_brand_model)) },
                    placeholder = { Text(stringResource(R.string.common_brand_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = btuInput,
                        onValueChange = { btuInput = it },
                        label = { Text(stringResource(R.string.common_btu)) },
                        singleLine = true,
                        isError = btuError,
                        supportingText = if (btuError) {
                            { Text(stringResource(R.string.home_msg_btu_positive)) }
                        } else {
                            null
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = wattsInput,
                        onValueChange = { wattsInput = it },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.common_watts))
                                InfoHint(text = stringResource(R.string.calc_watts_info))
                            }
                        },
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
                        label = { Text(stringResource(R.string.common_seer)) },
                        placeholder = { Text("12.5") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = consumoInput,
                        onValueChange = { value ->
                            consumoInput = value
                            consumoEdited = value.isNotBlank()
                        },
                        label = { Text(stringResource(R.string.home_kwh_month)) },
                        placeholder = { Text(stringResource(R.string.home_kwh_estimate_placeholder)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = hoursInput,
                    onValueChange = { hoursInput = it },
                    label = { Text(stringResource(R.string.home_hours)) },
                    placeholder = { Text("8") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.inverter), style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = stringResource(R.string.home_inverter_hint),
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

                if (unit != null) {
                    Button(
                        onClick = {
                            haptics.confirm()
                            onDelete(unit)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(imageVector = Icons.Rounded.Delete, contentDescription = null)
                        Text(
                            text = stringResource(R.string.home_delete_unit),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun BillDialog(
    group: PropertyGroupEntity,
    onDismiss: () -> Unit,
    onSave: (BillDraft) -> Unit,
) {
    val haptics = rememberPixelHaptics()
    val today = remember { YearMonth.now() }
    val months = remember(today) { (0L..11L).map { today.minusMonths(it) } }
    var selected by remember { mutableStateOf(months.first()) }
    var kwhInput by remember { mutableStateOf("") }
    var paidInput by remember { mutableStateOf("") }

    val realKwh = LocaleNumber.parse(kwhInput)
    val paid = LocaleNumber.parse(paidInput)
    val valid = realKwh != null && realKwh > 0.0 && paid != null && paid >= 0.0

    PixelDialog(
        title = stringResource(R.string.home_bill_dialog_title, group.nombreGrupo),
        confirmLabel = stringResource(R.string.common_save),
        confirmEnabled = valid,
        onConfirm = {
            haptics.confirm()
            onSave(
                BillDraft(
                    groupId = group.id,
                    year = selected.year,
                    month = selected.monthValue,
                    realKwh = realKwh ?: 0.0,
                    paidBalboas = paid ?: 0.0,
                    utility = group.distribuidora,
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
                Text(text = stringResource(R.string.home_bill_month), style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    months.forEach { month ->
                        FilterChip(
                            selected = month == selected,
                            onClick = {
                                haptics.tap()
                                selected = month
                            },
                            label = {
                                Text(
                                    month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()) +
                                        " " + month.year,
                                )
                            },
                        )
                    }
                }
                OutlinedTextField(
                    value = kwhInput,
                    onValueChange = { kwhInput = it },
                    label = { Text(stringResource(R.string.home_bill_real_kwh)) },
                    placeholder = { Text("180") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = paidInput,
                    onValueChange = { paidInput = it },
                    label = { Text(stringResource(R.string.home_bill_paid)) },
                    placeholder = { Text("32.50") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
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
        title = stringResource(R.string.home_history_title, unit.nombreEspacio),
        confirmLabel = stringResource(R.string.common_close),
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
                        text = stringResource(R.string.home_no_logs),
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
                                text = log.nota.ifBlank { stringResource(R.string.home_log_default) },
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

@Composable
private fun maintenanceText(days: Long): String = when {
    days > 1 -> stringResource(R.string.home_maintenance_days, days)
    days == 1L -> stringResource(R.string.home_maintenance_one)
    days == 0L -> stringResource(R.string.home_maintenance_today)
    days == -1L -> stringResource(R.string.home_maintenance_overdue_one)
    else -> stringResource(R.string.home_maintenance_overdue, -days)
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(millis))

/** Consumo mensual estimado con la potencia real o `W = BTU/h ÷ EER`. */
private fun estimatedMonthlyKwh(
    btu: Int,
    watts: Double?,
    seer: Double?,
    inverter: Boolean,
    hours: Double,
): Double? = runCatching {
    AcConsumptionCalculator.monthlyKwh(
        AcUnitSpec(
            btuPerHour = btu.toDouble(),
            nominalWatts = watts ?: PowerEstimator.fromBtu(
                btuPerHour = btu.toDouble(),
                seer = seer,
                isInverter = inverter,
            ),
            seer = seer,
            inverter = inverter,
        ),
        AcUsage(hoursPerDay = hours.coerceIn(1.0, 24.0)),
    )
}.getOrNull()

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
            onSaveBill = {},
            onDeleteUnit = {},
            onRestoreUnit = { _, _ -> },
            onDeleteBill = {},
            onRestoreBill = {},
            onRegisterMaintenance = {},
            onDismissMessage = {},
        )
    }
}
