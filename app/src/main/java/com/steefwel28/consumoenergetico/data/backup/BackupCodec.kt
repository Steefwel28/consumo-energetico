package com.steefwel28.consumoenergetico.data.backup

import com.steefwel28.consumoenergetico.core.signature.CraftedBy
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
import java.math.BigDecimal

/** Archivo corrupto, de otra versión o con relaciones rotas. */
class BackupFormatException(message: String) : IllegalArgumentException(message)

/** Contenido completo de un respaldo ya validado. */
@CraftedBy("steefwel28")
data class BackupSnapshot(
    val exportedAtMillis: Long,
    val unitPreferences: UnitPreferences,
    val utility: PanamaUtility,
    val baseMonthlyKwh: Double,
    val units: List<AcUnitEntity>,
    val logs: List<MaintenanceLogEntity>,
    val groups: List<PropertyGroupEntity> = emptyList(),
    val locations: List<CustomLocationEntity> = emptyList(),
    val bills: List<BillRecordEntity> = emptyList(),
)

/**
 * Serialización y validación del respaldo `.json`.
 *
 * El decodificador es estricto a propósito: verifica firma, esquema, tipos,
 * ids repetidos y que cada limpieza pertenezca a un equipo existente. Un
 * archivo inválido lanza [BackupFormatException] antes de tocar Room.
 *
 * El esquema 2 añade grupos/propiedades y ubicaciones personalizadas; el 3
 * guarda las horas de uso diario y el 4 el histórico de facturas reales. Los
 * archivos anteriores se siguen importando y caen en "Mi Casa"/"Sala"/8 h/sin
 * facturas.
 */
@CraftedBy("steefwel28")
object BackupCodec {

    const val SCHEMA_VERSION = 4
    const val CRAFTED_BY = "steefwel28"

    fun encode(snapshot: BackupSnapshot): String = buildString {
        require(snapshot.groups.isNotEmpty()) { "Un respaldo necesita al menos un grupo." }
        append("{\n")
        append("  \"crafted_by\": \"$CRAFTED_BY\",\n")
        append("  \"schema_version\": $SCHEMA_VERSION,\n")
        append("  \"exported_at_millis\": ${snapshot.exportedAtMillis},\n")
        append("  \"units\": {\n")
        append("    \"length\": \"${snapshot.unitPreferences.length.name}\",\n")
        append("    \"short_length\": \"${snapshot.unitPreferences.shortLength.name}\",\n")
        append("    \"volume\": \"${snapshot.unitPreferences.volume.name}\",\n")
        append("    \"temperature\": \"${snapshot.unitPreferences.temperature.name}\"\n")
        append("  },\n")
        append("  \"utility\": \"${snapshot.utility.name}\",\n")
        append("  \"base_monthly_kwh\": ${number(snapshot.baseMonthlyKwh)},\n")
        append("  \"property_groups\": [\n")
        snapshot.groups.forEachIndexed { index, group ->
            append(group.toJson())
            append(if (index < snapshot.groups.lastIndex) ",\n" else "\n")
        }
        append("  ],\n")
        append("  \"custom_locations\": [\n")
        snapshot.locations.forEachIndexed { index, location ->
            append(location.toJson())
            append(if (index < snapshot.locations.lastIndex) ",\n" else "\n")
        }
        append("  ],\n")
        append("  \"ac_units\": [\n")
        snapshot.units.forEachIndexed { index, unit ->
            append(unit.toJson())
            append(if (index < snapshot.units.lastIndex) ",\n" else "\n")
        }
        append("  ],\n")
        append("  \"maintenance_logs\": [\n")
        snapshot.logs.forEachIndexed { index, log ->
            append(log.toJson())
            append(if (index < snapshot.logs.lastIndex) ",\n" else "\n")
        }
        append("  ],\n")
        append("  \"bill_records\": [\n")
        snapshot.bills.forEachIndexed { index, bill ->
            append(bill.toJson())
            append(if (index < snapshot.bills.lastIndex) ",\n" else "\n")
        }
        append("  ]\n")
        append("}\n")
    }

