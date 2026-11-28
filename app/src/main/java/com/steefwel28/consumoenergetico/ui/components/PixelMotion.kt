package com.steefwel28.consumoenergetico.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.steefwel28.consumoenergetico.core.signature.CraftedBy

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
