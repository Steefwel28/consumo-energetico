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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.steefwel28.consumoenergetico.R
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
                text = stringResource(R.string.guide_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.guide_intro),
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

    val receiptCd = stringResource(R.string.guide_receipt_cd, utility.displayName)
    val strings = ReceiptStrings(
        service = stringResource(R.string.guide_service, utility.displayName),
        monthly = stringResource(R.string.guide_monthly),
        client = stringResource(R.string.guide_client),
        address = stringResource(R.string.guide_address),
        consumption = stringResource(R.string.guide_consumption),
        kwh = stringResource(R.string.guide_kwh),
        publicLight = stringResource(R.string.guide_public_light),
        total = stringResource(R.string.guide_total),
        totalValue = stringResource(R.string.guide_total_value),
        hint = stringResource(R.string.guide_hint),
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = utility.displayName, style = MaterialTheme.typography.titleMedium)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(206.dp)
                .semantics { contentDescription = receiptCd },
        ) {
            drawReceipt(palette, strings)
        }
        Text(
            text = stringResource(R.string.guide_marks),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private data class ReceiptStrings(
    val service: String,
    val monthly: String,
    val client: String,
    val address: String,
    val consumption: String,
    val kwh: String,
    val publicLight: String,
    val total: String,
    val totalValue: String,
    val hint: String,
)

private data class ReceiptPalette(
    val paper: Color,
    val ink: Color,
    val muted: Color,
    val accent: Color,
    val highlight: Color,
    val highlightInk: Color,
    val line: Color,
)

private fun DrawScope.drawReceipt(palette: ReceiptPalette, strings: ReceiptStrings) {
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

    drawText(strings.service, center, 26.dp.toPx(), palette.accent, 12f, center = true, bold = true)
    drawText(strings.monthly, center, 40.dp.toPx(), palette.muted, 9f, center = true)
    drawLine(
        color = palette.line,
        start = Offset(left, 50.dp.toPx()),
        end = Offset(right, 50.dp.toPx()),
        strokeWidth = 1.dp.toPx(),
    )

    drawText(strings.client, left, 68.dp.toPx(), palette.muted, 9.5f)
    drawText(strings.address, left, 82.dp.toPx(), palette.muted, 9.5f)

    drawHighlightedRow(
        top = 96.dp.toPx(),
        height = 30.dp.toPx(),
        left = left,
        right = right,
        highlight = palette.highlight,
    )
    drawText(strings.consumption, left + 8.dp.toPx(), 111.dp.toPx(), palette.highlightInk, 10f, bold = true)
    drawText(strings.kwh, right - 8.dp.toPx(), 111.dp.toPx(), palette.highlightInk, 10f, right = true, bold = true)

    drawText(strings.publicLight, left, 139.dp.toPx(), palette.muted, 9f)
    drawText("B/. 1.20", right, 139.dp.toPx(), palette.muted, 9f, right = true)

    drawHighlightedRow(
        top = 148.dp.toPx(),
        height = 32.dp.toPx(),
        left = left,
        right = right,
        highlight = palette.highlight,
    )
    drawText(strings.total, left + 8.dp.toPx(), 166.dp.toPx(), palette.highlightInk, 11f, bold = true)
    drawText(strings.totalValue, right - 8.dp.toPx(), 166.dp.toPx(), palette.highlightInk, 11f, right = true, bold = true)

    drawText(
        strings.hint,
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
