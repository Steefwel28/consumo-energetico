package com.steefwel28.consumoenergetico.domain.recommender

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ElectricalSpecAdvisorTest {

    @Test
    fun `todos los splits modernos van a 220 voltios`() {
        listOf(9_000, 12_000, 18_000, 24_000, 28_000, 36_000, 48_000, 60_000).forEach { btu ->
            assertEquals("BTU $btu", 220, ElectricalSpecAdvisor.advise(btu).voltage)
        }
    }

    @Test
    fun `nueve mil usa breaker de quince y cable doce`() {
        val spec = ElectricalSpecAdvisor.advise(9_000)
        assertEquals(15, spec.breakerAmps)
        assertEquals("#12", spec.wireGauge)
        assertEquals(850.0 / 220.0, spec.estimatedAmps, 1e-12)
        assertTrue(spec.isInverter)
    }

    @Test
    fun `un inverter lleva protector de linea directa de dos mil cien julios`() {
        val spec = ElectricalSpecAdvisor.advise(12_000)
        assertEquals("AVTEK SPC-PEBAS-B230", spec.protector.brandAndModel)
        assertEquals(2_100, spec.protector.joules)
        assertEquals("10 s", spec.protector.reconnectLabel)
        assertEquals("Forza FVP-6630B", spec.alternativeProtector?.brandAndModel)
        assertEquals(880, spec.alternativeProtector?.joules)
        assertNull(spec.voltageNote)
    }

    @Test
    fun `un no inverter usa protector convencional y aclara los 110 voltios viejos`() {
        val spec = ElectricalSpecAdvisor.advise(12_000, inverter = false)
        assertEquals(220, spec.voltage)
        assertFalse(spec.isInverter)
        assertEquals("AVTEK SPC-PABB-B230", spec.protector.brandAndModel)
        assertEquals(525, spec.protector.joules)
        assertEquals("3 min", spec.protector.reconnectLabel)
        assertNull(spec.alternativeProtector)
        assertTrue(spec.voltageNote!!.contains("110 V"))
    }

    @Test
    fun `calcula amperaje con la potencia de la etiqueta a 220 voltios`() {
        val spec = ElectricalSpecAdvisor.advise(12_000, nominalWatts = 1_100.0)
        assertEquals(5.0, spec.estimatedAmps, 1e-12)
    }

    @Test
    fun `sin potencia de placa usa el consumo tipico del escalon`() {
        val spec = ElectricalSpecAdvisor.advise(12_000)
        assertEquals(1_150.0 / 220.0, spec.estimatedAmps, 1e-12)
    }

    @Test
    fun `desde dieciocho mil usa breaker de veinte y cable numero diez`() {
        val spec = ElectricalSpecAdvisor.advise(18_000)
        assertEquals(20, spec.breakerAmps)
        assertEquals("#10", spec.wireGauge)
        assertEquals(2_100, spec.protector.joules)
    }

    @Test
    fun `veintiocho mil tambien exige breaker de treinta`() {
        val spec = ElectricalSpecAdvisor.advise(28_000)
        assertEquals(30, spec.breakerAmps)
        assertEquals("#10", spec.wireGauge)
        assertEquals(2_700.0 / 220.0, spec.estimatedAmps, 1e-12)
    }

    @Test
    fun `treinta y seis mil usa cable numero ocho`() {
        val spec = ElectricalSpecAdvisor.advise(36_000)
        assertEquals(30, spec.breakerAmps)
        assertEquals("#8", spec.wireGauge)
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
    fun `sesenta mil usa cable numero ocho y acepta placas reales de treinta y seis mil`() {
        val big = ElectricalSpecAdvisor.advise(60_000)
        assertEquals(40, big.breakerAmps)
        assertEquals("#8", big.wireGauge)
        assertEquals(5_400.0 / 220.0, big.estimatedAmps, 1e-12)

        val real = ElectricalSpecAdvisor.advise(36_679)
        assertEquals(30, real.breakerAmps)
    }

    @Test
    fun `rechaza capacidades fuera de la ficha`() {
        assertThrows(IllegalArgumentException::class.java) {
            ElectricalSpecAdvisor.advise(66_000)
        }
    }
}
