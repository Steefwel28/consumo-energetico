package com.steefwel28.consumoenergetico.data.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.steefwel28.consumoenergetico.R
import com.steefwel28.consumoenergetico.core.signature.CraftedBy
import com.steefwel28.consumoenergetico.domain.calculator.TariffComparison
import com.steefwel28.consumoenergetico.domain.recommender.ElectricalSpec
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

/** Todo lo que necesita la ficha para dibujarse sin volver a consultar datos. */
@CraftedBy("steefwel28")
data class ReportData(
    val spaceName: String,
    val brandModel: String,
    val btu: Int,
    val seer: Double?,
    val inverter: Boolean,
    val voltage: Int,
    val monthlyKwh: Double,
    val comparison: TariffComparison,
    val ensaMonthlyBalboas: Double,
    val naturgyMonthlyBalboas: Double,
    val displayMonthlyBalboas: Double,
    val tariffLabel: String,
    val recommendationText: String,
    val electrical: ElectricalSpec,
    val monthlySavingsBalboas: Double? = null,
    /** Etiqueta del periodo elegido: día, quincena o el mes real en curso. */
    val periodLabel: String = "Mes",
    val ensaMarginalBalboasPerKwh: Double = 0.0,
    val naturgyMarginalBalboasPerKwh: Double = 0.0,
    val generatedAtMillis: Long = System.currentTimeMillis(),
)

/**
 * Exportador nativo sin librerías: PDF vectorial carta y PNG de 1080 px.
 *
 * Ambos formatos comparten el mismo dibujo en coordenadas carta y respetan el
 * modo activo: solo ENSA, solo Naturgy o el duelo visual VS con ambas barras,
 * la diferencia exacta al mes y la ficha eléctrica completa.
 */
@CraftedBy("steefwel28")
object ReportExporter {

    fun exportPdf(context: Context, report: ReportData): File {
        val file = exportFile(context, report, "pdf")
        val document = PdfDocument()
        try {
            val page = document.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create(),
            )
            page.canvas.drawColor(Color.WHITE)
            ReportSheet(page.canvas, report, context).draw()
            document.finishPage(page)
            FileOutputStream(file).use { output -> document.writeTo(output) }
        } finally {
            document.close()
        }
        return file
    }

    fun exportPng(context: Context, report: ReportData): File {
        val file = exportFile(context, report, "png")
        val bitmap = Bitmap.createBitmap(PNG_WIDTH, PNG_HEIGHT, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.rgb(253, 252, 251))
            val scale = PNG_WIDTH.toFloat() / PAGE_WIDTH
            canvas.scale(scale, scale)
            ReportSheet(canvas, report, context).draw()
            FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
        } finally {
            bitmap.recycle()
        }
        return file
    }

    /** Intent listo para `Intent.createChooser` y compartir en WhatsApp. */
    fun shareIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = if (file.extension.equals("png", ignoreCase = true)) "image/png" else "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun exportFile(context: Context, report: ReportData, extension: String): File {
        val directory = File(context.cacheDir, "exports").apply { mkdirs() }
        return File(directory, "ficha-${report.generatedAtMillis}.$extension")
    }
}

