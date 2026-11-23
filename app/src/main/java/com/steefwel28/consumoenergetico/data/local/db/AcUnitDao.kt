package com.steefwel28.consumoenergetico.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.steefwel28.consumoenergetico.core.signature.CraftedBy
import kotlinx.coroutines.flow.Flow

/** Acceso reactivo a los equipos registrados. */
@CraftedBy("steefwel28")
@Dao
interface AcUnitDao {

    @Query("SELECT * FROM ac_units ORDER BY nombreEspacio COLLATE NOCASE ASC")
    fun observeUnits(): Flow<List<AcUnitEntity>>

    /** Suma del consumo estimado de todos los equipos del hogar o local. */
    @Query("SELECT COALESCE(SUM(consumoEstimadoKwhMes), 0) FROM ac_units")
    fun observeTotalMonthlyKwh(): Flow<Double>

    @Query("SELECT * FROM ac_units")
    suspend fun getAll(): List<AcUnitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(unit: AcUnitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(units: List<AcUnitEntity>)

    @Query("DELETE FROM ac_units")
    suspend fun deleteAll()
}
