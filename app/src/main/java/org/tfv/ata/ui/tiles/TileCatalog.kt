package org.tfv.ata.ui.tiles

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AirlineSeatReclineNormal
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Masks
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Window
import androidx.compose.ui.graphics.vector.ImageVector
import org.tfv.ata.R
import org.tfv.ata.core.AtaAction
import org.tfv.ata.core.AtaCategory
import org.tfv.ata.ui.components.SteeringWheelIcon

/**
 * Tiles that bundle several actions of one kind. Only the buttons, level dots and sliders inside are tappable.
 * [w] × [h] is the size in grid cells (at most 2 × 2).
 */
enum class MultiTile(
  val id: String,
  val w: Int,
  val h: Int,
  val labelRes: Int,
  val actions: List<AtaAction>,
) {
  LOCK("m_lock", 2, 1, R.string.mt_lock, listOf(AtaAction.LOCK, AtaAction.UNLOCK)),
  CLIMATE(
    "m_climate", 2, 2, R.string.mt_climate,
    listOf(
      AtaAction.CLIMATE_ON, AtaAction.CLIMATE_OFF, AtaAction.SET_TEMPERATURE,
      AtaAction.SET_TEMPERATURE_DRIVER, AtaAction.SET_TEMPERATURE_PASSENGER, AtaAction.DEFROST, AtaAction.DEFROST_OFF,
    ),
  ),
  /** Seat heating and ventilation in one tile (switch inside), with the front seats' automatic mode. */
  SEATS(
    "m_seats", 2, 2, R.string.mt_seats_climate,
    listOf(
      AtaAction.SEAT_HEATER_FRONT_LEFT, AtaAction.SEAT_HEATER_FRONT_RIGHT, AtaAction.SEAT_HEATER_REAR_LEFT,
      AtaAction.SEAT_HEATER_REAR_CENTER, AtaAction.SEAT_HEATER_REAR_RIGHT,
      AtaAction.SEAT_COOLER_FRONT_LEFT, AtaAction.SEAT_COOLER_FRONT_RIGHT,
      AtaAction.SEAT_AUTO_FRONT_LEFT, AtaAction.SEAT_AUTO_FRONT_RIGHT,
    ),
  ),
  CHARGING(
    "m_charging", 2, 2, R.string.mt_charging,
    listOf(AtaAction.CHARGE_START, AtaAction.CHARGE_STOP, AtaAction.CHARGE_LIMIT, AtaAction.CHARGING_AMPS),
  ),
  LIDS("m_lids", 2, 1, R.string.mt_lids, listOf(AtaAction.FRUNK, AtaAction.TRUNK, AtaAction.TRUNK_CLOSE)),
  DOORS("m_doors", 2, 1, R.string.mt_doors, listOf(AtaAction.DOOR_DRIVER_OPEN, AtaAction.DOOR_PASSENGER_OPEN)),
  WINDOWS("m_windows", 2, 1, R.string.mt_windows, listOf(AtaAction.VENT_WINDOWS, AtaAction.CLOSE_WINDOWS)),
  CHARGE_PORT("m_charge_port", 2, 1, R.string.mt_charge_port, listOf(AtaAction.CHARGE_PORT_OPEN, AtaAction.CHARGE_PORT_CLOSE)),
  VOLUME("m_volume", 2, 1, R.string.mt_volume, listOf(AtaAction.SET_VOLUME, AtaAction.VOLUME_UP, AtaAction.VOLUME_DOWN)),
  MEDIA("m_media", 2, 1, R.string.mt_media, listOf(AtaAction.MEDIA_PREVIOUS, AtaAction.MEDIA_TOGGLE, AtaAction.MEDIA_NEXT)),
  SENTRY("m_sentry", 1, 1, R.string.mt_sentry, listOf(AtaAction.SENTRY_ON, AtaAction.SENTRY_OFF)),
  STEERING("m_steering", 1, 1, R.string.mt_steering, listOf(AtaAction.STEERING_WHEEL_HEATER, AtaAction.STEERING_WHEEL_HEATER_OFF)),
  KEEPER(
    "m_keeper", 2, 1, R.string.mt_keeper,
    listOf(AtaAction.CLIMATE_KEEP_OFF, AtaAction.CLIMATE_KEEP, AtaAction.DOG_MODE, AtaAction.CAMP_MODE),
  ),
  OVERHEAT(
    "m_overheat", 2, 1, R.string.mt_overheat,
    listOf(AtaAction.OVERHEAT_PROTECTION_OFF, AtaAction.OVERHEAT_PROTECTION_ON, AtaAction.OVERHEAT_PROTECTION_FAN_ONLY),
  ),
  BIOWEAPON("m_bioweapon", 1, 1, R.string.mt_bioweapon, listOf(AtaAction.BIOWEAPON_ON, AtaAction.BIOWEAPON_OFF)),
  ;

  val icon: ImageVector
    get() = when (this) {
      LOCK -> Icons.Filled.Lock
      CLIMATE -> Icons.Filled.Thermostat
      SEATS -> Icons.Filled.AirlineSeatReclineNormal
      CHARGING -> Icons.Filled.EvStation
      LIDS -> Icons.Filled.Inventory2
      DOORS -> Icons.Filled.DoorFront
      WINDOWS -> Icons.Filled.Window
      CHARGE_PORT -> Icons.Filled.Power
      VOLUME -> Icons.Filled.VolumeUp
      MEDIA -> Icons.Filled.MusicNote
      SENTRY -> Icons.Filled.Security
      STEERING -> SteeringWheelIcon
      KEEPER -> Icons.Filled.Pets
      OVERHEAT -> Icons.Filled.DeviceThermostat
      BIOWEAPON -> Icons.Filled.Masks
    }

  companion object {
    /** Ids of tiles that were merged into others ("m_seat_cool" → seats since 1.9.3). */
    private val aliases = mapOf("m_seat_cool" to "m_seats")
    fun fromId(id: String): MultiTile? = (aliases[id] ?: id).let { key -> entries.firstOrNull { it.id == key } }
    fun covering(action: AtaAction): MultiTile? = entries.firstOrNull { action in it.actions }
  }
}

