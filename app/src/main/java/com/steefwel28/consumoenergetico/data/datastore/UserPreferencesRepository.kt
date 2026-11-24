package com.steefwel28.consumoenergetico.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.steefwel28.consumoenergetico.core.signature.CraftedBy
import com.steefwel28.consumoenergetico.domain.calculator.PanamaUtility
import com.steefwel28.consumoenergetico.domain.settings.AppearancePreferences
import com.steefwel28.consumoenergetico.domain.settings.ThemeMode
import com.steefwel28.consumoenergetico.domain.units.LengthUnit
import com.steefwel28.consumoenergetico.domain.units.ShortLengthUnit
import com.steefwel28.consumoenergetico.domain.units.TemperatureUnit
import com.steefwel28.consumoenergetico.domain.units.UnitPreferences
import com.steefwel28.consumoenergetico.domain.units.VolumeUnit
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences",
)

/** Ajustes globales del usuario persistidos de forma reactiva. */
@CraftedBy("steefwel28")
class UserPreferencesRepository internal constructor(
    private val dataStore: DataStore<Preferences>,
) {

    private val preferences: Flow<Preferences> = dataStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    val unitPreferences: Flow<UnitPreferences> = preferences.map { stored ->
        UnitPreferences(
            length = stored[LengthKey].toEnumOrDefault(LengthUnit.METERS),
            shortLength = stored[ShortLengthKey].toEnumOrDefault(ShortLengthUnit.CENTIMETERS),
            volume = stored[VolumeKey].toEnumOrDefault(VolumeUnit.LITERS),
            temperature = stored[TemperatureKey].toEnumOrDefault(TemperatureUnit.CELSIUS),
        )
    }

    val utility: Flow<PanamaUtility> = preferences.map { stored ->
        stored[UtilityKey].toEnumOrDefault(PanamaUtility.ENSA)
    }

    val baseMonthlyKwh: Flow<Double> = preferences.map { stored -> stored[BaseMonthlyKwhKey] ?: 0.0 }

    /** Apariencia completa; el primer valor se lee al arrancar para evitar el flash blanco. */
    val appearance: Flow<AppearancePreferences> = preferences.map { stored ->
        AppearancePreferences(
            mode = stored[ThemeModeKey].toEnumOrDefault(ThemeMode.SYSTEM),
            oledPure = stored[OledPureKey] ?: false,
            liquidGlass = stored[LiquidGlassKey] ?: false,
        )
    }

    suspend fun updateUnitPreferences(preferences: UnitPreferences) {
        dataStore.edit { stored ->
            stored[LengthKey] = preferences.length.name
            stored[ShortLengthKey] = preferences.shortLength.name
            stored[VolumeKey] = preferences.volume.name
            stored[TemperatureKey] = preferences.temperature.name
        }
    }

    suspend fun setUtility(utility: PanamaUtility) {
        dataStore.edit { stored -> stored[UtilityKey] = utility.name }
    }

    suspend fun setBaseMonthlyKwh(kwh: Double) {
        require(kwh >= 0.0) { "El consumo base no puede ser negativo." }
        dataStore.edit { stored -> stored[BaseMonthlyKwhKey] = kwh }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { stored -> stored[ThemeModeKey] = mode.name }
    }

    suspend fun setOledPure(enabled: Boolean) {
        dataStore.edit { stored -> stored[OledPureKey] = enabled }
    }

    suspend fun setLiquidGlass(enabled: Boolean) {
        dataStore.edit { stored -> stored[LiquidGlassKey] = enabled }
    }

    companion object {
        private val LengthKey = stringPreferencesKey("length_unit")
        private val ShortLengthKey = stringPreferencesKey("short_length_unit")
        private val VolumeKey = stringPreferencesKey("volume_unit")
        private val TemperatureKey = stringPreferencesKey("temperature_unit")
        private val UtilityKey = stringPreferencesKey("utility")
        private val BaseMonthlyKwhKey = doublePreferencesKey("base_monthly_kwh")
        private val ThemeModeKey = stringPreferencesKey("theme_mode")
        private val OledPureKey = booleanPreferencesKey("oled_pure")
        private val LiquidGlassKey = booleanPreferencesKey("liquid_glass")

        fun create(context: Context): UserPreferencesRepository =
            UserPreferencesRepository(context.applicationContext.userPreferencesDataStore)
    }
}

private inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T =
    enumValues<T>().firstOrNull { it.name == this } ?: default
