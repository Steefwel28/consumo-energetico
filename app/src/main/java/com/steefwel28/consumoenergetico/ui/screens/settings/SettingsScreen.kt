@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.steefwel28.consumoenergetico.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.steefwel28.consumoenergetico.BuildConfig
import com.steefwel28.consumoenergetico.core.di.AppContainer
import com.steefwel28.consumoenergetico.domain.calculator.PanamaUtility
import com.steefwel28.consumoenergetico.domain.units.LengthUnit
import com.steefwel28.consumoenergetico.domain.units.LocaleNumber
import com.steefwel28.consumoenergetico.domain.units.ShortLengthUnit
import com.steefwel28.consumoenergetico.domain.units.TemperatureUnit
import com.steefwel28.consumoenergetico.domain.units.VolumeUnit
import com.steefwel28.consumoenergetico.ui.AppViewModelProvider
import com.steefwel28.consumoenergetico.ui.components.AppFormat
import com.steefwel28.consumoenergetico.ui.components.PixelDialog
import com.steefwel28.consumoenergetico.ui.components.SectionCard
import com.steefwel28.consumoenergetico.ui.components.rememberPixelHaptics
import com.steefwel28.consumoenergetico.ui.theme.ConsumoEnergeticoTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private const val REPOSITORY_URL = "https://github.com/Steefwel28/consumo-energetico"

@Composable
fun SettingsRoute(
    container: AppContainer,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.settings(container)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val json = viewModel.exportJson()
            if (json.isEmpty()) return@launch
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(json.toByteArray(Charsets.UTF_8))
                } ?: error("No se pudo abrir el archivo elegido.")
            }.onFailure { error ->
                viewModel.reportFileProblem(
                    "No se pudo escribir el respaldo: ${error.message ?: "permiso denegado"}",
                )
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val json = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
                    ?: error("No se pudo leer el archivo.")
            }.getOrElse { error ->
                viewModel.reportFileProblem("No se pudo leer el respaldo: ${error.message}")
                return@launch
            }
            viewModel.restoreJson(json)
        }
    }

    SettingsScreen(
        state = state,
        onLength = viewModel::setLength,
        onShortLength = viewModel::setShortLength,
        onVolume = viewModel::setVolume,
        onTemperature = viewModel::setTemperature,
        onUtility = viewModel::setUtility,
        onBaseKwh = viewModel::setBaseMonthlyKwh,
        onCheckUpdate = viewModel::checkForUpdate,
        onSkipVersion = viewModel::skipPendingVersion,
        onVersionTapped = viewModel::onVersionTapped,
        onDismissSignature = viewModel::dismissSignature,
        onDismissMessage = viewModel::dismissMessage,
        onExport = { exportLauncher.launch(defaultBackupName()) },
        onImport = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
        onOpenRepository = {
            runCatching { uriHandler.openUri(REPOSITORY_URL) }
        },
        onOpenRelease = { url ->
            runCatching { uriHandler.openUri(url) }
        },
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onLength: (LengthUnit) -> Unit,
    onShortLength: (ShortLengthUnit) -> Unit,
    onVolume: (VolumeUnit) -> Unit,
    onTemperature: (TemperatureUnit) -> Unit,
    onUtility: (PanamaUtility) -> Unit,
    onBaseKwh: (Double) -> Unit,
    onCheckUpdate: () -> Unit,
    onSkipVersion: () -> Unit,
    onVersionTapped: () -> Unit,
    onDismissSignature: () -> Unit,
    onDismissMessage: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onOpenRepository: () -> Unit,
    onOpenRelease: (String) -> Unit,
) {
    val haptics = rememberPixelHaptics()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Ajustes", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Unidades, recibo, respaldo y actualizaciones de la app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SectionCard(
                title = "Unidades globales",
                subtitle = "Cambian cómo se captura y se muestra cada dato; el motor siempre trabaja en SI.",
            ) {
                UnitRow(
                    title = "Longitud",
                    options = LengthUnit.entries.map { it to it.symbol },
                    selected = state.unitPreferences.length,
                    onSelect = onLength,
                )
                UnitRow(
                    title = "Longitud corta",
                    options = ShortLengthUnit.entries.map { it to it.symbol },
                    selected = state.unitPreferences.shortLength,
                    onSelect = onShortLength,
                )
                UnitRow(
                    title = "Volumen",
                    options = VolumeUnit.entries.map { it to it.symbol },
                    selected = state.unitPreferences.volume,
                    onSelect = onVolume,
                )
                UnitRow(
                    title = "Temperatura",
                    options = TemperatureUnit.entries.map { it to it.symbol },
                    selected = state.unitPreferences.temperature,
                    onSelect = onTemperature,
                )
            }
        }

        item {
            BillBaseCard(
                utility = state.utility,
                baseKwh = state.baseMonthlyKwh,
                onUtility = onUtility,
                onBaseKwh = onBaseKwh,
            )
        }

        item {
            SectionCard(
                title = "Respaldo .json",
                subtitle = "Exporta grupos, equipos, ubicaciones e historial; restaura cuando cambies de teléfono.",
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            haptics.confirm()
                            onExport()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.CloudUpload, contentDescription = null)
                        Text(text = "Exportar", modifier = Modifier.padding(start = 6.dp))
                    }
                    OutlinedButton(
                        onClick = {
                            haptics.confirm()
                            onImport()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.CloudDownload, contentDescription = null)
                        Text(text = "Restaurar", modifier = Modifier.padding(start = 6.dp))
                    }
                }
                Text(
                    text = "La restauración reemplaza la base actual. El archivo debe estar firmado por steefwel28.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            UpdatesCard(
                state = state,
                onCheckUpdate = onCheckUpdate,
                onSkipVersion = onSkipVersion,
                onVersionTapped = onVersionTapped,
                onOpenRepository = onOpenRepository,
                onOpenRelease = onOpenRelease,
            )
        }
    }

    if (state.signatureUnlocked) {
        PixelDialog(
            title = "Firma de autor",
            text = "Consumo Energético v${BuildConfig.VERSION_NAME}\n\n" +
                "Diseñado y programado por ${BuildConfig.AUTHOR_SIGNATURE}.\n" +
                "Hecho en Panamá, offline-first y sin rastreo.",
            confirmLabel = "Cerrar",
            onConfirm = onDismissSignature,
            onDismissRequest = onDismissSignature,
        )
    }

    state.message?.let { message ->
        PixelDialog(
            title = "Ajustes",
            text = message,
            confirmLabel = "Entendido",
            onConfirm = onDismissMessage,
            onDismissRequest = onDismissMessage,
        )
    }
}

