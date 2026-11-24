package com.steefwel28.consumoenergetico.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.steefwel28.consumoenergetico.core.signature.CraftedBy
import kotlinx.coroutines.flow.Flow

/** Acceso reactivo al historial de facturas reales. */
@CraftedBy("steefwel28")
@Dao
interface BillRecordDao {

    @Query("SELECT * FROM bill_records ORDER BY year ASC, month ASC")
    fun observeAll(): Flow<List<BillRecordEntity>>

    @Query("SELECT * FROM bill_records")
    suspend fun getAll(): List<BillRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: BillRecordEntity)

    @Query("DELETE FROM bill_records WHERE id = :id")
    suspend fun delete(id: Long)
}
