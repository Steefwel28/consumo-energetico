package com.steefwel28.consumoenergetico.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.steefwel28.consumoenergetico.core.signature.CraftedBy
import kotlinx.coroutines.flow.Flow

/** Historial cronológico de limpiezas. */
@CraftedBy("steefwel28")
@Dao
interface MaintenanceLogDao {

    @Query("SELECT * FROM maintenance_logs WHERE acUnitId = :acUnitId ORDER BY fechaMillis DESC")
    fun observeHistory(acUnitId: Long): Flow<List<MaintenanceLogEntity>>

    @Query("SELECT * FROM maintenance_logs ORDER BY fechaMillis DESC")
    suspend fun getAll(): List<MaintenanceLogEntity>

    @Query("SELECT * FROM maintenance_logs ORDER BY fechaMillis DESC")
    fun observeAll(): Flow<List<MaintenanceLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: MaintenanceLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<MaintenanceLogEntity>)

    @Query("DELETE FROM maintenance_logs")
    suspend fun deleteAll()
}
