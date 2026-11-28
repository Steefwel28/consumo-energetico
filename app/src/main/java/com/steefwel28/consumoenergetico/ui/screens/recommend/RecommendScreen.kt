@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.steefwel28.consumoenergetico.ui.screens.recommend

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.steefwel28.consumoenergetico.R
import com.steefwel28.consumoenergetico.core.di.AppContainer
import com.steefwel28.consumoenergetico.domain.recommender.AcRecommendation
import com.steefwel28.consumoenergetico.domain.recommender.BtuFactor
import com.steefwel28.consumoenergetico.domain.recommender.CeilingType
import com.steefwel28.consumoenergetico.domain.recommender.ElectricalSpec
import com.steefwel28.consumoenergetico.domain.recommender.HeatSource
import com.steefwel28.consumoenergetico.domain.recommender.SunExposure
import com.steefwel28.consumoenergetico.ui.AppViewModelProvider
import com.steefwel28.consumoenergetico.ui.components.AppFormat
import com.steefwel28.consumoenergetico.ui.components.SectionCard
import com.steefwel28.consumoenergetico.ui.components.labelRes
import com.steefwel28.consumoenergetico.ui.components.rememberPixelHaptics
import com.steefwel28.consumoenergetico.ui.illustrations.EducationalSketchCarousel
import com.steefwel28.consumoenergetico.ui.theme.ConsumoEnergeticoTheme

