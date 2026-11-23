package com.steefwel28.consumoenergetico.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.steefwel28.consumoenergetico.core.signature.CraftedBy
import kotlinx.coroutines.flow.Flow

/** Acceso reactivo a las ubicaciones personalizadas. */
@CraftedBy("steefwel28")
@Dao
interface CustomLocationDao {

    @Query("SELECT * FROM custom_locations ORDER BY nombreUbicacion COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<CustomLocationEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(location: CustomLocationEntity): Long

    @Query("SELECT * FROM custom_locations")
    suspend fun getAll(): List<CustomLocationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(locations: List<CustomLocationEntity>)

    @Query("DELETE FROM custom_locations")
    suspend fun deleteAll()
}
