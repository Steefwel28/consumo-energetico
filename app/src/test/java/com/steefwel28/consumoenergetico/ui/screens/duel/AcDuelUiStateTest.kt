package com.steefwel28.consumoenergetico.ui.screens.duel

import com.steefwel28.consumoenergetico.domain.calculator.PanamaUtility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AcDuelUiStateTest {

    @Test
    fun `el inverter gana al convencional con el mismo btu`() {
        val state = AcDuelUiState(
            btuAInput = "12000",
            seerAInput = "21",
            inverterA = true,
            btuBInput = "12000",
            seerBInput = "16",
            inverterB = false,
            hoursPerDay = 8,
        )
        assertTrue(state.canCompare)
        assertTrue(checkNotNull(state.kwhA) < checkNotNull(state.kwhB))
        assertEquals(DuelWinner.A, state.winner)
        assertNotNull(state.monthlySavingsBalboas)
        assertTrue(checkNotNull(state.savingsPercent) > 0.0)
    }

    @Test
    fun `equipos identicos empatan sin ahorro`() {
        val state = AcDuelUiState(
            btuAInput = "12000",
            seerAInput = "21",
            inverterA = true,
            btuBInput = "12000",
            seerBInput = "21",
            inverterB = true,
        )
        assertEquals(DuelWinner.TIE, state.winner)
        assertNull(state.monthlySavingsBalboas)
        assertNull(state.paybackMonths)
    }

    @Test
    fun `el payback divide la diferencia de precio entre el ahorro mensual`() {
        val state = AcDuelUiState(
            btuAInput = "12000",
            seerAInput = "21",
            inverterA = true,
            btuBInput = "12000",
            seerBInput = "16",
            inverterB = false,
            priceDiffInput = "300",
        )
        val savings = checkNotNull(state.monthlySavingsBalboas)
        assertEquals(300.0 / savings, checkNotNull(state.paybackMonths), 1e-9)
        assertEquals(savings * 12.0, checkNotNull(state.annualSavingsBalboas), 1e-9)
    }

    @Test
    fun `sin btu no hay duelo`() {
        val state = AcDuelUiState(btuBInput = "")
        assertTrue(!state.canCompare)
        assertNull(state.winner)
    }

    @Test
    fun `el pliego edechi encarece el duelo frente a ensa en el mismo bloque`() {
        val base = AcDuelUiState(
            btuAInput = "12000",
            seerAInput = "21",
            inverterA = true,
            btuBInput = "12000",
            seerBInput = "16",
            inverterB = false,
        )
        val ensa = checkNotNull(base.copy(utility = PanamaUtility.ENSA).monthlyCostA)
        val edechi = checkNotNull(base.copy(utility = PanamaUtility.NATURGY_EDECHI).monthlyCostA)
        assertTrue(edechi > ensa)
    }

    @Test
    fun `la potencia de placa sustituye la estimada por seer`() {
        val state = AcDuelUiState(
            btuAInput = "12000",
            seerAInput = "",
            wattsAInput = "1000",
            inverterA = false,
            hoursPerDay = 8,
        )
        // 1000 W a 8 h/día con ciclo 75 %: 6 kWh/día, 180 kWh/mes.
        assertEquals(180.0, checkNotNull(state.kwhA), 1e-9)
    }
}
