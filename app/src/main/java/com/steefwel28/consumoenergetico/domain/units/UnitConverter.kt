package com.steefwel28.consumoenergetico.domain.units

import com.steefwel28.consumoenergetico.core.signature.CraftedBy
import java.util.Locale

/**
 * Conversiones exactas y formateo para la UI, sin dependencias de Android.
 *
 * Las constantes son exactas por definición internacional:
 * `1 ft = 0.3048 m`, `1 in = 2.54 cm` y `1 p³ = 28.316846592 L`.
 */
@CraftedBy("steefwel28")
object UnitConverter {

    const val METERS_PER_FOOT = 0.3048
    const val CENTIMETERS_PER_INCH = 2.54
    const val LITERS_PER_CUBIC_FOOT = 28.316846592

    fun lengthToDisplay(meters: Double, unit: LengthUnit): Double = when (unit) {
        LengthUnit.METERS -> meters
        LengthUnit.FEET -> meters / METERS_PER_FOOT
    }

    fun lengthToCanonical(value: Double, unit: LengthUnit): Double = when (unit) {
        LengthUnit.METERS -> value
        LengthUnit.FEET -> value * METERS_PER_FOOT
    }

    fun shortLengthToDisplay(centimeters: Double, unit: ShortLengthUnit): Double = when (unit) {
        ShortLengthUnit.CENTIMETERS -> centimeters
        ShortLengthUnit.INCHES -> centimeters / CENTIMETERS_PER_INCH
    }

    fun shortLengthToCanonical(value: Double, unit: ShortLengthUnit): Double = when (unit) {
        ShortLengthUnit.CENTIMETERS -> value
        ShortLengthUnit.INCHES -> value * CENTIMETERS_PER_INCH
    }

    fun volumeToDisplay(liters: Double, unit: VolumeUnit): Double = when (unit) {
        VolumeUnit.LITERS -> liters
        VolumeUnit.CUBIC_FEET -> liters / LITERS_PER_CUBIC_FOOT
    }

    fun volumeToCanonical(value: Double, unit: VolumeUnit): Double = when (unit) {
        VolumeUnit.LITERS -> value
        VolumeUnit.CUBIC_FEET -> value * LITERS_PER_CUBIC_FOOT
    }

    fun temperatureToDisplay(celsius: Double, unit: TemperatureUnit): Double = when (unit) {
        TemperatureUnit.CELSIUS -> celsius
        TemperatureUnit.FAHRENHEIT -> celsius * 9.0 / 5.0 + 32.0
    }

    fun temperatureToCanonical(value: Double, unit: TemperatureUnit): Double = when (unit) {
        TemperatureUnit.CELSIUS -> value
        TemperatureUnit.FAHRENHEIT -> (value - 32.0) * 5.0 / 9.0
    }

    fun formatLength(meters: Double, unit: LengthUnit): String =
        "${decimal(lengthToDisplay(meters, unit), 2)} ${unit.symbol}"

    fun formatShortLength(centimeters: Double, unit: ShortLengthUnit): String =
        "${decimal(shortLengthToDisplay(centimeters, unit), 1)} ${unit.symbol}"

    fun formatVolume(liters: Double, unit: VolumeUnit): String =
        "${decimal(volumeToDisplay(liters, unit), 2)} ${unit.symbol}"

    fun formatTemperature(celsius: Double, unit: TemperatureUnit): String =
        "${decimal(temperatureToDisplay(celsius, unit), 1)} ${unit.symbol}"

    /** Redondea y recorta ceros sobrantes; los valores no finitos no llegan a la UI. */
    private fun decimal(value: Double, decimals: Int): String {
        if (!value.isFinite()) return "—"
        val rounded = String.format(Locale.US, "%.${decimals}f", value)
        val trimmed = if (rounded.contains('.')) rounded.trimEnd('0').trimEnd('.') else rounded
        return if (trimmed == "-0") "0" else trimmed
    }
}
