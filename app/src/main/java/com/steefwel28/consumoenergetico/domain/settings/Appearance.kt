package com.steefwel28.consumoenergetico.domain.settings

import com.steefwel28.consumoenergetico.core.signature.CraftedBy

/** Cómo resuelve el tema la app; se guarda en disco para arrancar sin destello. */
enum class ThemeMode {
    DAY,
    NIGHT,
    SYSTEM,
}

/**
 * Apariencia global persistida: tema, negro puro OLED y la atmósfera
 * Liquid Glass. Vive en `domain` para que DataStore y la UI compartan tipos
 * sin depender de Android.
 */
@CraftedBy("steefwel28")
data class AppearancePreferences(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val oledPure: Boolean = false,
    val liquidGlass: Boolean = false,
)
