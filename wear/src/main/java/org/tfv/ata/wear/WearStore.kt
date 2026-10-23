package org.tfv.ata.wear

import android.content.ComponentName
import android.content.Context
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.tfv.ata.core.WearContract
import org.tfv.ata.wear.complication.ActionSlot1Complication
import org.tfv.ata.wear.complication.ActionSlot2Complication
import org.tfv.ata.wear.complication.ActionSlot3Complication
import org.tfv.ata.wear.complication.ActionSlot4Complication
import org.tfv.ata.wear.complication.BatteryComplication
import org.tfv.ata.wear.complication.InsideTempComplication
import org.tfv.ata.wear.complication.LockComplication
import org.tfv.ata.wear.complication.OutsideTempComplication
import org.tfv.ata.wear.complication.RangeComplication

@Serializable
data class WatchAction(val id: String, val label: String)

/** What the phone set up for the watch: actions (grid first), tile count, complication actions. */
@Serializable
data class WatchConfig(
  val actions: List<WatchAction> = emptyList(),
  val grid: Int = WearContract.GRID_SMALL,
  val complications: List<WatchAction> = emptyList(),
)

/** Last vehicle status the phone sent. */
@Serializable
data class WatchStatus(
  val vehicle: String = "",
  val battery: Int = 0,
  val range: Int = 0,
  val charging: Boolean = false,
  /** null = unknown. */
  val locked: Boolean? = null,
  val inside: Float = 0f,
  val outside: Float = 0f,
  val climate: Boolean = false,
  val updatedAt: Long = 0,
  /** Everything the phone read (charging power, time to full, energy added …); null from older phone apps. */
  val snapshot: org.tfv.ata.core.VehicleSnapshot? = null,
) {
  val known: Boolean get() = updatedAt > 0

  /** The basic values as a snapshot (phone apps before 1.9.4 send no full snapshot). */
  fun asSnapshot() = org.tfv.ata.core.VehicleSnapshot(
    batteryPercent = battery,
    rangeKm = range,
    charging = charging,
    locked = locked,
    climateOn = climate,
    insideTempC = inside,
    outsideTempC = outside,
    updatedAt = updatedAt,
  )
}

/**
 * Config and status from the phone, kept on the watch so the app, the tile and the complications show them even
 * while the phone is away. Updated by the Data Layer listener and the app.
 */
object WearStore {
  private const val PREFS = "ata_wear_store"
  private const val KEY_CONFIG = "config"
  private const val KEY_STATUS = "status"
  private val json = Json { ignoreUnknownKeys = true }

  private val _config = MutableStateFlow<WatchConfig?>(null)
  private val _status = MutableStateFlow<WatchStatus?>(null)

  fun config(context: Context): StateFlow<WatchConfig?> {
    load(context)
    return _config
  }

  fun status(context: Context): StateFlow<WatchStatus?> {
    load(context)
    return _status
  }

  fun currentConfig(context: Context): WatchConfig = config(context).value ?: WatchConfig()
  fun currentStatus(context: Context): WatchStatus = status(context).value ?: WatchStatus()

  @Volatile private var loaded = false

  private fun load(context: Context) {
    if (loaded) return
    val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    _config.value = prefs.getString(KEY_CONFIG, null)?.let { runCatching { json.decodeFromString<WatchConfig>(it) }.getOrNull() }
    _status.value = prefs.getString(KEY_STATUS, null)?.let { runCatching { json.decodeFromString<WatchStatus>(it) }.getOrNull() }
    loaded = true
  }

  /** Stores whatever [item] carries (config or status) and refreshes the complications that show it. */
  fun apply(context: Context, item: DataItem) {
    when (item.uri.path) {
      WearContract.PATH_CONFIG -> saveConfig(context, parseConfig(item))
      WearContract.PATH_STATUS -> saveStatus(context, parseStatus(item))
    }
  }

  fun saveConfig(context: Context, config: WatchConfig) {
    load(context)
    if (config == _config.value) return
    _config.value = config
    context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_CONFIG, json.encodeToString(config)).apply()
    update(context, ActionSlot1Complication::class.java, ActionSlot2Complication::class.java, ActionSlot3Complication::class.java, ActionSlot4Complication::class.java)
  }

  fun saveStatus(context: Context, status: WatchStatus) {
    load(context)
    if (status == _status.value) return
    _status.value = status
    context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_STATUS, json.encodeToString(status)).apply()
    update(
      context,
      BatteryComplication::class.java, RangeComplication::class.java, InsideTempComplication::class.java,
      OutsideTempComplication::class.java, LockComplication::class.java,
    )
  }

  private fun update(context: Context, vararg services: Class<*>) {
    for (service in services) {
      runCatching { ComplicationDataSourceUpdateRequester.create(context, ComponentName(context, service)).requestUpdateAll() }
    }
  }

  private fun parseConfig(item: DataItem): WatchConfig {
    val map = DataMapItem.fromDataItem(item).dataMap
    fun list(idsKey: String, labelsKey: String): List<WatchAction> {
      val ids = map.getStringArrayList(idsKey) ?: return emptyList()
      val labels = map.getStringArrayList(labelsKey) ?: ArrayList(ids)
      return ids.mapIndexed { i, id -> WatchAction(id, labels.getOrNull(i) ?: id) }
    }
    return WatchConfig(
      actions = list(WearContract.KEY_ACTION_IDS, WearContract.KEY_ACTION_LABELS),
      grid = map.getInt(WearContract.KEY_GRID, WearContract.GRID_SMALL),
      complications = list(WearContract.KEY_COMPLICATION_IDS, WearContract.KEY_COMPLICATION_LABELS),
    )
  }

  private fun parseStatus(item: DataItem): WatchStatus {
    val map = DataMapItem.fromDataItem(item).dataMap
    return WatchStatus(
      vehicle = map.getString(WearContract.KEY_VEHICLE) ?: "",
      battery = map.getInt(WearContract.KEY_BATTERY, 0),
      range = map.getInt(WearContract.KEY_RANGE, 0),
      charging = map.getBoolean(WearContract.KEY_CHARGING, false),
      locked = if (map.containsKey(WearContract.KEY_LOCKED)) map.getBoolean(WearContract.KEY_LOCKED) else null,
      inside = map.getFloat(WearContract.KEY_INSIDE, 0f),
      outside = map.getFloat(WearContract.KEY_OUTSIDE, 0f),
      climate = map.getBoolean(WearContract.KEY_CLIMATE, false),
      updatedAt = map.getLong(WearContract.KEY_UPDATED, 0),
      snapshot = map.getString(WearContract.KEY_SNAPSHOT)?.let {
        runCatching { json.decodeFromString(org.tfv.ata.core.VehicleSnapshot.serializer(), it) }.getOrNull()
      },
    )
  }
}
