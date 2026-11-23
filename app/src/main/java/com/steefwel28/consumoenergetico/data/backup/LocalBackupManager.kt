package com.steefwel28.consumoenergetico.data.backup

import androidx.room.withTransaction
import com.steefwel28.consumoenergetico.core.signature.CraftedBy
import com.steefwel28.consumoenergetico.data.datastore.UserPreferencesRepository
import com.steefwel28.consumoenergetico.data.local.db.ConsumoDatabase
import com.steefwel28.consumoenergetico.data.local.db.PropertyGroupEntity
import com.steefwel28.consumoenergetico.domain.calculator.PanamaUtility
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
 * la transacción; si algo está corrupto, Room no se toca. El esquema 2
 * incluye grupos de propiedades y ubicaciones personalizadas.
 */
@CraftedBy("steefwel28")
class LocalBackupManager(
    private val database: ConsumoDatabase,
    private val preferences: UserPreferencesRepository,
) {

    suspend fun exportTo(file: File) {
        file.parentFile?.mkdirs()
        file.writeText(exportJson())
    }

    suspend fun importFrom(file: File): BackupRestoreResult = try {
        importJson(file.readText())
    } catch (error: IOException) {
        BackupRestoreResult.Invalid("No se pudo leer el archivo: ${error.message}")
    }

    /** Texto completo del respaldo, listo para el selector SAF. */
    suspend fun exportJson(): String {
        val groups = database.propertyGroupDao().getAll().ifEmpty {
            val default = PropertyGroupEntity(
                id = PropertyGroupEntity.DEFAULT_ID,
                nombreGrupo = PropertyGroupEntity.DEFAULT_NAME,
                distribuidora = PanamaUtility.ENSA,
                consumoBaseKwh = 0.0,
            )
            database.propertyGroupDao().insert(default)
            listOf(default)
        }
        val snapshot = BackupSnapshot(
            exportedAtMillis = System.currentTimeMillis(),
            unitPreferences = preferences.unitPreferences.first(),
            utility = preferences.utility.first(),
            baseMonthlyKwh = preferences.baseMonthlyKwh.first(),
            units = database.acUnitDao().getAll(),
            logs = database.maintenanceLogDao().getAll(),
            groups = groups,
            locations = database.customLocationDao().getAll(),
        )
        return BackupCodec.encode(snapshot)
    }

    suspend fun importJson(json: String): BackupRestoreResult {
        val snapshot = try {
            BackupCodec.decode(json)
        } catch (error: BackupFormatException) {
            return BackupRestoreResult.Invalid(error.message ?: "El respaldo no es válido.")
        }

        database.withTransaction {
            database.maintenanceLogDao().deleteAll()
            database.acUnitDao().deleteAll()
            database.propertyGroupDao().deleteAll()
            database.customLocationDao().deleteAll()
            database.propertyGroupDao().insertAll(snapshot.groups)
            database.customLocationDao().insertAll(snapshot.locations)
            database.acUnitDao().insertAll(snapshot.units)
            database.maintenanceLogDao().insertAll(snapshot.logs)
        }
        preferences.updateUnitPreferences(snapshot.unitPreferences)
        preferences.setUtility(snapshot.utility)
        preferences.setBaseMonthlyKwh(snapshot.baseMonthlyKwh)
        return BackupRestoreResult.Success(snapshot)
    }
}
