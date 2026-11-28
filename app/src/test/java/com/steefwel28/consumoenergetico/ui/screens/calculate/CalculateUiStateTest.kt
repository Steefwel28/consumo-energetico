package com.steefwel28.consumoenergetico.ui.screens.calculate

import com.steefwel28.consumoenergetico.domain.calculator.PanamaUtility
import com.steefwel28.consumoenergetico.domain.calculator.TariffComparison
import com.steefwel28.consumoenergetico.domain.calculator.TariffMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class CalculateUiStateTest {

    private fun baseState() = CalculateUiState(btuInput = "12000", wattsInput = "1150")

    @Test
    fun `el estado inicial calcula consumo y ficha electrica`() {
        val state = baseState()

        assertNotNull(state.spec)
        assertTrue(state.monthlyKwh!! > 0.0)
        assertEquals(220, state.electrical!!.voltage)
        assertTrue(state.canSave)
        assertTrue(state.reportNarrative().contains("kWh"))
    }

    @Test
    fun `la tarifa directa manual cobra exactamente por kWh`() {
        val state = CalculateUiState(
            btuInput = "12000",
            wattsInput = "1150",
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
            btuInput = "12000",
            wattsInput = "1150",
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
            btuInput = "12000",
            wattsInput = "1150",
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
        val clean = baseState()

        assertTrue(clean.copy(setpointCelsius = 18).monthlyKwh!! > clean.monthlyKwh!!)
        assertTrue(clean.copy(cleanFilters = false).monthlyKwh!! > clean.monthlyKwh!!)
    }

    @Test
    fun `el comparador detecta ahorro y retorno`() {
        val state = CalculateUiState(
            btuInput = "12000",
            wattsInput = "1150",
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
        val entity = CalculateUiState(
            spaceName = "  Sala  ",
            brandModel = "",
            btuInput = "12000",
            wattsInput = "1150",
        ).toEntity(nowMillis = 123_456L)!!

        assertEquals("Sala", entity.nombreEspacio)
        assertEquals("Sin marca/modelo", entity.marcaModelo)
        assertEquals(123_456L, entity.fechaInstalacionMillis)
        assertEquals(entity.fechaInstalacionMillis, entity.fechaUltimoMantenimientoMillis)
        assertEquals(12_000, entity.btu)
        assertTrue(entity.consumoEstimadoKwhMes > 0.0)
    }

    @Test
    fun `la comparativa calcula ENSA y Naturgy`() {
        val state = baseState()

        val ensa = state.ensaEstimate!!
        val naturgy = state.naturgyEstimate!!

        assertTrue(ensa.addedCostBalboas > 0.0)
        assertTrue(naturgy.addedCostBalboas > 0.0)
    }

    @Test
    fun `el equipo guardado se vincula al grupo y la ubicacion elegidos`() {
        val entity = baseState()
            .toEntity(nowMillis = 99L, groupId = 7L, ubicacion = "Cocina")!!

        assertEquals(7L, entity.groupId)
        assertEquals("Cocina", entity.ubicacion)
    }

    @Test
    fun `acepta placas comerciales de tres toneladas y conserva guardar`() {
        val state = CalculateUiState(btuInput = "36,679", wattsInput = "3,450")

        assertEquals(36_679.0, state.btu!!, 0.001)
        assertNotNull(state.spec)
        assertTrue(state.canSave)
    }

    @Test
    fun `el periodo escala kWh y costo pero el escalon usa el mes real`() {
        val month = baseState().copy(usagePeriod = UsagePeriod.MONTH)
        val day = month.copy(usagePeriod = UsagePeriod.DAY)

        assertEquals(day.monthlyKwh!! / month.daysInMonth, day.periodKwh!!, 0.0001)
        assertEquals(month.monthlyKwh!!, month.periodKwh!!, 0.0001)
        assertEquals(
            month.tariffEstimate!!.marginalBalboasPerKwh,
            day.tariffEstimate!!.marginalBalboasPerKwh,
            0.0000001,
        )
        assertEquals(
            month.tariffEstimate!!.addedCostBalboas / month.daysInMonth,
            day.tariffEstimate!!.addedCostBalboas,
            0.0001,
        )
    }

    @Test
    fun `la quincena escala kWh y costo por quince dias`() {
        val month = baseState().copy(usagePeriod = UsagePeriod.MONTH)
        val fortnight = month.copy(usagePeriod = UsagePeriod.FORTNIGHT)
        val factor = 15.0 / YearMonth.now().lengthOfMonth()

        assertEquals(month.monthlyKwh!! * factor, fortnight.periodKwh!!, 0.0000001)
        assertEquals(
            month.tariffEstimate!!.addedCostBalboas * factor,
            fortnight.tariffEstimate!!.addedCostBalboas,
            0.0000001,
        )
    }

    @Test
    fun `las tres distribuidoras cotizan simultaneamente para el periodo activo`() {
        val state = baseState().copy(baseMonthlyKwh = 350.0)
        val quotes = state.distributorQuotes!!

        assertEquals(3, quotes.size)
        assertEquals(PanamaUtility.ENSA, quotes[0].utility)
        assertEquals(PanamaUtility.NATURGY, quotes[1].utility)
        assertEquals(PanamaUtility.NATURGY_EDECHI, quotes[2].utility)

        assertEquals(state.ensaEstimate!!.addedCostBalboas, quotes[0].costBalboas, 0.0000001)
        assertEquals(state.naturgyEstimate!!.addedCostBalboas, quotes[1].costBalboas, 0.0000001)
        assertEquals(state.edechiEstimate!!.addedCostBalboas, quotes[2].costBalboas, 0.0000001)

        // EDEMET cobra menos que ENSA en cada bloque y EDECHI es la más cara.
        assertEquals(
            state.naturgyEstimate!!.addedCostBalboas,
            quotes.minOf { it.costBalboas },
            0.0000001,
        )
        assertEquals(
            state.edechiEstimate!!.addedCostBalboas,
            quotes.maxOf { it.costBalboas },
            0.0000001,
        )
        assertTrue(quotes.maxOf { it.costBalboas } - quotes.minOf { it.costBalboas } > 0.0)

        val dayQuotes = state.copy(usagePeriod = UsagePeriod.DAY).distributorQuotes!!
        val dayFactor = 1.0 / YearMonth.now().lengthOfMonth()
        assertEquals(quotes[0].costBalboas * dayFactor, dayQuotes[0].costBalboas, 0.0000001)
        assertEquals(quotes[1].costBalboas * dayFactor, dayQuotes[1].costBalboas, 0.0000001)
        assertEquals(quotes[2].costBalboas * dayFactor, dayQuotes[2].costBalboas, 0.0000001)
    }

    @Test
    fun `el equipo guardado hereda las horas configuradas`() {
        val entity = baseState().copy(hoursPerDay = 12).toEntity(nowMillis = 5L)!!

        assertEquals(12.0, entity.horasUsoDiario, 0.0)
    }

    @Test
    fun `el simulador no altera el consumo principal de la grafica`() {
        val base = baseState()
        val simulated = base.copy(
            simulatedSetpointCelsius = 18,
            simulatedCleanFilters = false,
        )

        assertEquals(base.monthlyKwh!!, simulated.monthlyKwh!!, 0.0000001)
        assertTrue(simulated.simulatedMonthlyKwh!! > base.simulatedMonthlyKwh!!)
    }

    @Test
    fun `la comparativa arranca en VS para mostrar ambas tarifas`() {
        val state = CalculateUiState()

        assertEquals(TariffComparison.VS, state.chartMode)
    }

    @Test
    fun `sin datos la grafica queda en estado vacio`() {        val state = CalculateUiState()

        assertNull(state.btu)
        assertNull(state.spec)
        assertNull(state.monthlyKwh)
    }

    @Test
    fun `sin watts estima la potencia con BTU y SEER`() {
        val state = CalculateUiState(btuInput = "12000", seerInput = "21")

        assertTrue(state.wattsEstimatedBySeer)
        assertEquals(12_000.0 / (21.0 * 0.875), state.nominalWatts!!, 0.01)
        assertNotNull(state.spec)
        assertTrue(state.monthlyKwh!! > 0.0)
    }
}
