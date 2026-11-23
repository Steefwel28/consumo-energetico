package com.steefwel28.consumoenergetico.domain.units

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConverterTest {

    @Test
    fun `convierte longitud en ambos sentidos`() {
        assertEquals(0.3048, UnitConverter.lengthToCanonical(1.0, LengthUnit.FEET), 0.0)
        assertEquals(1.0, UnitConverter.lengthToDisplay(0.3048, LengthUnit.FEET), 1e-12)
        assertEquals(3.28, UnitConverter.lengthToDisplay(1.0, LengthUnit.FEET), 0.001)
    }

    @Test
    fun `convierte pulgadas y centimetros`() {
        assertEquals(1.0, UnitConverter.shortLengthToDisplay(2.54, ShortLengthUnit.INCHES), 1e-12)
        assertEquals(2.54, UnitConverter.shortLengthToCanonical(1.0, ShortLengthUnit.INCHES), 0.0)
    }

    @Test
    fun `convierte pies cubicos y litros sin perder precision`() {
        assertEquals(
            28.316846592,
            UnitConverter.volumeToCanonical(1.0, VolumeUnit.CUBIC_FEET),
            0.0,
        )
        val roundTrip = UnitConverter.volumeToCanonical(
            UnitConverter.volumeToDisplay(50.0, VolumeUnit.CUBIC_FEET),
            VolumeUnit.CUBIC_FEET,
        )
        assertEquals(50.0, roundTrip, 1e-9)
    }

    @Test
    fun `convierte temperatura entre celsius y fahrenheit`() {
        assertEquals(32.0, UnitConverter.temperatureToDisplay(0.0, TemperatureUnit.FAHRENHEIT), 0.0)
        assertEquals(212.0, UnitConverter.temperatureToDisplay(100.0, TemperatureUnit.FAHRENHEIT), 1e-12)
        assertEquals(24.0, UnitConverter.temperatureToCanonical(75.2, TemperatureUnit.FAHRENHEIT), 1e-12)
    }

    @Test
    fun `formatea con simbolo y sin ceros sobrantes`() {
        assertEquals("24 °C", UnitConverter.formatTemperature(24.0, TemperatureUnit.CELSIUS))
        assertEquals("75.2 °F", UnitConverter.formatTemperature(24.0, TemperatureUnit.FAHRENHEIT))
        assertEquals("3.28 ft", UnitConverter.formatLength(1.0, LengthUnit.FEET))
        assertEquals("15 cm", UnitConverter.formatShortLength(15.0, ShortLengthUnit.CENTIMETERS))
        assertEquals("28.32 L", UnitConverter.formatVolume(28.316846592, VolumeUnit.LITERS))
        assertEquals("1.48 p³", UnitConverter.formatVolume(42.0, VolumeUnit.CUBIC_FEET))
    }
}
