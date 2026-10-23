package org.tfv.ata.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import java.util.UUID

/**
 * Export and import of layout, appearance and vehicles (`.atabackup`).
 *
 * Pure functions: nothing is written here. [AtaSettings] is not changed structurally; the import
 * builds the new value with [AtaSettings.copy] of the whitelist only.
 *
 * File: `{format, schema, app, created, layout, vehicles, selectedVin}`.
 * Tokens, keys, criticalDirectIds, addresses, biometricLock, active, location and migration flags
 * are never read, even when the file contains them.
 *
 * `widgetActionIds`, `wearComplicationIds` and `vehicleActionIds` are global lists of action ids
 * (not per app-widget id, not a system complication id, not per vehicle id), so they are part of
 * the whitelist. `presets` are not. Home and vehicle lists also keep the known multi-tile ids
 * (`m_lock`, …), because those lists store tiles, not only actions.
 */
object SettingsBackup {
  const val FORMAT = "ata-settings"
  const val SCHEMA = 1
  const val MAX_BYTES = 64 * 1024
  const val MAX_VEHICLES = 10
  const val MAX_NAME = 40

  private const val MAX_IDS = 96
  private const val MAX_LAUNCHER = 4
  private const val ANDROID_12 = 31

  private val json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
    prettyPrint = true
  }

  /** Multi-tile ids stored in [AtaSettings.homeActionIds] and [AtaSettings.vehicleActionIds]. */
  private val TILE_ACTIONS: Map<String, Set<String>> = mapOf(
    "m_lock" to setOf("lock", "unlock"),
    "m_climate" to setOf(
      "climate_on", "climate_off", "set_temperature", "set_temperature_driver",
      "set_temperature_passenger", "defrost", "defrost_off",
    ),
    "m_seats" to setOf(
      "seat_heater_fl", "seat_heater_fr", "seat_heater_rl", "seat_heater_rc", "seat_heater_rr",
      "seat_cooler_fl", "seat_cooler_fr", "seat_auto_fl", "seat_auto_fr",
    ),
    "m_charging" to setOf("charge_start", "charge_stop", "charge_limit", "charging_amps"),
    "m_lids" to setOf("frunk", "trunk", "trunk_close"),
    "m_doors" to setOf("door_driver_open", "door_passenger_open"),
    "m_windows" to setOf("vent_windows", "close_windows"),
    "m_charge_port" to setOf("charge_port_open", "charge_port_close"),
    "m_volume" to setOf("set_volume", "volume_up", "volume_down"),
    "m_media" to setOf("media_previous", "media_toggle", "media_next"),
    "m_sentry" to setOf("sentry_on", "sentry_off"),
    "m_steering" to setOf("steering_wheel_heater", "steering_wheel_heater_off"),
    "m_keeper" to setOf("climate_keep_off", "climate_keep", "dog_mode", "camp_mode"),
    "m_overheat" to setOf("overheat_protection_off", "overheat_protection_on", "overheat_protection_fan_only"),
    "m_bioweapon" to setOf("bioweapon_on", "bioweapon_off"),
  )

  /** Merged into [m_seats] in 1.9.3. */
  private val TILE_ALIAS = mapOf("m_seat_cool" to "m_seats")

  private val TESLA_WMI = setOf("5YJ", "7SA", "7G2", "XP7", "LRW", "SFZ")

  private val BODY_STYLES = BodyStyle.entries.map { it.name }.toSet()
  private val PAINTS = CarPaint.entries.map { it.name }.toSet()
  private val NEONS = NeonColor.entries.map { it.name }.toSet()

  private val VIN_VALUE = mapOf(
    'A' to 1, 'B' to 2, 'C' to 3, 'D' to 4, 'E' to 5, 'F' to 6, 'G' to 7, 'H' to 8,
    'J' to 1, 'K' to 2, 'L' to 3, 'M' to 4, 'N' to 5, 'P' to 7, 'R' to 9,
    'S' to 2, 'T' to 3, 'U' to 4, 'V' to 5, 'W' to 6, 'X' to 7, 'Y' to 8, 'Z' to 9,
  )
  private val VIN_WEIGHT = intArrayOf(8, 7, 6, 5, 4, 3, 2, 10, 0, 9, 8, 7, 6, 5, 4, 3, 2)

  enum class RejectReason { NOT_ATA, NEWER, CORRUPT, TOO_LARGE }

  enum class DropReason { INVALID, DUPLICATE, FILE_LIMIT, TOTAL_LIMIT, DEMO }

  data class Drop(val label: String, val reason: DropReason)

  data class Summary(
    val tileCount: Int,
    val wearActionCount: Int,
    val vehiclesNew: Int,
    val vehiclesUpdated: Int,
    val drops: List<Drop>,
    val wmiWarnings: List<String>,
    val checkDigitWarnings: List<String>,
    val criticalOverlap: Boolean,
  )

  /** Whitelist applied on top of the current settings, plus the vehicle list and selection. */
  data class Write(
    val settings: AtaSettings,
    val vehicles: List<Vehicle>,
    val selectedId: String?,
  )

  sealed class Outcome {
    data class Rejected(val reason: RejectReason) : Outcome()
    data class Ready(val write: Write, val summary: Summary) : Outcome()
  }

  fun export(
    settings: AtaSettings,
    vehicles: List<Vehicle>,
    selectedId: String?,
    createdEpochMs: Long,
    appVersion: String,
  ): String {
    val exported = vehicles.filter { it.id != DemoCar.VEHICLE_ID }.map { v ->
      BackupVehicle(
        name = cleanName(v.name),
        vin = Vin.normalize(v.vin),
        bodyStyle = v.bodyStyle,
        paint = v.paint,
        neonColor = v.neonColor,
      )
    }
    val selectedVin = vehicles
      .firstOrNull { it.id == selectedId && it.id != DemoCar.VEHICLE_ID }
      ?.vin
      ?.let(Vin::normalize)
      .orEmpty()
    return json.encodeToString(
      BackupFile(
        format = FORMAT,
        schema = SCHEMA,
        app = appVersion,
        created = java.time.Instant.ofEpochMilli(createdEpochMs).toString(),
        layout = BackupLayout(
          theme = settings.theme.name,
          design = settings.design.name,
          dynamicColor = settings.dynamicColor,
          car3d = settings.car3d,
          directionFx = settings.directionFx,
          haptics = settings.haptics,
          tileColumns = settings.tileColumns,
          multiTiles = settings.multiTiles,
          homeActionIds = settings.homeActionIds,
          vehicleActionIds = settings.vehicleActionIds,
          launcherMenuActionIds = settings.launcherMenuActionIds,
          routineActionIds = settings.routineActionIds,
          widgetActionIds = settings.widgetActionIds,
          widgetDesign = settings.widgetDesign.name,
          widgetTranslucent = settings.widgetTranslucent,
          wearActionIds = settings.wearActionIds,
          wearGrid = settings.wearGrid,
          wearComplicationIds = settings.wearComplicationIds,
        ),
        vehicles = exported,
        selectedVin = selectedVin,
      ),
    )
  }

  /**
   * Validates [bytes] and merges the whitelist onto [current]. Nothing is stored.
   * [newId] supplies ids for vehicles that do not exist yet (VIN match keeps the old id).
   */
  fun plan(
    bytes: ByteArray,
    current: AtaSettings,
    vehicles: List<Vehicle>,
    selectedId: String?,
    sdkInt: Int,
    newId: () -> String = { UUID.randomUUID().toString() },
  ): Outcome {
    if (bytes.size > MAX_BYTES) return Outcome.Rejected(RejectReason.TOO_LARGE)
    if (bytes.isEmpty()) return Outcome.Rejected(RejectReason.CORRUPT)
    val text = bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
    val root = try {
      json.parseToJsonElement(text)
    } catch (_: SerializationException) {
      return Outcome.Rejected(RejectReason.CORRUPT)
    } catch (_: IllegalArgumentException) {
      return Outcome.Rejected(RejectReason.CORRUPT)
    }
    if (root !is JsonObject) return Outcome.Rejected(RejectReason.CORRUPT)
    val formatEl = root["format"] ?: return Outcome.Rejected(RejectReason.NOT_ATA)
    if (formatEl !is JsonPrimitive || !formatEl.isString) return Outcome.Rejected(RejectReason.CORRUPT)
    if (formatEl.content != FORMAT) return Outcome.Rejected(RejectReason.NOT_ATA)
    val schemaEl = root["schema"] ?: return Outcome.Rejected(RejectReason.CORRUPT)
    if (schemaEl !is JsonPrimitive) return Outcome.Rejected(RejectReason.CORRUPT)
    val schema = schemaEl.intOrNull ?: return Outcome.Rejected(RejectReason.CORRUPT)
    if (schema > SCHEMA) return Outcome.Rejected(RejectReason.NEWER)
    if (schema != SCHEMA) return Outcome.Rejected(RejectReason.CORRUPT)

    val env = try {
      json.decodeFromString<Envelope>(text)
    } catch (_: SerializationException) {
      return Outcome.Rejected(RejectReason.CORRUPT)
    } catch (_: IllegalArgumentException) {
      return Outcome.Rejected(RejectReason.CORRUPT)
    }
    val patch = try {
      readPatch(env.layout)
    } catch (_: BadFile) {
      return Outcome.Rejected(RejectReason.CORRUPT)
    }
    val batch = readVehicles(env.vehicles)
    return merge(current, vehicles, selectedId, patch, batch, env.selectedVin, sdkInt, newId)
  }

  private fun merge(
    current: AtaSettings,
    vehicles: List<Vehicle>,
    selectedId: String?,
    patch: Patch,
    batch: VehicleBatch,
    selectedVinRaw: String,
    sdkInt: Int,
    newId: () -> String,
  ): Outcome.Ready {
    val dynamic = when {
      sdkInt < ANDROID_12 -> false
      patch.dynamicColor != null -> patch.dynamicColor
      else -> current.dynamicColor
    }
    val settings = current.copy(
      theme = patch.theme ?: current.theme,
      design = patch.design ?: current.design,
      dynamicColor = dynamic,
      haptics = patch.haptics ?: current.haptics,
      directionFx = patch.directionFx ?: current.directionFx,
      car3d = patch.car3d ?: current.car3d,
      homeActionIds = patch.homeActionIds ?: current.homeActionIds,
      vehicleActionIds = patch.vehicleActionIds ?: current.vehicleActionIds,
      multiTiles = patch.multiTiles ?: current.multiTiles,
      tileColumns = patch.tileColumns ?: current.tileColumns,
      widgetDesign = patch.widgetDesign ?: current.widgetDesign,
      widgetTranslucent = patch.widgetTranslucent ?: current.widgetTranslucent,
      wearActionIds = patch.wearActionIds ?: current.wearActionIds,
      wearGrid = patch.wearGrid ?: current.wearGrid,
      wearComplicationIds = patch.wearComplicationIds ?: current.wearComplicationIds,
      launcherMenuActionIds = patch.launcherMenuActionIds ?: current.launcherMenuActionIds,
      routineActionIds = patch.routineActionIds ?: current.routineActionIds,
      widgetActionIds = patch.widgetActionIds ?: current.widgetActionIds,
    )

    val merged = vehicles.toMutableList()
    var room = (MAX_VEHICLES - merged.size).coerceAtLeast(0)
    val drops = batch.drops.toMutableList()
    val wmi = mutableListOf<String>()
    val checks = mutableListOf<String>()
    var updated = 0
    var added = 0
    for (draft in batch.accepted) {
      val index = merged.indexOfFirst { it.id != DemoCar.VEHICLE_ID && Vin.normalize(it.vin) == draft.vin }
      if (index >= 0) {
        val old = merged[index]
        merged[index] = old.copy(
          name = draft.name,
          bodyStyle = draft.bodyStyle,
          paint = draft.paint,
          neonColor = draft.neonColor,
        )
        updated++
        warn(draft, wmi, checks)
      } else if (merged.any { it.id == DemoCar.VEHICLE_ID && Vin.normalize(it.vin) == draft.vin }) {
        drops += Drop(draft.label, DropReason.DEMO)
      } else if (room <= 0) {
        drops += Drop(draft.label, DropReason.TOTAL_LIMIT)
      } else {
        val used = merged.mapTo(HashSet()) { it.id }
        merged += Vehicle(
          id = freshId(used, newId),
          name = draft.name,
          vin = draft.vin,
          navHome = "",
          navWork = "",
          bodyStyle = draft.bodyStyle,
          paint = draft.paint,
          neonColor = draft.neonColor,
        )
        room--
        added++
        warn(draft, wmi, checks)
      }
    }

    val wanted = Vin.normalize(selectedVinRaw)
    val selectedOut = if (Vin.isValid(wanted)) {
      merged.firstOrNull { it.id != DemoCar.VEHICLE_ID && Vin.normalize(it.vin) == wanted }?.id ?: selectedId
    } else {
      selectedId
    }

    val importedTiles = buildList {
      patch.homeActionIds?.let { addAll(it) }
      patch.vehicleActionIds?.let { addAll(it) }
    }
    val critical = current.criticalDirectIds.toSet()
    val overlap = importedTiles.any { id -> actionsOf(id).any { it in critical } }

    return Outcome.Ready(
      write = Write(settings, merged.toList(), selectedOut),
      summary = Summary(
        tileCount = settings.homeActionIds.size + settings.vehicleActionIds.size,
        wearActionCount = settings.wearActionIds.size,
        vehiclesNew = added,
        vehiclesUpdated = updated,
        drops = drops,
        wmiWarnings = wmi,
        checkDigitWarnings = checks,
        criticalOverlap = overlap,
      ),
    )
  }

  private fun warn(draft: Draft, wmi: MutableList<String>, checks: MutableList<String>) {
    if (draft.vin.take(3) !in TESLA_WMI) wmi += draft.label
    if (!checkDigitMatches(draft.vin)) checks += draft.label
  }

  private fun actionsOf(id: String): Set<String> = TILE_ACTIONS[id] ?: setOf(id)

  private fun freshId(used: Set<String>, newId: () -> String): String {
    repeat(8) {
      val id = newId()
      if (id.isNotBlank() && id != DemoCar.VEHICLE_ID && id !in used) return id
    }
    var n = 0
    while (true) {
      val id = "v$n"
      if (id !in used && id != DemoCar.VEHICLE_ID) return id
      n++
    }
  }

  /** Position 9 is informational. A mismatch does not block the import. */
  private fun checkDigitMatches(vin: String): Boolean {
    if (vin.length != VIN_WEIGHT.size) return false
    var sum = 0
    for (i in vin.indices) {
      val c = vin[i]
      val value = if (c.isDigit()) c - '0' else VIN_VALUE[c] ?: return false
      sum += value * VIN_WEIGHT[i]
    }
    val rem = sum % 11
    val expected = if (rem == 10) 'X' else ('0' + rem)
    return vin[8] == expected
  }

  private fun readPatch(layout: JsonObject): Patch = Patch(
    theme = enumField<ThemeMode>(layout, "theme", ThemeMode.DARK),
    design = enumField<UiDesign>(layout, "design", UiDesign.NEON),
    dynamicColor = boolField(layout, "dynamicColor"),
    car3d = boolField(layout, "car3d"),
    directionFx = boolField(layout, "directionFx"),
    haptics = boolField(layout, "haptics"),
    tileColumns = intField(layout, "tileColumns")?.let { clampColumns(it) },
    multiTiles = boolField(layout, "multiTiles"),
    homeActionIds = listField(layout, "homeActionIds")?.let { filterIds(it, MAX_IDS, tiles = true) },
    vehicleActionIds = listField(layout, "vehicleActionIds")?.let { filterIds(it, MAX_IDS, tiles = true) },
    launcherMenuActionIds = listField(layout, "launcherMenuActionIds")?.let { filterIds(it, MAX_LAUNCHER, tiles = false) },
    routineActionIds = listField(layout, "routineActionIds")?.let { filterIds(it, MAX_IDS, tiles = false) },
    widgetActionIds = listField(layout, "widgetActionIds")?.let { filterIds(it, MAX_IDS, tiles = false) },
    widgetDesign = enumField<WidgetDesign>(layout, "widgetDesign", WidgetDesign.APP),
    widgetTranslucent = boolField(layout, "widgetTranslucent"),
    wearActionIds = listField(layout, "wearActionIds")?.let { filterIds(it, MAX_IDS, tiles = false) },
    wearGrid = intField(layout, "wearGrid")?.let { clampGrid(it) },
    wearComplicationIds = listField(layout, "wearComplicationIds")?.let {
      filterIds(it, WearContract.COMPLICATION_SLOTS, tiles = false)
    },
  )

  private fun readVehicles(raw: List<BackupVehicle>): VehicleBatch {
    val accepted = mutableListOf<Draft>()
    val drops = mutableListOf<Drop>()
    val seen = HashSet<String>()
    for (item in raw) {
      val name = cleanName(item.name)
      val vin = Vin.normalize(item.vin)
      val label = labelOf(name, vin.ifBlank { item.vin.trim() })
      if (!Vin.isValid(vin)) {
        drops += Drop(label, DropReason.INVALID)
        continue
      }
      if (!seen.add(vin)) {
        drops += Drop(label, DropReason.DUPLICATE)
        continue
      }
      if (accepted.size >= MAX_VEHICLES) {
        drops += Drop(label, DropReason.FILE_LIMIT)
        continue
      }
      accepted += Draft(
        name = name,
        vin = vin,
        bodyStyle = item.bodyStyle.trim().takeIf { it in BODY_STYLES }.orEmpty(),
        paint = item.paint.trim().takeIf { it in PAINTS }.orEmpty(),
        neonColor = item.neonColor.trim().takeIf { it in NEONS }.orEmpty(),
        label = label,
      )
    }
    return VehicleBatch(accepted, drops)
  }

  private fun filterIds(raw: List<String>, limit: Int, tiles: Boolean): List<String> {
    val out = LinkedHashSet<String>()
    for (item in raw) {
      if (out.size >= limit) break
      val id = if (tiles) TILE_ALIAS[item] ?: item else item
      if (id.isBlank()) continue
      val known = if (tiles) id in TILE_ACTIONS || AtaAction.fromId(id) != null else AtaAction.fromId(id) != null
      if (known) out += id
    }
    return out.toList()
  }

  /** 0 = automatic, 2 … 6 = fixed. 1 is not a choice in the UI. */
  private fun clampColumns(n: Int): Int = when {
    n <= 0 -> 0
    n == 1 -> 2
    n > 6 -> 6
    else -> n
  }

  /** Only 4 and 7 are real watch layouts. Anything above 7 becomes 7, everything else 4. */
  private fun clampGrid(n: Int): Int = when {
    n >= WearContract.GRID_LARGE -> WearContract.GRID_LARGE
    else -> WearContract.GRID_SMALL
  }

  private fun cleanName(raw: String): String = raw.trim().take(MAX_NAME)

  private fun labelOf(name: String, vin: String): String = when {
    name.isNotBlank() && vin.isNotBlank() -> "$name ($vin)"
    name.isNotBlank() -> name
    vin.isNotBlank() -> vin
    else -> "?"
  }

  private inline fun <reified T : Enum<T>> enumField(obj: JsonObject, key: String, default: T): T? {
    if (key !in obj) return null
    val el = obj[key] ?: return null
    if (el !is JsonPrimitive || !el.isString) throw BadFile()
    return enumValues<T>().firstOrNull { it.name == el.content } ?: default
  }

  private fun boolField(obj: JsonObject, key: String): Boolean? {
    if (key !in obj) return null
    val el = obj[key] ?: return null
    if (el !is JsonPrimitive) throw BadFile()
    return el.booleanOrNull ?: throw BadFile()
  }

  private fun intField(obj: JsonObject, key: String): Int? {
    if (key !in obj) return null
    val el = obj[key] ?: return null
    if (el !is JsonPrimitive) throw BadFile()
    return el.intOrNull ?: throw BadFile()
  }

  private fun listField(obj: JsonObject, key: String): List<String>? {
    if (key !in obj) return null
    val el = obj[key] ?: return null
    if (el !is JsonArray) throw BadFile()
    return el.map { item ->
      if (item !is JsonPrimitive || !item.isString) throw BadFile()
      item.content
    }
  }

  private data class Patch(
    val theme: ThemeMode?,
    val design: UiDesign?,
    val dynamicColor: Boolean?,
    val car3d: Boolean?,
    val directionFx: Boolean?,
    val haptics: Boolean?,
    val tileColumns: Int?,
    val multiTiles: Boolean?,
    val homeActionIds: List<String>?,
    val vehicleActionIds: List<String>?,
    val launcherMenuActionIds: List<String>?,
    val routineActionIds: List<String>?,
    val widgetActionIds: List<String>?,
    val widgetDesign: WidgetDesign?,
    val widgetTranslucent: Boolean?,
    val wearActionIds: List<String>?,
    val wearGrid: Int?,
    val wearComplicationIds: List<String>?,
  )

  private data class Draft(
    val name: String,
    val vin: String,
    val bodyStyle: String,
    val paint: String,
    val neonColor: String,
    val label: String,
  )

  private data class VehicleBatch(val accepted: List<Draft>, val drops: List<Drop>)

  private class BadFile : IllegalArgumentException()

  @Serializable
  private data class BackupFile(
    val format: String,
    val schema: Int,
    val app: String,
    val created: String,
    val layout: BackupLayout,
    val vehicles: List<BackupVehicle>,
    val selectedVin: String,
  )

  @Serializable
  private data class BackupLayout(
    val theme: String,
    val design: String,
    val dynamicColor: Boolean,
    val car3d: Boolean,
    val directionFx: Boolean,
    val haptics: Boolean,
    val tileColumns: Int,
    val multiTiles: Boolean,
    val homeActionIds: List<String>,
    val vehicleActionIds: List<String>,
    val launcherMenuActionIds: List<String>,
    val routineActionIds: List<String>,
    val widgetActionIds: List<String>,
    val widgetDesign: String,
    val widgetTranslucent: Boolean,
    val wearActionIds: List<String>,
    val wearGrid: Int,
    val wearComplicationIds: List<String>,
  )

  @Serializable
  private data class BackupVehicle(
    val name: String = "",
    val vin: String = "",
    val bodyStyle: String = "",
    val paint: String = "",
    val neonColor: String = "",
  )

  @Serializable
  private data class Envelope(
    val format: String = "",
    val schema: Int = 0,
    val app: String = "",
    val created: String = "",
    val layout: JsonObject = JsonObject(emptyMap()),
    val vehicles: List<BackupVehicle> = emptyList(),
    val selectedVin: String = "",
  )
}
