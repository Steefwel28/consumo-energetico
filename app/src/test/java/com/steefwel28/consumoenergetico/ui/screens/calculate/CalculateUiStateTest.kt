package com.steefwel28.consumoenergetico.ui.screens.calculate

import com.steefwel28.consumoenergetico.domain.calculator.TariffMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateUiStateTest {

    @Test
    fun `el estado inicial calcula consumo y ficha electrica`() {
        val state = CalculateUiState()

        assertNotNull(state.spec)
        assertTrue(state.monthlyKwh!! > 0.0)
        assertEquals(110, state.electrical!!.voltage)
        assertTrue(state.canSave)
        assertTrue(state.reportNarrative().contains("kWh"))
    }

    @Test
    fun `la tarifa directa manual cobra exactamente por kWh`() {
        val state = CalculateUiState(
            manualCost = true,
            manualMode = ManualCostMode.DIRECT_RATE,
            directRateInput = "0.25",
            baseMonthlyKwh = 0.0,
        )

        val cost = state.monthlyCost!!

        assertTrue(state.tariffMode is TariffMode.DirectRate)
        assertEquals(state.monthlyKwh!! * 0.25, cost, 0.0001)
    }

    @Test
    fun `el recibo deduce la tarifa del total entre los kWh`() {
        val state = CalculateUiState(
            manualCost = true,
            manualMode = ManualCostMode.RECEIPT,
            receiptTotalInput = "52",
            receiptKwhInput = "200",
        )

        val rate = (state.tariffMode as TariffMode.Receipt).balboasPerKwh

        assertEquals(0.26, rate, 0.0000001)
        assertEquals(state.monthlyKwh!! * 0.26, state.monthlyCost!!, 0.001)
    }

    @Test
    fun `un recibo sin kWh no produce tarifa`() {
        val state = CalculateUiState(
            manualCost = true,
            manualMode = ManualCostMode.RECEIPT,
            receiptTotalInput = "52",
            receiptKwhInput = "0",
        )

        assertNull(state.tariffMode)
        assertNull(state.tariffEstimate)
    }

    @Test
    fun `bajar el termostato y ensuciar los filtros sube el consumo`() {
        val clean = CalculateUiState()

        assertTrue(clean.copy(setpointCelsius = 18).monthlyKwh!! > clean.monthlyKwh!!)
        assertTrue(clean.copy(cleanFilters = false).monthlyKwh!! > clean.monthlyKwh!!)
    }

    @Test
    fun `el comparador detecta ahorro y retorno`() {
        val state = CalculateUiState(
            currentBtuInput = "12000",
            currentWattsInput = "1600",
            currentInverter = false,
            investmentInput = "500",
        )

        val comparison = state.comparison!!

        assertTrue(comparison.monthlySavingsBalboas > 0.0)
        assertNotNull(comparison.paybackMonths)
        assertTrue(comparison.paybackMonths!! > 0.0)
    }

    @Test
    fun `acepta miles con coma en los campos de formulario`() {
        val state = CalculateUiState(btuInput = "12,000", wattsInput = "1.150")

        assertEquals(12_000.0, state.btu!!, 0.001)
        assertEquals(1_150.0, state.nominalWatts!!, 0.001)
    }

    @Test
    fun `el equipo guardado hereda nombre y fecha de hoy`() {
        val entity = CalculateUiState(spaceName = "  Sala  ", brandModel = "")
            .toEntity(nowMillis = 123_456L)!!

        assertEquals("Sala", entity.nombreEspacio)
        assertEquals("Sin marca/modelo", entity.marcaModelo)
        assertEquals(123_456L, entity.fechaInstalacionMillis)
        assertEquals(entity.fechaInstalacionMillis, entity.fechaUltimoMantenimientoMillis)
        assertEquals(12_000, entity.btu)
        assertTrue(entity.consumoEstimadoKwhMes > 0.0)
    }
}
