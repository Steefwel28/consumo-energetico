package com.steefwel28.consumoenergetico.domain.scanner

import com.steefwel28.consumoenergetico.core.signature.CraftedBy
import com.steefwel28.consumoenergetico.domain.units.LocaleNumber

/** Lectura confirmada de una etiqueta amarilla RTCA 23.01.78:20. */
@CraftedBy("steefwel28")
data class RtcaLabelReading(
    val btuPerHour: Double? = null,
    val nominalWatts: Double? = null,
    val seer: Double? = null,
) {
    val hasValues: Boolean
        get() = btuPerHour != null || nominalWatts != null || seer != null
}

/**
 * Extrae capacidad, potencia y REEE/SEER del texto OCR de una etiqueta RTCA.
 *
 * La etiqueta puede venir en una o varias líneas y con separadores
 * panameños (`12,000` o `12.000`, `12,5`). Se ignoran números de modelo:
 * solo se acepta el número pegado a la palabra clave (`BTU/h`, `W`, `SEER`).
 */
@CraftedBy("steefwel28")
object RtcaLabelParser {

    private val BtuKeyword = Regex("""(?i)BTU\s*/\s*[hH](?![wW])|\bBTU\b(?!\s*/)""")
    private val WattsKeyword = Regex("""(?i)\bW(?:ATTS?)?\b""")
    private val SeerKeyword = Regex("""(?i)\bSEER\b|\bREEE\b|BTU\s*/\s*[hH]\s*[wW]|BTU\s*/\s*[wW]""")
    private val NumberToken = Regex("""\d{1,3}(?:[.,\s]\d{3})+(?:[.,]\d+)?|\d+(?:[.,]\d+)?""")

    fun parse(text: String): RtcaLabelReading {
        val lines = text
            .replace('\u00A0', ' ')
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val seerLines = lines.filter { SeerKeyword.containsMatchIn(it) }
        val btuLines = lines.filter { BtuKeyword.containsMatchIn(it) && it !in seerLines }
        val wattLines = lines.filter { WattsKeyword.containsMatchIn(it) && it !in seerLines }

        return RtcaLabelReading(
            btuPerHour = btuLines.valueNear(BtuKeyword, 1.0..60_000.0),
            nominalWatts = wattLines.valueNear(WattsKeyword, 1.0..20_000.0),
            seer = seerLines.valueNear(SeerKeyword, 1.0..40.0),
        )
    }

    private fun List<String>.valueNear(keyword: Regex, range: ClosedFloatingPointRange<Double>): Double? =
        firstNotNullOfOrNull { line -> line.numberNear(keyword, range) }

    /**
     * Prefiere el número anterior a la palabra clave ("12,000 BTU/h") y cae al
     * posterior cuando la etiqueta usa "SEER: 12,5".
     */
    private fun String.numberNear(
        keyword: Regex,
        range: ClosedFloatingPointRange<Double>,
    ): Double? {
        val match = keyword.find(this) ?: return null
        val before = substring(0, match.range.first)
        val after = substring(match.range.last + 1)

        val previous = NumberToken.findAll(before).lastOrNull()?.value?.toDecimal()
        if (previous != null && previous in range) return previous

        return NumberToken.findAll(after).firstOrNull()?.value?.toDecimal()?.takeIf { it in range }
    }

    /** Interpreta coma o punto como decimal solo cuando no forma miles. */
    private fun String.toDecimal(): Double? = LocaleNumber.parse(this)
}
