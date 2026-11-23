package com.steefwel28.consumoenergetico.ui.screens.calculate

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.steefwel28.consumoenergetico.domain.calculator.PanamaUtility
import com.steefwel28.consumoenergetico.ui.components.PixelModalBottomSheet
import android.graphics.Paint as NativePaint
import android.graphics.Typeface

/**
 * Guía visual del "Costo Manual": dibuja con Canvas dos recibos genéricos y
 * marca exactamente dónde están el consumo en kWh y el total en B/.
 */
@Composable
fun BillGuideSheet(onDismiss: () -> Unit) {
    PixelModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "¿Dónde está el dato?",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Con Costo Manual usamos el precio real de tu factura. Toma el consumo del mes y el total a pagar; no uses el desglose de alumbrado público.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BillGuideCard(PanamaUtility.ENSA)
            BillGuideCard(PanamaUtility.NATURGY)
        }
    }
}

@Composable
private fun BillGuideCard(utility: PanamaUtility) {
    val colors = MaterialTheme.colorScheme
    val palette = ReceiptPalette(
        paper = colors.surfaceContainerLowest,
        ink = colors.onSurface,
        muted = colors.onSurfaceVariant,
        accent = colors.primary,
        highlight = colors.secondaryContainer,
        highlightInk = colors.onSecondaryContainer,
        line = colors.outlineVariant,
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = utility.displayName, style = MaterialTheme.typography.titleMedium)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(206.dp)
                .semantics {
                    contentDescription = "Recibo de ${utility.displayName} con el consumo en kWh " +
                        "y el total en balboas marcados en verde."
                },
        ) {
            drawReceipt(utility, palette)
        }
        Text(
            text = "Marca 1: consumo del mes en kWh. Marca 2: total a pagar en B/. (incluye cargo fijo).",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private data class ReceiptPalette(
    val paper: Color,
    val ink: Color,
    val muted: Color,
    val accent: Color,
    val highlight: Color,
    val highlightInk: Color,
    val line: Color,
)

private fun DrawScope.drawReceipt(utility: PanamaUtility, palette: ReceiptPalette) {
    val margin = 6.dp.toPx()
    val width = size.width - margin * 2
    drawRoundRect(
        color = palette.paper,
        topLeft = Offset(margin, 0f),
        size = Size(width, size.height),
        cornerRadius = CornerRadius(20.dp.toPx()),
    )
    drawRoundRect(
        color = palette.line,
        topLeft = Offset(margin, 0f),
        size = Size(width, size.height),
        cornerRadius = CornerRadius(20.dp.toPx()),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
    )

    val left = margin + 18.dp.toPx()
    val right = margin + width - 18.dp.toPx()
    val center = size.width / 2f

    drawText("${utility.displayName} · Servicio eléctrico", center, 26.dp.toPx(), palette.accent, 12f, center = true, bold = true)
    drawText("Recibo mensual · Panamá", center, 40.dp.toPx(), palette.muted, 9f, center = true)
    drawLine(
        color = palette.line,
        start = Offset(left, 50.dp.toPx()),
        end = Offset(right, 50.dp.toPx()),
        strokeWidth = 1.dp.toPx(),
    )

    drawText("Cliente: ______________________", left, 68.dp.toPx(), palette.muted, 9.5f)
    drawText("Dirección: ____________________", left, 82.dp.toPx(), palette.muted, 9.5f)

    drawHighlightedRow(
        top = 96.dp.toPx(),
        height = 30.dp.toPx(),
        left = left,
        right = right,
        highlight = palette.highlight,
    )
    drawText("① Consumo del mes", left + 8.dp.toPx(), 111.dp.toPx(), palette.highlightInk, 10f, bold = true)
    drawText("______ kWh", right - 8.dp.toPx(), 111.dp.toPx(), palette.highlightInk, 10f, right = true, bold = true)

    drawText("Alumbrado público", left, 139.dp.toPx(), palette.muted, 9f)
    drawText("B/. 1.20", right, 139.dp.toPx(), palette.muted, 9f, right = true)

    drawHighlightedRow(
        top = 148.dp.toPx(),
        height = 32.dp.toPx(),
        left = left,
        right = right,
        highlight = palette.highlight,
    )
    drawText("② Total a pagar", left + 8.dp.toPx(), 166.dp.toPx(), palette.highlightInk, 11f, bold = true)
    drawText("B/. ________", right - 8.dp.toPx(), 166.dp.toPx(), palette.highlightInk, 11f, right = true, bold = true)

    drawText(
        "Toma estos dos valores y escríbelos en Costo Manual → Desde el recibo.",
        center,
        194.dp.toPx(),
        palette.muted,
        8.5f,
        center = true,
    )
}

private fun DrawScope.drawHighlightedRow(
    top: Float,
    height: Float,
    left: Float,
    right: Float,
    highlight: Color,
) {
    drawRoundRect(
        color = highlight,
        topLeft = Offset(left, top),
        size = Size(right - left, height),
        cornerRadius = CornerRadius(8.dp.toPx()),
    )
}

private fun DrawScope.drawText(
    value: String,
    x: Float,
    y: Float,
    color: Color,
    sizeSp: Float,
    center: Boolean = false,
    right: Boolean = false,
    bold: Boolean = false,
) {
    val paint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb()
        textSize = sizeSp.sp.toPx()
        textAlign = when {
            center -> NativePaint.Align.CENTER
            right -> NativePaint.Align.RIGHT
            else -> NativePaint.Align.LEFT
        }
        typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
    }
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawText(value, x, y, paint)
    }
}
