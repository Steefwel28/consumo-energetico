package com.steefwel28.consumoenergetico.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.steefwel28.consumoenergetico.core.signature.CraftedBy

/** Equipo registrado en Mi Casa / Mi Local. */
@CraftedBy("steefwel28")
@Entity(tableName = "ac_units")
data class AcUnitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombreEspacio: String,
    val marcaModelo: String,
    val btu: Int,
    val seer: Double?,
    val esInverter: Boolean,
    val voltaje: Int,
    val fechaInstalacionMillis: Long,
    val fechaUltimoMantenimientoMillis: Long,
    val consumoEstimadoKwhMes: Double,
)
