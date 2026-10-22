package org.tfv.ata.proximity

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.tfv.ata.R
import org.tfv.ata.automation.ActionRequest
import org.tfv.ata.automation.ActionRunner
import org.tfv.ata.automation.Notifier
import org.tfv.ata.core.AtaAction
import org.tfv.ata.core.AtaSettings
import org.tfv.ata.core.TriggerSource
import org.tfv.ata.core.Vehicle
import org.tfv.ata.core.Vin
import org.tfv.ata.core.ble.BleLog
import org.tfv.ata.core.ble.BleTransport
import org.tfv.ata.core.ble.TeslaController
import org.tfv.ata.core.proximity.ProximityDetector
import org.tfv.ata.data.AtaStore
import org.tfv.ata.ui.MainActivity

/**
 * Proximity unlock: watches the signal strength of the selected car's Bluetooth advertisement (filtered by its
 * BLE name, so it also works with the screen off) and unlocks once when the phone walks up to the car. If the
 * Tesla app holds the car's connection, the signal strength is read from that link instead. Runs as a
 * foreground service with a quiet notification while the setting is on.
 */
class ProximityService : Service() {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  private var watch: Job? = null
  private var ticker: Job? = null
  private var scanCallback: ScanCallback? = null
  private var probe: BluetoothGatt? = null
  private var detector: ProximityDetector? = null
  private var lastAdvert = 0L

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (!startInForeground()) {
      stopSelf()
      return START_NOT_STICKY
    }
    if (watch == null) watch = scope.launch { follow() }
    return START_STICKY
  }

  override fun onDestroy() {
    stopWatching()
    scope.cancel()
    super.onDestroy()
  }

  /** Re-arms the watch whenever the car or the distance changes; stops when the setting is turned off. */
  private suspend fun follow() {
    val store = AtaStore.get(this)
    combine(store.settings, store.vehicles, store.selectedVehicleId) { s, vehicles, id ->
      val vehicle = vehicles.firstOrNull { it.id == id } ?: vehicles.firstOrNull()
      if (shouldRun(s, vehicle)) vehicle!!.vin to s.proximityMeters else null
    }.distinctUntilChanged().collect { target ->
      stopWatching()
      if (target == null) {
        stopSelf()
        return@collect
      }
      startWatching(Vin.bleLocalName(target.first), target.first, target.second)
    }
  }

  @SuppressLint("MissingPermission") // checked in shouldRun / startInForeground
  private fun startWatching(localName: String, vin: String, meters: Int) {
    val near = ProximityDetector.rssiForMeters(meters.coerceIn(MIN_METERS, MAX_METERS))
    detector = ProximityDetector(near)
    BleLog.add("proximity: watching $localName, unlock at ~${meters} m (rssi ≥ $near)")
    val scanner = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter?.bluetoothLeScanner
    val callback = object : ScanCallback() {
      override fun onScanResult(callbackType: Int, result: ScanResult) {
        scope.launch {
          lastAdvert = System.currentTimeMillis()
          sample(result.rssi, vin)
        }
      }

      override fun onBatchScanResults(results: MutableList<ScanResult>) {
        results.forEach { onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, it) }
      }

      override fun onScanFailed(errorCode: Int) {
        BleLog.add("proximity: scan failed error=$errorCode")
      }
    }
    runCatching {
      scanner?.startScan(
        listOf(ScanFilter.Builder().setDeviceName(localName).build()),
        ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build(),
        callback,
      )
      scanCallback = callback
    }
    // Every few seconds: let the detector notice a car that went out of range, and read the signal strength from
    // an existing connection (e.g. the Tesla app's) when no advertisements arrive.
    ticker = scope.launch {
      while (isActive) {
        val now = System.currentTimeMillis()
        detector?.onTick(now)
        if (now - lastAdvert > NO_ADVERT_MS) probeConnected(localName, vin) else closeProbe()
        delay(TICK_MS)
      }
    }
  }

  private fun sample(rssi: Int, vin: String) {
    val d = detector ?: return
    if (d.onSample(rssi, System.currentTimeMillis())) {
      BleLog.add("proximity: near (rssi ${d.smoothed?.toInt()}) → unlock")
      scope.launch { ActionRunner.execute(this@ProximityService, ActionRequest(AtaAction.UNLOCK.id, TriggerSource.PROXIMITY)) }
    }
  }

  @SuppressLint("MissingPermission")
  private fun probeConnected(localName: String, vin: String) {
    val manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val device = runCatching { manager.getConnectedDevices(BluetoothProfile.GATT) }.getOrDefault(emptyList())
      .firstOrNull { it.name == localName }
    if (device == null) {
      closeProbe()
      return
    }
    val gatt = probe
    if (gatt != null) {
      runCatching { gatt.readRemoteRssi() }
      return
    }
    probe = runCatching {
      device.connectGatt(this, false, object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
          if (newState == BluetoothProfile.STATE_CONNECTED) runCatching { g.readRemoteRssi() }
          if (newState == BluetoothProfile.STATE_DISCONNECTED) scope.launch { closeProbe() }
        }

        override fun onReadRemoteRssi(g: BluetoothGatt, rssi: Int, status: Int) {
          if (status == BluetoothGatt.GATT_SUCCESS) scope.launch { sample(rssi, vin) }
        }
      }, BluetoothDevice.TRANSPORT_LE)
    }.getOrNull()
  }

  @SuppressLint("MissingPermission")
  private fun closeProbe() {
    probe?.let { runCatching { it.close() } }
    probe = null
  }

  @SuppressLint("MissingPermission")
  private fun stopWatching() {
    scanCallback?.let { cb ->
      runCatching { (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter?.bluetoothLeScanner?.stopScan(cb) }
    }
    scanCallback = null
    ticker?.cancel()
    ticker = null
    detector = null
    closeProbe()
  }

  private fun startInForeground(): Boolean {
    if (!TeslaController.hasBlePermission(this)) return false
    Notifier.ensureChannels(this)
    val open = PendingIntent.getActivity(
      this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    val notification = NotificationCompat.Builder(this, Notifier.CHANNEL_RUNNING)
      .setSmallIcon(R.drawable.ic_launcher_monochrome)
      .setContentTitle(getString(R.string.proximity_notif_title))
      .setContentText(getString(R.string.proximity_notif_text))
      .setOngoing(true)
      .setSilent(true)
      .setContentIntent(open)
      .build()
    return try {
      startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
      true
    } catch (e: Exception) {
      false
    }
  }

  companion object {
    const val MIN_METERS = 2
    const val MAX_METERS = 20
    private const val NOTIF_ID = 45
    private const val TICK_MS = 4_000L
    private const val NO_ADVERT_MS = 10_000L

    /** True when proximity unlock is on and everything it needs is there. */
    fun shouldRun(settings: AtaSettings, vehicle: Vehicle?): Boolean =
      settings.proximityUnlock && settings.experimentalBle && settings.active && vehicle != null && Vin.isValid(vehicle.vin)

    /** Starts or stops the service to match the settings (call after changes, at app start and after boot). */
    fun sync(context: Context, settings: AtaSettings, vehicle: Vehicle?) {
      val intent = Intent(context, ProximityService::class.java)
      if (shouldRun(settings, vehicle) && TeslaController.hasBlePermission(context) && BleTransport.hasLocationPermission(context)) {
        runCatching { ContextCompat.startForegroundService(context, intent) }
      } else {
        context.stopService(intent)
      }
    }
  }
}
