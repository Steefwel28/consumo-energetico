package com.steefwel28.consumoenergetico.core.di

import android.content.Context
import com.steefwel28.consumoenergetico.core.signature.CraftedBy
import com.steefwel28.consumoenergetico.core.updater.GitHubReleaseChecker
import com.steefwel28.consumoenergetico.data.backup.LocalBackupManager
import com.steefwel28.consumoenergetico.data.datastore.UpdatePreferences
import com.steefwel28.consumoenergetico.data.datastore.UserPreferencesRepository
import com.steefwel28.consumoenergetico.data.local.db.ConsumoDatabase

/**
 * Contenedor único de dependencias de la app.
 *
 * Todo se crea de forma perezosa y vive mientras viva el proceso: una sola
 * base Room, un solo DataStore y un verificador compartido que la navegación
 * observa para el aviso de actualización.
 */
@CraftedBy("steefwel28")
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val database: ConsumoDatabase by lazy { ConsumoDatabase.get(appContext) }

    val preferences: UserPreferencesRepository by lazy {
        UserPreferencesRepository.create(appContext)
    }

    val updatePreferences: UpdatePreferences by lazy { UpdatePreferences.create(appContext) }

    val releaseChecker: GitHubReleaseChecker by lazy { GitHubReleaseChecker(updatePreferences) }

    val backupManager: LocalBackupManager by lazy {
        LocalBackupManager(database, preferences)
    }
}
