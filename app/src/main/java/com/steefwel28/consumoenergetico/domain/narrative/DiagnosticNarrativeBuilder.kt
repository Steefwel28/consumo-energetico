package com.steefwel28.consumoenergetico.domain.narrative

import com.steefwel28.consumoenergetico.core.signature.CraftedBy
import com.steefwel28.consumoenergetico.domain.recommender.AcRecommendation
import com.steefwel28.consumoenergetico.domain.recommender.AcSpaceRecommender
import java.util.Locale

/** Explicación lista para la UI, con alerta opcional y cápsulas educativas. */
@CraftedBy("steefwel28")
data class AcDiagnostic(
    val explanation: String,
    val undersizedAlert: String?,
    val capsules: List<String>,
)

/**
 * Explicación conversacional del cálculo de BTU.
 *
 * También detecta la "Trampa del Aire Pequeño": si el equipo elegido entrega
 * menos del 90 % de los BTU exactos, el compresor deja de modular y trabaja al
 * límite, gastando más y durando menos.
 */
@CraftedBy("steefwel28")
object DiagnosticNarrativeBuilder {

    const val UNDERSIZED_RATIO = 0.9

    fun build(recommendation: AcRecommendation, selectedBtu: Int? = null): AcDiagnostic {
        require(selectedBtu == null || selectedBtu > 0) {
            "La capacidad elegida debe ser mayor que cero BTU/h."
        }

        val factors = recommendation.factors
        val explanation = buildString {
            append("Tu espacio mide ${area(recommendation.areaSquareMeters)} m² en el clima húmedo de Panamá, ")
            append("y el rango comercial de taller lo cubre con un equipo base de ")
            append("${btu(recommendation.baseBtu)} BTU/h. ")
            if (factors.isEmpty()) {
                append("No marcaste detalles del lugar, así que no hay recargos. ")
            } else {
                append("Los detalles del lugar suman ${btu(factors.sumOf { it.addedBtu })} BTU/h: ")
                append(factors.joinToString("; ") { "${it.label} (+${btu(it.addedBtu)} BTU/h)" })
                append(". ")
            }
            append("El cálculo exacto queda en ${btu(recommendation.exactBtu)} BTU/h, ")
            append("y el escalón comercial que te cubre sin quedarte corto es ")
            append("${btu(recommendation.commercialBtu.toDouble())} BTU/h. ")
            when {
                recommendation.exactBtu > recommendation.commercialBtu ->
                    append("Ese cálculo supera los 36,000 BTU/h: conviene dividir el espacio o instalar dos equipos.")

                recommendation.areaSquareMeters > AcSpaceRecommender.MAX_WELL_INSULATED_SQUARE_METERS ->
                    append("Con más de 50 m² ningún split de línea residencial basta: divide el espacio o instala dos equipos.")

                recommendation.areaSquareMeters > AcSpaceRecommender.MAX_SQUARE_METERS ->
                    append("Entre 45 y 50 m² el split de 36,000 BTU/h solo rinde con aislamiento y sombra; si no, divide el espacio.")
            }
        }

        val undersizedAlert = selectedBtu
            ?.takeIf { it < recommendation.exactBtu * UNDERSIZED_RATIO }
            ?.let { selected ->
                "Cuidado con la Trampa del Aire Pequeño: ${btu(selected.toDouble())} BTU/h no cubren " +
                    "los ${btu(recommendation.exactBtu)} BTU/h que pide tu espacio. El equipo se queda corto, " +
                    "el compresor trabaja al 100 % sin modular y se desgasta antes. Aunque el cuarto se sienta " +
                    "frío al principio, la factura y las reparaciones salen más caras que comprar el tamaño correcto."
            }

        return AcDiagnostic(
            explanation = explanation,
            undersizedAlert = undersizedAlert,
            capsules = capsules,
        )
    }

    private val capsules = listOf(
        "Cada grado bajo 24 °C cuesta cerca de 6 % más. Pasar de 24 °C a 18 °C sube el consumo " +
            "alrededor de 36 %; con el ventilador de techo se siente igual y la factura no sube.",
        "En días lluviosos usa Modo Dry: baja la humedad sin forzar el compresor como el modo frío. " +
            "Cierra ventanas y mantén los filtros limpios para que el equipo respire.",
    )

    private fun btu(value: Double): String = String.format(Locale.US, "%,.0f", value)

    private fun area(value: Double): String =
        String.format(Locale.US, "%.1f", value).removeSuffix(".0")
}
