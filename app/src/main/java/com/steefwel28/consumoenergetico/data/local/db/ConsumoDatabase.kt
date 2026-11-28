package com.steefwel28.consumoenergetico.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.steefwel28.consumoenergetico.core.signature.CraftedBy

/**
 * Base local offline-first: equipos, historial de limpiezas, grupos de
 * propiedades y ubicaciones personalizadas.
 *
 * La versión 2 añade `property_groups` y `custom_locations`, y vincula cada
 * equipo con su grupo y ubicación. La versión 3 guarda las horas de uso diario
 * de cada equipo. La versión 4 añade `bill_records` con el histórico de
 * facturas reales por grupo. La versión 5 añade `nominalWatts` y borra en
 * cascada equipos y facturas cuando se elimina su grupo. Las migraciones
 * siembran "Mi Casa" para que los equipos existentes mantengan compatibilidad
 * sin fricción.
 */
@CraftedBy("steefwel28")
@Database(
    entities = [
        AcUnitEntity::class,
        MaintenanceLogEntity::class,
        PropertyGroupEntity::class,
        CustomLocationEntity::class,
        BillRecordEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
@TypeConverters(ConsumoConverters::class)
abstract class ConsumoDatabase : RoomDatabase() {

    abstract fun acUnitDao(): AcUnitDao

    abstract fun maintenanceLogDao(): MaintenanceLogDao

    abstract fun propertyGroupDao(): PropertyGroupDao

    abstract fun customLocationDao(): CustomLocationDao

    abstract fun billRecordDao(): BillRecordDao

    companion object {
        private const val DATABASE_NAME = "consumo_energetico.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `property_groups` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`nombreGrupo` TEXT NOT NULL, `distribuidora` TEXT NOT NULL, " +
                        "`consumoBaseKwh` REAL NOT NULL)",
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO `property_groups` " +
                        "(`id`, `nombreGrupo`, `distribuidora`, `consumoBaseKwh`) " +
                        "VALUES (1, 'Mi Casa', 'ENSA', 0.0)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `custom_locations` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`nombreUbicacion` TEXT NOT NULL)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "`index_custom_locations_nombreUbicacion` " +
                        "ON `custom_locations` (`nombreUbicacion`)",
                )
                db.execSQL(
                    "ALTER TABLE `ac_units` ADD COLUMN `groupId` INTEGER NOT NULL DEFAULT 1",
                )
                db.execSQL(
                    "ALTER TABLE `ac_units` ADD COLUMN `ubicacion` TEXT NOT NULL DEFAULT 'Sala'",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ac_units_groupId` ON `ac_units` (`groupId`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `ac_units` ADD COLUMN `horasUsoDiario` REAL NOT NULL DEFAULT 8.0",
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `bill_records` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`groupId` INTEGER NOT NULL, `year` INTEGER NOT NULL, " +
                        "`month` INTEGER NOT NULL, `realKwh` REAL NOT NULL, " +
                        "`paidBalboas` REAL NOT NULL, `utility` TEXT NOT NULL, " +
                        "`createdAtMillis` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "`index_bill_records_groupId_year_month` " +
                        "ON `bill_records` (`groupId`, `year`, `month`)",
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // SQLite no permite añadir FKs con ALTER TABLE: se recrea la
                // tabla conservando cada fila y se enlaza el grupo con CASCADE.
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `ac_units_new` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`nombreEspacio` TEXT NOT NULL, `marcaModelo` TEXT NOT NULL, " +
                        "`btu` INTEGER NOT NULL, `nominalWatts` INTEGER, `seer` REAL, " +
                        "`esInverter` INTEGER NOT NULL, `voltaje` INTEGER NOT NULL, " +
                        "`fechaInstalacionMillis` INTEGER NOT NULL, " +
                        "`fechaUltimoMantenimientoMillis` INTEGER NOT NULL, " +
                        "`consumoEstimadoKwhMes` REAL NOT NULL, " +
                        "`horasUsoDiario` REAL NOT NULL DEFAULT 8.0, " +
                        "`groupId` INTEGER NOT NULL DEFAULT 1, " +
                        "`ubicacion` TEXT NOT NULL DEFAULT 'Sala', " +
                        "FOREIGN KEY(`groupId`) REFERENCES `property_groups`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL(
                    "INSERT INTO `ac_units_new` " +
                        "(`id`, `nombreEspacio`, `marcaModelo`, `btu`, `nominalWatts`, `seer`, " +
                        "`esInverter`, `voltaje`, `fechaInstalacionMillis`, " +
                        "`fechaUltimoMantenimientoMillis`, `consumoEstimadoKwhMes`, " +
                        "`horasUsoDiario`, `groupId`, `ubicacion`) " +
                        "SELECT `id`, `nombreEspacio`, `marcaModelo`, `btu`, NULL, `seer`, " +
                        "`esInverter`, `voltaje`, `fechaInstalacionMillis`, " +
                        "`fechaUltimoMantenimientoMillis`, `consumoEstimadoKwhMes`, " +
                        "`horasUsoDiario`, `groupId`, `ubicacion` FROM `ac_units`",
                )
                db.execSQL("DROP TABLE `ac_units`")
                db.execSQL("ALTER TABLE `ac_units_new` RENAME TO `ac_units`")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_ac_units_groupId` ON `ac_units` (`groupId`)",
                )

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `bill_records_new` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`groupId` INTEGER NOT NULL, `year` INTEGER NOT NULL, " +
                        "`month` INTEGER NOT NULL, `realKwh` REAL NOT NULL, " +
                        "`paidBalboas` REAL NOT NULL, `utility` TEXT NOT NULL, " +
                        "`createdAtMillis` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`groupId`) REFERENCES `property_groups`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL(
                    "INSERT INTO `bill_records_new` " +
                        "(`id`, `groupId`, `year`, `month`, `realKwh`, `paidBalboas`, " +
                        "`utility`, `createdAtMillis`) " +
                        "SELECT `id`, `groupId`, `year`, `month`, `realKwh`, `paidBalboas`, " +
                        "`utility`, `createdAtMillis` FROM `bill_records`",
                )
                db.execSQL("DROP TABLE `bill_records`")
                db.execSQL("ALTER TABLE `bill_records_new` RENAME TO `bill_records`")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "`index_bill_records_groupId_year_month` " +
                        "ON `bill_records` (`groupId`, `year`, `month`)",
                )
            }
        }

        @Volatile
        private var instance: ConsumoDatabase? = null

        fun get(context: Context): ConsumoDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ConsumoDatabase::class.java,
                    DATABASE_NAME,
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { instance = it }
            }
    }
}
