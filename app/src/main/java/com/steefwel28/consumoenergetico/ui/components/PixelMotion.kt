package com.steefwel28.consumoenergetico.ui.components

import android.os.SystemClock
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.steefwel28.consumoenergetico.core.signature.CraftedBy
import kotlinx.coroutines.delay

/**
 * Movimiento Pixel: rebote corto para selecciones y despliegues.
 *
 * El resorte expresivo acompaña cambios de estado grandes; el snappy responde
 * a toques de chips, botones y desplegables sin sentirse elástico.
 */
val PixelSpringExpressive: SpringSpec<Float> = spring(
    dampingRatio = 0.62f,
    stiffness = Spring.StiffnessMediumLow,
)

val PixelSpringSnappy: SpringSpec<Float> = spring(
    dampingRatio = 0.78f,
    stiffness = Spring.StiffnessMedium,
)

/** Toques secos y precisos; `tap` para selección y `confirm` para acciones. */
@Stable
@CraftedBy("steefwel28")
class PixelHaptics internal constructor(private val haptic: HapticFeedback) {

    fun tap() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun confirm() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

@Composable
fun rememberPixelHaptics(): PixelHaptics {
    val haptic = LocalHapticFeedback.current
    return remember(haptic) { PixelHaptics(haptic) }
}

/**
 * Carga háptica al mantener presionado: el elemento crece elásticamente
 * (1.0 → 1.08) mientras el sistema emite ticks y a los 3,000 ms suelta el
 * "pop" con rebote. Usa la háptica nativa de Material 3
 * (`SegmentTick` y `Confirm`), sin `Vibrator` directo.
 * Observa el gesto en el paso Initial sin consumir eventos, así el clic corto
 * normal sigue funcionando.
 */
@Composable
fun Modifier.pixelChargeHaptics(): Modifier {
    val haptic = LocalHapticFeedback.current
    var charging by remember { mutableStateOf(false) }
    var popped by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = when {
            popped -> 1.14f
            charging -> 1.08f
            else -> 1f
        },
        animationSpec = PixelSpringExpressive,
        label = "pixel-charge-scale",
    )

    LaunchedEffect(charging) {
        if (!charging) return@LaunchedEffect
        val start = SystemClock.uptimeMillis()
        var elapsed = SystemClock.uptimeMillis() - start
        while (elapsed < CHARGE_MILLIS) {
            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
            delay(55)
            elapsed = SystemClock.uptimeMillis() - start
        }
        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        popped = true
        delay(160)
        popped = false
    }

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(pass = PointerEventPass.Initial)
                charging = true
                var pressed = true
                while (pressed) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    pressed = event.changes.any { it.id == down.id && it.pressed }
                }
                charging = false
            }
        }
}

private const val CHARGE_MILLIS = 3_000L
