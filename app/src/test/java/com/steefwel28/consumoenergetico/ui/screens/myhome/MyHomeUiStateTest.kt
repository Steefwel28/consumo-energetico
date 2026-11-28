package com.steefwel28.consumoenergetico.ui.screens.myhome

import com.steefwel28.consumoenergetico.data.local.db.AcUnitEntity
import com.steefwel28.consumoenergetico.data.local.db.BillRecordEntity
import com.steefwel28.consumoenergetico.data.local.db.PropertyGroupEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MyHomeUiStateTest {

    private val group = PropertyGroupEntity(id = 3L, nombreGrupo = "Casa")

    private fun unit(kwh: Double) = AcUnitEntity(
        id = 1L,
        nombreEspacio = "Sala",
        marcaModelo = "Hisense",
        btu = 12_000,
        seer = 21.0,
        esInverter = true,
        voltaje = 220,
        fechaInstalacionMillis = 0L,
        fechaUltimoMantenimientoMillis = 0L,
        consumoEstimadoKwhMes = kwh,
        groupId = group.id,
    )

    private fun bill(id: Long, month: Int, kwh: Double, paid: Double) = BillRecordEntity(
        id = id,
        groupId = group.id,
        year = 2026,
        month = month,
        realKwh = kwh,
        paidBalboas = paid,
    )

    @Test
    fun `reparte climatizacion y resto del hogar con el ultimo recibo`() {
        val state = MyHomeUiState(
            groups = listOf(group),
            units = listOf(unit(54.0)),
            bills = listOf(bill(1, 10, 100.0, 18.0)),
            selectedGroupId = group.id,
        )

        val split = state.climatizationSplit!!

        assertEquals(54.0, split.first, 0.0001)
        assertEquals(46.0, split.second, 0.0001)
    }

    @Test
    fun `la variacion compara contra el mes anterior en kWh`() {
        val state = MyHomeUiState(
            groups = listOf(group),
            bills = listOf(
                bill(1, 10, 100.0, 18.0),
                bill(2, 11, 115.0, 20.0),
            ),
            selectedGroupId = group.id,
        )

        assertEquals(15.0, state.variationPercent(state.groupBills.last())!!, 0.0001)
        assertNull(state.variationPercent(state.groupBills.first()))
    }

    @Test
    fun `la tarifa efectiva usa el total pagado entre los kWh reales`() {
        val record = bill(9, 11, 180.0, 32.5)

        assertEquals(32.5 / 180.0, record.effectiveBalboasPerKwh!!, 0.0000001)
    }

    @Test
    fun `el estado inicial de Mi Casa llega sin equipos`() {
        val state = MyHomeUiState()

        assertTrue(state.units.isEmpty())
        assertTrue(state.groupUnits.isEmpty())
        assertTrue(state.unitsByLocation.isEmpty())
        assertNull(state.climatizationSplit)
    }
}
