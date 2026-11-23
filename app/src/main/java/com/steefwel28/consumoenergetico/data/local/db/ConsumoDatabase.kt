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
 * equipo con su grupo y ubicación. La migración siembra "Mi Casa" para que los
 * equipos existentes mantengan compatibilidad sin fricción.
 */
@CraftedBy("steefwel28")
@Database(
    entities = [
        AcUnitEntity::class,
        MaintenanceLogEntity::class,
        PropertyGroupEntity::class,
        CustomLocationEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(ConsumoConverters::class)
abstract class ConsumoDatabase : RoomDatabase() {

    abstract fun acUnitDao(): AcUnitDao

    abstract fun maintenanceLogDao(): MaintenanceLogDao

    abstract fun propertyGroupDao(): PropertyGroupDao

    abstract fun customLocationDao(): CustomLocationDao

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

        @Volatile
        private var instance: ConsumoDatabase? = null

        fun get(context: Context): ConsumoDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ConsumoDatabase::class.java,
                    DATABASE_NAME,
                ).addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
