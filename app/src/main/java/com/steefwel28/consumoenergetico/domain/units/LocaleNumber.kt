package com.steefwel28.consumoenergetico.domain.units

import com.steefwel28.consumoenergetico.core.signature.CraftedBy

/**
 * Lectura de números escritos a la panameña: `12,000` y `12.000` son doce mil,
 * `12,5` es doce y medio y `1,150.75` mezcla ambos separadores.
 *
 * Se usa en los campos de formulario y en el OCR para que ambos caminos
 * interpreten igual.
 */
@CraftedBy("steefwel28")
object LocaleNumber {

    fun parse(text: String): Double? {
        val token = text.trim().replace(" ", "")
        if (token.isEmpty()) return null

        val commas = token.count { it == ',' }
        val dots = token.count { it == '.' }
        val normalized = when {
            commas > 0 && dots > 0 -> {
                if (token.lastIndexOf(',') > token.lastIndexOf('.')) {
                    token.replace(".", "").replace(',', '.')
                } else {
                    token.replace(",", "")
                }
            }

            commas > 1 -> token.replace(",", "")
            dots > 1 -> token.replace(".", "")
            commas == 1 -> token.groupOrDecimal(',')
            dots == 1 -> token.groupOrDecimal('.')
            else -> token
        }
        return normalized.toDoubleOrNull()
    }

    private fun String.groupOrDecimal(separator: Char): String {
        val parts = split(separator)
        val integerPart = parts.first()
        // "0.180" y ".5" son decimales: un cero inicial nunca agrupa miles.
        val looksLikeThousands = parts.last().length == 3 &&
            integerPart.length in 1..3 &&
            !integerPart.startsWith("0")
        return if (looksLikeThousands) parts.joinToString("") else replace(separator, '.')
    }
}
