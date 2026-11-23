package com.steefwel28.consumoenergetico.domain.narrative

import com.steefwel28.consumoenergetico.domain.recommender.AcRecommendation
import com.steefwel28.consumoenergetico.domain.recommender.AcSpaceRecommender
import com.steefwel28.consumoenergetico.domain.recommender.SpaceDetails
import com.steefwel28.consumoenergetico.domain.recommender.SunExposure
import com.steefwel28.consumoenergetico.domain.units.UnitPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticNarrativeBuilderTest {

    private val metric = UnitPreferences()

    private fun recommend(area: Double, details: SpaceDetails = SpaceDetails()): AcRecommendation =
        AcSpaceRecommender.recommend(metric, area, details)

    @Test
    fun `explica la base y cada factor activado`() {
        val recommendation = recommend(20.0, SpaceDetails(sunExposure = SunExposure.DIRECT_AFTERNOON_SUN))
        val diagnostic = DiagnosticNarrativeBuilder.build(recommendation)

        assertTrue(diagnostic.explanation.contains("20 m²"))
        assertTrue(diagnostic.explanation.contains("13,600 BTU/h"))
        assertTrue(diagnostic.explanation.contains("Ventanal con sol directo de tarde"))
        assertTrue(diagnostic.explanation.contains("2,992 BTU/h"))
        assertTrue(diagnostic.explanation.contains("16,592 BTU/h"))
        assertTrue(diagnostic.explanation.contains("18,000 BTU/h"))
        assertNull(diagnostic.undersizedAlert)
    }

    @Test
    fun `alerta la trampa del aire pequeno bajo el noventa por ciento`() {
        val recommendation = recommend(20.0)
        val diagnostic = DiagnosticNarrativeBuilder.build(recommendation, selectedBtu = 12_000)

        val alert = requireNotNull(diagnostic.undersizedAlert)
        assertTrue(alert.contains("Trampa del Aire Pequeño"))
        assertTrue(alert.contains("12,000 BTU/h"))
        assertTrue(alert.contains("13,600 BTU/h"))
        assertTrue(alert.contains("100 %"))
    }

    @Test
    fun `no alerta cuando el equipo elegido cubre el calculo`() {
        val recommendation = recommend(20.0)
        assertNull(DiagnosticNarrativeBuilder.build(recommendation, selectedBtu = 18_000).undersizedAlert)
    }

    @Test
    fun `incluye capsulas de temperatura y modo dry`() {
        val diagnostic = DiagnosticNarrativeBuilder.build(recommend(20.0))
        assertEquals(2, diagnostic.capsules.size)
        assertTrue(diagnostic.capsules.any { it.contains("24 °C") && it.contains("6 %") })
        assertTrue(diagnostic.capsules.any { it.contains("Modo Dry") })
    }

    @Test
    fun `sugiere dividir el espacio cuando supera treinta y seis mil BTU`() {
        val diagnostic = DiagnosticNarrativeBuilder.build(recommend(area = 60.0))
        assertTrue(diagnostic.explanation.contains("dividir el espacio"))
        assertEquals(36_000, recommend(area = 60.0).commercialBtu)
    }
}
