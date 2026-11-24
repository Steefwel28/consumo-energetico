package com.steefwel28.consumoenergetico.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.steefwel28.consumoenergetico.BuildConfig
import com.steefwel28.consumoenergetico.R
import com.steefwel28.consumoenergetico.ui.components.PixelEmblem
import com.steefwel28.consumoenergetico.ui.components.rememberPixelChargeVibrator
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Splash monocromático de 2.0 s: el emblema muta con resorte por las
 * Material Expressive Shapes (óvalo → diamante suave → squircle → cookie),
 * los contornos translúcidos hacen eco de la mutación y las fichas con la
 * chispa aparecen escalonadas dentro del marco festoneado.
 */
@Composable
fun ExpressiveSplashScreen(onFinished: () -> Unit) {
    val dark = isSystemInDarkTheme()
    val paper = if (dark) Color.Black else Color.White
    val ink = if (dark) Color.White else Color.Black
    val haptics = rememberPixelChargeVibrator()

    val morph = remember { Animatable(0f) }
    val emblemAppear = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val emblemScale = remember { Animatable(1f) }

    // Fichas translúcidas que acompañan a la cookie en la etapa 3.
    val scheme = MaterialTheme.colorScheme
    val chips = listOf(
        SplashChip(Alignment.TopStart, 30.dp, 56.dp, 44.dp, 16.dp, scheme.surfaceVariant, 0.85f, 1_300L),
        SplashChip(Alignment.TopEnd, (-28).dp, 104.dp, 26.dp, 16.dp, scheme.primary, 0.45f, 1_450L),
        SplashChip(Alignment.BottomStart, 50.dp, (-60).dp, 28.dp, 14.dp, scheme.primary, 0.35f, 1_600L),
        SplashChip(Alignment.BottomEnd, (-42).dp, (-40).dp, 46.dp, 16.dp, scheme.surfaceVariant, 0.70f, 1_750L),
    )
    val chipReveal = remember { List(chips.size) { Animatable(0f) } }

    // Coreografía háptica absoluta en 3 etapas sobre los 2.0 s del splash:
    // 0–600 ms un tick, 600–1,200 ms dos pulsos, 1,200–2,000 ms rampa + "pop".
    LaunchedEffect(Unit) {
        haptics.tick(0.25f)
        delay(600)
        haptics.tick(0.45f)
        delay(65)
        haptics.tick(0.52f)
        delay(535) // cierra la etapa 2 en 1,200 ms
        for (step in 0..15) {
            haptics.tick(0.58f + 0.42f * step / 15f)
            delay(50) // la última iteración aterriza en 2,000 ms
        }
        // El "pop" suena antes de ceder el control a la pantalla principal.
        haptics.pop()
        onFinished()
    }
    // Rebote del emblema: sobrepasa a 1.16 y aterriza en 1.12 exactos a los 2,000 ms.
    LaunchedEffect(Unit) {
        delay(1_850)
        emblemScale.animateTo(
            targetValue = 1.12f,
            animationSpec = keyframes {
                durationMillis = 150
                1.16f at 90
            },
        )
    }
    LaunchedEffect(Unit) {
        // Etapa 1 (0–600 ms) óvalo → diamante, etapa 2 (600–1,200 ms)
        // diamante → squircle y etapa 3 (1,200–2,000 ms) squircle → cookie.
        morph.animateTo(
            targetValue = 3f,
            animationSpec = keyframes {
                durationMillis = 2_000
                0f at 0
                1f at 600
                2f at 1_200
                3f at 2_000
            },
        )
    }
    LaunchedEffect(Unit) {
        delay(600)
        emblemAppear.animateTo(1f, tween(700))
    }
    LaunchedEffect(Unit) {
        delay(1_100)
        textAlpha.animateTo(1f, tween(450))
    }
    LaunchedEffect(Unit) {
        chips.forEachIndexed { index, chip ->
            launch {
                delay(chip.revealAtMs)
                chipReveal[index].animateTo(1f, tween(durationMillis = 160))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(paper),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.graphicsLayer {
                    scaleX = emblemScale.value
                    scaleY = emblemScale.value
                },
            ) {
                Canvas(modifier = Modifier.size(240.dp)) {
                    val base = size.minDimension * 0.30f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val index = floor(morph.value).toInt().coerceIn(0, 2)
                    val fraction = (morph.value - index).coerceIn(0f, 1f)
                    val path = morphPath(base, index, fraction, center)
                    drawPath(path, ink)
                    listOf(1.24f to 0.16f, 1.48f to 0.08f).forEach { (scale, alpha) ->
                        withTransform({ scale(scale, scale, center) }) {
                            drawPath(path, ink.copy(alpha = alpha), style = Stroke(width = 2.dp.toPx()))
                        }
                    }
                }
                PixelEmblem(
                    color = paper,
                    appear = emblemAppear.value,
                    modifier = Modifier.size(124.dp),
                )
                chips.forEachIndexed { index, chip ->
                    val eased = chipReveal[index].value
                    Box(
                        modifier = Modifier
                            .align(chip.alignment)
                            .offset(x = chip.x, y = chip.y + 8.dp * (1f - eased))
                            .size(chip.width, chip.height)
                            .graphicsLayer {
                                alpha = eased * chip.maxAlpha
                                scaleX = 0.55f + 0.45f * eased
                                scaleY = 0.55f + 0.45f * eased
                            }
                            .background(chip.color, RoundedCornerShape(percent = 45)),
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.alpha(textAlpha.value),
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    color = ink,
                )
                Text(
                    text = stringResource(
                        R.string.version_signature,
                        BuildConfig.VERSION_NAME,
                        BuildConfig.AUTHOR_SIGNATURE,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = ink.copy(alpha = 0.6f),
                )
            }
        }
    }
}

/** Interpola el radio polar entre dos formas expresivas consecutivas. */
private fun morphPath(
    base: Float,
    index: Int,
    fraction: Float,
    center: Offset,
): Path {
    val steps = 140
    return Path().apply {
        for (step in 0..steps) {
            val theta = (2 * Math.PI * step / steps).toFloat()
            val from = shapeRadius(index, theta, base)
            val to = shapeRadius(index + 1, theta, base)
            val radius = from + (to - from) * fraction
            val x = center.x + radius * cos(theta)
            val y = center.y + radius * sin(theta)
            if (step == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}

private fun shapeRadius(shape: Int, theta: Float, base: Float): Float = when (shape) {
    0 -> {
        val a = base * 1.30f
        val b = base * 0.82f
        a * b / sqrt((b * cos(theta)).pow(2) + (a * sin(theta)).pow(2))
    }
    1 -> superellipse(theta - (Math.PI / 4).toFloat(), 2.6f) * base * 1.14f
    2 -> superellipse(theta, 4f) * base * 1.04f
    else -> base * (1f + 0.09f * cos(9f * theta))
}

private fun superellipse(theta: Float, n: Float): Float =
    (abs(cos(theta)).pow(n) + abs(sin(theta)).pow(n)).pow(-1f / n)

/** Ficha translúcida de la etapa 3: posición relativa, tamaño y momento de aparición. */
private data class SplashChip(
    val alignment: Alignment,
    val x: Dp,
    val y: Dp,
    val width: Dp,
    val height: Dp,
    val color: Color,
    val maxAlpha: Float,
    val revealAtMs: Long,
)