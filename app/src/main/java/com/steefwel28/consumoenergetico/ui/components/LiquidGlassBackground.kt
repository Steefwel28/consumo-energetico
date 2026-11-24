package com.steefwel28.consumoenergetico.ui.components

import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.steefwel28.consumoenergetico.ui.theme.LocalLiquidGlass
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlin.math.abs

/**
 * Captura el scroll vertical de cualquier descendiente (`LazyColumn` o
 * `verticalScroll`) y publica una fracción `0f..1f` para paralaxear los orbes.
 * Se acumula por delta, sin recomponer el contenido: solo lee el fondo.
 *
 * shortcut: la fracción es acumulativa (no posición exacta en píxeles) y se
 * reinicia al cambiar de pestaña; suficiente para el parallax atmosférico.
 */
class LiquidGlassScrollConnection(
    private val target: MutableFloatState,
) : NestedScrollConnection {
    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        val next = target.floatValue - available.y / 2_000f
        val changed = next.coerceIn(0f, 1f)
        if (abs(changed - target.floatValue) > 0.002f) target.floatValue = changed
        return Offset.Zero
    }
}

/**
 * Fondo atmosférico de Liquid Glass: orbes cian/esmeralda con desenfoque
 * acelerado por hardware (`Modifier.blur` usa `RenderEffect` desde Android 12).
 * En ahorro de batería el blur se apaga y quedan solo las superficies
 * translúcidas, sosteniendo los 120 FPS.
 *
 * Los orbes combinan una deriva temporal lenta con la fracción de scroll real
 * que publica [LiquidGlassScrollConnection] desde la pantalla activa.
 */
@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    scrollFraction: Float = 0f,
) {
    val powerSave = rememberPowerSaveMode()
    val blurAllowed = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !powerSave
    val scroll = scrollFraction.coerceIn(0f, 1f)
    val transition = rememberInfiniteTransition(label = "liquid-glass")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "liquid-drift",
    )

    val cyan = Color(0xFF22D3EE)
    val emerald = Color(0xFF34D399)
    val teal = Color(0xFF2DD4BF)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF04110F)),
    ) {
        Orb(
            color = cyan,
            diameter = 320.dp,
            x = (-90 + 140 * drift - 40 * scroll).dp,
            y = (-60 + 90 * drift - 120 * scroll).dp,
            alpha = 0.30f,
            blurRadius = 96.dp,
            blurAllowed = blurAllowed,
        )
        Orb(
            color = emerald,
            diameter = 360.dp,
            x = (180 - 120 * drift + 60 * scroll).dp,
            y = (360 + 150 * drift - 70 * scroll).dp,
            alpha = 0.26f,
            blurRadius = 110.dp,
            blurAllowed = blurAllowed,
        )
        Orb(
            color = teal,
            diameter = 220.dp,
            x = (60 + 180 * drift - 80 * scroll).dp,
            y = (620 - 120 * drift - 160 * scroll).dp,
            alpha = 0.20f,
            blurRadius = 80.dp,
            blurAllowed = blurAllowed,
        )
    }
}

/**
 * Estado Haze compartido: el fondo lo publica con `hazeSource` y cada tarjeta,
 * barra o menú lo consume con `hazeEffect` para desenfocar lo que hay detrás.
 */
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

/** El ahorro de energía apaga el blur real y sube el alfa del tinte. */
@Composable
fun rememberPowerSaveMode(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isPowerSaveMode == true
    }
}

/**
 * Desenfoque real del fondo para cualquier superficie de cristal. Devuelve el
 * modifier intacto si Liquid Glass está apagado o no hay `HazeState` (hojas y
 * diálogos viven en otra ventana de Android).
 */
@Composable
fun Modifier.liquidGlassEffect(): Modifier {
    if (!LocalLiquidGlass.current) return this
    val hazeState = LocalHazeState.current ?: return this
    val powerSave = rememberPowerSaveMode()
    return hazeEffect(
        state = hazeState,
        style = HazeStyle(
            backgroundColor = Color(0xFF0A1F1C),
            tint = HazeTint(Color(0xFF0A1F1C).copy(alpha = if (powerSave) 0.86f else 0.52f)),
            blurRadius = if (powerSave) 0.dp else 26.dp,
            noiseFactor = 0.03f,
        ),
    )
}

/**
 * Superficie de vidrio esmerilado estilo iOS 27: blur real del fondo (`haze`),
 * tinte translúcido, brillo interno superior y canto especular de 1 dp.
 *
 * Con Liquid Glass apagado se comporta como una tarjeta tonal normal.
 * `containerColor = Color.Transparent` deja el vidrio solo con su tinte, para
 * envolver hojas modales cuyo contenedor ya es transparente.
 *
 * shortcut: dentro de `ModalBottomSheet`/`DropdownMenu` (ventana propia de
 * Android) no hay `HazeState` accesible; ahí se pinta tinte + canto y el blur
 * real del fondo lo hace [com.steefwel28.consumoenergetico.ui.components.PixelOverlayState].
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    containerColor: Color = Color.Transparent,
    content: @Composable BoxScope.() -> Unit,
) {
    val glass = LocalLiquidGlass.current
    val hazeState = LocalHazeState.current
    val powerSave = rememberPowerSaveMode()
    val tint = Color(0xFF0A1F1C).copy(alpha = if (powerSave) 0.86f else 0.52f)

    Box(
        modifier = modifier
            .clip(shape)
            .then(Modifier.liquidGlassEffect())
            .then(
                when {
                    !glass -> Modifier.background(containerColor, shape)
                    hazeState == null -> Modifier.background(tint, shape)
                    else -> Modifier
                },
            )
            .then(if (glass) Modifier.border(liquidGlassBorder(), shape) else Modifier),
    ) {
        if (glass) {
            // Brillo interno en el 22 % superior: simula el grosor del cristal.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.White.copy(alpha = 0.14f),
                                0.22f to Color.Transparent,
                            ),
                        ),
                    ),
            )
        }
        content()
    }
}

@Composable
private fun BoxScope.Orb(
    color: Color,
    diameter: Dp,
    x: Dp,
    y: Dp,
    alpha: Float,
    blurRadius: Dp,
    blurAllowed: Boolean,
) {
    Box(
        modifier = Modifier
            .offset(x = x, y = y)
            .size(diameter)
            .graphicsLayer { this.alpha = alpha }
            .then(
                if (blurAllowed) {
                    Modifier.blur(blurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                } else {
                    Modifier
                },
            )
            .background(
                brush = Brush.radialGradient(
                    listOf(color.copy(alpha = 0.9f), color.copy(alpha = 0f)),
                ),
                shape = CircleShape,
            ),
    )
}
