package com.steefwel28.consumoenergetico.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.steefwel28.consumoenergetico.R

/**
 * Guías de lectura dibujadas a plumilla (línea blanca/cian sobre vidrio).
 * Explican qué número copiar de la etiqueta RTCA y de la placa metálica.
 */
@Composable
fun RtcaLabelGuideDialog(onDismiss: () -> Unit) {
    GuideDialog(
        title = stringResource(R.string.calc_guide_label_title),
        onDismiss = onDismiss,
    ) { RtcaLabelSketch() }
}

@Composable
fun NameplateGuideDialog(onDismiss: () -> Unit) {
    GuideDialog(
        title = stringResource(R.string.calc_guide_plate_title),
        onDismiss = onDismiss,
    ) { NameplateSketch() }
}

@Composable
private fun GuideDialog(
    title: String,
    onDismiss: () -> Unit,
    sketch: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        // Sin HazeState dentro de la ventana del diálogo: cristal por tinte + canto.
        CompositionLocalProvider(LocalHazeState provides null) {
            LiquidGlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(text = title, style = MaterialTheme.typography.titleLarge)
                    sketch()
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.End),
                    ) { Text(stringResource(R.string.calc_guide_close)) }
                }
            }
        }
    }
}

private val Ink = Color(0xFFE8FFFA)
private val InkSoft = Color(0xFF7FE3F7)
private val LabelAmber = Color(0xFFFFDE7A)

@Composable
private fun RtcaLabelSketch() {
    val measurer = rememberTextMeasurer()
    Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        val sheetLeft = size.width * 0.04f
        val sheetTop = size.height * 0.06f
        val sheetWidth = size.width * 0.52f
        val sheetHeight = size.height * 0.88f
        drawRoundRect(
            color = LabelAmber.copy(alpha = 0.16f),
            topLeft = Offset(sheetLeft, sheetTop),
            size = Size(sheetWidth, sheetHeight),
            cornerRadius = CornerRadius(10f, 10f),
        )
        drawRoundRect(
            color = Ink.copy(alpha = 0.55f),
            topLeft = Offset(sheetLeft, sheetTop),
            size = Size(sheetWidth, sheetHeight),
            cornerRadius = CornerRadius(10f, 10f),
            style = Stroke(width = 1.4f),
        )
        // Renglones simulados de la etiqueta.
        repeat(4) { index ->
            val y = sheetTop + sheetHeight * (0.16f + index * 0.2f)
            drawLine(
                color = Ink.copy(alpha = 0.22f),
                start = Offset(sheetLeft + 12f, y),
                end = Offset(sheetLeft + sheetWidth * 0.62f, y),
                strokeWidth = 1f,
            )
        }

        val textX = size.width * 0.62f
        annotation(
            measurer = measurer,
            text = "BTU/h",
            textPos = Offset(textX, size.height * 0.16f),
            target = Offset(sheetLeft + sheetWidth * 0.46f, size.height * 0.22f),
        )
        annotation(
            measurer = measurer,
            text = "W",
            textPos = Offset(textX, size.height * 0.48f),
            target = Offset(sheetLeft + sheetWidth * 0.36f, size.height * 0.54f),
        )
        annotation(
            measurer = measurer,
            text = "SEER",
            textPos = Offset(textX, size.height * 0.78f),
            target = Offset(sheetLeft + sheetWidth * 0.52f, size.height * 0.82f),
        )
    }
}

@Composable
private fun NameplateSketch() {
    val measurer = rememberTextMeasurer()
    Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        val plateLeft = size.width * 0.04f
        val plateTop = size.height * 0.06f
        val plateWidth = size.width * 0.52f
        val plateHeight = size.height * 0.88f
        drawRoundRect(
            color = Color.White.copy(alpha = 0.10f),
            topLeft = Offset(plateLeft, plateTop),
            size = Size(plateWidth, plateHeight),
            cornerRadius = CornerRadius(6f, 6f),
        )
        drawRoundRect(
            color = Ink.copy(alpha = 0.55f),
            topLeft = Offset(plateLeft, plateTop),
            size = Size(plateWidth, plateHeight),
            cornerRadius = CornerRadius(6f, 6f),
            style = Stroke(width = 1.4f),
        )
        // Cuatro remaches en las esquinas de la placa.
        listOf(
            Offset(plateLeft + 8f, plateTop + 8f),
            Offset(plateLeft + plateWidth - 8f, plateTop + 8f),
            Offset(plateLeft + 8f, plateTop + plateHeight - 8f),
            Offset(plateLeft + plateWidth - 8f, plateTop + plateHeight - 8f),
        ).forEach { drawCircle(color = Ink.copy(alpha = 0.4f), radius = 2.6f, center = it) }
        drawLine(
            color = Ink.copy(alpha = 0.25f),
            start = Offset(plateLeft + 10f, plateTop + plateHeight * 0.5f),
            end = Offset(plateLeft + plateWidth - 10f, plateTop + plateHeight * 0.5f),
            strokeWidth = 1f,
        )

        val textX = size.width * 0.64f
        annotation(
            measurer = measurer,
            text = "V · 220",
            textPos = Offset(textX, size.height * 0.06f),
            target = Offset(plateLeft + plateWidth * 0.42f, size.height * 0.16f),
        )
        annotation(
            measurer = measurer,
            text = "A · 30",
            textPos = Offset(textX, size.height * 0.32f),
            target = Offset(plateLeft + plateWidth * 0.36f, size.height * 0.42f),
        )
        annotation(
            measurer = measurer,
            text = "Hz · 60",
            textPos = Offset(textX, size.height * 0.58f),
            target = Offset(plateLeft + plateWidth * 0.46f, size.height * 0.68f),
        )
        annotation(
            measurer = measurer,
            text = "W",
            textPos = Offset(textX, size.height * 0.82f),
            target = Offset(plateLeft + plateWidth * 0.38f, size.height * 0.86f),
        )
    }
}

/** Etiqueta + flecha punteada de plumilla hacia el dato en el dibujo. */
private fun DrawScope.annotation(
    measurer: TextMeasurer,
    text: String,
    textPos: Offset,
    target: Offset,
) {
    drawText(
        textMeasurer = measurer,
        text = text,
        topLeft = textPos,
        style = TextStyle(color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Medium),
    )
    val start = Offset(textPos.x - 6f, textPos.y + 9f)
    drawLine(
        color = InkSoft,
        start = start,
        end = target,
        strokeWidth = 1.2f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)),
    )
    drawCircle(color = InkSoft, radius = 2.4f, center = target)
}
