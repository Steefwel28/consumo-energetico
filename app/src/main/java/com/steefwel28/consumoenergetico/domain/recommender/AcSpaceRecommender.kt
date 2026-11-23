package com.steefwel28.consumoenergetico.domain.recommender

import com.steefwel28.consumoenergetico.core.signature.CraftedBy
import com.steefwel28.consumoenergetico.domain.units.LengthUnit
import com.steefwel28.consumoenergetico.domain.units.UnitConverter
import com.steefwel28.consumoenergetico.domain.units.UnitPreferences

/** Exposición solar del espacio; el sol de tarde panameño es el peor caso. */
enum class SunExposure(val label: String, val multiplier: Double) {
    NONE("Sin ventanas grandes", 0.0),
    CURTAIN("Ventanal con cortina", 0.10),
    DIRECT_AFTERNOON_SUN("Ventanal con sol directo de tarde", 0.22),
}

/** Techo del espacio; la losa al sol y el zinc castigan la carga térmica. */
enum class CeilingType(val label: String, val multiplier: Double) {
    BETWEEN_FLOORS("Entre pisos", 0.0),
    TOP_FLOOR_SUN("Último piso losa al sol", 0.15),
    ZINC_OR_HIGH_CEILING("Zinc o cielo raso alto", 0.18),
}

/** Fuentes de calor que el usuario marca en "Más detalles del lugar". */
enum class HeatSource(val label: String, val btuPerHour: Double) {
    GAMING_PC("PC Gamer/Servidores", 400.0),
    OPEN_KITCHEN("Cocina abierta", 700.0),
    DISPLAYS("Vitrinas/Iluminación", 300.0),
}

/**
 * Detalles opcionales del lugar. [pipeDistance] llega en la unidad de
 * longitud activa en [UnitPreferences] (`m` o `ft`).
 */
@CraftedBy("steefwel28")
data class SpaceDetails(
    val sunExposure: SunExposure = SunExposure.NONE,
    val people: Int = 2,
    val ceiling: CeilingType = CeilingType.BETWEEN_FLOORS,
    val heatSources: Set<HeatSource> = emptySet(),
    val pipeDistance: Double = 0.0,
)

/** Impacto real de un factor sobre el total, listo para la explicación. */
@CraftedBy("steefwel28")
data class BtuFactor(
    val label: String,
    val addedBtu: Double,
)

/** Resultado del recomendador: cálculo exacto y escalón comercial. */
@CraftedBy("steefwel28")
data class AcRecommendation(
    val areaSquareMeters: Double,
    val baseBtu: Double,
    val exactBtu: Double,
    val commercialBtu: Int,
    val factors: List<BtuFactor>,
)

/**
 * Recomendador de capacidad para aires acondicionados en Panamá.
 *
 * Base tropicalizada: 680 BTU/h por metro cuadrado. Los recargos porcentuales
 * (sol, techo, tubería) se aplican en cascada sobre el subtotal acumulado, de
 * modo que cada [BtuFactor] refleja el BTU real que sumó. Si el cálculo excede
 * 36,000 BTU/h, [AcRecommendation.commercialBtu] se topa en ese escalón y la
 * explicación sugiere dividir el espacio.
 */
@CraftedBy("steefwel28")
object AcSpaceRecommender {

    const val BTU_PER_SQUARE_METER = 680.0
    const val BTU_PER_EXTRA_PERSON = 550.0
    const val EXTRA_PEOPLE_FREE = 2
    const val PIPE_THERMAL_THRESHOLD_METERS = 5.0
    const val PIPE_PENALTY_PER_METER = 0.02
    const val PIPE_PENALTY_CAP = 0.15

    val commercialSteps = listOf(9_000, 12_000, 18_000, 24_000, 36_000)

    private const val SQUARE_METERS_PER_SQUARE_FOOT = 0.09290304

    /** [area] en m² o ft² según la preferencia de longitud. */
    fun recommend(
        preferences: UnitPreferences,
        area: Double,
        details: SpaceDetails = SpaceDetails(),
    ): AcRecommendation {
        require(area > 0.0) { "El área debe ser mayor que cero." }
        val squareMeters = when (preferences.length) {
            LengthUnit.METERS -> area
            LengthUnit.FEET -> area * SQUARE_METERS_PER_SQUARE_FOOT
        }
        return build(squareMeters, details, pipeMeters(preferences, details))
    }

    /** [length] y [width] en la unidad activa de [preferences]. */
    fun recommend(
        preferences: UnitPreferences,
        length: Double,
        width: Double,
        details: SpaceDetails = SpaceDetails(),
    ): AcRecommendation {
        require(length > 0.0 && width > 0.0) { "El largo y el ancho deben ser mayores que cero." }
        val meters = UnitConverter.lengthToCanonical(length, preferences.length)
        val metersAcross = UnitConverter.lengthToCanonical(width, preferences.length)
        return build(meters * metersAcross, details, pipeMeters(preferences, details))
    }

    private fun pipeMeters(preferences: UnitPreferences, details: SpaceDetails): Double {
        require(details.people >= 0) { "Las personas no pueden ser negativas." }
        require(details.pipeDistance >= 0.0) { "La tubería no puede ser negativa." }
        return UnitConverter.lengthToCanonical(details.pipeDistance, preferences.length)
    }

    private fun build(
        squareMeters: Double,
        details: SpaceDetails,
        pipeMeters: Double,
    ): AcRecommendation {
        val base = squareMeters * BTU_PER_SQUARE_METER
        val factors = mutableListOf<BtuFactor>()
        var total = base

        val extraPeople = (details.people - EXTRA_PEOPLE_FREE).coerceAtLeast(0)
        if (extraPeople > 0) {
            val added = extraPeople * BTU_PER_EXTRA_PERSON
            factors += BtuFactor("${details.people} personas habituales", added)
            total += added
        }

        details.heatSources.sortedBy { it.ordinal }.forEach { source ->
            factors += BtuFactor(source.label, source.btuPerHour)
            total += source.btuPerHour
        }

        fun applyPercent(multiplier: Double, label: String) {
            if (multiplier <= 0.0) return
            val added = total * multiplier
            factors += BtuFactor(label, added)
            total += added
        }

        applyPercent(details.sunExposure.multiplier, details.sunExposure.label)
        applyPercent(details.ceiling.multiplier, details.ceiling.label)
        if (pipeMeters > PIPE_THERMAL_THRESHOLD_METERS) {
            val penalty = minOf(
                (pipeMeters - PIPE_THERMAL_THRESHOLD_METERS) * PIPE_PENALTY_PER_METER,
                PIPE_PENALTY_CAP,
            )
            val label = "Tubería larga (${UnitConverter.formatLength(pipeMeters, LengthUnit.METERS)})"
            applyPercent(penalty, label)
        }

        val commercial = commercialSteps.firstOrNull { it >= total } ?: commercialSteps.last()

        return AcRecommendation(
            areaSquareMeters = squareMeters,
            baseBtu = base,
            exactBtu = total,
            commercialBtu = commercial,
            factors = factors,
        )
    }
}
