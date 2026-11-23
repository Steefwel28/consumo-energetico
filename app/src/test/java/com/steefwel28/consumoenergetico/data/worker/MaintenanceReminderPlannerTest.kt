package com.steefwel28.consumoenergetico.data.worker

import com.steefwel28.consumoenergetico.data.local.db.AcUnitEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MaintenanceReminderPlannerTest {

    private val now = 1_700_000_000_000L
    private val day = MaintenanceReminderPlanner.DAY_MILLIS

    private fun unit(
        id: Long,
        name: String,
        installedDaysAgo: Long = 200,
        maintainedDaysAgo: Long = 100,
    ) = AcUnitEntity(
        id = id,
        nombreEspacio = name,
        marcaModelo = "Split 12k",
        btu = 12_000,
        seer = 12.0,
        esInverter = true,
        voltaje = 220,
        fechaInstalacionMillis = now - installedDaysAgo * day,
        fechaUltimoMantenimientoMillis = now - maintainedDaysAgo * day,
        consumoEstimadoKwhMes = 120.0,
    )

    @Test
    fun `incluye vencidos y los que vencen dentro de 21 dias`() {
        val units = listOf(
            unit(1, "Recámara Principal", maintainedDaysAgo = 100),
            unit(2, "Sala", maintainedDaysAgo = 80),
            unit(3, "Cocina", maintainedDaysAgo = 10),
        )
        val due = MaintenanceReminderPlanner.dueUnits(units, now)
        assertEquals(listOf(1L, 2L), due.map { it.unitId })
    }

    @Test
    fun `respeta el borde exacto de la ventana de 21 dias`() {
        val onTheEdge = listOf(unit(1, "Sala", maintainedDaysAgo = 69))
        val justOutside = listOf(unit(2, "Sala", maintainedDaysAgo = 68))

        assertEquals(1, MaintenanceReminderPlanner.dueUnits(onTheEdge, now).size)
        assertEquals(0, MaintenanceReminderPlanner.dueUnits(justOutside, now).size)
    }

    @Test
    fun `usa la instalacion cuando es mas reciente que el mantenimiento`() {
        val units = listOf(unit(1, "Estudio", installedDaysAgo = 95, maintainedDaysAgo = 120))
        val due = MaintenanceReminderPlanner.dueUnits(units, now)
        assertEquals(1, due.size)
        assertEquals(now - 5 * day, due.single().dueAtMillis)
    }

    @Test
    fun `no avisa cuando el mantenimiento esta lejos`() {
        assertEquals(
            emptyList<MaintenanceDue>(),
            MaintenanceReminderPlanner.dueUnits(listOf(unit(1, "Sala", maintainedDaysAgo = 1)), now),
        )
    }

    @Test
    fun `consolida dos espacios con y`() {
        val text = MaintenanceReminderPlanner.reminderText(
            MaintenanceReminderPlanner.dueUnits(
                listOf(
                    unit(1, "Recámara Principal", maintainedDaysAgo = 100),
                    unit(2, "Sala", maintainedDaysAgo = 80),
                ),
                now,
            ),
        )
        assertEquals("Esta semana toca mantenimiento en: Recámara Principal y Sala", text)
    }

    @Test
    fun `consolida tres espacios con coma y y`() {
        val due = listOf(
            MaintenanceDue(1, "Sala", now),
            MaintenanceDue(2, "Cocina", now + 1),
            MaintenanceDue(3, "Estudio", now + 2),
        )
        assertEquals(
            "Esta semana toca mantenimiento en: Sala, Cocina y Estudio",
            MaintenanceReminderPlanner.reminderText(due),
        )
    }

    @Test
    fun `sin equipos no hay texto de notificacion`() {
        assertNull(MaintenanceReminderPlanner.reminderText(emptyList()))
    }
}
