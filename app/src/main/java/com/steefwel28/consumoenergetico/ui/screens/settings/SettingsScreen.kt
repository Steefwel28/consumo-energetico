@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.steefwel28.consumoenergetico.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.steefwel28.consumoenergetico.BuildConfig
import com.steefwel28.consumoenergetico.R
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
import com.steefwel28.consumoenergetico.ui.components.PixelEmblem
import com.steefwel28.consumoenergetico.ui.components.PixelSpringExpressive
import com.steefwel28.consumoenergetico.ui.components.SectionCard
import com.steefwel28.consumoenergetico.ui.components.pixelChargeHaptics
import com.steefwel28.consumoenergetico.ui.components.rememberPixelChargeVibrator
import com.steefwel28.consumoenergetico.ui.components.rememberPixelHaptics
import com.steefwel28.consumoenergetico.ui.theme.ConsumoEnergeticoTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private const val REPOSITORY_URL = "https://github.com/Steefwel28/consumo-energetico"
private const val YAPPY_PACKAGE = "com.bgeneral.yappy"
private const val BANCO_GENERAL_PACKAGE = "pa.com.bgeneral.mobile"
private const val YAPPY_NUMBER_RAW = "63285079"

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
                } ?: error(context.getString(R.string.set_msg_open_file))
            }.onFailure { error ->
                viewModel.reportFileProblem(
                    context.getString(
                        R.string.set_msg_write_error,
                        error.message ?: context.getString(R.string.set_permission_denied),
                    ),
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
                    ?: error(context.getString(R.string.set_msg_read_file))
            }.getOrElse { error ->
                viewModel.reportFileProblem(
                    context.getString(R.string.set_msg_read_error, error.message.orEmpty()),
                )
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
                Text(text = stringResource(R.string.set_title), style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = stringResource(R.string.set_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item { YappyCard() }

        item { LanguageCard() }

        item {
            SectionCard(
                title = stringResource(R.string.set_units_title),
                subtitle = stringResource(R.string.set_units_subtitle),
            ) {
                UnitRow(
                    title = stringResource(R.string.set_length),
                    options = LengthUnit.entries.map { it to it.symbol },
                    selected = state.unitPreferences.length,
                    onSelect = onLength,
                )
                UnitRow(
                    title = stringResource(R.string.set_short_length),
                    options = ShortLengthUnit.entries.map { it to it.symbol },
                    selected = state.unitPreferences.shortLength,
                    onSelect = onShortLength,
                )
                UnitRow(
                    title = stringResource(R.string.set_volume),
                    options = VolumeUnit.entries.map { it to it.symbol },
                    selected = state.unitPreferences.volume,
                    onSelect = onVolume,
                )
                UnitRow(
                    title = stringResource(R.string.set_temperature),
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
                title = stringResource(R.string.set_backup_title),
                subtitle = stringResource(R.string.set_backup_subtitle),
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
                        Text(text = stringResource(R.string.set_export), modifier = Modifier.padding(start = 6.dp))
                    }
                    OutlinedButton(
                        onClick = {
                            haptics.confirm()
                            onImport()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.CloudDownload, contentDescription = null)
                        Text(text = stringResource(R.string.set_import), modifier = Modifier.padding(start = 6.dp))
                    }
                }
                Text(
                    text = stringResource(R.string.set_backup_warning),
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
        SignatureDialog(onDismiss = onDismissSignature)
    }

    state.message?.let { message ->
        PixelDialog(
            title = stringResource(R.string.set_title),
            text = message,
            confirmLabel = stringResource(R.string.common_understood),
            onConfirm = onDismissMessage,
            onDismissRequest = onDismissMessage,
        )
    }
}

/** Tarjeta "Invítame un café": copia el número Yappy y abre la app si existe. */
@Composable
private fun YappyCard() {
    val context = LocalContext.current
    val haptics = rememberPixelHaptics()
    val yappyBlue = Color(0xFF00B4D8)
    val yappyContainer = if (androidx.compose.foundation.isSystemInDarkTheme()) {
        Color(0xFF062A33)
    } else {
        Color(0xFFDDF4FA)
    }
    val yappyInk = if (androidx.compose.foundation.isSystemInDarkTheme()) {
        Color(0xFF7FE3F7)
    } else {
        Color(0xFF00566B)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(yappyContainer)
            .pixelChargeHaptics()
            .clickable {
                haptics.confirm()
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Yappy", YAPPY_NUMBER_RAW))
                val launch = context.packageManager.getLaunchIntentForPackage(YAPPY_PACKAGE)
                    ?: context.packageManager.getLaunchIntentForPackage(BANCO_GENERAL_PACKAGE)
                if (launch != null) {
                    Toast.makeText(context, R.string.yappy_toast_opening, Toast.LENGTH_SHORT).show()
                    runCatching { context.startActivity(launch) }
                } else {
                    Toast.makeText(context, R.string.yappy_toast_copied, Toast.LENGTH_SHORT).show()
                }
            }
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.LocalCafe,
            contentDescription = null,
            tint = yappyInk,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(yappyBlue.copy(alpha = 0.18f))
                .padding(10.dp),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.yappy_title),
                style = MaterialTheme.typography.titleMedium,
                color = yappyInk,
            )
            Text(
                text = stringResource(R.string.yappy_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = yappyInk.copy(alpha = 0.8f),
            )
            Text(
                text = stringResource(R.string.yappy_number),
                style = MaterialTheme.typography.headlineSmall,
                color = yappyBlue,
            )
        }
    }
}

/** Selector de idioma conectado a AppCompatDelegate; cambia al instante. */
@Composable
private fun LanguageCard() {
    val haptics = rememberPixelHaptics()
    val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        .ifBlank { Locale.getDefault().language }
    val options = listOf(
        "es" to stringResource(R.string.lang_es),
        "en" to stringResource(R.string.lang_en),
        "zh-CN" to stringResource(R.string.lang_zh),
    )
    SectionCard(
        title = stringResource(R.string.lang_title),
        subtitle = stringResource(R.string.lang_subtitle),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { (tag, label) ->
                val selected = current == tag || current.startsWith("$tag-") ||
                    (tag == "zh-CN" && current.startsWith("zh"))
                FilterChip(
                    selected = selected,
                    onClick = {
                        haptics.tap()
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                    },
                    label = { Text(label) },
                    leadingIcon = if (selected) {
                        { Icon(Icons.Rounded.Language, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

/** Easter egg: emblema animado + la frase meme y la firma de autor. */
@Composable
private fun SignatureDialog(onDismiss: () -> Unit) {
    val vibrator = rememberPixelChargeVibrator()
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        vibrator.pop()
        appear.animateTo(1f, PixelSpringExpressive)
    }
    PixelDialog(
        title = stringResource(R.string.sig_title),
        confirmLabel = stringResource(R.string.common_close),
        onConfirm = onDismiss,
        onDismissRequest = onDismiss,
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PixelEmblem(
                    color = MaterialTheme.colorScheme.primary,
                    appear = appear.value,
                    modifier = Modifier.size(96.dp),
                )
                Text(
                    text = stringResource(R.string.sig_meme),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.sig_author),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.sig_handle),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
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
        title = stringResource(R.string.set_bill_title),
        subtitle = stringResource(R.string.set_bill_subtitle),
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
            label = { Text(stringResource(R.string.common_base_kwh)) },
            placeholder = { Text("180") },
            singleLine = true,
            isError = invalid,
            supportingText = if (invalid) {
                { Text(stringResource(R.string.set_base_invalid)) }
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
            Text(stringResource(R.string.set_base_save))
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
        title = stringResource(R.string.set_updates_title),
        subtitle = stringResource(R.string.set_updates_subtitle),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.set_version, BuildConfig.VERSION_NAME),
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
                        stringResource(R.string.set_update_available)
                    } else {
                        stringResource(R.string.set_version_hint)
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
                    text = if (state.isChecking) {
                        stringResource(R.string.set_searching)
                    } else {
                        stringResource(R.string.set_search)
                    },
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }

        state.pendingRelease?.let { release ->
            val noNotes = stringResource(R.string.set_no_notes)
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
                    text = release.notes.take(240).ifBlank { noNotes },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            haptics.confirm()
                            onOpenRelease(release.apkDownloadUrl ?: release.releaseUrl)
                        },
                    ) { Text(stringResource(R.string.set_update)) }
                    OutlinedButton(
                        onClick = {
                            haptics.tap()
                            onSkipVersion()
                        },
                    ) { Text(stringResource(R.string.set_skip)) }
                }
            }
        }

        OutlinedButton(onClick = onOpenRepository, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null)
            Text(text = stringResource(R.string.set_repository), modifier = Modifier.padding(start = 8.dp))
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