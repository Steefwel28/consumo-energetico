package com.steefwel28.consumoenergetico.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.steefwel28.consumoenergetico.BuildConfig
import com.steefwel28.consumoenergetico.R
import com.steefwel28.consumoenergetico.ui.components.PixelEmblem
import com.steefwel28.consumoenergetico.ui.components.rememberPixelHaptics
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.delay

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
    val haptics = rememberPixelHaptics()

    val morph = remember { Animatable(0f) }
    val emblemAppear = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        val springSpec = spring<Float>(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)
        haptics.tap()
        morph.animateTo(1f, springSpec)
        haptics.tap()
        morph.animateTo(2f, springSpec)
        haptics.tap()
        morph.animateTo(3f, springSpec)
    }
    LaunchedEffect(Unit) {
        delay(850)
        emblemAppear.animateTo(1f, tween(700))
    }
    LaunchedEffect(Unit) {
        delay(1_100)
        textAlpha.animateTo(1f, tween(450))
    }
    LaunchedEffect(Unit) {
        delay(2_000)
        onFinished()
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
            Box(contentAlignment = Alignment.Center) {
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