    fun decode(json: String): BackupSnapshot {
        val root = JsonParser(json).parse().asObject("el respaldo")

        if (root.string("crafted_by") != CRAFTED_BY) {
            throw BackupFormatException("El respaldo no está firmado por $CRAFTED_BY.")
        }
        val version = root.long("schema_version")
        if (version < 1L || version > SCHEMA_VERSION.toLong()) {
            throw BackupFormatException("Versión de esquema no soportada: $version.")
        }

        val prefs = root.objectValue("units")
        val unitPreferences = UnitPreferences(
            length = prefs.enumValue("length"),
            shortLength = prefs.enumValue("short_length"),
            volume = prefs.enumValue("volume"),
            temperature = prefs.enumValue("temperature"),
        )
        val utility = root.enumValue<PanamaUtility>("utility")
        val baseMonthlyKwh = root.double("base_monthly_kwh")
        val exportedAtMillis = root.long("exported_at_millis")

        requireNonNegative(baseMonthlyKwh, "base_monthly_kwh")
        requireNonNegative(exportedAtMillis.toDouble(), "exported_at_millis")

        val groups = if (version >= 2L) {
            root.array("property_groups").mapIndexed { index, element ->
                element.asObject("property_groups[$index]").toGroup()
            }
        } else {
            listOf(
                PropertyGroupEntity(
                    id = PropertyGroupEntity.DEFAULT_ID,
                    nombreGrupo = PropertyGroupEntity.DEFAULT_NAME,
                    distribuidora = PanamaUtility.ENSA,
                    consumoBaseKwh = 0.0,
                ),
            )
        }
        val locations = if (version >= 2L) {
            root.array("custom_locations").mapIndexed { index, element ->
                element.asObject("custom_locations[$index]").toLocation()
            }
        } else {
            emptyList()
        }

        val groupsById = groups.associateBy { it.id }
        if (groupsById.size != groups.size) {
            throw BackupFormatException("Hay grupos con id repetido.")
        }
        if (groupsById.isEmpty()) {
            throw BackupFormatException("El respaldo no trae ningún grupo.")
        }
        val locationIds = locations.map { it.id }
        if (locationIds.size != locationIds.toSet().size) {
            throw BackupFormatException("Hay ubicaciones con id repetido.")
        }
        val locationNames = locations.map { it.nombreUbicacion }
        if (locationNames.size != locationNames.toSet().size) {
            throw BackupFormatException("Hay ubicaciones con el mismo nombre.")
        }

        val units = root.array("ac_units").mapIndexed { index, element ->
            element.asObject("ac_units[$index]").toUnit(version, groupsById.keys)
        }
        val logs = root.array("maintenance_logs").mapIndexed { index, element ->
            element.asObject("maintenance_logs[$index]").toLog()
        }

        val unitIds = units.map { it.id }
        if (unitIds.size != unitIds.toSet().size) {
            throw BackupFormatException("Hay equipos con id repetido.")
        }
        val logIds = logs.map { it.id }
        if (logIds.size != logIds.toSet().size) {
            throw BackupFormatException("Hay limpiezas con id repetido.")
        }
        logs.firstOrNull { it.acUnitId !in unitIds }?.let { orphan ->
            throw BackupFormatException(
                "La limpieza ${orphan.id} apunta al equipo ${orphan.acUnitId}, que no existe.",
            )
        }

        // Los grupos se restauran con sus ids originales, así que el mapa
        // groupsById es la identidad: sirve igual que para los equipos para
        // descartar facturas de un grupo que ya no está en el respaldo.
        val bills = if (version >= 4L) {
            root.array("bill_records").mapIndexedNotNull { index, element ->
                element.asObject("bill_records[$index]").toBill(groupsById.keys)
            }
        } else {
            emptyList()
        }
        val billIds = bills.map { it.id }
        if (billIds.size != billIds.toSet().size) {
            throw BackupFormatException("Hay facturas con id repetido.")
        }
        val billSlots = bills.map { Triple(it.groupId, it.year, it.month) }
        if (billSlots.size != billSlots.toSet().size) {
            throw BackupFormatException("Hay dos facturas del mismo grupo y mes.")
        }

        return BackupSnapshot(
            exportedAtMillis = exportedAtMillis,
            unitPreferences = unitPreferences,
            utility = utility,
            baseMonthlyKwh = baseMonthlyKwh,
            units = units,
            logs = logs,
            groups = groups,
            locations = locations,
            bills = bills,
        )
    }

    private fun PropertyGroupEntity.toJson(): String =
        "    {\"id\": $id, \"nombre_grupo\": \"${escape(nombreGrupo)}\", " +
            "\"distribuidora\": \"${distribuidora.name}\", " +
            "\"consumo_base_kwh\": ${number(consumoBaseKwh)}}"

    private fun CustomLocationEntity.toJson(): String =
        "    {\"id\": $id, \"nombre_ubicacion\": \"${escape(nombreUbicacion)}\"}"

