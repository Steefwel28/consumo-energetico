package com.steefwel28.consumoenergetico.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.steefwel28.consumoenergetico.core.signature.CraftedBy
import com.steefwel28.consumoenergetico.domain.calculator.PanamaUtility

/**
 * Factura real registrada por el usuario para un grupo/propiedad.
 *
 * Guarda el consumo del recibo ([realKwh]) y lo pagado ([paidBalboas]) para
 * comparar el histórico contra la estimación de la app y calcular la tarifa
 * efectiva mes a mes.
 */
@CraftedBy("steefwel28")
@Entity(
    tableName = "bill_records",
    indices = [Index(value = ["groupId", "year", "month"], unique = true)],
)
data class BillRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val groupId: Long = PropertyGroupEntity.DEFAULT_ID,
    val year: Int,
    val month: Int,
    val realKwh: Double,
    val paidBalboas: Double,
    val utility: PanamaUtility = PanamaUtility.ENSA,
    val createdAtMillis: Long = System.currentTimeMillis(),
) {

    /** Tarifa efectiva del recibo, con toda su precisión. */
    val effectiveBalboasPerKwh: Double?
        get() = paidBalboas.takeIf { realKwh > 0.0 }?.div(realKwh)
}
