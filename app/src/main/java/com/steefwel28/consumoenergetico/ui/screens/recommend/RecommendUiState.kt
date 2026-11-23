package com.steefwel28.consumoenergetico.ui.screens.recommend

import com.steefwel28.consumoenergetico.domain.narrative.AcDiagnostic
import com.steefwel28.consumoenergetico.domain.narrative.DiagnosticNarrativeBuilder
import com.steefwel28.consumoenergetico.domain.recommender.AcRecommendation
import com.steefwel28.consumoenergetico.domain.recommender.AcSpaceRecommender
import com.steefwel28.consumoenergetico.domain.recommender.CeilingType
import com.steefwel28.consumoenergetico.domain.recommender.ElectricalSpec
import com.steefwel28.consumoenergetico.domain.recommender.ElectricalSpecAdvisor
import com.steefwel28.consumoenergetico.domain.recommender.HeatSource
import com.steefwel28.consumoenergetico.domain.recommender.SpaceDetails
import com.steefwel28.consumoenergetico.domain.recommender.SunExposure
import com.steefwel28.consumoenergetico.domain.units.LengthUnit
import com.steefwel28.consumoenergetico.domain.units.LocaleNumber
import com.steefwel28.consumoenergetico.domain.units.UnitConverter
import com.steefwel28.consumoenergetico.domain.units.UnitPreferences
import kotlin.math.roundToInt

/**
 * Estado de la pantalla Recomendar.
 *
 * Las dimensiones y la tubería se capturan en la unidad activa de Ajustes
 * (`m`/`ft`) y el recomendador las lleva a metros por dentro.
 */
data class RecommendUiState(
    val unitPreferences: UnitPreferences = UnitPreferences(),
    val useDimensions: Boolean = false,
    val areaInput: String = "25",
    val lengthInput: String = "5",
    val widthInput: String = "5",
    val detailsExpanded: Boolean = false,
    val sunExposure: SunExposure = SunExposure.NONE,
    val people: Int = 2,
    val ceiling: CeilingType = CeilingType.BETWEEN_FLOORS,
    val heatSources: Set<HeatSource> = emptySet(),
    val pipeDistanceInput: String = "3",
    val selectedBtuInput: String = "",
) {

    val lengthUnit: LengthUnit get() = unitPreferences.length
    val areaSymbol: String get() = if (lengthUnit == LengthUnit.METERS) "m²" else "ft²"
    val lengthSymbol: String get() = lengthUnit.symbol

    val area: Double? get() = areaInput.toDecimal(1.0..100_000.0)
    val length: Double? get() = lengthInput.toDecimal(0.1..1_000.0)
    val width: Double? get() = widthInput.toDecimal(0.1..1_000.0)
    val pipeDistance: Double get() = pipeDistanceInput.toDecimal(0.0..1_000.0) ?: 0.0
    val pipeDistanceMeters: Double
        get() = UnitConverter.lengthToCanonical(pipeDistance, lengthUnit)

    val selectedBtu: Int?
        get() = selectedBtuInput.toDecimal(1.0..60_000.0)?.roundToInt()

    private val details: SpaceDetails
        get() = SpaceDetails(
            sunExposure = sunExposure,
            people = people.coerceAtLeast(0),
            ceiling = ceiling,
            heatSources = heatSources,
            pipeDistance = pipeDistance,
        )

    val recommendation: AcRecommendation?
        get() = runCatching {
            if (useDimensions) {
                val roomLength = length ?: return null
                val roomWidth = width ?: return null
                AcSpaceRecommender.recommend(unitPreferences, roomLength, roomWidth, details)
            } else {
                val roomArea = area ?: return null
                AcSpaceRecommender.recommend(unitPreferences, roomArea, details)
            }
        }.getOrNull()

    val diagnostic: AcDiagnostic?
        get() = recommendation?.let { DiagnosticNarrativeBuilder.build(it, selectedBtu) }

    val electrical: ElectricalSpec?
        get() {
            val recommendation = recommendation ?: return null
            val btu = selectedBtu ?: recommendation.commercialBtu
            return runCatching {
                ElectricalSpecAdvisor.advise(btuPerHour = btu, pipeDistanceMeters = pipeDistanceMeters)
            }.getOrNull()
        }
}

private fun String.toDecimal(range: ClosedFloatingPointRange<Double>): Double? =
    LocaleNumber.parse(this)?.takeIf { it in range }
