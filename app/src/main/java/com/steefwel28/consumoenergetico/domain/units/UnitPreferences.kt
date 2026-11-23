package com.steefwel28.consumoenergetico.domain.units

import com.steefwel28.consumoenergetico.core.signature.CraftedBy

/** Unidad de longitud para distancias mayores (tubería, habitación). */
enum class LengthUnit(val symbol: String) {
    METERS("m"),
    FEET("ft"),
}

/** Unidad de longitud corta (espesores, separaciones). */
enum class ShortLengthUnit(val symbol: String) {
    CENTIMETERS("cm"),
    INCHES("in"),
}

/** Unidad de volumen de aire o refrigerante. */
enum class VolumeUnit(val symbol: String) {
    CUBIC_FEET("p³"),
    LITERS("L"),
}

/** Unidad de temperatura del Simulador de Control Remoto. */
enum class TemperatureUnit(val symbol: String) {
    CELSIUS("°C"),
    FAHRENHEIT("°F"),
}

/**
 * Preferencias globales de unidades configuradas en Ajustes.
 *
 * Los motores de cálculo siempre trabajan en SI (`m`, `cm`, `L`, `°C`);
 * estas preferencias solo deciden cómo se captura y se muestra el dato.
 */
@CraftedBy("steefwel28")
data class UnitPreferences(
    val length: LengthUnit = LengthUnit.METERS,
    val shortLength: ShortLengthUnit = ShortLengthUnit.CENTIMETERS,
    val volume: VolumeUnit = VolumeUnit.LITERS,
    val temperature: TemperatureUnit = TemperatureUnit.CELSIUS,
)
