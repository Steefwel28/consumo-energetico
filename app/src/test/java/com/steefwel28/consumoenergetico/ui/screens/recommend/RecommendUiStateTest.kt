package com.steefwel28.consumoenergetico.ui.screens.recommend

import com.steefwel28.consumoenergetico.domain.recommender.SunExposure
import com.steefwel28.consumoenergetico.domain.units.LengthUnit
import com.steefwel28.consumoenergetico.domain.units.UnitPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendUiStateTest {

    @Test
    fun `area en metros produce capacidad y ficha electrica`() {
        val state = RecommendUiState(areaInput = "36")

        val recommendation = state.recommendation!!

        assertEquals(24_480.0, recommendation.exactBtu, 0.001)
        assertEquals(36_000, recommendation.commercialBtu)
        assertEquals(220, state.electrical!!.voltage)
    }

    @Test
    fun `convierte pies cuadrados a metros cuadrados`() {
        val state = RecommendUiState(
            unitPreferences = UnitPreferences(length = LengthUnit.FEET),
            areaInput = "1000",
        )

        assertEquals(92.90304, state.recommendation!!.areaSquareMeters, 0.0001)
    }

    @Test
    fun `alerta cuando el equipo elegido queda corto`() {
        val state = RecommendUiState(
            areaInput = "36",
            selectedBtuInput = "18000",
        )

        val alert = state.diagnostic!!.undersizedAlert!!

        assertTrue(alert.contains("Trampa del Aire Pequeño"))
        assertEquals(220, state.electrical!!.voltage)
    }

    @Test
    fun `tuberia mayor a tres metros dispara la alerta de instalacion`() {
        val state = RecommendUiState(areaInput = "36", pipeDistanceInput = "4")

        assertNotNull(state.electrical!!.installationAlert)
    }

    @Test
    fun `el sol directo aumenta la carga y se explica en un factor`() {
        val base = RecommendUiState(areaInput = "20").recommendation!!
        val sunny = RecommendUiState(
            areaInput = "20",
            sunExposure = SunExposure.DIRECT_AFTERNOON_SUN,
        ).recommendation!!

        assertTrue(sunny.exactBtu > base.exactBtu)
        assertTrue(sunny.factors.any { it.label == SunExposure.DIRECT_AFTERNOON_SUN.label })
    }

    @Test
    fun `sin detalles no hay recargos`() {
        assertTrue(RecommendUiState(areaInput = "20").recommendation!!.factors.isEmpty())
    }

    @Test
    fun `un area invalida no recomienda nada`() {
        assertNull(RecommendUiState(areaInput = "0").recommendation)
        assertNull(RecommendUiState(areaInput = "texto").recommendation)
    }
}