@Composable
private fun <T> UnitRow(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    val haptics = rememberPixelHaptics()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (value, label) ->
                SegmentedButton(
                    selected = value == selected,
                    onClick = {
                        haptics.tap()
                        onSelect(value)
                    },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = options.size,
                    ),
                ) { Text(label) }
            }
        }
    }
}

@Composable
private fun BillBaseCard(
    utility: PanamaUtility,
    baseKwh: Double,
    onUtility: (PanamaUtility) -> Unit,
    onBaseKwh: (Double) -> Unit,
) {
    val haptics = rememberPixelHaptics()
    var input by remember(baseKwh) {
        mutableStateOf(if (baseKwh == 0.0) "" else AppFormat.kwhNumber(baseKwh))
    }
    val parsed = LocaleNumber.parse(input)
    val invalid = input.isNotBlank() && (parsed == null || parsed < 0.0)

    SectionCard(
        title = "Recibo y distribuidora",
        subtitle = "El consumo base es lo que gastas sin contar los aires; define el salto de bloque.",
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            PanamaUtility.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = utility == option,
                    onClick = {
                        haptics.tap()
                        onUtility(option)
                    },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = PanamaUtility.entries.size,
                    ),
                ) { Text(option.displayName) }
            }
        }
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Consumo base del recibo (kWh/mes)") },
            placeholder = { Text("180") },
            singleLine = true,
            isError = invalid,
            supportingText = if (invalid) {
                { Text("Escribe un número válido (0 o más).") }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                haptics.confirm()
                onBaseKwh(parsed ?: 0.0)
            },
            enabled = !invalid,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Guardar consumo base")
        }
    }
}

@Composable
private fun UpdatesCard(
    state: SettingsUiState,
    onCheckUpdate: () -> Unit,
    onSkipVersion: () -> Unit,
    onVersionTapped: () -> Unit,
    onOpenRepository: () -> Unit,
    onOpenRelease: (String) -> Unit,
) {
    val haptics = rememberPixelHaptics()
    SectionCard(
        title = "Actualizaciones",
        subtitle = "La app solo consulta GitHub: sin analítica ni servicios de terceros.",
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Versión ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                haptics.tap()
                                onVersionTapped()
                            }
                            .padding(4.dp),
                    )
                    if (state.hasPendingUpdate) {
                        BadgedBox(
                            modifier = Modifier.padding(start = 6.dp),
                            badge = {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError,
                                ) { Text("A") }
                            },
                        ) {
                            Text(
                                text = state.pendingRelease?.version.orEmpty(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Text(
                    text = if (state.hasPendingUpdate) {
                        "Hay una versión nueva publicada en GitHub."
                    } else {
                        "Toca la versión 5 veces para ver la firma."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(
                onClick = {
                    haptics.tap()
                    onCheckUpdate()
                },
                enabled = !state.isChecking,
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null)
                Text(
                    text = if (state.isChecking) "Buscando…" else "Buscar",
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }

        state.pendingRelease?.let { release ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = release.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = release.notes.take(240).ifBlank { "Sin notas de la versión." },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            haptics.confirm()
                            onOpenRelease(release.apkDownloadUrl ?: release.releaseUrl)
                        },
                    ) { Text("Actualizar") }
                    OutlinedButton(
                        onClick = {
                            haptics.tap()
                            onSkipVersion()
                        },
                    ) { Text("Omitir") }
                }
            }
        }

        OutlinedButton(onClick = onOpenRepository, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.OpenInNew, contentDescription = null)
            Text(text = "Repositorio en GitHub", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

private fun defaultBackupName(): String {
    val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
    return "consumo-energetico-$stamp.json"
}

@Preview(name = "Ajustes claro", showBackground = true, heightDp = 1200)
@Preview(
    name = "Ajustes oscuro",
    showBackground = true,
    heightDp = 1200,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun SettingsScreenPreview() {
    ConsumoEnergeticoTheme {
        SettingsScreen(
            state = SettingsUiState(),
            onLength = {},
            onShortLength = {},
            onVolume = {},
            onTemperature = {},
            onUtility = {},
            onBaseKwh = {},
            onCheckUpdate = {},
            onSkipVersion = {},
            onVersionTapped = {},
            onDismissSignature = {},
            onDismissMessage = {},
            onExport = {},
            onImport = {},
            onOpenRepository = {},
            onOpenRelease = {},
        )
    }
}
