package com.steefwel28.consumoenergetico.domain.recommender

import com.steefwel28.consumoenergetico.domain.units.LengthUnit
import com.steefwel28.consumoenergetico.domain.units.UnitPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AcSpaceRecommenderTest {

    private val metric = UnitPreferences(length = LengthUnit.METERS)
    private val imperial = UnitPreferences(length = LengthUnit.FEET)

    @Test
    fun `calcula la base de 680 BTU por metro cuadrado`() {
        val recommendation = AcSpaceRecommender.recommend(metric, length = 5.0, width = 4.0)
        assertEquals(20.0, recommendation.areaSquareMeters, 1e-12)
        assertEquals(13_600.0, recommendation.exactBtu, 1e-9)
        assertEquals(18_000, recommendation.commercialBtu)
        assertTrue(recommendation.factors.isEmpty())
    }

    @Test
    fun `convierte largo y ancho recibidos en pies`() {
        val recommendation = AcSpaceRecommender.recommend(imperial, length = 10.0, width = 20.0)
        assertEquals(18.580608, recommendation.areaSquareMeters, 1e-9)
        assertEquals(12_634.81344, recommendation.exactBtu, 1e-6)
        assertEquals(18_000, recommendation.commercialBtu)
    }

    @Test
    fun `el area directa tambien respeta la preferencia de pies`() {
        val recommendation = AcSpaceRecommender.recommend(imperial, area = 200.0)
        assertEquals(18.580608, recommendation.areaSquareMeters, 1e-9)
    }

    @Test
    fun `el sol directo de tarde recarga veintidos por ciento`() {
        val recommendation = AcSpaceRecommender.recommend(
            preferences = metric,
            area = 20.0,
            details = SpaceDetails(sunExposure = SunExposure.DIRECT_AFTERNOON_SUN),
        )
        assertEquals(16_592.0, recommendation.exactBtu, 1e-9)
        assertEquals(2_992.0, recommendation.factors.single().addedBtu, 1e-9)
        assertEquals(18_000, recommendation.commercialBtu)
    }

    @Test
    fun `suma quinientos cincuenta BTU por persona sobre dos`() {
        val recommendation = AcSpaceRecommender.recommend(
            preferences = metric,
            area = 20.0,
            details = SpaceDetails(people = 5),
        )
        assertEquals(15_250.0, recommendation.exactBtu, 1e-9)
        assertEquals("5 personas habituales", recommendation.factors.single().label)
    }

    @Test
    fun `el techo de zinc recarga dieciocho por ciento`() {
        val recommendation = AcSpaceRecommender.recommend(
            preferences = metric,
            area = 20.0,
            details = SpaceDetails(ceiling = CeilingType.ZINC_OR_HIGH_CEILING),
        )
        assertEquals(16_048.0, recommendation.exactBtu, 1e-9)
    }

    @Test
    fun `suma las fuentes de calor marcadas`() {
        val recommendation = AcSpaceRecommender.recommend(
            preferences = metric,
            area = 20.0,
            details = SpaceDetails(heatSources = setOf(HeatSource.GAMING_PC, HeatSource.OPEN_KITCHEN)),
        )
        assertEquals(14_700.0, recommendation.exactBtu, 1e-9)
        assertEquals(2, recommendation.factors.size)
    }

    @Test
    fun `la tuberia larga recarga dos por ciento por metro sobre cinco`() {
        val recommendation = AcSpaceRecommender.recommend(
            preferences = metric,
            area = 20.0,
            details = SpaceDetails(pipeDistance = 10.0),
        )
        assertEquals(14_960.0, recommendation.exactBtu, 1e-9)
        assertTrue(recommendation.factors.single().label.startsWith("Tubería larga"))
    }

    @Test
    fun `la tuberia dentro del umbral no recarga`() {
        val recommendation = AcSpaceRecommender.recommend(
            preferences = metric,
            area = 20.0,
            details = SpaceDetails(pipeDistance = 5.0),
        )
        assertEquals(13_600.0, recommendation.exactBtu, 1e-9)
    }

    @Test
    fun `la tuberia en pies se convierte antes de aplicar el recargo`() {
        val recommendation = AcSpaceRecommender.recommend(
            preferences = imperial,
            area = 200.0,
            details = SpaceDetails(pipeDistance = 32.8),
        )
        assertEquals(9.997, 32.8 * 0.3048, 1e-3)
        assertTrue(recommendation.exactBtu > 12_634.81344)
        assertEquals(1, recommendation.factors.size)
    }

    @Test
    fun `un espacio enorme se topa en el escalon de treinta y seis mil`() {
        val recommendation = AcSpaceRecommender.recommend(metric, area = 100.0)
        assertEquals(68_000.0, recommendation.exactBtu, 1e-9)
        assertEquals(36_000, recommendation.commercialBtu)
    }
}
