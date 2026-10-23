package org.tfv.ata.status

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.first
import org.tfv.ata.core.AtaSettings
import org.tfv.ata.core.DemoCar
import org.tfv.ata.core.StatusMode
import org.tfv.ata.core.projected
import org.tfv.ata.core.VehicleSnapshot
import org.tfv.ata.core.ble.TeslaController
import org.tfv.ata.data.AtaStore
import java.util.concurrent.TimeUnit
import androidx.glance.appwidget.updateAll

object StatusPoller {
  private const val OLD_WORK_NAME = "ata-status-poll"
  private const val WORK_NAME = "ata-status-next"

  fun hasBlePermission(context: Context): Boolean =
    arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT).all {
      ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

  /**
   * Reads the car status over BLE. Background polls ([wake] = false) never wake the car: if it sleeps only the
   * lock state is refreshed and battery/range are kept from the previous reading.
   */
  suspend fun pollNow(context: Context, wake: Boolean = false): VehicleSnapshot? {
    val store = AtaStore.get(context)
    val settings = store.settings.first()
    if (settings.demoMode) return pollDemo(context)
    if (!settings.experimentalBle || !settings.active) return null
    if (!hasBlePermission(context)) return null
    val vehicles = store.vehicles.first()
    val selectedId = store.selectedVehicleId.first()
    val vehicle = vehicles.firstOrNull { it.id == selectedId } ?: vehicles.firstOrNull() ?: return null
    val fresh = runCatching { TeslaController.readStatus(context, vehicle.vin, wake) }.getOrNull() ?: return null
    val snapshot = if (fresh.asleep) {
      store.snapshot(vehicle.id).first()
        ?.projected(fresh.updatedAt)
        ?.copy(locked = fresh.locked, asleep = true, updatedAt = fresh.updatedAt)
        ?: fresh
    } else {
      fresh
    }
    store.saveSnapshot(vehicle.id, snapshot)
    // The car answered over Bluetooth, so the phone is within a few metres of it: remember where it is parked.
    rememberCarLocation(context)
    if (settings.statusBroadcast) broadcastStatus(context, vehicle.id, snapshot)
    org.tfv.ata.wear.WearPush.pushStatus(context, snapshot, vehicle.name)
    return snapshot
  }

  /** Demo mode: the made-up car "answers" with its charging state carried forward to now. */
  private suspend fun pollDemo(context: Context): VehicleSnapshot {
    val store = AtaStore.get(context)
    val now = System.currentTimeMillis()
    val snapshot = (store.snapshot(DemoCar.VEHICLE_ID).first() ?: DemoCar.start(now)).projected(now).copy(updatedAt = now, asleep = false)
    store.saveSnapshot(DemoCar.VEHICLE_ID, snapshot)
    org.tfv.ata.wear.WearPush.pushStatus(context, snapshot, DemoCar.vehicle.name)
    return snapshot
  }

  /** Stores the phone's current position as the car's position (call only right after reaching the car). */
  suspend fun rememberCarLocation(context: Context) {
    val loc = org.tfv.ata.geo.Geo.lastLocation(context) ?: return
    // Old fixes (e.g. from home) would put the car in the wrong place.
    if (System.currentTimeMillis() - loc.time > 2 * 60_000L) return
    AtaStore.get(context).updateSettings {
      it.copy(carLat = loc.latitude, carLng = loc.longitude, carLocAt = System.currentTimeMillis())
    }
    runCatching { org.tfv.ata.widget.CarFinderWidget().updateAll(context) }
  }

  private fun broadcastStatus(context: Context, vehicleId: String, snapshot: VehicleSnapshot) {
    val intent = android.content.Intent(org.tfv.ata.core.IntentContract.ACTION_STATUS).apply {
      putExtra(org.tfv.ata.core.IntentContract.EXTRA_VEHICLE_ID, vehicleId)
      putExtra(org.tfv.ata.core.IntentContract.EXTRA_BATTERY, snapshot.batteryPercent)
      putExtra(org.tfv.ata.core.IntentContract.EXTRA_RANGE_KM, snapshot.rangeKm)
      putExtra(org.tfv.ata.core.IntentContract.EXTRA_CHARGING, snapshot.charging)
      snapshot.locked?.let { putExtra(org.tfv.ata.core.IntentContract.EXTRA_LOCKED, it) }
      putExtra(org.tfv.ata.core.IntentContract.EXTRA_CLIMATE_ON, snapshot.climateOn)
      putExtra(org.tfv.ata.core.IntentContract.EXTRA_INSIDE_C, snapshot.insideTempC)
      putExtra(org.tfv.ata.core.IntentContract.EXTRA_OUTSIDE_C, snapshot.outsideTempC)
    }
    runCatching { context.sendBroadcast(intent) }
  }

  /**
   * Plans the next background read for [AtaSettings.statusMode] (none for battery saver or the demo). Each read
   * plans the following one, so intervals below WorkManager's 15-minute minimum work too.
   */
  fun sync(context: Context, settings: AtaSettings) {
    val wm = WorkManager.getInstance(context)
    wm.cancelUniqueWork(OLD_WORK_NAME)
    val minutes = minutesFor(settings)
    if (minutes == null) {
      wm.cancelUniqueWork(WORK_NAME)
      return
    }
    schedule(context, minutes, ExistingWorkPolicy.REPLACE)
  }

  /** Minutes until the next read, or null when nothing reads in the background. */
  fun minutesFor(settings: AtaSettings): Int? {
    if (settings.demoMode || !settings.experimentalBle || !settings.active) return null
    return when (settings.statusMode) {
      StatusMode.BATTERY_SAVER -> null
      StatusMode.SMART -> settings.smartMinutes.coerceIn(5, 180)
      StatusMode.INTERVAL -> settings.intervalMinutes.coerceIn(5, 180)
    }
  }

  fun schedule(context: Context, minutes: Int, policy: ExistingWorkPolicy) {
    val request = OneTimeWorkRequestBuilder<StatusPollWorker>().setInitialDelay(minutes.toLong(), TimeUnit.MINUTES).build()
    WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, policy, request)
  }

  /**
   * Whether a planned read should run now: interval always; smart only while the phone is in use and the car was
   * plugged in at the last reading.
   */
  suspend fun shouldReadNow(context: Context, settings: AtaSettings): Boolean = when (settings.statusMode) {
    StatusMode.INTERVAL -> true
    StatusMode.SMART -> {
      val power = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
      val store = AtaStore.get(context)
      val vehicles = store.vehicles.first()
      val id = store.selectedVehicleId.first()
      val vehicle = vehicles.firstOrNull { it.id == id } ?: vehicles.firstOrNull()
      val last = vehicle?.let { store.snapshot(it.id).first() }
      power.isInteractive && (last?.pluggedIn == true || last?.charging == true)
    }
    StatusMode.BATTERY_SAVER -> false
  }
}