    private fun AcUnitEntity.toJson(): String =
        "    {\"id\": $id, \"nombre_espacio\": \"${escape(nombreEspacio)}\", " +
            "\"marca_modelo\": \"${escape(marcaModelo)}\", \"btu\": $btu, " +
            "\"nominal_watts\": ${nominalWatts ?: "null"}, " +
            "\"seer\": ${seer?.let { number(it) } ?: "null"}, \"es_inverter\": $esInverter, " +
            "\"voltaje\": $voltaje, \"fecha_instalacion_millis\": $fechaInstalacionMillis, " +
            "\"fecha_ultimo_mantenimiento_millis\": $fechaUltimoMantenimientoMillis, " +
            "\"consumo_estimado_kwh_mes\": ${number(consumoEstimadoKwhMes)}, " +
            "\"horas_uso_diario\": ${number(horasUsoDiario)}, " +
            "\"group_id\": $groupId, \"ubicacion\": \"${escape(ubicacion)}\"}"

    private fun MaintenanceLogEntity.toJson(): String =
        "    {\"id\": $id, \"ac_unit_id\": $acUnitId, \"fecha_millis\": $fechaMillis, " +
            "\"nota\": \"${escape(nota)}\"}"

    private fun BillRecordEntity.toJson(): String =
        "    {\"id\": $id, \"group_id\": $groupId, \"year\": $year, \"month\": $month, " +
            "\"real_kwh\": ${number(realKwh)}, \"paid_balboas\": ${number(paidBalboas)}, " +
            "\"utility\": \"${utility.name}\", \"created_at_millis\": $createdAtMillis}"

    private fun Map<String, Json>.toGroup(): PropertyGroupEntity {
        val name = string("nombre_grupo").trim()
        if (name.isEmpty()) throw BackupFormatException("Un grupo tiene el nombre vacío.")
        val base = double("consumo_base_kwh")
        requireNonNegative(base, "consumo_base_kwh de '$name'")
        return PropertyGroupEntity(
            id = long("id"),
            nombreGrupo = name,
            distribuidora = enumValue("distribuidora"),
            consumoBaseKwh = base,
        )
    }

    private fun Map<String, Json>.toLocation(): CustomLocationEntity {
        val name = string("nombre_ubicacion").trim()
        if (name.isEmpty()) throw BackupFormatException("Una ubicación tiene el nombre vacío.")
        return CustomLocationEntity(id = long("id"), nombreUbicacion = name)
    }

    private fun Map<String, Json>.toUnit(
        version: Long,
        groupIds: Set<Long>,
    ): AcUnitEntity {
        val name = string("nombre_espacio").trim()
        if (name.isEmpty()) throw BackupFormatException("Un equipo tiene el nombre vacío.")
        val btu = long("btu").toInt()
        if (btu <= 0) throw BackupFormatException("El BTU de '$name' debe ser mayor que cero.")
        val seer = optionalDouble("seer")
        if (seer != null && seer <= 0.0) {
            throw BackupFormatException("El SEER de '$name' debe ser mayor que cero.")
        }
        val watts = optionalDouble("nominal_watts")
        if (watts != null && watts <= 0.0) {
            throw BackupFormatException("La potencia de '$name' debe ser mayor que cero.")
        }
        val voltage = long("voltaje").toInt()
        if (voltage <= 0) throw BackupFormatException("El voltaje de '$name' debe ser mayor que cero.")
        val installedOn = long("fecha_instalacion_millis")
        val maintainedOn = long("fecha_ultimo_mantenimiento_millis")
        if (installedOn < 0 || maintainedOn < 0) {
            throw BackupFormatException("Las fechas de '$name' no pueden ser negativas.")
        }
        val monthlyKwh = double("consumo_estimado_kwh_mes")
        requireNonNegative(monthlyKwh, "consumo_estimado_kwh_mes de '$name'")
        val hours = if (version >= 3L) optionalDouble("horas_uso_diario") else null
        val dailyHours = (hours ?: 8.0).takeIf { it.isFinite() && it in 0.0..24.0 }
            ?: throw BackupFormatException("Las horas de uso de '$name' deben estar entre 0 y 24.")

        val groupId = if (version >= 2L) long("group_id") else PropertyGroupEntity.DEFAULT_ID
        if (groupId !in groupIds) {
            throw BackupFormatException("El equipo '$name' apunta a un grupo que no existe.")
        }
        val location = if (version >= 2L) string("ubicacion").trim() else "Sala"
        if (location.isEmpty()) throw BackupFormatException("El equipo '$name' no tiene ubicación.")

        return AcUnitEntity(
            id = long("id"),
            nombreEspacio = name,
            marcaModelo = string("marca_modelo"),
            btu = btu,
            nominalWatts = watts?.toInt(),
            seer = seer,
            esInverter = boolean("es_inverter"),
            voltaje = voltage,
            fechaInstalacionMillis = installedOn,
            fechaUltimoMantenimientoMillis = maintainedOn,
            consumoEstimadoKwhMes = monthlyKwh,
            horasUsoDiario = dailyHours,
            groupId = groupId,
            ubicacion = location,
        )
    }

