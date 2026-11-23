package com.steefwel28.consumoenergetico.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class AcConsumptionCalculatorTest {

    private val inverter = AcUnitSpec(btuPerHour = 12_000.0, nominalWatts = 1_000.0, inverter = true)
    private val nonInverter = AcUnitSpec(btuPerHour = 12_000.0, nominalWatts = 1_000.0)
    private val night = AcUsage(hoursPerDay = 8.0, setpointCelsius = 24, schedule = AcSchedule.NIGHT)

    @Test
    fun `el inverter consume menos que el ciclo on off`() {
        assertEquals(121.44, AcConsumptionCalculator.monthlyKwh(inverter, night), 1e-9)
        assertEquals(165.6, AcConsumptionCalculator.monthlyKwh(nonInverter, night), 1e-9)
    }

    @Test
    fun `bajar un grado sobre 24 penaliza seis por ciento`() {
        val at24 = AcConsumptionCalculator.monthlyKwh(nonInverter, night)
        val at18 = AcConsumptionCalculator.monthlyKwh(nonInverter, night.copy(setpointCelsius = 18))
        assertEquals(1.36, at18 / at24, 1e-9)
    }

    @Test
    fun `los filtros sucios suman doce por ciento`() {
        val clean = AcConsumptionCalculator.monthlyKwh(inverter, night)
        val dirty = AcConsumptionCalculator.monthlyKwh(inverter, night.copy(dirtyFilters = true))
        assertEquals(1.12, dirty / clean, 1e-9)
    }

    @Test
    fun `el comparador devuelve ahorro y retorno de inversion`() {
        val comparison = AcConsumptionCalculator.compare(
            current = nonInverter,
            currentUsage = night,
            new = inverter,
            investmentBalboas = 300.0,
            balboasPerKwh = 0.25,
        )
        assertEquals(11.04, comparison.monthlySavingsBalboas, 1e-9)
        assertEquals(132.48, comparison.annualSavingsBalboas, 1e-9)
        assertEquals(300.0 / 11.04, comparison.paybackMonths!!, 1e-9)
    }

    @Test
    fun `sin ahorro no hay retorno de inversion`() {
        val comparison = AcConsumptionCalculator.compare(
            current = nonInverter,
            currentUsage = night,
            new = nonInverter,
            investmentBalboas = 500.0,
            balboasPerKwh = 0.25,
        )
        assertNull(comparison.paybackMonths)
    }

    @Test
    fun `convierte placa a BTU por hora`() {
        assertEquals(18_000.0, AcPlateConverter.tonsToBtuPerHour(1.5), 1e-9)
        assertEquals(3_412.142, AcPlateConverter.thermalKilowattsToBtuPerHour(1.0), 1e-9)
        assertEquals(7_506.7124, AcPlateConverter.amperesToBtuPerHour(10.0, 220.0), 1e-9)
    }

    @Test
    fun `rechaza termostatos fuera del rango del motor`() {
        assertThrows(IllegalArgumentException::class.java) {
            night.copy(setpointCelsius = 35)
        }
    }
}
