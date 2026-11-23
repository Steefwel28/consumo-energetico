package com.steefwel28.consumoenergetico.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.steefwel28.consumoenergetico.core.signature.CraftedBy

/** Base local offline-first: equipos e historial de limpiezas. */
@CraftedBy("steefwel28")
@Database(
    entities = [AcUnitEntity::class, MaintenanceLogEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class ConsumoDatabase : RoomDatabase() {

    abstract fun acUnitDao(): AcUnitDao

    abstract fun maintenanceLogDao(): MaintenanceLogDao

    companion object {
        private const val DATABASE_NAME = "consumo_energetico.db"

        @Volatile
        private var instance: ConsumoDatabase? = null

        fun get(context: Context): ConsumoDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ConsumoDatabase::class.java,
                    DATABASE_NAME,
                ).build().also { instance = it }
            }
    }
}