    private fun Map<String, Json>.toLog(): MaintenanceLogEntity {
        val acUnitId = long("ac_unit_id")
        if (acUnitId <= 0) throw BackupFormatException("Una limpieza tiene un equipo inválido.")
        val date = long("fecha_millis")
        if (date < 0) throw BackupFormatException("Una limpieza tiene fecha negativa.")
        return MaintenanceLogEntity(
            id = long("id"),
            acUnitId = acUnitId,
            fechaMillis = date,
            nota = string("nota"),
        )
    }

    /** Devuelve `null` si la factura apunta a un grupo ausente del respaldo. */
    private fun Map<String, Json>.toBill(groupIds: Set<Long>): BillRecordEntity? {
        val groupId = long("group_id")
        if (groupId !in groupIds) return null
        val year = long("year")
        if (year !in 1L..9_999L) throw BackupFormatException("Una factura tiene un año inválido.")
        val month = long("month")
        if (month !in 1L..12L) throw BackupFormatException("Una factura tiene un mes inválido.")
        val realKwh = double("real_kwh")
        requireNonNegative(realKwh, "real_kwh de una factura")
        val paid = double("paid_balboas")
        requireNonNegative(paid, "paid_balboas de una factura")
        val createdAt = long("created_at_millis")
        if (createdAt < 0) throw BackupFormatException("Una factura tiene fecha negativa.")
        return BillRecordEntity(
            id = long("id"),
            groupId = groupId,
            year = year.toInt(),
            month = month.toInt(),
            realKwh = realKwh,
            paidBalboas = paid,
            utility = enumValue("utility"),
            createdAtMillis = createdAt,
        )
    }

    private fun requireNonNegative(value: Double, field: String) {
        if (!value.isFinite() || value < 0.0) {
            throw BackupFormatException("\"$field\" debe ser un número no negativo.")
        }
    }

    private fun number(value: Double): String {
        if (!value.isFinite()) throw BackupFormatException("No se pueden respaldar valores infinitos.")
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
    }

    private fun escape(value: String): String = buildString {
        value.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else ->
                    if (character < ' ') {
                        append("\\u").append(character.code.toString(16).padStart(4, '0'))
                    } else {
                        append(character)
                    }
            }
        }
    }
}

// --- AST y parser JSON mínimo, sin dependencias externas ---

private sealed interface Json {
    data class Obj(val values: Map<String, Json>) : Json
    data class Arr(val values: List<Json>) : Json
    data class Str(val value: String) : Json
    data class Num(val value: Double) : Json
    data class Bool(val value: Boolean) : Json
    data object Null : Json
}

private class JsonParser(private val text: String) {

    private var index = 0

    fun parse(): Json {
        val value = readValue()
        skipWhitespace()
        if (index != text.length) fail("Hay contenido extra después del respaldo.")
        return value
    }

    private fun readValue(): Json {
        skipWhitespace()
        if (index >= text.length) fail("El respaldo termina antes de tiempo.")
        return when (text[index]) {
            '{' -> readObject()
            '[' -> readArray()
            '"' -> Json.Str(readString())
            't' -> readLiteral("true", Json.Bool(true))
            'f' -> readLiteral("false", Json.Bool(false))
            'n' -> readLiteral("null", Json.Null)
            else -> readNumber()
        }
    }

    private fun readObject(): Json {
        expect('{')
        val values = LinkedHashMap<String, Json>()
        skipWhitespace()
        if (peek('}')) {
            index++
            return Json.Obj(values)
        }
        while (true) {
            skipWhitespace()
            if (!peek('"')) fail("Se esperaba una clave de texto.")
            val key = readString()
            skipWhitespace()
            expect(':')
            if (values.containsKey(key)) fail("La clave '$key' está repetida.")
            values[key] = readValue()
            skipWhitespace()
            when {
                peek(',') -> index++
                peek('}') -> {
                    index++
                    return Json.Obj(values)
                }
                else -> fail("Se esperaba ',' o '}'.")
            }
        }
    }

