@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.steefwel28.consumoenergetico.ui.screens.duel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.steefwel28.consumoenergetico.domain.calculator.PanamaUtility
import com.steefwel28.consumoenergetico.ui.AppViewModelProvider
import com.steefwel28.consumoenergetico.ui.components.AppFormat
import com.steefwel28.consumoenergetico.ui.components.InfoHint
import com.steefwel28.consumoenergetico.ui.components.PixelSpringExpressive
import com.steefwel28.consumoenergetico.ui.components.PixelSpringSnappy
import com.steefwel28.consumoenergetico.ui.components.SectionCard
import com.steefwel28.consumoenergetico.ui.components.rememberPixelHaptics
import com.steefwel28.consumoenergetico.ui.theme.ConsumoEnergeticoTheme
import com.steefwel28.consumoenergetico.ui.theme.ExtendedColors
import com.steefwel28.consumoenergetico.ui.theme.LocalExtendedColors
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun AcDuelRoute(
    container: AppContainer,
    onBack: () -> Unit,
    viewModel: AcDuelViewModel = viewModel(factory = AppViewModelProvider.duel(container)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AcDuelScreen(
        state = state,
        onUpdate = viewModel::update,
        onBack = onBack,
    )
}

@Composable
fun AcDuelScreen(
    state: AcDuelUiState,
    onUpdate: ((AcDuelUiState) -> AcDuelUiState) -> Unit,
    onBack: () -> Unit,
) {
    val extended = LocalExtendedColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.settings_back_cd),
                )
            }
            Column {
                Text(
                    text = stringResource(R.string.menu_duel),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = stringResource(R.string.menu_duel_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SharedConditionsCard(state = state, onUpdate = onUpdate)

        DuelTeamCard(
            title = stringResource(R.string.duel_team_a),
            alias = state.aliasAInput,
            onAlias = { value -> onUpdate { it.copy(aliasAInput = value) } },
            btu = state.btuAInput,
            onBtu = { value -> onUpdate { it.copy(btuAInput = value) } },
            seer = state.seerAInput,
            onSeer = { value -> onUpdate { it.copy(seerAInput = value) } },
            watts = state.wattsAInput,
            onWatts = { value -> onUpdate { it.copy(wattsAInput = value) } },
            inverter = state.inverterA,
            onInverter = { value -> onUpdate { it.copy(inverterA = value) } },
        )

        DuelTeamCard(
            title = stringResource(R.string.duel_team_b),
            alias = state.aliasBInput,
            onAlias = { value -> onUpdate { it.copy(aliasBInput = value) } },
            btu = state.btuBInput,
            onBtu = { value -> onUpdate { it.copy(btuBInput = value) } },
            seer = state.seerBInput,
            onSeer = { value -> onUpdate { it.copy(seerBInput = value) } },
            watts = state.wattsBInput,
            onWatts = { value -> onUpdate { it.copy(wattsBInput = value) } },
            inverter = state.inverterB,
            onInverter = { value -> onUpdate { it.copy(inverterB = value) } },
        )

        DuelResultCard(state = state, onUpdate = onUpdate, extended = extended)

        Text(
            text = stringResource(R.string.calc_footer),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        )
    }
}

/** Horas, consumo base y distribuidora compartidas por los dos equipos. */
@Composable
private fun SharedConditionsCard(
    state: AcDuelUiState,
    onUpdate: ((AcDuelUiState) -> AcDuelUiState) -> Unit,
) {
    val haptics = rememberPixelHaptics()
    SectionCard(title = stringResource(R.string.duel_shared_title)) {
        Text(
            text = stringResource(R.string.calculate_hours, state.hoursPerDay),
            style = MaterialTheme.typography.titleSmall,
        )
        Slider(
            value = state.hoursPerDay.toFloat(),
            onValueChange = { value ->
                val hours = value.roundToInt()
                onUpdate { it.copy(hoursPerDay = hours) }
            },
            valueRange = 1f..24f,
            steps = 22,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PanamaUtility.entries.forEach { utility ->
                val isSelected = state.utility == utility
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.96f,
                    animationSpec = PixelSpringSnappy,
                    label = "duel-utility-${utility.name}",
                )
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        haptics.tap()
                        onUpdate { it.copy(utility = utility) }
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

        NumberField(
            label = stringResource(R.string.common_base_kwh),
            value = state.baseKwhInput,
            onValueChange = { value -> onUpdate { it.copy(baseKwhInput = value) } },
            placeholder = "350",
        )

        Text(
            text = stringResource(
                R.string.calc_tariff_validity,
                state.tariffValidFrom,
                state.tariffValidTo,
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Ficha de un contendiente: alias, BTU/h, Inverter, SEER y W opcionales. */
@Composable
private fun DuelTeamCard(
    title: String,
    alias: String,
    onAlias: (String) -> Unit,
    btu: String,
    onBtu: (String) -> Unit,
    seer: String,
    onSeer: (String) -> Unit,
    watts: String,
    onWatts: (String) -> Unit,
    inverter: Boolean,
    onInverter: (Boolean) -> Unit,
) {
    val haptics = rememberPixelHaptics()
    SectionCard(title = title) {
        NumberField(
            label = stringResource(R.string.duel_alias_label),
            value = alias,
            onValueChange = onAlias,
            placeholder = "Hisense ATR362MA",
        )
        NumberField(
            label = stringResource(R.string.duel_btu_label),
            value = btu,
            onValueChange = onBtu,
            placeholder = "12000",
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(if (inverter) R.string.inverter else R.string.no_inverter),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = inverter,
                onCheckedChange = { value ->
                    haptics.tap()
                    onInverter(value)
                },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(
                label = stringResource(R.string.duel_seer_label),
                value = seer,
                onValueChange = onSeer,
                placeholder = "21",
                modifier = Modifier.weight(1f),
            )
            NumberField(
                label = stringResource(R.string.duel_watts_label),
                value = watts,
                onValueChange = onWatts,
                placeholder = "1150",
                info = stringResource(R.string.calc_watts_info),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Veredicto animado: barras con spring, % de ahorro, B/. al mes y al año. */
@Composable
private fun DuelResultCard(
    state: AcDuelUiState,
    onUpdate: ((AcDuelUiState) -> AcDuelUiState) -> Unit,
    extended: ExtendedColors,
) {
    val nameA = state.aliasAInput.trim().ifBlank { stringResource(R.string.duel_team_a) }
    val nameB = state.aliasBInput.trim().ifBlank { stringResource(R.string.duel_team_b) }

    SectionCard(title = stringResource(R.string.duel_result_title)) {
        if (!state.canCompare) {
            Text(
                text = stringResource(R.string.duel_missing),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@SectionCard
        }
        val kwhA = checkNotNull(state.kwhA)
        val kwhB = checkNotNull(state.kwhB)
        val costA = state.monthlyCostA
        val costB = state.monthlyCostB

        DuelBars(
            label = stringResource(R.string.common_kwh_month),
            textA = nameA,
            textB = nameB,
            valueA = kwhA,
            valueB = kwhB,
            colorA = extended.ensa,
            colorB = extended.edemet,
            format = { value -> "${AppFormat.kwhNumber(value)} kWh" },
        )
        if (costA != null && costB != null) {
            DuelBars(
                label = stringResource(R.string.duel_cost_month),
                textA = nameA,
                textB = nameB,
                valueA = costA,
                valueB = costB,
                colorA = extended.ensa,
                colorB = extended.edemet,
                format = { value -> AppFormat.balboas(value) },
            )
        }

        // Píldora de veredicto: entra con spring + fade sobre su fondo M3.
        val winner = state.winner
        if (winner != null) {
            val (text, container, content) = when (winner) {
                DuelWinner.TIE -> Triple(
                    stringResource(R.string.duel_verdict_tie),
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    MaterialTheme.colorScheme.onSurface,
                )
                DuelWinner.A, DuelWinner.B -> {
                    val champion = if (winner == DuelWinner.A) nameA else nameB
                    Triple(
                        stringResource(R.string.duel_verdict_wins, champion),
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + scaleIn(animationSpec = PixelSpringExpressive, initialScale = 0.86f),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(container)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleMedium,
                        color = content,
                    )
                }
            }
        }

        state.savingsPercent?.let { percent ->
            Text(
                text = stringResource(
                    R.string.duel_savings_percent,
                    String.format(Locale.US, "%.0f %%", percent),
                ),
                style = MaterialTheme.typography.titleSmall,
            )
        }
        state.monthlySavingsBalboas?.let { savings ->
            Text(
                text = stringResource(R.string.duel_monthly_savings, AppFormat.balboas(savings)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    R.string.duel_annual_savings,
                    AppFormat.balboas(savings * 12.0),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        NumberField(
            label = stringResource(R.string.duel_price_diff_label),
            value = state.priceDiffInput,
            onValueChange = { value -> onUpdate { it.copy(priceDiffInput = value) } },
            placeholder = "150",
        )
        val payback = state.paybackMonths
        val priceDiff = state.priceDiff
        when {
            payback != null -> Text(
                text = stringResource(
                    R.string.duel_payback,
                    String.format(Locale.US, "%.0f", payback),
                ),
                style = MaterialTheme.typography.titleSmall,
            )
            priceDiff != null && priceDiff > 0.0 -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(extended.warningContainer)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(
                    text = stringResource(R.string.duel_no_payback),
                    style = MaterialTheme.typography.bodyMedium,
                    color = extended.onWarningContainer,
                )
            }
        }
    }
}

/** Par de barras animadas con física de resorte, cara a cara. */
@Composable
private fun DuelBars(
    label: String,
    textA: String,
    textB: String,
    valueA: Double,
    valueB: Double,
    colorA: Color,
    colorB: Color,
    format: (Double) -> String,
) {
    val max = maxOf(valueA, valueB).takeIf { it > 0.0 } ?: 1.0
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, style = MaterialTheme.typography.titleSmall)
        listOf(
            Triple(textA, valueA, colorA),
            Triple(textB, valueB, colorB),
        ).forEach { (text, value, color) ->
            val fraction by animateFloatAsState(
                targetValue = (value / max).toFloat().coerceIn(0.02f, 1f),
                animationSpec = PixelSpringExpressive,
                label = "duel-bar-fraction",
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(text = format(value), style = MaterialTheme.typography.titleSmall)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(5.dp))
                            .background(color),
                    )
                }
            }
        }
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

@Preview(name = "Duelo claro", showBackground = true, heightDp = 1400)
@Preview(
    name = "Duelo oscuro",
    showBackground = true,
    heightDp = 1400,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AcDuelScreenPreview() {
    ConsumoEnergeticoTheme(dynamicColor = false) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            AcDuelScreen(
                state = AcDuelUiState(),
                onUpdate = {},
                onBack = {},
            )
        }
    }
}
