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
import com.steefwel28.consumoenergetico.core.signature.CraftedBy
import com.steefwel28.consumoenergetico.domain.recommender.ElectricalSpec
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val monthlyBalboas: Double,
    val tariffLabel: String,
    val recommendationText: String,
    val electrical: ElectricalSpec,
    val monthlySavingsBalboas: Double? = null,
    val generatedAtMillis: Long = System.currentTimeMillis(),
)

/**
 * Exportador nativo sin librerías: PDF vectorial carta y PNG de 1080 px.
 *
 * Ambos formatos comparten el mismo dibujo en coordenadas carta; el PNG solo
 * escala el lienzo. El archivo queda en `cacheDir/exports`, listo para
 * compartir por WhatsApp con [shareIntent].
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
            ReportSheet(page.canvas, report).draw()
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
            ReportSheet(canvas, report).draw()
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
) {

    private val ink = paint(10f, INK)
    private val muted = paint(9.5f, MUTED)
    private val sectionPaint = paint(12f, ACCENT, bold = true)
    private val divider = paint(0.8f, DIVIDER)

    private var y = MARGIN + 20f

    fun draw() {
        drawHeader()
        drawSection(
            title = "Equipo",
            lines = listOf(
                "Espacio: ${report.spaceName}",
                "Marca y modelo: ${report.brandModel}",
                "Capacidad: ${btuText(report.btu)} BTU/h · ${seerText()}" +
                    " · ${if (report.inverter) "Inverter" else "No Inverter"}",
                "Voltaje de placa: ${report.voltage} V",
            ),
        )
        drawSection(
            title = "Consumo mensual",
            lines = buildList {
                add("Consumo estimado: ${decimal(report.monthlyKwh)} kWh/mes")
                add("Costo estimado: B/. ${decimal(report.monthlyBalboas)} (${report.tariffLabel})")
                report.monthlySavingsBalboas?.let { saving ->
                    add("Ahorro del equipo nuevo: B/. ${decimal(saving)}/mes")
                }
            },
        )
        drawRecommendation()
        drawElectrical()
        drawFooter()
    }

    private fun drawHeader() {
        canvas.drawText("Consumo Energético", MARGIN, y, paint(20f, INK, bold = true))
        canvas.drawText("Ficha técnica del aire acondicionado", MARGIN, y + 15f, muted)
        val date = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("es-PA"))
            .format(Date(report.generatedAtMillis))
        canvas.drawText(date, PAGE_WIDTH - MARGIN, y, paint(9f, MUTED, alignRight = true))
        y += 26f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, divider)
    }

    private fun drawSection(title: String, lines: List<String>) {
        startSection(title)
        lines.forEach { line ->
            canvas.drawText(line, MARGIN, y, ink)
            y += 15f
        }
    }

    private fun drawRecommendation() {
        startSection("Recomendación del espacio")
        wrapped(report.recommendationText, ink, CONTENT_WIDTH).forEach { line ->
            canvas.drawText(line, MARGIN, y, ink)
            y += 14f
        }
    }

    private fun drawElectrical() {
        val spec = report.electrical
        val lines = buildList {
            add("Breaker dedicado: ${spec.breakerAmps} A · Cable ${spec.wireGauge}")
            add("Corriente estimada: ${decimal(spec.estimatedAmps)} A a ${spec.voltage} V")
            add(
                "Protector de voltaje: ≥ ${surgeText(spec.surgeProtectorJoules)} J · " +
                    "reconexión ${spec.reconnectDelayMinutes.first}–${spec.reconnectDelayMinutes.last} min",
            )
        }
        drawSection("Ficha eléctrica", lines)
        spec.installationAlert?.let { alert ->
            wrapped(alert, muted, CONTENT_WIDTH).forEach { line ->
                canvas.drawText(line, MARGIN, y, muted)
                y += 13f
            }
        }
    }

    private fun drawFooter() {
        val footerY = PAGE_HEIGHT - MARGIN
        canvas.drawLine(MARGIN, footerY - 18f, PAGE_WIDTH - MARGIN, footerY - 18f, divider)
        canvas.drawText(
            "Crafted by steefwel28",
            PAGE_WIDTH - MARGIN,
            footerY,
            paint(8f, MUTED, alignRight = true),
        )
    }

    private fun startSection(title: String) {
        y += 16f
        canvas.drawText(title, MARGIN, y, sectionPaint)
        y += 6f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, divider)
        y += 14f
    }

    private fun seerText(): String =
        report.seer?.let { "SEER ${decimal(it)}" } ?: "SEER no indicado"

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

    private fun decimal(value: Double): String = String.format(Locale.US, "%.1f", value)

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

private fun paint(size: Float, color: Int, bold: Boolean = false, alignRight: Boolean = false) =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        textAlign = if (alignRight) Paint.Align.RIGHT else Paint.Align.LEFT
    }
