package com.steefwel28.consumoenergetico.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.steefwel28.consumoenergetico.core.signature.CraftedBy
import com.steefwel28.consumoenergetico.domain.calculator.PanamaUtility

/**
 * Grupo de propiedades: cada casa, local o negocio con su propia distribuidora
 * y consumo base para calcular su salto de escalón tarifario.
 */
@CraftedBy("steefwel28")
@Entity(tableName = "property_groups")
data class PropertyGroupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombreGrupo: String,
    val distribuidora: PanamaUtility = PanamaUtility.ENSA,
    val consumoBaseKwh: Double = 0.0,
) {
    companion object {
        const val DEFAULT_ID = 1L
        const val DEFAULT_NAME = "Mi Casa"
    }
}
