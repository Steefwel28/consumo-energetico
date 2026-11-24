package com.steefwel28.consumoenergetico.data.backup

import com.steefwel28.consumoenergetico.data.local.db.AcUnitEntity
import com.steefwel28.consumoenergetico.data.local.db.BillRecordEntity
import com.steefwel28.consumoenergetico.data.local.db.CustomLocationEntity
import com.steefwel28.consumoenergetico.data.local.db.MaintenanceLogEntity
import com.steefwel28.consumoenergetico.data.local.db.PropertyGroupEntity
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
        groups = listOf(
            PropertyGroupEntity(
                id = 1,
                nombreGrupo = "Mi Casa",
                distribuidora = PanamaUtility.ENSA,
                consumoBaseKwh = 180.0,
            ),
            PropertyGroupEntity(
                id = 2,
                nombreGrupo = "Trabajo",
                distribuidora = PanamaUtility.NATURGY,
                consumoBaseKwh = 240.5,
            ),
        ),
        locations = listOf(
            CustomLocationEntity(id = 1, nombreUbicacion = "Oficina"),
            CustomLocationEntity(id = 2, nombreUbicacion = "Balcón"),
        ),
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
                groupId = 1,
                ubicacion = "Habitación",
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
                groupId = 2,
                ubicacion = "Oficina",
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
        bills = listOf(
            BillRecordEntity(
                id = 1,
                groupId = 1,
                year = 2026,
                month = 1,
                realKwh = 175.5,
                paidBalboas = 41.25,
                utility = PanamaUtility.ENSA,
                createdAtMillis = 1_700_000_000_000L,
            ),
            BillRecordEntity(
                id = 2,
                groupId = 2,
                year = 2026,
                month = 2,
                realKwh = 240.5,
                paidBalboas = 60.0,
                utility = PanamaUtility.NATURGY,
                createdAtMillis = 1_700_100_000_000L,
            ),
        ),
    )

    @Test
    fun `serializa y recupera el respaldo completo con grupos y ubicaciones`() {
        val json = BackupCodec.encode(snapshot)
        assertTrue(json.contains("\"crafted_by\": \"steefwel28\""))
        assertTrue(json.contains("\"schema_version\": 4"))
        assertTrue(json.contains("\"horas_uso_diario\""))
        assertTrue(json.contains("\"property_groups\""))
        assertTrue(json.contains("\"custom_locations\""))
        assertTrue(json.contains("\"bill_records\""))
        assertTrue(json.contains("\"utility\": \"NATURGY\""))
        assertEquals(snapshot, BackupCodec.decode(json))
    }

    @Test
    fun `importa un archivo del esquema uno y lo acomoda en Mi Casa y Sala`() {
        val legacy = """
            {
              "crafted_by": "steefwel28",
              "schema_version": 1,
              "exported_at_millis": 1700000000000,
              "units": {
                "length": "METERS",
                "short_length": "CENTIMETERS",
                "volume": "LITERS",
                "temperature": "CELSIUS"
              },
              "utility": "ENSA",
              "base_monthly_kwh": 120,
              "ac_units": [
                {"id": 4, "nombre_espacio": "Sala", "marca_modelo": "Hisense",
                 "btu": 18000, "seer": 12.0, "es_inverter": true, "voltaje": 220,
                 "fecha_instalacion_millis": 1690000000000,
                 "fecha_ultimo_mantenimiento_millis": 1695000000000,
                 "consumo_estimado_kwh_mes": 210.5}
              ],
              "maintenance_logs": []
            }
        """.trimIndent()

        val decoded = BackupCodec.decode(legacy)

        assertEquals(1, decoded.groups.size)
        assertEquals(PropertyGroupEntity.DEFAULT_ID, decoded.groups.single().id)
        assertEquals(PropertyGroupEntity.DEFAULT_NAME, decoded.groups.single().nombreGrupo)
        assertTrue(decoded.locations.isEmpty())
        assertEquals(PropertyGroupEntity.DEFAULT_ID, decoded.units.single().groupId)
        assertEquals("Sala", decoded.units.single().ubicacion)
        assertTrue(decoded.bills.isEmpty())
    }

    @Test
    fun `descarta facturas de un grupo que no viene en el respaldo`() {
        val orphan = snapshot.copy(
            bills = snapshot.bills + snapshot.bills.first().copy(id = 9, groupId = 99, month = 3),
        )

        val decoded = BackupCodec.decode(BackupCodec.encode(orphan))

        assertEquals(snapshot.bills, decoded.bills)
    }

    @Test
    fun `rechaza un esquema de otra version`() {
        val json = BackupCodec.encode(snapshot).replace("\"schema_version\": 4", "\"schema_version\": 5")
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
    fun `rechaza un equipo que apunta a un grupo inexistente`() {
        val invalid = snapshot.copy(
            units = listOf(snapshot.units.first().copy(groupId = 99)),
        )
        val error = assertThrows(BackupFormatException::class.java) {
            BackupCodec.decode(BackupCodec.encode(invalid))
        }
        assertTrue(error.message!!.contains("grupo"))
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
