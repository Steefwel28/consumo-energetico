package com.steefwel28.consumoenergetico.ui.components

import com.steefwel28.consumoenergetico.domain.units.TemperatureUnit
import com.steefwel28.consumoenergetico.domain.units.UnitConverter
import java.util.Locale

/** Formato único de cifras para que todas las pantallas hablen igual. */
object AppFormat {

    fun btu(value: Double): String = String.format(Locale.US, "%,.0f BTU/h", value)

    fun btuNumber(value: Double): String = String.format(Locale.US, "%,.0f", value)

    fun kwh(value: Double): String = String.format(Locale.US, "%.1f kWh", value)

    fun kwhNumber(value: Double): String = String.format(Locale.US, "%.1f", value)

    fun balboas(value: Double): String = String.format(Locale.US, "B/. %.2f", value)

    fun decimal(value: Double): String = String.format(Locale.US, "%.1f", value)

    fun temperature(celsius: Int, unit: TemperatureUnit): String =
        UnitConverter.formatTemperature(celsius.toDouble(), unit)

    fun amperes(value: Double): String = String.format(Locale.US, "%.1f A", value)
}