private class ReportSheet(
    private val canvas: Canvas,
    private val report: ReportData,
    private val context: Context,
) {

    private fun text(resId: Int, vararg args: Any): String = context.getString(resId, *args)

    private val ink = paint(10f, INK)
    private val muted = paint(9f, MUTED)
    private val sectionPaint = paint(11.5f, ACCENT, bold = true)
    private val divider = paint(0.8f, DIVIDER)

    private var y = 40f

    fun draw() {
        drawHeader()
        drawStats()
        drawEquipment()
        drawComparison()
        drawRecommendation()
        drawElectrical()
        drawFooter()
    }

    private fun drawHeader() {
        canvas.drawRoundRect(
            MARGIN,
            28f,
            PAGE_WIDTH - MARGIN,
            122f,
            20f,
            20f,
            paint(0f, HEADER_BG),
        )
        canvas.drawText(
            text(R.string.app_name),
            MARGIN + 20f,
            62f,
            paint(21f, Color.WHITE, bold = true),
        )
        canvas.drawText(
            text(R.string.report_subtitle),
            MARGIN + 20f,
            82f,
            paint(9.5f, HEADER_MUTED),
        )
        val date = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
            .format(Date(report.generatedAtMillis))
        canvas.drawText(
            date,
            PAGE_WIDTH - MARGIN - 20f,
            62f,
            paint(9f, HEADER_MUTED, alignRight = true),
        )
        canvas.drawText(
            "Crafted by steefwel28",
            PAGE_WIDTH - MARGIN - 20f,
            106f,
            paint(8f, HEADER_MUTED, alignRight = true),
        )
        y = 148f
    }

    private fun drawStats() {
        val gap = 10f
        val tileWidth = (CONTENT_WIDTH - gap * 2f) / 3f
        val top = y
        drawStatTile(
            x = MARGIN,
            top = top,
            width = tileWidth,
            value = String.format(Locale.US, "%.1f", report.monthlyKwh),
            label = text(R.string.report_kwh_period),
        )
        drawStatTile(
            x = MARGIN + tileWidth + gap,
            top = top,
            width = tileWidth,
            value = report.seer?.let { String.format(Locale.US, "%.1f", it) } ?: "—",
            label = "SEER",
        )
        drawStatTile(
            x = MARGIN + (tileWidth + gap) * 2f,
            top = top,
            width = tileWidth,
            value = "B/. " + decimal(report.displayMonthlyBalboas),
            label = if (report.comparison == TariffComparison.VS) {
                text(R.string.report_lower_cost)
            } else {
                text(R.string.report_equipment_cost)
            },
        )
        y = top + 62f
    }

    private fun drawStatTile(x: Float, top: Float, width: Float, value: String, label: String) {
        canvas.drawRoundRect(
            x,
            top,
            x + width,
            top + 56f,
            14f,
            14f,
            paint(0f, TILE),
        )
        canvas.drawText(value, x + 12f, top + 27f, paint(15f, INK, bold = true))
        canvas.drawText(label, x + 12f, top + 44f, paint(7.5f, MUTED))
    }

    private fun drawEquipment() {
        startSection(text(R.string.report_equipment))
        drawTextLine(text(R.string.report_space, report.spaceName))
        drawTextLine(text(R.string.report_brand, report.brandModel))
        drawTextLine(
            text(
                R.string.report_capacity,
                btuText(report.btu),
                seerText(),
                text(if (report.inverter) R.string.inverter else R.string.no_inverter),
            ),
        )
        drawTextLine(text(R.string.report_voltage, report.voltage))
    }

    private fun drawComparison() {
        startSection(
            if (report.comparison == TariffComparison.VS) {
                text(R.string.report_vs_title)
            } else {
                text(R.string.report_cost_title, report.comparison.label)
            },
        )
        drawTextLine(text(R.string.report_period, report.periodLabel))
        when (report.comparison) {
            TariffComparison.ENSA -> drawBarRow(
                label = "ENSA",
                value = report.displayMonthlyBalboas,
                maxValue = report.displayMonthlyBalboas,
                color = ENSA_COLOR,
            )

            TariffComparison.NATURGY -> drawBarRow(
                label = "Naturgy",
                value = report.displayMonthlyBalboas,
                maxValue = report.displayMonthlyBalboas,
                color = NATURGY_COLOR,
            )

            TariffComparison.VS -> {
                val maxValue = max(
                    report.ensaMonthlyBalboas,
                    report.naturgyMonthlyBalboas,
                ).coerceAtLeast(0.01)
                drawBarRow("ENSA", report.ensaMonthlyBalboas, maxValue, ENSA_COLOR)
                drawBarRow("Naturgy", report.naturgyMonthlyBalboas, maxValue, NATURGY_COLOR)
                val difference = abs(
                    report.ensaMonthlyBalboas - report.naturgyMonthlyBalboas,
                )
                val cheaper = if (report.ensaMonthlyBalboas <= report.naturgyMonthlyBalboas) {
                    "ENSA"
                } else {
                    "Naturgy"
                }
                canvas.drawText(
                    if (difference < 0.005) {
                        text(R.string.report_difference_tie)
                    } else {
                        text(R.string.report_difference, decimal(difference), cheaper)
                    },
                    MARGIN,
                    y,
                    paint(9.5f, ACCENT, bold = true),
                )
                y += 14f
            }
        }
        canvas.drawText(
            text(R.string.report_tariff_applied, report.tariffLabel),
            MARGIN,
            y,
            muted,
        )
        y += 13f
        // Lectura dual con 4 decimales: el pliego real de cada distribuidora.
        canvas.drawText(
            text(
                R.string.report_vs_rates,
                rateText(report.ensaMarginalBalboasPerKwh),
                rateText(report.naturgyMarginalBalboasPerKwh),
            ),
            MARGIN,
            y,
            muted,
        )
        y += 13f
        report.monthlySavingsBalboas?.let { saving ->
            canvas.drawText(
                text(R.string.report_savings, decimal(saving)),
                MARGIN,
                y,
                paint(9f, ACCENT, bold = true),
            )
            y += 13f
        }
    }

    private fun drawBarRow(label: String, value: Double, maxValue: Double, color: Int) {
        canvas.drawText(label, MARGIN, y, paint(10.5f, INK, bold = true))
        canvas.drawText(
            "B/. ${decimal(value)}",
            PAGE_WIDTH - MARGIN,
            y,
            paint(10.5f, color, bold = true, alignRight = true),
        )
        y += 8f
        canvas.drawRoundRect(
            MARGIN,
            y,
            PAGE_WIDTH - MARGIN,
            y + 11f,
            5.5f,
            5.5f,
            paint(0f, TRACK),
        )
        val ratio = (value / maxValue.coerceAtLeast(0.01)).toFloat().coerceIn(0f, 1f)
        if (ratio > 0f) {
            canvas.drawRoundRect(
                MARGIN,
                y,
                MARGIN + CONTENT_WIDTH * ratio,
                y + 11f,
                5.5f,
                5.5f,
                paint(0f, color),
            )
        }
        y += 24f
    }

    private fun drawRecommendation() {
        startSection(text(R.string.report_recommendation))
        wrapped(report.recommendationText, ink, CONTENT_WIDTH).forEach { line ->
            canvas.drawText(line, MARGIN, y, ink)
            y += 13f
        }
    }

    private fun drawElectrical() {
        val spec = report.electrical
        startSection(text(R.string.report_electrical))
        drawTextLine(text(R.string.report_breaker, spec.breakerAmps, spec.wireGauge))
        drawTextLine(text(R.string.report_current, decimal(spec.estimatedAmps), spec.voltage))
        drawTextLine(
            text(
                R.string.report_protector,
                spec.protector.brandAndModel,
                surgeText(spec.protector.joules),
                spec.protector.reconnectLabel,
            ),
        )
        spec.alternativeProtector?.let { alternative ->
            drawTextLine(
                text(
                    R.string.report_alternative,
                    alternative.brandAndModel,
                    surgeText(alternative.joules),
                    alternative.reconnectLabel,
                ),
            )
        }
        spec.voltageNote?.let { note ->
            wrapped(note, muted, CONTENT_WIDTH).forEach { line ->
                canvas.drawText(line, MARGIN, y, muted)
                y += 12f
            }
        }
        spec.installationAlert?.let { alert ->
            wrapped(alert, muted, CONTENT_WIDTH).forEach { line ->
                canvas.drawText(line, MARGIN, y, muted)
                y += 12f
            }
        }
    }

    private fun drawFooter() {
        val footerY = PAGE_HEIGHT - MARGIN
        canvas.drawLine(MARGIN, footerY - 18f, PAGE_WIDTH - MARGIN, footerY - 18f, divider)
        canvas.drawText(
            text(R.string.report_footer),
            MARGIN,
            footerY,
            paint(8f, MUTED),
        )
        canvas.drawText(
            "Crafted by steefwel28",
            PAGE_WIDTH - MARGIN,
            footerY,
            paint(8f, MUTED, alignRight = true),
        )
    }

    private fun startSection(title: String) {
        y += 12f
        canvas.drawText(title, MARGIN, y, sectionPaint)
        y += 5f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, divider)
        y += 12f
    }

    private fun drawTextLine(text: String) {
        canvas.drawText(text, MARGIN, y, ink)
        y += 13f
    }

    private fun seerText(): String =
        report.seer?.let { text(R.string.report_seer, decimal(it)) }
            ?: text(R.string.report_seer_missing)

    private fun wrapped(text: String, textPaint: Paint, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        text.split(' ').filter { it.isNotEmpty() }.forEach { word ->
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (textPaint.measureText(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) lines += current.toString()
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) lines += current.toString()
        return lines
    }

    private fun btuText(value: Int): String = String.format(Locale.US, "%,d", value)

    private fun decimal(value: Double): String = String.format(Locale.US, "%.2f", value)

    private fun rateText(value: Double): String = String.format(Locale.US, "B/. %.4f/kWh", value)

    private fun surgeText(joules: Int): String = String.format(Locale.US, "%,d", joules)
}

private const val PAGE_WIDTH = 612
private const val PAGE_HEIGHT = 792
private const val PNG_WIDTH = 1080
private const val PNG_HEIGHT = 1400
private const val MARGIN = 44f
private const val CONTENT_WIDTH = 612f - MARGIN * 2f
private const val INK = 0xFF1B1B1F.toInt()
private const val MUTED = 0xFF5F6368.toInt()
private const val ACCENT = 0xFF00696D.toInt()
private const val DIVIDER = 0xFFE0E0E0.toInt()
private const val TRACK = 0xFFE7EAED.toInt()
private const val TILE = 0xFFF2F4F5.toInt()
private const val HEADER_BG = 0xFF07484C.toInt()
private const val HEADER_MUTED = 0xFFB8D4D4.toInt()
private const val ENSA_COLOR = 0xFF00696D.toInt()
private const val NATURGY_COLOR = 0xFFB26A00.toInt()

private fun paint(size: Float, color: Int, bold: Boolean = false, alignRight: Boolean = false) =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        textAlign = if (alignRight) Paint.Align.RIGHT else Paint.Align.LEFT
    }
