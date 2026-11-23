package com.steefwel28.consumoenergetico.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.steefwel28.consumoenergetico.core.signature.CraftedBy
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.updateDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "update_preferences",
)

/**
 * Persistencia local de las versiones que el usuario decidió omitir.
 */
@CraftedBy("steefwel28")
class UpdatePreferences internal constructor(
    private val dataStore: DataStore<Preferences>,
) {

    val skippedVersions: Flow<Set<String>> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> preferences[SkippedVersionsKey].orEmpty() }

    suspend fun skipVersion(version: String) {
        dataStore.edit { preferences ->
            preferences[SkippedVersionsKey] = preferences[SkippedVersionsKey].orEmpty() + version
        }
    }

    suspend fun clearSkippedVersions() {
        dataStore.edit { preferences -> preferences.remove(SkippedVersionsKey) }
    }

    companion object {
        private val SkippedVersionsKey = stringSetPreferencesKey("skipped_versions")

        fun create(context: Context): UpdatePreferences =
            UpdatePreferences(context.applicationContext.updateDataStore)
    }
}
