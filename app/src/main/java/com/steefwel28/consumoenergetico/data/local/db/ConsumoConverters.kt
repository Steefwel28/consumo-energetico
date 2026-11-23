package com.steefwel28.consumoenergetico.data.local.db

import androidx.room.TypeConverter
import com.steefwel28.consumoenergetico.domain.calculator.PanamaUtility

/** Room guarda la distribuidora por nombre para que el respaldo sea legible. */
class ConsumoConverters {

    @TypeConverter
    fun utilityName(value: PanamaUtility): String = value.name

    @TypeConverter
    fun utility(value: String): PanamaUtility =
        PanamaUtility.entries.firstOrNull { it.name == value } ?: PanamaUtility.ENSA
}
