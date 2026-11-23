package com.steefwel28.consumoenergetico.data.backup

import com.steefwel28.consumoenergetico.data.local.db.AcUnitEntity
import com.steefwel28.consumoenergetico.data.local.db.MaintenanceLogEntity
import com.steefwel28.consumoenergetico.domain.calculator.PanamaUtility
import com.steefwel28.consumoenergetico.domain.units.LengthUnit
import com.steefwel28.consumoenergetico.domain.units.ShortLengthUnit
import com.steefwel28.consumoenergetico.domain.units.TemperatureUnit
import com.steefwel28.consumoenergetico.domain.units.UnitPreferences
import com.steefwel28.consumoenergetico.domain.units.VolumeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {

    private val snapshot = BackupSnapshot(
        exportedAtMillis = 1_700_000_000_000L,
        unitPreferences = UnitPreferences(
            length = LengthUnit.FEET,
            shortLength = ShortLengthUnit.INCHES,
            volume = VolumeUnit.CUBIC_FEET,
            temperature = TemperatureUnit.FAHRENHEIT,
        ),
        utility = PanamaUtility.NATURGY,
        baseMonthlyKwh = 240.5,
        units = listOf(
            AcUnitEntity(
                id = 1,
                nombreEspacio = "Recámara \"Principal\"",
                marcaModelo = "LG Dual Inverter",
                btu = 12_000,
                seer = 12.5,
                esInverter = true,
                voltaje = 220,
                fechaInstalacionMillis = 1_690_000_000_000L,
                fechaUltimoMantenimientoMillis = 1_695_000_000_000L,
                consumoEstimadoKwhMes = 121.44,
            ),
            AcUnitEntity(
                id = 2,
                nombreEspacio = "Sala",
                marcaModelo = "Midea",
                btu = 18_000,
                seer = null,
                esInverter = false,
                voltaje = 220,
                fechaInstalacionMillis = 1_680_000_000_000L,
                fechaUltimoMantenimientoMillis = 1_690_000_000_000L,
                consumoEstimadoKwhMes = 210.0,
            ),
        ),
        logs = listOf(
            MaintenanceLogEntity(
                id = 1,
                acUnitId = 1,
                fechaMillis = 1_695_000_000_000L,
                nota = "Limpieza de filtros\ncon nota y \"comillas\"",
            ),
            MaintenanceLogEntity(id = 2, acUnitId = 2, fechaMillis = 1_690_000_000_000L, nota = ""),
        ),
    )

    @Test
    fun `serializa y recupera el respaldo completo`() {
        val json = BackupCodec.encode(snapshot)
        assertTrue(json.contains("\"crafted_by\": \"steefwel28\""))
        assertTrue(json.contains("\"schema_version\": 1"))
        assertTrue(json.contains("\"utility\": \"NATURGY\""))
        assertEquals(snapshot, BackupCodec.decode(json))
    }

    @Test
    fun `rechaza un esquema de otra version`() {
        val json = BackupCodec.encode(snapshot).replace("\"schema_version\": 1", "\"schema_version\": 2")
        val error = assertThrows(BackupFormatException::class.java) { BackupCodec.decode(json) }
        assertTrue(error.message!!.contains("esquema"))
    }

    @Test
    fun `rechaza un archivo sin la firma`() {
        val json = BackupCodec.encode(snapshot).replace("steefwel28", "otro")
        assertThrows(BackupFormatException::class.java) { BackupCodec.decode(json) }
    }

    @Test
    fun `rechaza un historial que apunta a un equipo inexistente`() {
        val corrupted = snapshot.copy(
            logs = listOf(
                MaintenanceLogEntity(id = 9, acUnitId = 99, fechaMillis = 1_700_000_000_000L, nota = "x"),
            ),
        )
        val error = assertThrows(BackupFormatException::class.java) {
            BackupCodec.decode(BackupCodec.encode(corrupted))
        }
        assertTrue(error.message!!.contains("no existe"))
    }

    @Test
    fun `rechaza ids de equipos repetidos`() {
        val duplicated = snapshot.copy(units = listOf(snapshot.units.first(), snapshot.units.first()))
        val error = assertThrows(BackupFormatException::class.java) {
            BackupCodec.decode(BackupCodec.encode(duplicated))
        }
        assertTrue(error.message!!.contains("id repetido"))
    }

    @Test
    fun `rechaza consumos negativos`() {
        val invalid = snapshot.copy(baseMonthlyKwh = -1.0)
        assertThrows(BackupFormatException::class.java) {
            BackupCodec.decode(BackupCodec.encode(invalid))
        }
    }

    @Test
    fun `rechaza valores de unidad desconocidos`() {
        val json = BackupCodec.encode(snapshot).replace("\"FEET\"", "\"YARDS\"")
        assertThrows(BackupFormatException::class.java) { BackupCodec.decode(json) }
    }

    @Test
    fun `rechaza un json incompleto`() {
        assertThrows(BackupFormatException::class.java) {
            BackupCodec.decode("{ \"crafted_by\": \"steefwel28\", ")
        }
    }
}
