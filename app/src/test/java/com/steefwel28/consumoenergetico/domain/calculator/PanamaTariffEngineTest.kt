package com.steefwel28.consumoenergetico.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Test

class PanamaTariffEngineTest {

    @Test
    fun `cobra el cargo fijo dentro de los primeros diez kWh`() {
        assertEquals(2.76, PanamaTariffEngine.bill(TariffMode.Tiered(PanamaUtility.ENSA), 5.0), 1e-12)
        assertEquals(0.0, PanamaTariffEngine.marginalRate(TariffMode.Tiered(PanamaUtility.ENSA), 5.0), 0.0)
    }

    @Test
    fun `aplica el pliego ASEP de ENSA y Naturgy con cuatro decimales`() {
        val mode = TariffMode.Tiered(PanamaUtility.ENSA)
        assertEquals(36.4603, PanamaTariffEngine.bill(mode, 200.0), 1e-9)
        assertEquals(0.24666, PanamaTariffEngine.marginalRate(mode, 320.0), 0.0)
        assertEquals(0.27005, PanamaTariffEngine.marginalRate(mode, 900.0), 0.0)
        assertEquals(0.17737, PanamaTariffEngine.marginalRate(mode, 100.0), 0.0)
        assertEquals(0.17624, PanamaTariffEngine.marginalRate(TariffMode.Tiered(PanamaUtility.NATURGY), 100.0), 0.0)
        assertEquals(0.23177, PanamaTariffEngine.marginalRate(TariffMode.Tiered(PanamaUtility.NATURGY), 320.0), 0.0)
        assertEquals(0.25027, PanamaTariffEngine.marginalRate(TariffMode.Tiered(PanamaUtility.NATURGY), 900.0), 0.0)
        assertEquals(0.19175, PanamaTariffEngine.marginalRate(TariffMode.Tiered(PanamaUtility.NATURGY_EDECHI), 100.0), 0.0)
        assertEquals(0.25341, PanamaTariffEngine.marginalRate(TariffMode.Tiered(PanamaUtility.NATURGY_EDECHI), 320.0), 0.0)
        assertEquals(0.27486, PanamaTariffEngine.marginalRate(TariffMode.Tiered(PanamaUtility.NATURGY_EDECHI), 900.0), 0.0)
    }

    @Test
    fun `la frontera de 300 kwh activa el segundo escalon en las tres distribuidoras`() {
        val ensa = TariffMode.Tiered(PanamaUtility.ENSA)
        val edemet = TariffMode.Tiered(PanamaUtility.NATURGY)
        val edechi = TariffMode.Tiered(PanamaUtility.NATURGY_EDECHI)
        // 299,99 kWh sigue en el primer bloque tras el cargo fijo.
        assertEquals(0.17737, PanamaTariffEngine.marginalRate(ensa, 299.99), 0.0)
        assertEquals(0.17624, PanamaTariffEngine.marginalRate(edemet, 299.99), 0.0)
        assertEquals(0.19175, PanamaTariffEngine.marginalRate(edechi, 299.99), 0.0)
        // 300,00 kWh ya paga el segundo escalón (fromKwh incluido).
        assertEquals(0.24666, PanamaTariffEngine.marginalRate(ensa, 300.0), 0.0)
        assertEquals(0.23177, PanamaTariffEngine.marginalRate(edemet, 300.0), 0.0)
        assertEquals(0.25341, PanamaTariffEngine.marginalRate(edechi, 300.0), 0.0)
        // El kWh 301 cuesta exactamente la tarifa marginal del segundo bloque.
        assertEquals(
            0.24666,
            PanamaTariffEngine.bill(ensa, 301.0) - PanamaTariffEngine.bill(ensa, 300.0),
            1e-9,
        )
        assertEquals(
            0.23177,
            PanamaTariffEngine.bill(edemet, 301.0) - PanamaTariffEngine.bill(edemet, 300.0),
            1e-9,
        )
        assertEquals(
            0.25341,
            PanamaTariffEngine.bill(edechi, 301.0) - PanamaTariffEngine.bill(edechi, 300.0),
            1e-9,
        )
    }

    @Test
    fun `el costo añadido es exacto cuando el aire cruza de bloque`() {
        val estimate = PanamaTariffEngine.estimate(
            mode = TariffMode.Tiered(PanamaUtility.ENSA),
            baseKwh = 290.0,
            addedKwh = 30.0,
        )
        assertEquals(6.7069, estimate.addedCostBalboas, 1e-9)
        assertEquals(59.1305, estimate.totalBillBalboas, 1e-9)
        assertEquals(0.24666, estimate.marginalBalboasPerKwh, 0.0)
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
