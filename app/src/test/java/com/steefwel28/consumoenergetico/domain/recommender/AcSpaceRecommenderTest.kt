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
    fun `diez metros cuadrados se cubren con nueve mil BTU`() {
        val recommendation = AcSpaceRecommender.recommend(metric, area = 10.0)
        assertEquals(9_000.0, recommendation.baseBtu, 1e-9)
        assertEquals(9_000.0, recommendation.exactBtu, 1e-9)
        assertEquals(9_000, recommendation.commercialBtu)
        assertTrue(recommendation.factors.isEmpty())
    }

    @Test
    fun `los rangos de taller se respetan salto por salto`() {
        val expected = mapOf(
            10.1 to 12_000,
            16.0 to 12_000,
            16.5 to 18_000,
            22.0 to 18_000,
            23.0 to 24_000,
            32.0 to 24_000,
            33.0 to 28_000,
            38.0 to 28_000,
            39.0 to 36_000,
            45.0 to 36_000,
        )
        expected.forEach { (area, btu) ->
            val recommendation = AcSpaceRecommender.recommend(metric, area = area)
            assertEquals("Área $area m²", btu, recommendation.commercialBtu)
        }
    }

    @Test
    fun `veinte metros cuadrados usan la base de dieciocho mil BTU`() {
        val recommendation = AcSpaceRecommender.recommend(metric, length = 5.0, width = 4.0)
        assertEquals(20.0, recommendation.areaSquareMeters, 1e-12)
        assertEquals(18_000.0, recommendation.exactBtu, 1e-9)
        assertEquals(18_000, recommendation.commercialBtu)
        assertTrue(recommendation.factors.isEmpty())
    }

    @Test
    fun `convierte largo y ancho recibidos en pies`() {
        val recommendation = AcSpaceRecommender.recommend(imperial, length = 10.0, width = 20.0)
        assertEquals(18.580608, recommendation.areaSquareMeters, 1e-9)
        assertEquals(18_000.0, recommendation.exactBtu, 1e-9)
        assertEquals(18_000, recommendation.commercialBtu)
    }

    @Test
    fun `el area directa tambien respeta la preferencia de pies`() {
        val recommendation = AcSpaceRecommender.recommend(imperial, area = 200.0)
        assertEquals(18.580608, recommendation.areaSquareMeters, 1e-9)
    }

    @Test
    fun `el sol directo de tarde sube el escalon a veinticuatro mil`() {
        val recommendation = AcSpaceRecommender.recommend(
            preferences = metric,
            area = 20.0,
            details = SpaceDetails(sunExposure = SunExposure.DIRECT_AFTERNOON_SUN),
        )
        assertEquals(21_960.0, recommendation.exactBtu, 1e-9)
        assertEquals(3_960.0, recommendation.factors.single().addedBtu, 1e-9)
        assertEquals(24_000, recommendation.commercialBtu)
    }

    @Test
    fun `suma quinientos cincuenta BTU por persona sobre dos`() {
        val recommendation = AcSpaceRecommender.recommend(
            preferences = metric,
            area = 20.0,
            details = SpaceDetails(people = 5),
        )
        assertEquals(19_650.0, recommendation.exactBtu, 1e-9)
        assertEquals("5 personas habituales", recommendation.factors.single().label)
        assertEquals(24_000, recommendation.commercialBtu)
    }

    @Test
    fun `el techo de zinc recarga dieciocho por ciento`() {
        val recommendation = AcSpaceRecommender.recommend(
            preferences = metric,
            area = 20.0,
            details = SpaceDetails(ceiling = CeilingType.ZINC_OR_HIGH_CEILING),
        )
        assertEquals(21_240.0, recommendation.exactBtu, 1e-9)
    }

    @Test
    fun `suma las fuentes de calor marcadas`() {
        val recommendation = AcSpaceRecommender.recommend(
            preferences = metric,
            area = 20.0,
            details = SpaceDetails(heatSources = setOf(HeatSource.GAMING_PC, HeatSource.OPEN_KITCHEN)),
        )
        assertEquals(19_100.0, recommendation.exactBtu, 1e-9)
        assertEquals(2, recommendation.factors.size)
    }

    @Test
    fun `la tuberia larga recarga dos por ciento por metro sobre cinco`() {
        val recommendation = AcSpaceRecommender.recommend(
            preferences = metric,
            area = 20.0,
            details = SpaceDetails(pipeDistance = 10.0),
        )
        assertEquals(19_800.0, recommendation.exactBtu, 1e-9)
        assertTrue(recommendation.factors.single().label.startsWith("Tubería larga"))
    }

    @Test
    fun `la tuberia dentro del umbral no recarga`() {
        val recommendation = AcSpaceRecommender.recommend(
            preferences = metric,
            area = 20.0,
            details = SpaceDetails(pipeDistance = 5.0),
        )
        assertEquals(18_000.0, recommendation.exactBtu, 1e-9)
    }

    @Test
    fun `la tuberia en pies se convierte antes de aplicar el recargo`() {
        val recommendation = AcSpaceRecommender.recommend(
            preferences = imperial,
            area = 200.0,
            details = SpaceDetails(pipeDistance = 32.8),
        )
        assertEquals(9.997, 32.8 * 0.3048, 1e-3)
        assertTrue(recommendation.exactBtu > 18_000.0)
        assertEquals(1, recommendation.factors.size)
    }

    @Test
    fun `un espacio enorme se topa en el escalon de treinta y seis mil`() {
        val recommendation = AcSpaceRecommender.recommend(metric, area = 100.0)
        assertEquals(36_000.0, recommendation.exactBtu, 1e-9)
        assertEquals(36_000, recommendation.commercialBtu)
    }

    @Test
    fun `entre cuarenta y cinco y cincuenta metros sigue en treinta y seis mil`() {
        val recommendation = AcSpaceRecommender.recommend(metric, area = 50.0)
        assertEquals(36_000, recommendation.commercialBtu)
    }
}
