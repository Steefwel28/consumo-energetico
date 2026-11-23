package com.steefwel28.consumoenergetico.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Test

class PanamaTariffEngineTest {

    @Test
    fun `cobra el cargo fijo dentro de los primeros diez kWh`() {
        assertEquals(1.20, PanamaTariffEngine.bill(TariffMode.Tiered(PanamaUtility.ENSA), 5.0), 1e-12)
        assertEquals(0.0, PanamaTariffEngine.marginalRate(TariffMode.Tiered(PanamaUtility.ENSA), 5.0), 0.0)
    }

    @Test
    fun `aplica el pliego escalonado de ENSA`() {
        val mode = TariffMode.Tiered(PanamaUtility.ENSA)
        assertEquals(34.811, PanamaTariffEngine.bill(mode, 200.0), 1e-9)
        assertEquals(0.2084, PanamaTariffEngine.marginalRate(mode, 320.0), 0.0)
        assertEquals(0.2320, PanamaTariffEngine.marginalRate(mode, 900.0), 0.0)
        assertEquals(0.1815, PanamaTariffEngine.marginalRate(TariffMode.Tiered(PanamaUtility.NATURGY), 100.0), 0.0)
    }

    @Test
    fun `el costo añadido es exacto cuando el aire cruza de bloque`() {
        val estimate = PanamaTariffEngine.estimate(
            mode = TariffMode.Tiered(PanamaUtility.ENSA),
            baseKwh = 290.0,
            addedKwh = 30.0,
        )
        assertEquals(5.937, estimate.addedCostBalboas, 1e-9)
        assertEquals(56.669, estimate.totalBillBalboas, 1e-9)
        assertEquals(0.2084, estimate.marginalBalboasPerKwh, 0.0)
    }

    @Test
    fun `modo costo manual deduce la tarifa del recibo`() {
        val receipt = TariffMode.Receipt(totalBalboas = 60.0, consumedKwh = 300.0)
        assertEquals(0.20, receipt.balboasPerKwh, 1e-12)

        val estimate = PanamaTariffEngine.estimate(receipt, baseKwh = 300.0, addedKwh = 100.0)
        assertEquals(20.0, estimate.addedCostBalboas, 1e-9)
        assertEquals(0.20, estimate.effectiveBalboasPerKwh, 1e-12)
    }

    @Test
    fun `tarifa directa multiplica consumo sin cargo fijo`() {
        val estimate = PanamaTariffEngine.estimate(
            mode = TariffMode.DirectRate(0.22),
            baseKwh = 150.0,
            addedKwh = 50.0,
        )
        assertEquals(11.0, estimate.addedCostBalboas, 1e-12)
        assertEquals(33.0, estimate.baseBillBalboas, 1e-12)
    }
}
