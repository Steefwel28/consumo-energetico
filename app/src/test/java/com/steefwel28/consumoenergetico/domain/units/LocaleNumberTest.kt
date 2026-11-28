package com.steefwel28.consumoenergetico.domain.units

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocaleNumberTest {

    @Test
    fun `coma con tres digitos es separador de miles`() {
        assertEquals(12_000.0, LocaleNumber.parse("12,000")!!, 0.001)
        assertEquals(12_000.0, LocaleNumber.parse("12.000")!!, 0.001)
    }

    @Test
    fun `coma con uno o dos digitos es decimal`() {
        assertEquals(12.5, LocaleNumber.parse("12,5")!!, 0.001)
        assertEquals(0.18, LocaleNumber.parse("0,18")!!, 0.001)
        assertEquals(0.18, LocaleNumber.parse("0.180")!!, 0.001)
        assertEquals(0.18, LocaleNumber.parse("0,180")!!, 0.001)
    }

    @Test
    fun `mezcla separadores y respeta el ultimo como decimal`() {
        assertEquals(1_150.75, LocaleNumber.parse("1,150.75")!!, 0.001)
        assertEquals(1_150.75, LocaleNumber.parse("1.150,75")!!, 0.001)
    }

    @Test
    fun `tarifa con cuatro decimales conserva el punto`() {
        assertEquals(0.1769, LocaleNumber.parse("0.1769")!!, 0.000001)
    }

    @Test
    fun `texto vacio o invalido no produce numero`() {
        assertNull(LocaleNumber.parse(""))
        assertNull(LocaleNumber.parse("abc"))
    }
}
