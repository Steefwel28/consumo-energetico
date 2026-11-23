package com.steefwel28.consumoenergetico.data.worker

import com.steefwel28.consumoenergetico.core.signature.CraftedBy
import com.steefwel28.consumoenergetico.data.local.db.AcUnitEntity

/** Equipo cuyo mantenimiento ya venció o vence dentro de la ventana. */
@CraftedBy("steefwel28")
data class MaintenanceDue(
    val unitId: Long,
    val spaceName: String,
    val dueAtMillis: Long,
)

/**
 * Decide qué equipos entran en la notificación consolidada.
 *
 * La fecha base es el último mantenimiento o, si nunca se limpió, la
 * instalación. Un equipo entra si su vencimiento (90 días) ya pasó o cae
 * dentro de los próximos 21 días.
 */
@CraftedBy("steefwel28")
object MaintenanceReminderPlanner {

    const val INTERVAL_DAYS = 90L
    const val WINDOW_DAYS = 21L
    const val DAY_MILLIS = 86_400_000L

    fun dueUnits(
        units: List<AcUnitEntity>,
        nowMillis: Long,
        windowDays: Long = WINDOW_DAYS,
    ): List<MaintenanceDue> {
        require(windowDays >= 0) { "La ventana de aviso no puede ser negativa." }
        val limit = nowMillis + windowDays * DAY_MILLIS

        return units.mapNotNull { unit ->
            val dueAt = nextDueAtMillis(unit)
            if (dueAt <= limit) {
                MaintenanceDue(unitId = unit.id, spaceName = unit.nombreEspacio, dueAtMillis = dueAt)
            } else {
                null
            }
        }.sortedWith(compareBy({ it.dueAtMillis }, { it.spaceName }))
    }

    /** Fecha de la próxima limpieza: 90 días desde la instalación o la última limpieza. */
    fun nextDueAtMillis(unit: AcUnitEntity): Long {
        val anchor = maxOf(unit.fechaInstalacionMillis, unit.fechaUltimoMantenimientoMillis)
        return anchor + INTERVAL_DAYS * DAY_MILLIS
    }

    /**
     * Días completos hasta el próximo mantenimiento; negativo cuando ya venció.
     * Sirve para el indicador de Mi Casa, no para decidir la notificación.
     */
    fun daysUntilDue(unit: AcUnitEntity, nowMillis: Long): Long {
        val difference = nextDueAtMillis(unit) - nowMillis
        return if (difference >= 0) {
            (difference + DAY_MILLIS - 1) / DAY_MILLIS
        } else {
            -((-difference + DAY_MILLIS - 1) / DAY_MILLIS)
        }
    }

    /** Texto único y consolidado; `null` cuando no hay nada que avisar. */
    fun reminderText(due: List<MaintenanceDue>): String? =
        if (due.isEmpty()) {
            null
        } else {
            "Esta semana toca mantenimiento en: ${joinNames(due.map { it.spaceName })}"
        }

    private fun joinNames(names: List<String>): String = when (names.size) {
        1 -> names.first()
        2 -> "${names[0]} y ${names[1]}"
        else -> names.dropLast(1).joinToString(", ") + " y " + names.last()
    }
}
