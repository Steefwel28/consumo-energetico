package com.steefwel28.consumoenergetico.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.steefwel28.consumoenergetico.core.signature.CraftedBy

/** Ubicaciones creadas desde "Otros" para reutilizarlas en próximos equipos. */
@CraftedBy("steefwel28")
@Entity(
    tableName = "custom_locations",
    indices = [Index(value = ["nombreUbicacion"], unique = true)],
)
data class CustomLocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombreUbicacion: String,
)
