package com.steefwel28.consumoenergetico.domain.recommender

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ElectricalSpecAdvisorTest {

    @Test
    fun `un equipo de nueve mil usa 110 voltios y breaker de 15`() {
        val spec = ElectricalSpecAdvisor.advise(9_000)
        assertEquals(110, spec.voltage)
        assertEquals(15, spec.breakerAmps)
        assertEquals("#12", spec.wireGauge)
        assertEquals(900, spec.surgeProtectorJoules)
        assertEquals(3..5, spec.reconnectDelayMinutes)
    }

    @Test
    fun `calcula amperaje con la potencia de la etiqueta`() {
        val spec = ElectricalSpecAdvisor.advise(12_000, nominalWatts = 1_100.0)
        assertEquals(110, spec.voltage)
        assertEquals(10.0, spec.estimatedAmps, 1e-12)
    }

    @Test
    fun `sin potencia de placa usa el consumo tipico del escalon`() {
        val spec = ElectricalSpecAdvisor.advise(12_000)
        assertEquals(1_150.0 / 110.0, spec.estimatedAmps, 1e-12)
    }

    @Test
    fun `desde dieciocho mil pasa a 220 voltios con cable numero diez`() {
        val spec = ElectricalSpecAdvisor.advise(18_000)
        assertEquals(220, spec.voltage)
        assertEquals(20, spec.breakerAmps)
        assertEquals("#10", spec.wireGauge)
        assertEquals(1_200, spec.surgeProtectorJoules)
    }

    @Test
    fun `veinticuatro mil exige breaker de treinta amperios`() {
        val spec = ElectricalSpecAdvisor.advise(24_000)
        assertEquals(220, spec.voltage)
        assertEquals(30, spec.breakerAmps)
        assertEquals(1_400, spec.surgeProtectorJoules)
    }

    @Test
    fun `treinta y seis mil usa cable numero ocho`() {
        val spec = ElectricalSpecAdvisor.advise(36_000)
        assertEquals(220, spec.voltage)
        assertEquals(30, spec.breakerAmps)
        assertEquals("#8", spec.wireGauge)
        assertEquals(1_400, spec.surgeProtectorJoules)
    }

    @Test
    fun `tuberia dentro del kit estandar no genera alerta`() {
        assertNull(ElectricalSpecAdvisor.advise(12_000, pipeDistanceMeters = 3.0).installationAlert)
    }

    @Test
    fun `tuberia sobre tres metros alerta cobre extra y ajuste de gas`() {
        val alert = requireNotNull(
            ElectricalSpecAdvisor.advise(12_000, pipeDistanceMeters = 6.5).installationAlert,
        )
        assertTrue(alert.contains("6.5 m"))
        assertTrue(alert.contains("tubería de cobre adicional"))
        assertTrue(alert.contains("gas refrigerante"))
    }

    @Test
    fun `rechaza capacidades fuera de la ficha`() {
        assertThrows(IllegalArgumentException::class.java) {
            ElectricalSpecAdvisor.advise(48_000)
        }
    }
}
