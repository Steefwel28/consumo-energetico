package com.steefwel28.consumoenergetico.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.steefwel28.consumoenergetico.core.signature.CraftedBy

/** Equipo registrado en un grupo del perfil (Mi Casa, Trabajo, etc.). */
@CraftedBy("steefwel28")
@Entity(
    tableName = "ac_units",
    foreignKeys = [
        ForeignKey(
            entity = PropertyGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("groupId")],
)
data class AcUnitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombreEspacio: String,
    val marcaModelo: String,
    val btu: Int,
    /** Potencia real de placa; nula cuando solo hay estimación por SEER. */
    val nominalWatts: Int? = null,
    val seer: Double?,
    val esInverter: Boolean,
    val voltaje: Int,
    val fechaInstalacionMillis: Long,
    val fechaUltimoMantenimientoMillis: Long,
    val consumoEstimadoKwhMes: Double,
    /** Horas de uso diario que el usuario configuró al guardar el equipo. */
    @ColumnInfo(defaultValue = "8.0")
    val horasUsoDiario: Double = 8.0,
    /** Grupo/propiedad al que pertenece; por defecto "Mi Casa". */
    @ColumnInfo(defaultValue = "1")
    val groupId: Long = 1,
    @ColumnInfo(defaultValue = "'Sala'")
    val ubicacion: String = "Sala",
)
