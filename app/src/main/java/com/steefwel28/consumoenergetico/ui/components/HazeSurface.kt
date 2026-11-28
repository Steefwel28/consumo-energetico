package com.steefwel28.consumoenergetico.ui.components

import android.content.Context
import android.os.PowerManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.steefwel28.consumoenergetico.ui.theme.LocalLiquidGlass
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * Estado Haze compartido: el contenido lo publica con `hazeSource` y cada
 * barra, menú o tarjeta lo consume con `hazeEffect` para desenfocar lo que hay
 * detrás.
 */
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

/** El ahorro de energía apaga el blur real para sostener los 120 FPS. */
@Composable
fun rememberPowerSaveMode(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isPowerSaveMode == true
    }
}

/**
 * Desenfoque real del fondo para cualquier superficie flotante. Devuelve el
 * modifier intacto si el usuario apagó el efecto en Ajustes, si no hay
 * `HazeState` (hojas y diálogos viven en otra ventana de Android) o si el
 * equipo está en ahorro de energía.
 */
@Composable
fun Modifier.liquidGlassEffect(): Modifier {
    val hazeState = LocalHazeState.current
    val glass = LocalLiquidGlass.current
    val powerSave = rememberPowerSaveMode()
    if (!glass || hazeState == null || powerSave) return this
    val surface = MaterialTheme.colorScheme.surfaceContainer
    return hazeEffect(
        state = hazeState,
        style = HazeStyle(
            backgroundColor = surface,
            tint = HazeTint(surface.copy(alpha = 0.72f)),
            blurRadius = 20.dp,
            noiseFactor = 0.02f,
        ),
    )
}

/**
 * Superficie flotante de Material 3 con desenfoque Haze: color tonal sólido,
 * esquinas de 24 dp y blur del contenido que pasa por detrás. Sustituye al
 * shader AGSL y a los orbes animados, sin sacrificar legibilidad.
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable BoxScope.() -> Unit,
) {
    val hazeState = LocalHazeState.current
    val glass = LocalLiquidGlass.current
    val powerSave = rememberPowerSaveMode()
    val blur = glass && hazeState != null && !powerSave
    Box(
        modifier = modifier
            .clip(shape)
            .background(containerColor, shape)
            .then(
                if (blur) {
                    Modifier.hazeEffect(
                        state = hazeState,
                        style = HazeStyle(
                            backgroundColor = containerColor,
                            tint = HazeTint(containerColor.copy(alpha = 0.72f)),
                            blurRadius = 20.dp,
                            noiseFactor = 0.02f,
                        ),
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        content()
    }
}