    private fun readArray(): Json {
        expect('[')
        val values = mutableListOf<Json>()
        skipWhitespace()
        if (peek(']')) {
            index++
            return Json.Arr(values)
        }
        while (true) {
            values += readValue()
            skipWhitespace()
            when {
                peek(',') -> index++
                peek(']') -> {
                    index++
                    return Json.Arr(values)
                }
                else -> fail("Se esperaba ',' o ']'.")
            }
        }
    }

    private fun readString(): String {
        expect('"')
        val result = StringBuilder()
        while (index < text.length) {
            when (val character = text[index++]) {
                '"' -> return result.toString()
                '\\' -> {
                    if (index >= text.length) fail("Escape incompleto en el respaldo.")
                    when (val escaped = text[index++]) {
                        '"' -> result.append('"')
                        '\\' -> result.append('\\')
                        '/' -> result.append('/')
                        'b' -> result.append('\b')
                        'f' -> result.append('\u000C')
                        'n' -> result.append('\n')
                        'r' -> result.append('\r')
                        't' -> result.append('\t')
                        'u' -> result.append(readUnicodeEscape())
                        else -> fail("Escape desconocido: '\\$escaped'.")
                    }
                }
                else -> {
                    if (character < ' ') fail("Texto sin cerrar en el respaldo.")
                    result.append(character)
                }
            }
        }
        fail("Texto sin cerrar en el respaldo.")
    }

    private fun readUnicodeEscape(): Char {
        if (index + 4 > text.length) fail("Escape unicode incompleto.")
        val hex = text.substring(index, index + 4)
        index += 4
        return hex.toIntOrNull(16)?.toChar() ?: fail("Escape unicode inválido: '$hex'.")
    }

    private fun readNumber(): Json {
        val start = index
        while (index < text.length && (text[index].isDigit() || text[index] in "-+.eE")) index++
        val token = text.substring(start, index)
        val value = token.toDoubleOrNull() ?: fail("Número inválido: '$token'.")
        return Json.Num(value)
    }

    private fun readLiteral(word: String, value: Json): Json {
        if (!text.startsWith(word, index)) fail("Se esperaba '$word'.")
        index += word.length
        return value
    }

    private fun skipWhitespace() {
        while (index < text.length && text[index].isWhitespace()) index++
    }

    private fun peek(character: Char): Boolean = index < text.length && text[index] == character

    private fun expect(character: Char) {
        if (!peek(character)) fail("Se esperaba '$character'.")
        index++
    }

    private fun fail(message: String): Nothing = throw BackupFormatException(message)
}

private fun Json.asObject(context: String): Map<String, Json> =
    (this as? Json.Obj)?.values ?: throw BackupFormatException("$context debe ser un objeto.")

private fun Map<String, Json>.objectValue(field: String): Map<String, Json> =
    (this[field] as? Json.Obj)?.values
        ?: throw BackupFormatException("\"$field\" debe ser un objeto.")

private fun Map<String, Json>.array(field: String): List<Json> =
    (this[field] as? Json.Arr)?.values
        ?: throw BackupFormatException("\"$field\" debe ser una lista.")

private fun Map<String, Json>.string(field: String): String =
    (this[field] as? Json.Str)?.value
        ?: throw BackupFormatException("\"$field\" debe ser texto.")

private fun Map<String, Json>.double(field: String): Double =
    (this[field] as? Json.Num)?.value
        ?: throw BackupFormatException("\"$field\" debe ser un número.")

private fun Map<String, Json>.optionalDouble(field: String): Double? = when (val value = this[field]) {
    null, Json.Null -> null
    is Json.Num -> value.value
    else -> throw BackupFormatException("\"$field\" debe ser un número o null.")
}

private fun Map<String, Json>.long(field: String): Long {
    val value = double(field)
    if (value % 1.0 != 0.0) throw BackupFormatException("\"$field\" debe ser un entero.")
    return value.toLong()
}

private fun Map<String, Json>.boolean(field: String): Boolean =
    (this[field] as? Json.Bool)?.value
        ?: throw BackupFormatException("\"$field\" debe ser verdadero o falso.")

private inline fun <reified T : Enum<T>> Map<String, Json>.enumValue(field: String): T {
    val raw = string(field)
    return enumValues<T>().firstOrNull { it.name == raw }
        ?: throw BackupFormatException("Valor desconocido '$raw' en \"$field\".")
}
