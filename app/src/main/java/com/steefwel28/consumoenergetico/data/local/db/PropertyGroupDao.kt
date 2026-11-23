package com.steefwel28.consumoenergetico.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.steefwel28.consumoenergetico.core.signature.CraftedBy
import kotlinx.coroutines.flow.Flow

/** Acceso reactivo a los grupos/propiedades del perfil. */
@CraftedBy("steefwel28")
@Dao
interface PropertyGroupDao {

    @Query("SELECT * FROM property_groups ORDER BY id ASC")
    fun observeAll(): Flow<List<PropertyGroupEntity>>

    @Query("SELECT * FROM property_groups ORDER BY id ASC")
    suspend fun getAll(): List<PropertyGroupEntity>

    @Query("SELECT * FROM property_groups WHERE id = :id")
    suspend fun getById(id: Long): PropertyGroupEntity?

    @Query("SELECT * FROM property_groups WHERE LOWER(nombreGrupo) = LOWER(:name) LIMIT 1")
    suspend fun getByName(name: String): PropertyGroupEntity?

    @Query("SELECT COUNT(*) FROM property_groups")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: PropertyGroupEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(groups: List<PropertyGroupEntity>)

    @Query("DELETE FROM property_groups")
    suspend fun deleteAll()
}
