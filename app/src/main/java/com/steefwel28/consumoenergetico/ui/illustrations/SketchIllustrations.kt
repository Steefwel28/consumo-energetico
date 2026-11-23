@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.steefwel28.consumoenergetico.ui.illustrations

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.steefwel28.consumoenergetico.R
import com.steefwel28.consumoenergetico.core.signature.CraftedBy
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Las cinco cápsulas educativas viven dibujadas a plumilla sobre `Canvas`.
 *
 * El trazo es fino y no depende de assets: `ink` y `accent` salen del tema,
 * así que el boceto funciona igual en modo claro y oscuro.
 */

/* ---------------- Las cinco ilustraciones ---------------- */

@Composable
fun SeerEfficiencySketch(modifier: Modifier = Modifier) {
    SketchCanvas(modifier) { ink, accent, muted ->
        val stroke = sketchStroke()
        // Split de pared con rejilla.
        drawRoundRect(
            color = ink,
            topLeft = Offset(size.width * 0.08f, size.height * 0.16f),
            size = Size(size.width * 0.52f, size.height * 0.30f),
            cornerRadius = CornerRadius(size.height * 0.08f),
            style = stroke,
        )
        repeat(3) { index ->
            val y = size.height * (0.24f + index * 0.06f)
            drawLine(
                color = muted,
                start = Offset(size.width * 0.14f, y),
                end = Offset(size.width * 0.30f, y),
                strokeWidth = stroke.width,
                cap = StrokeCap.Round,
            )
        }
        // Flujo de aire hacia la hoja.
        repeat(3) { index ->
            val startY = size.height * (0.52f + index * 0.10f)
            drawPath(
                path = curledArrow(
                    start = Offset(size.width * 0.12f, startY),
                    end = Offset(size.width * 0.52f, startY + size.height * 0.02f),
                ),
                color = accent,
                style = stroke,
            )
        }
        // Hoja de eficiencia.
        val leaf = Path().apply {
            moveTo(size.width * 0.60f, size.height * 0.40f)
            cubicTo(
                size.width * 0.76f,
                size.height * 0.10f,
                size.width * 0.92f,
                size.height * 0.30f,
                size.width * 0.82f,
                size.height * 0.58f,
            )
            cubicTo(
                size.width * 0.70f,
                size.height * 0.44f,
                size.width * 0.66f,
                size.height * 0.50f,
                size.width * 0.60f,
                size.height * 0.40f,
            )
        }
        drawPath(leaf, color = accent, style = stroke)
        drawLine(
            color = accent,
            start = Offset(size.width * 0.64f, size.height * 0.48f),
            end = Offset(size.width * 0.78f, size.height * 0.30f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
        // Medidor SEER.
        drawCircle(
            color = muted,
            radius = size.height * 0.20f,
            center = Offset(size.width * 0.88f, size.height * 0.78f),
            style = stroke,
        )
        drawLine(
            color = ink,
            start = Offset(size.width * 0.88f, size.height * 0.78f),
            end = Offset(size.width * 0.95f, size.height * 0.68f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = ink,
            radius = size.height * 0.018f,
            center = Offset(size.width * 0.88f, size.height * 0.78f),
        )
    }
}

@Composable
fun RemoteControlMythSketch(modifier: Modifier = Modifier) {
    SketchCanvas(modifier) { ink, accent, muted ->
        val stroke = sketchStroke()
        // Control remoto.
        drawRoundRect(
            color = ink,
            topLeft = Offset(size.width * 0.10f, size.height * 0.08f),
            size = Size(size.width * 0.30f, size.height * 0.84f),
            cornerRadius = CornerRadius(size.height * 0.07f),
            style = stroke,
        )
        drawRoundRect(
            color = muted,
            topLeft = Offset(size.width * 0.145f, size.height * 0.16f),
            size = Size(size.width * 0.21f, size.height * 0.16f),
            cornerRadius = CornerRadius(size.height * 0.02f),
            style = stroke,
        )
        drawCircle(
            color = accent,
            radius = size.height * 0.06f,
            center = Offset(size.width * 0.25f, size.height * 0.48f),
            style = stroke,
        )
        drawCircle(
            color = muted,
            radius = size.height * 0.045f,
            center = Offset(size.width * 0.25f, size.height * 0.68f),
            style = stroke,
        )
        // Celebración de los 17 °C tachados.
        val frostCenter = Offset(size.width * 0.62f, size.height * 0.42f)
        val radius = size.height * 0.20f
        repeat(3) { index ->
            val angle = index * PI / 3.0
            drawLine(
                color = accent,
                start = Offset(
                    frostCenter.x - (cos(angle) * radius).toFloat(),
                    frostCenter.y - (sin(angle) * radius).toFloat(),
                ),
                end = Offset(
                    frostCenter.x + (cos(angle) * radius).toFloat(),
                    frostCenter.y + (sin(angle) * radius).toFloat(),
                ),
                strokeWidth = stroke.width,
                cap = StrokeCap.Round,
            )
        }
        drawLine(
            color = ink,
            start = Offset(size.width * 0.48f, size.height * 0.60f),
            end = Offset(size.width * 0.78f, size.height * 0.20f),
            strokeWidth = stroke.width * 1.6f,
            cap = StrokeCap.Round,
        )
        // 24 °C con ventilador.
        val fanCenter = Offset(size.width * 0.80f, size.height * 0.66f)
        drawCircle(color = accent, radius = size.height * 0.18f, center = fanCenter, style = stroke)
        repeat(3) { index ->
            val angle = index * 2.0 * PI / 3.0
            drawLine(
                color = accent,
                start = fanCenter,
                end = Offset(
                    fanCenter.x + (cos(angle) * radius * 0.8f).toFloat(),
                    fanCenter.y + (sin(angle) * radius * 0.8f).toFloat(),
                ),
                strokeWidth = stroke.width,
                cap = StrokeCap.Round,
            )
        }
        // Corchete de "correcto".
        val check = Path().apply {
            moveTo(size.width * 0.72f, size.height * 0.86f)
            lineTo(size.width * 0.78f, size.height * 0.92f)
            lineTo(size.width * 0.92f, size.height * 0.78f)
        }
        drawPath(check, color = accent, style = stroke)
    }
}

@Composable
fun UndersizedAcSketch(modifier: Modifier = Modifier) {
    SketchCanvas(modifier) { ink, accent, muted ->
        val stroke = sketchStroke()
        // Habitación.
        drawRect(
            color = ink,
            topLeft = Offset(size.width * 0.06f, size.height * 0.10f),
            size = Size(size.width * 0.88f, size.height * 0.78f),
            style = stroke,
        )
        // Equipo chico a la izquierda.
        drawRoundRect(
            color = accent,
            topLeft = Offset(size.width * 0.10f, size.height * 0.20f),
            size = Size(size.width * 0.22f, size.height * 0.18f),
            cornerRadius = CornerRadius(size.height * 0.04f),
            style = stroke,
        )
        // Aire que no alcanza la esquina caliente.
        repeat(2) { index ->
            drawPath(
                path = curledArrow(
                    start = Offset(size.width * (0.34f + index * 0.05f), size.height * 0.28f),
                    end = Offset(size.width * (0.56f + index * 0.04f), size.height * 0.36f),
                ),
                color = accent,
                style = stroke,
            )
        }
        val dashed = Stroke(
            width = stroke.width,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(size.height * 0.05f, size.height * 0.035f),
            ),
        )
        drawLine(
            color = muted,
            start = Offset(size.width * 0.62f, size.height * 0.18f),
            end = Offset(size.width * 0.62f, size.height * 0.80f),
            strokeWidth = stroke.width,
            pathEffect = dashed.pathEffect,
            cap = StrokeCap.Round,
        )
        // Zona que queda sin cubrir.
        drawCircle(
            color = muted,
            radius = size.height * 0.13f,
            center = Offset(size.width * 0.80f, size.height * 0.46f),
            style = dashed,
        )
        drawLine(
            color = muted,
            start = Offset(size.width * 0.73f, size.height * 0.39f),
            end = Offset(size.width * 0.87f, size.height * 0.53f),
            strokeWidth = stroke.width,
        )
        drawLine(
            color = muted,
            start = Offset(size.width * 0.87f, size.height * 0.39f),
            end = Offset(size.width * 0.73f, size.height * 0.53f),
            strokeWidth = stroke.width,
        )
        // Termómetro alto.
        drawLine(
            color = accent,
            start = Offset(size.width * 0.80f, size.height * 0.62f),
            end = Offset(size.width * 0.80f, size.height * 0.78f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = accent,
            radius = size.height * 0.05f,
            center = Offset(size.width * 0.80f, size.height * 0.80f),
            style = stroke,
        )
    }
}

@Composable
fun InverterProtectorJoulesSketch(modifier: Modifier = Modifier) {
    val badge = MaterialTheme.colorScheme.primaryContainer
    val onBadge = MaterialTheme.colorScheme.onPrimaryContainer
    Box(modifier = modifier) {
        SketchCanvas(Modifier.fillMaxSize()) { ink, accent, muted ->
            val stroke = sketchStroke()
            // Tablero con breaker dedicado.
            drawRoundRect(
                color = ink,
                topLeft = Offset(size.width * 0.05f, size.height * 0.14f),
                size = Size(size.width * 0.26f, size.height * 0.60f),
                cornerRadius = CornerRadius(size.height * 0.04f),
                style = stroke,
            )
            repeat(2) { index ->
                val top = size.height * (0.22f + index * 0.18f)
                drawRoundRect(
                    color = muted,
                    topLeft = Offset(size.width * 0.10f, top),
                    size = Size(size.width * 0.16f, size.height * 0.10f),
                    cornerRadius = CornerRadius(size.height * 0.02f),
                    style = stroke,
                )
            }
            // Protector de línea directa (borneras).
            drawRoundRect(
                color = accent,
                topLeft = Offset(size.width * 0.38f, size.height * 0.30f),
                size = Size(size.width * 0.26f, size.height * 0.34f),
                cornerRadius = CornerRadius(size.height * 0.04f),
                style = stroke,
            )
            repeat(3) { index ->
                val x = size.width * (0.43f + index * 0.08f)
                drawCircle(
                    color = accent,
                    radius = size.height * 0.026f,
                    center = Offset(x, size.height * 0.38f),
                    style = stroke,
                )
                drawLine(
                    color = accent,
                    start = Offset(x, size.height * 0.42f),
                    end = Offset(x, size.height * 0.52f),
                    strokeWidth = stroke.width,
                )
            }
            // Rayo que entra y se detiene.
            val bolt = Path().apply {
                moveTo(size.width * 0.20f, size.height * 0.80f)
                lineTo(size.width * 0.28f, size.height * 0.64f)
                lineTo(size.width * 0.24f, size.height * 0.63f)
                lineTo(size.width * 0.33f, size.height * 0.52f)
            }
            drawPath(bolt, color = accent, style = stroke)
            // Tarjeta Inverter protegida.
            drawRoundRect(
                color = ink,
                topLeft = Offset(size.width * 0.72f, size.height * 0.34f),
                size = Size(size.width * 0.22f, size.height * 0.30f),
                cornerRadius = CornerRadius(size.height * 0.03f),
                style = stroke,
            )
            val wave = Path().apply {
                moveTo(size.width * 0.76f, size.height * 0.49f)
                cubicTo(
                    size.width * 0.80f,
                    size.height * 0.40f,
                    size.width * 0.84f,
                    size.height * 0.58f,
                    size.width * 0.90f,
                    size.height * 0.49f,
                )
            }
            drawPath(wave, color = accent, style = stroke)
            drawLine(
                color = muted,
                start = Offset(size.width * 0.64f, size.height * 0.47f),
                end = Offset(size.width * 0.72f, size.height * 0.47f),
                strokeWidth = stroke.width,
                cap = StrokeCap.Round,
            )
        }
        Text(
            text = "2,100 J",
            style = MaterialTheme.typography.labelMedium,
            color = onBadge,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 12.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(badge)
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
fun DryModeVsDehumidifierSketch(modifier: Modifier = Modifier) {
    SketchCanvas(modifier) { ink, accent, muted ->
        val stroke = sketchStroke()
        // Divisor central a lápiz.
        drawLine(
            color = muted,
            start = Offset(size.width * 0.50f, size.height * 0.12f),
            end = Offset(size.width * 0.50f, size.height * 0.86f),
            strokeWidth = stroke.width,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(size.height * 0.04f, size.height * 0.03f)),
        )
        // Modo Dry del split.
        drawRoundRect(
            color = accent,
            topLeft = Offset(size.width * 0.08f, size.height * 0.16f),
            size = Size(size.width * 0.30f, size.height * 0.18f),
            cornerRadius = CornerRadius(size.height * 0.05f),
            style = stroke,
        )
        drawDroplet(
            center = Offset(size.width * 0.23f, size.height * 0.52f),
            height = size.height * 0.20f,
            color = accent,
            stroke = stroke,
        )
        repeat(2) { index ->
            drawPath(
                path = curledArrow(
                    start = Offset(size.width * 0.23f, size.height * 0.66f),
                    end = Offset(size.width * 0.23f + size.width * (0.04f + index * 0.03f), size.height * 0.76f),
                ),
                color = muted,
                style = stroke,
            )
        }
        // Deshumidificador con tanque.
        drawRoundRect(
            color = ink,
            topLeft = Offset(size.width * 0.62f, size.height * 0.18f),
            size = Size(size.width * 0.26f, size.height * 0.60f),
            cornerRadius = CornerRadius(size.height * 0.04f),
            style = stroke,
        )
        drawLine(
            color = muted,
            start = Offset(size.width * 0.62f, size.height * 0.52f),
            end = Offset(size.width * 0.88f, size.height * 0.52f),
            strokeWidth = stroke.width,
        )
        drawCircle(
            color = muted,
            radius = size.height * 0.055f,
            center = Offset(size.width * 0.75f, size.height * 0.36f),
            style = stroke,
        )
        drawDroplet(
            center = Offset(size.width * 0.75f, size.height * 0.66f),
            height = size.height * 0.14f,
            color = muted,
            stroke = stroke,
        )
        // Corriente eléctrica del deshumidificador.
        val cable = Path().apply {
            moveTo(size.width * 0.88f, size.height * 0.80f)
            cubicTo(
                size.width * 0.98f,
                size.height * 0.88f,
                size.width * 0.94f,
                size.height * 0.96f,
                size.width * 1.0f,
                size.height * 0.98f,
            )
        }
        drawPath(cable, color = accent, style = stroke)
    }
}

/* ---------------- Carrusel ---------------- */

private data class SketchCapsule(
    val titleRes: Int,
    val bodyRes: Int,
    val sketch: @Composable (Modifier) -> Unit,
)

private val capsules = listOf(
    SketchCapsule(
        titleRes = R.string.capsule_seer_title,
        bodyRes = R.string.capsule_seer_body,
        sketch = { modifier -> SeerEfficiencySketch(modifier) },
    ),
    SketchCapsule(
        titleRes = R.string.capsule_myth_title,
        bodyRes = R.string.capsule_myth_body,
        sketch = { modifier -> RemoteControlMythSketch(modifier) },
    ),
    SketchCapsule(
        titleRes = R.string.capsule_small_title,
        bodyRes = R.string.capsule_small_body,
        sketch = { modifier -> UndersizedAcSketch(modifier) },
    ),
    SketchCapsule(
        titleRes = R.string.capsule_protector_title,
        bodyRes = R.string.capsule_protector_body,
        sketch = { modifier -> InverterProtectorJoulesSketch(modifier) },
    ),
    SketchCapsule(
        titleRes = R.string.capsule_dry_title,
        bodyRes = R.string.capsule_dry_body,
        sketch = { modifier -> DryModeVsDehumidifierSketch(modifier) },
    ),
)

/**
 * Carrusel horizontal elástico de las cápsulas; se usa en Recomendar y desde
 * Calcular. Cada tarjeta conserva su propio boceto a plumilla.
 */
@CraftedBy("steefwel28")
@Composable
fun EducationalSketchCarousel(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
) {
    val listState = rememberLazyListState()
    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        flingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(listState),
    ) {
        items(capsules, key = { it.titleRes }) { capsule ->
            val title = stringResource(capsule.titleRes)
            val body = stringResource(capsule.bodyRes)
            Surface(
                modifier = Modifier.width(272.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    modifier = Modifier
                        .height(280.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        capsule.sketch(Modifier.fillMaxWidth().height(132.dp))
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start,
                    )
                }
            }
        }
    }
}

/* ---------------- Utilidades de trazo ---------------- */

@Composable
private fun SketchCanvas(
    modifier: Modifier = Modifier,
    draw: DrawScope.(ink: Color, accent: Color, muted: Color) -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val accent = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.outline
    Canvas(modifier = modifier.fillMaxWidth()) {
        draw(ink, accent, muted)
    }
}

/** Trazo de plumilla: fino, redondeado y proporcional a la densidad. */
private fun DrawScope.sketchStroke(): Stroke = Stroke(
    width = 1.4.dp.toPx(),
    cap = StrokeCap.Round,
    join = StrokeJoin.Round,
)

/** Flecha de aire con una curva suave, como boceto a mano. */
private fun curledArrow(start: Offset, end: Offset): Path {
    val midY = (start.y + end.y) / 2f
    return Path().apply {
        moveTo(start.x, start.y)
        cubicTo(
            start.x + (end.x - start.x) * 0.35f,
            midY - (end.y - start.y) * 0.9f,
            end.x - (end.x - start.x) * 0.25f,
            midY + (end.y - start.y) * 0.9f,
            end.x,
            end.y,
        )
        val head = (end.x - start.x) * 0.08f
        moveTo(end.x - head, end.y - head)
        lineTo(end.x, end.y)
        lineTo(end.x - head * 0.4f, end.y + head)
    }
}

private fun DrawScope.drawDroplet(
    center: Offset,
    height: Float,
    color: Color,
    stroke: Stroke,
) {
    val half = height * 0.32f
    val path = Path().apply {
        moveTo(center.x, center.y - height / 2f)
        cubicTo(
            center.x + half,
            center.y - height * 0.05f,
            center.x + half,
            center.y + height * 0.28f,
            center.x,
            center.y + height * 0.42f,
        )
        cubicTo(
            center.x - half,
            center.y + height * 0.28f,
            center.x - half,
            center.y - height * 0.05f,
            center.x,
            center.y - height / 2f,
        )
    }
    drawPath(path, color = color, style = stroke)
}