@Composable
fun RecommendRoute(
    container: AppContainer,
    viewModel: RecommendViewModel = viewModel(
        factory = AppViewModelProvider.recommend(container),
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RecommendScreen(state = state, onUpdate = viewModel::update)
}

@Composable
fun RecommendScreen(
    state: RecommendUiState,
    onUpdate: ((RecommendUiState) -> RecommendUiState) -> Unit,
) {
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
                Text(text = stringResource(R.string.rec_title), style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = stringResource(R.string.rec_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SpaceInputCard(state = state, onUpdate = onUpdate)
        }

        val recommendation = state.recommendation
        if (recommendation == null) {
            item {
                SectionCard(
                    title = stringResource(R.string.rec_missing_size_title),
                    subtitle = stringResource(R.string.rec_missing_size_subtitle, state.areaSymbol),
                ) {}
            }
        } else {
            item {
                RecommendationHero(state = state, recommendation = recommendation)
            }
            item {
                ElectricalCard(spec = state.electrical, inverter = state.inverter, onUpdate = onUpdate)
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = stringResource(R.string.common_capsules_title), style = MaterialTheme.typography.titleLarge)
                Text(
                    text = stringResource(R.string.rec_capsules_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                EducationalSketchCarousel(contentPadding = PaddingValues(horizontal = 0.dp))
            }
        }

        item {
            Text(
                text = stringResource(R.string.rec_footer),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SpaceInputCard(
    state: RecommendUiState,
    onUpdate: ((RecommendUiState) -> RecommendUiState) -> Unit,
) {
    val haptics = rememberPixelHaptics()
    SectionCard(
        title = stringResource(R.string.rec_place_title),
        subtitle = stringResource(R.string.rec_place_subtitle),
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !state.useDimensions,
                onClick = {
                    haptics.tap()
                    onUpdate { it.copy(useDimensions = false) }
                },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) { Text(stringResource(R.string.rec_area_total)) }
            SegmentedButton(
                selected = state.useDimensions,
                onClick = {
                    haptics.tap()
                    onUpdate { it.copy(useDimensions = true) }
                },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) { Text(stringResource(R.string.rec_dimensions)) }
        }

        if (state.useDimensions) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricField(
                    label = stringResource(R.string.rec_length),
                    value = state.lengthInput,
                    suffix = state.lengthSymbol,
                    onValueChange = { value -> onUpdate { it.copy(lengthInput = value) } },
                    modifier = Modifier.weight(1f),
                )
                MetricField(
                    label = stringResource(R.string.rec_width),
                    value = state.widthInput,
                    suffix = state.lengthSymbol,
                    onValueChange = { value -> onUpdate { it.copy(widthInput = value) } },
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            MetricField(
                label = stringResource(R.string.rec_area),
                value = state.areaInput,
                suffix = state.areaSymbol,
                onValueChange = { value -> onUpdate { it.copy(areaInput = value) } },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        OutlinedButton(
            onClick = {
                haptics.tap()
                onUpdate { it.copy(detailsExpanded = !it.detailsExpanded) }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = if (state.detailsExpanded) {
                    Icons.Rounded.ExpandLess
                } else {
                    Icons.Rounded.ExpandMore
                },
                contentDescription = null,
            )
            Text(
                text = stringResource(
                    if (state.detailsExpanded) R.string.rec_hide_details else R.string.rec_more_details,
                ),
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        AnimatedVisibility(
            visible = state.detailsExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(text = stringResource(R.string.rec_sun), style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SunExposure.entries.forEach { exposure ->
                        FilterChip(
                            selected = state.sunExposure == exposure,
                            onClick = {
                                haptics.tap()
                                onUpdate { it.copy(sunExposure = exposure) }
                            },
                            label = { Text(stringResource(exposure.labelRes)) },
                        )
                    }
                }

                Text(text = stringResource(R.string.rec_people), style = MaterialTheme.typography.titleMedium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    IconButton(
                        onClick = {
                            haptics.tap()
                            onUpdate { it.copy(people = (it.people - 1).coerceAtLeast(0)) }
                        },
                        enabled = state.people > 0,
                    ) {
                        Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.rec_people_minus))
                    }
                    Text(
                        text = "${state.people}",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    IconButton(
                        onClick = {
                            haptics.tap()
                            onUpdate { it.copy(people = (it.people + 1).coerceAtMost(50)) }
                        },
                        enabled = state.people < 50,
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.rec_people_plus))
                    }
                }

                Text(text = stringResource(R.string.rec_ceiling), style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CeilingType.entries.forEach { ceiling ->
                        FilterChip(
                            selected = state.ceiling == ceiling,
                            onClick = {
                                haptics.tap()
                                onUpdate { it.copy(ceiling = ceiling) }
                            },
                            label = { Text(stringResource(ceiling.labelRes)) },
                        )
                    }
                }

                Text(text = stringResource(R.string.rec_heat), style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeatSource.entries.forEach { source ->
                        FilterChip(
                            selected = source in state.heatSources,
                            onClick = {
                                haptics.tap()
                                onUpdate { current ->
                                    val sources = if (source in current.heatSources) {
                                        current.heatSources - source
                                    } else {
                                        current.heatSources + source
                                    }
                                    current.copy(heatSources = sources)
                                }
                            },
                            label = { Text(stringResource(source.labelRes)) },
                        )
                    }
                }

                MetricField(
                    label = stringResource(R.string.rec_pipe),
                    value = state.pipeDistanceInput,
                    suffix = state.lengthSymbol,
                    onValueChange = { value -> onUpdate { it.copy(pipeDistanceInput = value) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                MetricField(
                    label = stringResource(R.string.rec_selected_btu),
                    value = state.selectedBtuInput,
                    suffix = stringResource(R.string.common_btu),
                    onValueChange = { value -> onUpdate { it.copy(selectedBtuInput = value) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun RecommendationHero(
    state: RecommendUiState,
    recommendation: AcRecommendation,
) {
    val diagnostic = state.diagnostic

    SectionCard(
        title = stringResource(R.string.rec_capacity_title),
        subtitle = stringResource(R.string.rec_capacity_subtitle),
    ) {
        Text(
            text = AppFormat.btu(recommendation.commercialBtu.toDouble()),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(
                R.string.rec_exact,
                AppFormat.btu(recommendation.exactBtu),
                AppFormat.decimal(recommendation.areaSquareMeters),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        diagnostic?.let { result ->
            Text(
                text = result.explanation,
                style = MaterialTheme.typography.bodyLarge,
            )
            result.undersizedAlert?.let { alert ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Text(
                            text = stringResource(R.string.rec_small_trap),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                    Text(
                        text = alert,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }

        if (recommendation.factors.isNotEmpty()) {
            Text(text = stringResource(R.string.rec_factors), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                recommendation.factors.forEach { factor ->
                    FactorPill(factor)
                }
            }
        }
    }
}

@Composable
private fun ElectricalCard(
    spec: ElectricalSpec?,
    inverter: Boolean,
    onUpdate: ((RecommendUiState) -> RecommendUiState) -> Unit,
) {
    val haptics = rememberPixelHaptics()
    SectionCard(
        title = stringResource(R.string.rec_electrical_title),
        subtitle = stringResource(R.string.rec_electrical_subtitle),
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            listOf(true to R.string.inverter, false to R.string.no_inverter).forEachIndexed { index, (value, label) ->
                SegmentedButton(
                    selected = inverter == value,
                    onClick = {
                        haptics.tap()
                        onUpdate { it.copy(inverter = value) }
                    },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = 2,
                    ),
                ) { Text(stringResource(label)) }
            }
        }

        if (spec == null) {
            Text(
                text = stringResource(R.string.rec_valid_capacity),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@SectionCard
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SpecTile(stringResource(R.string.rec_voltage), "${spec.voltage} V", Modifier.weight(1f))
            SpecTile(stringResource(R.string.rec_breaker), "${spec.breakerAmps} A", Modifier.weight(1f))
            SpecTile(stringResource(R.string.rec_wire), spec.wireGauge, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SpecTile(stringResource(R.string.rec_current), AppFormat.amperes(spec.estimatedAmps), Modifier.weight(1f))
            SpecTile(stringResource(R.string.rec_protector), "${spec.protector.joules} J", Modifier.weight(1f))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(
                    if (inverter) R.string.rec_protector_inverter else R.string.rec_protector_standard,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = spec.protector.brandAndModel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = stringResource(
                    R.string.rec_protector_detail,
                    spec.protector.joules,
                    spec.protector.reconnectLabel,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = spec.protector.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            spec.alternativeProtector?.let { alternative ->
                Text(
                    text = stringResource(
                        R.string.rec_alternative,
                        alternative.brandAndModel,
                        alternative.joules,
                        alternative.reconnectLabel,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

        if (inverter) {
            Text(
                text = stringResource(R.string.rec_protector_mandatory),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        spec.voltageNote?.let { note ->
            Text(
                text = note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        spec.installationAlert?.let { alert ->
            Text(
                text = alert,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(12.dp),
            )
        }
    }
}

@Composable
private fun SpecTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun FactorPill(factor: BtuFactor) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = factor.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "+${AppFormat.btuNumber(factor.addedBtu)} BTU/h",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun MetricField(
    label: String,
    value: String,
    suffix: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue -> if (newValue.length <= 10) onValueChange(newValue) },
        label = { Text(label) },
        suffix = { Text(suffix) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

@Preview(name = "Recomendar claro", showBackground = true, heightDp = 1700)
@Preview(
    name = "Recomendar oscuro",
    showBackground = true,
    heightDp = 1700,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun RecommendScreenPreview() {
    ConsumoEnergeticoTheme {
        RecommendScreen(state = RecommendUiState(), onUpdate = {})
    }
}