/** One tile of the grid: a single action or a [MultiTile]. */
sealed interface TileSpec {
  val key: String
  val w: Int
  val h: Int

  data class Single(val action: AtaAction) : TileSpec {
    override val key get() = action.id
    override val w get() = 1
    override val h get() = 1
  }

  data class Multi(val tile: MultiTile) : TileSpec {
    override val key get() = tile.id
    override val w get() = tile.w
    override val h get() = tile.h
  }
}

object TileCatalog {
  private val ble = AtaAction.entries.filter { it.category == AtaCategory.FUNCTION_A && it.enabled }
  private val basic = AtaAction.entries.filter { it.category == AtaCategory.FUNCTION_B && it.enabled }

  /** Actions-tab items that end up at the end of the vehicle tab. */
  val basicDefaults = basic.filter { !it.beta }

  /** Vehicle tab: car functions first, then the app actions (navigation, Tesla app …). */
  val vehicleDefaults: List<String> =
    MultiTile.entries.map { it.id } +
      ble.filter { MultiTile.covering(it) == null }.map { it.id } +
      basicDefaults.map { it.id }

  /** Tiles added for existing layouts on the update to 1.9.2. */
  val newIn192: List<String> = listOf(MultiTile.SEATS.id, MultiTile.VOLUME.id)

  val favoriteDefaults: List<String> =
    listOf(MultiTile.LOCK.id, MultiTile.CLIMATE.id, MultiTile.LIDS.id) + basicDefaults.map { it.id }

  /** Every tile the grid can show in the given mode. */
  fun all(multi: Boolean): List<TileSpec> =
    if (multi) {
      MultiTile.entries.map { TileSpec.Multi(it) } +
        (ble + basic).filter { MultiTile.covering(it) == null }.map { TileSpec.Single(it) }
    } else {
      (ble + basic).map { TileSpec.Single(it) }
    }

  /**
   * Turns stored ids into tiles. With [multi], actions of a multi-tile collapse into that tile (at the place of
   * the first one); without, a multi-tile expands into its actions. So one stored order serves both modes.
   */
  fun resolve(ids: List<String>, multi: Boolean): List<TileSpec> {
    val out = LinkedHashMap<String, TileSpec>()
    ids.forEach { id ->
      val mt = MultiTile.fromId(id)
      val action = AtaAction.fromId(id)?.takeIf { it.enabled }
      when {
        mt != null && multi -> out.putIfAbsent(mt.id, TileSpec.Multi(mt))
        mt != null -> mt.actions.filter { it.enabled }.forEach { out.putIfAbsent(it.id, TileSpec.Single(it)) }
        action == null -> Unit
        multi && MultiTile.covering(action) != null -> MultiTile.covering(action)!!.let { out.putIfAbsent(it.id, TileSpec.Multi(it)) }
        else -> out.putIfAbsent(action.id, TileSpec.Single(action))
      }
    }
    return out.values.toList()
  }

  /** Ids that stand for [key]: the tile itself and, for a multi-tile, its actions (saved without multi-tiles). */
  private fun idsFor(key: String): Set<String> =
    setOf(key) + (MultiTile.fromId(key)?.actions?.map { it.id } ?: emptyList()) +
      (AtaAction.fromId(key)?.let { MultiTile.covering(it)?.id }?.let { listOf(it) } ?: emptyList())

  fun contains(keys: List<String>, key: String): Boolean = keys.any { it in idsFor(key) }

  /** [keys] with [key] added, or removed (with everything standing for it) when it is already there. */
  fun toggle(keys: List<String>, key: String): List<String> {
    val ids = idsFor(key)
    return if (keys.any { it in ids }) keys.filter { it !in ids } else keys + key
  }

  /** Whether [spec] needs Bluetooth to the car. */
  fun needsCar(spec: TileSpec): Boolean = when (spec) {
    is TileSpec.Single -> spec.action.category == AtaCategory.FUNCTION_A
    is TileSpec.Multi -> true
  }
}
