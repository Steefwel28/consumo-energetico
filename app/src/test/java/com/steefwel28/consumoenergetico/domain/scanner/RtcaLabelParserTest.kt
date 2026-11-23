package com.steefwel28.consumoenergetico.domain.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RtcaLabelParserTest {

    @Test
    fun `lee una etiqueta tipica con separadores panamenos`() {
        val text = """
            AIRE ACONDICIONADO TIPO SPLIT
            CAPACIDAD DE ENFRIAMIENTO 12,000 BTU/h
            POTENCIA NOMINAL 1.150 W
            REEE 12,5 BTU/hW
            MODELO ABC-24000
        """.trimIndent()

        val reading = RtcaLabelParser.parse(text)

        assertEquals(12_000.0, reading.btuPerHour!!, 0.001)
        assertEquals(1_150.0, reading.nominalWatts!!, 0.001)
        assertEquals(12.5, reading.seer!!, 0.001)
        assertTrue(reading.hasValues)
    }

    @Test
    fun `acepta el numero despues de la palabra clave`() {
        val text = "CAPACIDAD: BTU/h 18,000\nPOTENCIA (W): 1,700\nSEER: 11.4"

        val reading = RtcaLabelParser.parse(text)

        assertEquals(18_000.0, reading.btuPerHour!!, 0.001)
        assertEquals(1_700.0, reading.nominalWatts!!, 0.001)
        assertEquals(11.4, reading.seer!!, 0.001)
    }

    @Test
    fun `no confunde el seer con la potencia`() {
        val reading = RtcaLabelParser.parse("REEE 12,5 BTU/hW\nMODELO X")

        assertEquals(12.5, reading.seer!!, 0.001)
        assertNull(reading.nominalWatts)
        assertNull(reading.btuPerHour)
    }

    @Test
    fun `ignora capacidades fuera de rango`() {
        val reading = RtcaLabelParser.parse("CAPACIDAD 99,999 BTU/h")

        assertNull(reading.btuPerHour)
        assertFalse(reading.hasValues)
    }

    @Test
    fun `texto sin etiqueta no produce valores`() {
        assertFalse(RtcaLabelParser.parse("FACTURA DE PRUEBA").hasValues)
        assertFalse(RtcaLabelParser.parse("").hasValues)
    }
}
