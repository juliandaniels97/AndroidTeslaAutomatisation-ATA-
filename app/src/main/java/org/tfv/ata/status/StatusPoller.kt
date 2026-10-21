package org.tfv.ata.status

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.first
import org.tfv.ata.core.AtaSettings
import org.tfv.ata.core.VehicleSnapshot
import org.tfv.ata.core.ble.TeslaController
import org.tfv.ata.data.AtaStore
import java.util.concurrent.TimeUnit

object StatusPoller {
  private const val WORK_NAME = "ata-status-poll"
  const val MIN_INTERVAL_MINUTES = 15L

  fun hasBlePermission(context: Context): Boolean =
    arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT).all {
      ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

  suspend fun pollNow(context: Context): VehicleSnapshot? {
    val store = AtaStore.get(context)
    val settings = store.settings.first()
    if (!settings.experimentalBle || !settings.active) return null
    if (!hasBlePermission(context)) return null
    val vehicles = store.vehicles.first()
    val selectedId = store.selectedVehicleId.first()
    val vehicle = vehicles.firstOrNull { it.id == selectedId } ?: vehicles.firstOrNull() ?: return null
    val snapshot = runCatching { TeslaController.readStatus(context, vehicle.vin) }.getOrNull() ?: return null
    store.saveSnapshot(vehicle.id, snapshot)
    broadcastStatus(context, vehicle.id, snapshot)
    org.tfv.ata.wear.WearPush.pushStatus(context, snapshot, vehicle.name)
    return snapshot
  }

  private fun broadcastStatus(context: Context, vehicleId: String, snapshot: VehicleSnapshot) {
    val intent = android.content.Intent(org.tfv.ata.core.IntentContract.ACTION_STATUS).apply {
      putExtra(org.tfv.ata.core.IntentContract.EXTRA_VEHICLE_ID, vehicleId)
      putExtra(org.tfv.ata.core.IntentContract.EXTRA_BATTERY, snapshot.batteryPercent)
      putExtra(org.tfv.ata.core.IntentContract.EXTRA_RANGE_KM, snapshot.rangeKm)
      putExtra(org.tfv.ata.core.IntentContract.EXTRA_CHARGING, snapshot.charging)
      putExtra(org.tfv.ata.core.IntentContract.EXTRA_LOCKED, snapshot.locked)
      putExtra(org.tfv.ata.core.IntentContract.EXTRA_CLIMATE_ON, snapshot.climateOn)
      putExtra(org.tfv.ata.core.IntentContract.EXTRA_INSIDE_C, snapshot.insideTempC)
      putExtra(org.tfv.ata.core.IntentContract.EXTRA_OUTSIDE_C, snapshot.outsideTempC)
    }
    runCatching { context.sendBroadcast(intent) }
  }

  fun sync(context: Context, settings: AtaSettings) {
    val wm = WorkManager.getInstance(context)
    val enabled = settings.experimentalBle && settings.active && settings.pollIntervalMinutes > 0
    if (!enabled) {
      wm.cancelUniqueWork(WORK_NAME)
      return
    }
    val minutes = maxOf(settings.pollIntervalMinutes.toLong(), MIN_INTERVAL_MINUTES)
    val request = PeriodicWorkRequestBuilder<StatusPollWorker>(minutes, TimeUnit.MINUTES).build()
    wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
  }
}
