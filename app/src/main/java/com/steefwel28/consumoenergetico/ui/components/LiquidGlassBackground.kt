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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    // Se consulta al componer; si el usuario cambia el ahorro, se reevalúa al volver.
    val powerSave = remember(context) {
        (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isPowerSaveMode == true
    }
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

    val cyan = if (dark) Color(0xFF22D3EE) else Color(0xFF7FE3F7)
    val emerald = if (dark) Color(0xFF34D399) else Color(0xFF6EE7B7)
    val teal = if (dark) Color(0xFF2DD4BF) else Color(0xFF99F6E4)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (dark) Color(0xFF04110F) else Color(0xFFF2FBF8)),
    ) {
        Orb(
            color = cyan,
            diameter = 320.dp,
            x = (-90 + 140 * drift - 40 * scroll).dp,
            y = (-60 + 90 * drift - 120 * scroll).dp,
            alpha = if (dark) 0.30f else 0.42f,
            blurRadius = 96.dp,
            blurAllowed = blurAllowed,
        )
        Orb(
            color = emerald,
            diameter = 360.dp,
            x = (180 - 120 * drift + 60 * scroll).dp,
            y = (360 + 150 * drift - 70 * scroll).dp,
            alpha = if (dark) 0.26f else 0.34f,
            blurRadius = 110.dp,
            blurAllowed = blurAllowed,
        )
        Orb(
            color = teal,
            diameter = 220.dp,
            x = (60 + 180 * drift - 80 * scroll).dp,
            y = (620 - 120 * drift - 160 * scroll).dp,
            alpha = if (dark) 0.20f else 0.28f,
            blurRadius = 80.dp,
            blurAllowed = blurAllowed,
        )
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
