package com.steefwel28.consumoenergetico.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PowerEstimatorTest {

    @Test
    fun `inverter aplica el factor estacional al SEER`() {
        assertEquals(
            12_000.0 / (21.0 * 0.875),
            PowerEstimator.fromBtu(12_000.0, seer = 21.0, isInverter = true),
            1e-9,
        )
    }

    @Test
    fun `no inverter divide directo entre el SEER`() {
        assertEquals(
            12_000.0 / 21.0,
            PowerEstimator.fromBtu(12_000.0, seer = 21.0, isInverter = false),
            1e-9,
        )
    }

    @Test
    fun `sin SEER usa el EER de respaldo`() {
        assertEquals(12_000.0 / 3.412, PowerEstimator.fromBtu(12_000.0), 1e-9)
        assertEquals(12_000.0 / 3.412, PowerEstimator.fromBtu(12_000.0, seer = 0.0), 1e-9)
    }

    @Test
    fun `rechaza capacidades no positivas`() {
        assertThrows(IllegalArgumentException::class.java) { PowerEstimator.fromBtu(0.0) }
    }
}
