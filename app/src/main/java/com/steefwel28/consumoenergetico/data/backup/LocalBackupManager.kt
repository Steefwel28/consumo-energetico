package com.steefwel28.consumoenergetico.data.backup

import androidx.room.withTransaction
import com.steefwel28.consumoenergetico.core.signature.CraftedBy
import com.steefwel28.consumoenergetico.data.datastore.UserPreferencesRepository
import com.steefwel28.consumoenergetico.data.local.db.ConsumoDatabase
import java.io.File
import java.io.IOException
import kotlinx.coroutines.flow.first

/** Resultado de una importación; nunca deja la base a medias. */
sealed interface BackupRestoreResult {
    data class Success(val snapshot: BackupSnapshot) : BackupRestoreResult

    data class Invalid(val reason: String) : BackupRestoreResult
}

/**
 * Exporta e importa la base completa más las preferencias.
 *
 * La importación valida el archivo entero con [BackupCodec] antes de abrir
 * la transacción; si algo está corrupto, Room no se toca.
 */
@CraftedBy("steefwel28")
class LocalBackupManager(
    private val database: ConsumoDatabase,
    private val preferences: UserPreferencesRepository,
) {

    suspend fun exportTo(file: File) {
        val snapshot = BackupSnapshot(
            exportedAtMillis = System.currentTimeMillis(),
            unitPreferences = preferences.unitPreferences.first(),
            utility = preferences.utility.first(),
            baseMonthlyKwh = preferences.baseMonthlyKwh.first(),
            units = database.acUnitDao().getAll(),
            logs = database.maintenanceLogDao().getAll(),
        )
        file.parentFile?.mkdirs()
        file.writeText(BackupCodec.encode(snapshot))
    }

    suspend fun importFrom(file: File): BackupRestoreResult {
        val snapshot = try {
            BackupCodec.decode(file.readText())
        } catch (error: BackupFormatException) {
            return BackupRestoreResult.Invalid(error.message ?: "El respaldo no es válido.")
        } catch (error: IOException) {
            return BackupRestoreResult.Invalid("No se pudo leer el archivo: ${error.message}")
        }

        database.withTransaction {
            database.maintenanceLogDao().deleteAll()
            database.acUnitDao().deleteAll()
            database.acUnitDao().insertAll(snapshot.units)
            database.maintenanceLogDao().insertAll(snapshot.logs)
        }
        preferences.updateUnitPreferences(snapshot.unitPreferences)
        preferences.setUtility(snapshot.utility)
        preferences.setBaseMonthlyKwh(snapshot.baseMonthlyKwh)
        return BackupRestoreResult.Success(snapshot)
    }
}
