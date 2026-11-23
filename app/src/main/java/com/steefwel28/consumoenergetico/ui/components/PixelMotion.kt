package com.steefwel28.consumoenergetico.ui.components

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
 * Vibración de carga progresiva estilo Duolingo/IG: `LOW_TICK → TICK → SPIN`
 * subiendo de 0.15 a 1.0 cada ~55 ms, con fallback de amplitud. Al cumplir
 * 3 s dispara el "pop" de liberación. Sin primitivas (API < 31) usa
 * `EFFECT_HEAVY_CLICK`.
 */
@Stable
@CraftedBy("steefwel28")
class PixelChargeVibrator internal constructor(private val vibrator: Vibrator?) {

    fun tick(progress: Float) {
        val v = vibrator ?: return
        val p = progress.coerceIn(0f, 1f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            v.areAllPrimitivesSupported(
                VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
                VibrationEffect.Composition.PRIMITIVE_TICK,
                VibrationEffect.Composition.PRIMITIVE_SPIN,
            )
        ) {
            val primitive = when {
                p < 0.34f -> VibrationEffect.Composition.PRIMITIVE_LOW_TICK
                p < 0.67f -> VibrationEffect.Composition.PRIMITIVE_TICK
                else -> VibrationEffect.Composition.PRIMITIVE_SPIN
            }
            val scale = 0.15f + 0.85f * p
            v.vibrate(VibrationEffect.startComposition().addPrimitive(primitive, scale).compose())
        } else if (v.hasAmplitudeControl()) {
            v.vibrate(VibrationEffect.createOneShot(30, (40 + 215 * p).toInt().coerceIn(1, 255)))
        } else {
            v.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    fun pop() {
        val v = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            v.areAllPrimitivesSupported(
                VibrationEffect.Composition.PRIMITIVE_THUD,
                VibrationEffect.Composition.PRIMITIVE_QUICK_FALL,
            )
        ) {
            v.vibrate(
                VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 1f)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 0.7f)
                    .compose(),
            )
        } else {
            v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        }
    }
}

@Composable
fun rememberPixelChargeVibrator(): PixelChargeVibrator {
    val context = LocalContext.current
    return remember(context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        PixelChargeVibrator(vibrator?.takeIf { it.hasVibrator() })
    }
}

/**
 * Carga háptica al mantener presionado: el elemento crece elásticamente
 * (1.0 → 1.08) mientras sube la vibración y a los 3,000 ms suelta el "pop"
 * con rebote. Observa el gesto en el paso Initial sin consumir eventos, así
 * el clic corto normal sigue funcionando.
 */
@Composable
fun Modifier.pixelChargeHaptics(): Modifier {
    val vibrator = rememberPixelChargeVibrator()
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
            vibrator.tick(elapsed.toFloat() / CHARGE_MILLIS)
            delay(55)
            elapsed = SystemClock.uptimeMillis() - start
        }
        vibrator.pop()
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
