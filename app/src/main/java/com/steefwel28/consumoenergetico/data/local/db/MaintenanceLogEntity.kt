package com.steefwel28.consumoenergetico.data.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.steefwel28.consumoenergetico.core.signature.CraftedBy

/** Limpieza registrada en el historial de un equipo; se borra con su equipo. */
@CraftedBy("steefwel28")
@Entity(
    tableName = "maintenance_logs",
    foreignKeys = [
        ForeignKey(
            entity = AcUnitEntity::class,
            parentColumns = ["id"],
            childColumns = ["acUnitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("acUnitId")],
)
data class MaintenanceLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val acUnitId: Long,
    val fechaMillis: Long,
    val nota: String,
)
