package com.steefwel28.consumoenergetico.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.steefwel28.consumoenergetico.ui.theme.LocalLiquidGlass

/**
 * Controla el desenfoque del contenido de fondo mientras hay una hoja modal o
 * un diálogo abierto. Varias capas pueden coexistir: solo se restaura al
 * cerrarse la última.
 */
@Stable
class PixelOverlayState {

    var blurRadius by mutableStateOf(0.dp)
        private set

    private var visibleLayers = 0

    fun acquire() {
        visibleLayers += 1
        blurRadius = 10.dp
    }

    fun release() {
        visibleLayers = (visibleLayers - 1).coerceAtLeast(0)
        if (visibleLayers == 0) blurRadius = 0.dp
    }
}

val LocalPixelOverlayState = staticCompositionLocalOf { PixelOverlayState() }

/** Registra la capa visible y la desregistra al salir de composición. */
@Composable
fun BlurBackgroundWhileVisible() {
    val overlay = LocalPixelOverlayState.current
    DisposableEffect(overlay) {
        overlay.acquire()
        onDispose { overlay.release() }
    }
}

/**
 * Hoja modal Pixel: esquinas de 24 dp, tono bajo y fondo desenfocado.
 *
 * `Modifier.blur` solo dibuja en Android 12+; en versiones anteriores la hoja
 * conserva el scrim normal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PixelModalBottomSheet(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    BlurBackgroundWhileVisible()
    val haptics = rememberPixelHaptics()
    val glass = LocalLiquidGlass.current
    LaunchedEffect(Unit) { haptics.tap() }
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = if (glass) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = Color.Black.copy(alpha = if (glass) 0.42f else 0.32f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        if (glass) {
            // Sin HazeState dentro de la ventana del diálogo: tinte + canto de cristal.
            CompositionLocalProvider(LocalHazeState provides null) {
                LiquidGlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    lens = true,
                ) {
                    Column(content = content)
                }
            }
        } else {
            Column(content = content)
        }
    }
}

/** Diálogo de decisión con superficies tonales y el mismo desenfoque de fondo. */
@Composable
fun PixelDialog(
    title: String,
    text: String? = null,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    dismissLabel: String? = null,
    confirmEnabled: Boolean = true,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    BlurBackgroundWhileVisible()
    val haptics = rememberPixelHaptics()
    val glass = LocalLiquidGlass.current
    LaunchedEffect(Unit) { haptics.tap() }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = if (glass) {
            Modifier
                .clip(MaterialTheme.shapes.large)
                .liquidGlassLens(cornerRadius = 24.dp)
        } else {
            Modifier
        },
        title = { Text(title) },
        text = when {
            content != null -> {
                { Column { content() } }
            }
            text != null -> {
                { Text(text) }
            }
            else -> null
        },
        shape = MaterialTheme.shapes.large,
        containerColor = if (glass) {
            Color.Transparent
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = confirmEnabled,
                modifier = Modifier.pixelChargeHaptics(),
            ) { Text(confirmLabel) }
        },
        dismissButton = dismissLabel?.let { label ->
            { TextButton(onClick = onDismissRequest) { Text(label) } }
        },
    )
}
