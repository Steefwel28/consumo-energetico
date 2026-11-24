package com.steefwel28.consumoenergetico.data.datastore

import android.content.Context
import com.steefwel28.consumoenergetico.domain.settings.AppearancePreferences
import com.steefwel28.consumoenergetico.domain.settings.ThemeMode

/**
 * Espejo de [AppearancePreferences] en `SharedPreferences` para leer el tema y
 * Liquid Glass en `onCreate` sin corrutinas ni bloqueo del hilo principal.
 * DataStore sigue siendo la fuente de verdad; esto solo evita el destello.
 */
object AppearanceCache {

    private const val FileName = "appearance_cache"
    private const val ModeKey = "mode"
    private const val OledKey = "oled_pure"
    private const val GlassKey = "liquid_glass"

    fun read(context: Context): AppearancePreferences {
        val stored = context.getSharedPreferences(FileName, Context.MODE_PRIVATE)
        return AppearancePreferences(
            mode = stored.getString(ModeKey, null)
                ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                ?: ThemeMode.SYSTEM,
            oledPure = stored.getBoolean(OledKey, false),
            liquidGlass = stored.getBoolean(GlassKey, false),
        )
    }

    fun write(context: Context, appearance: AppearancePreferences) {
        context.getSharedPreferences(FileName, Context.MODE_PRIVATE)
            .edit()
            .putString(ModeKey, appearance.mode.name)
            .putBoolean(OledKey, appearance.oledPure)
            .putBoolean(GlassKey, appearance.liquidGlass)
            .apply()
    }
}
