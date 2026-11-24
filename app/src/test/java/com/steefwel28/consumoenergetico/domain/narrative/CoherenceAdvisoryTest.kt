package com.steefwel28.consumoenergetico.domain.narrative

import com.steefwel28.consumoenergetico.domain.calculator.AcConsumptionCalculator
import com.steefwel28.consumoenergetico.ui.screens.calculate.CalculateUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoherenceAdvisoryTest {

    @Test
    fun `potencia baja para los BTU dispara el aviso termico`() {
        val warnings = DiagnosticNarrativeBuilder.coherenceWarnings(12_000.0, 850.0, seer = 21.0)
        assertTrue(warnings.contains(CoherenceWarning.LOW_POWER_FOR_BTU))
    }

    @Test
    fun `SEER bajo RTCA dispara el aviso normativo`() {
        val warnings = DiagnosticNarrativeBuilder.coherenceWarnings(12_000.0, 1_134.0, seer = 16.0)
        assertEquals(listOf(CoherenceWarning.LOW_SEER_RTCA), warnings)
    }

    @Test
    fun `equipo coherente no genera avisos`() {
        val warnings = DiagnosticNarrativeBuilder.coherenceWarnings(12_000.0, 1_134.0, seer = 21.0)
        assertTrue(warnings.isEmpty())
    }

    @Test
    fun `potencia alta con SEER alto pide revisar placa`() {
        val warnings = DiagnosticNarrativeBuilder.coherenceWarnings(12_000.0, 2_000.0, seer = 21.0)
        assertTrue(warnings.contains(CoherenceWarning.HIGH_POWER_VS_SEER))
    }

    @Test
    fun `los avisos no alteran kWh ni costo base`() {
        val state = CalculateUiState(
            btuInput = "12000",
            wattsInput = "850",
            seerInput = "21",
        )
        val spec = checkNotNull(state.spec)
        assertTrue(state.coherenceWarnings.isNotEmpty())
        assertEquals(
            AcConsumptionCalculator.monthlyKwh(spec, state.usage, state.daysInMonth.toDouble()),
            checkNotNull(state.monthlyKwh),
            1e-9,
        )
        assertEquals(
            checkNotNull(state.tariffEstimate).addedCostBalboas,
            checkNotNull(state.monthlyCost),
            1e-9,
        )
    }
}
