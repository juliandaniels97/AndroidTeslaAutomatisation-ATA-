package org.tfv.ata.wear

import android.content.Context
import android.os.Build
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.tfv.ata.core.WatchState
import org.tfv.ata.core.WearContract
import org.tfv.ata.core.ble.BleLog
import org.tfv.ata.core.ble.BleTransport
import org.tfv.ata.core.ble.PairOutcome
import org.tfv.ata.core.ble.PairedKeys
import org.tfv.ata.core.ble.TeslaController

/**
 * Commands from the phone's watch-pairing assistant: report the watch's setup, test the connection to the car
 * or pair the watch's own key. Every step is reported back, including the watch's Bluetooth log.
 * The Data Layer only delivers messages from apps signed with the same key.
 */
class WatchCommandListener : WearableListenerService() {
  /** Keeps the car synced from the phone even while ATA isn't open on the watch. */
  override fun onDataChanged(events: DataEventBuffer) {
    for (event in events) {
      if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == WearContract.PATH_CONFIG) {
        storeVehicle(applicationContext, event.dataItem)
      }
    }
  }

  override fun onMessageReceived(event: MessageEvent) {
    if (event.path != WearContract.MSG_WATCH_COMMAND) return
    val phone = event.sourceNodeId
    val context = applicationContext
    // Runs on the listener's background thread; blocking keeps the service alive until the run ends.
    runBlocking {
      syncVehicle(context)
      when (String(event.data, Charsets.UTF_8).trim()) {
        WearContract.WATCH_CMD_STATE -> send(context, phone, state(context))
        WearContract.WATCH_CMD_DIRECT_ON -> {
          if (WearControl.directSupported) WearPrefs.setDirectMode(context, true)
          send(context, phone, state(context))
        }
        WearContract.WATCH_CMD_DIRECT_OFF -> {
          WearPrefs.setDirectMode(context, false)
          send(context, phone, state(context))
        }
        WearContract.WATCH_CMD_KEYCARD_ON -> {
          if (org.tfv.ata.core.nfc.NfcInfo.hasCardEmulation(context)) WearPrefs.setKeyCard(context, true)
          send(context, phone, state(context))
        }
        WearContract.WATCH_CMD_KEYCARD_OFF -> {
          WearPrefs.setKeyCard(context, false)
          send(context, phone, state(context))
        }
        WearContract.WATCH_CMD_PROBE -> probe(context, phone)
        WearContract.WATCH_CMD_PAIR -> pair(context, phone)
      }
    }
  }

  private suspend fun probe(context: Context, phone: String) {
    val blocked = blocker(context)
    if (blocked != null) {
      send(context, phone, state(context).copy(probe = "fail", code = blocked))
      return
    }
    send(context, phone, state(context).copy(running = "probe"))
    val result = withContext(Dispatchers.IO) { TeslaController.probe(context, WearPrefs.vin(context)) }
    val probe = when {
      !result.reached -> "fail"
      result.paired == true -> "paired"
      else -> "ok"
    }
    send(context, phone, state(context).copy(probe = probe, code = result.code?.name.orEmpty()))
  }

  private suspend fun pair(context: Context, phone: String) {
    val blocked = blocker(context)
    if (blocked != null) {
      send(context, phone, state(context).copy(outcome = PairOutcome.FAILED.name, code = blocked))
      return
    }
    send(context, phone, state(context).copy(running = "pair", step = "SEARCHING"))
    var last = ""
    val result = withContext(Dispatchers.IO) {
      TeslaController.pairKey(context, WearPrefs.vin(context)) { step ->
        last = step.name
        // Fire and forget: the next state message carries the full picture anyway.
        Wearable.getMessageClient(context).sendMessage(
          phone,
          WearContract.MSG_WATCH_STATE,
          state(context).copy(running = "pair", step = step.name).encode().toByteArray(),
        )
      }
    }
    // A paired watch controls the car directly from now on.
    if (result.outcome == PairOutcome.ADDED) WearPrefs.setDirectMode(context, true)
    send(context, phone, state(context).copy(step = last, outcome = result.outcome.name, code = result.code?.name.orEmpty()))
  }

  /** Reads the latest config the phone published, in case a change arrived while nothing was listening. */
  private suspend fun syncVehicle(context: Context) {
    runCatching {
      val buffer = com.google.android.gms.tasks.Tasks.await(Wearable.getDataClient(context).dataItems)
      buffer.filter { it.uri.path == WearContract.PATH_CONFIG }.forEach { storeVehicle(context, it) }
      buffer.release()
    }
  }

  private fun storeVehicle(context: Context, item: DataItem) {
    val map = DataMapItem.fromDataItem(item).dataMap
    val vin = map.getString(WearContract.KEY_VIN).orEmpty()
    if (vin.isNotBlank()) WearPrefs.setVehicle(context, vin, map.getString(WearContract.KEY_VEHICLE).orEmpty())
  }

  /** Why a run can't start on this watch, as a FailureCode name; null if it can. */
  private fun blocker(context: Context): String? = when {
    !WearControl.directSupported -> "UNSUPPORTED_DEVICE"
    !WearControl.hasBlePermission(context) -> "PERMISSION"
    !BleTransport.isBluetoothOn(context) -> "BLUETOOTH_OFF"
    WearPrefs.vin(context).isBlank() -> "OTHER"
    else -> null
  }

  private fun state(context: Context) = WatchState(
    model = "${Build.MANUFACTURER} ${Build.MODEL}",
    sdk = Build.VERSION.SDK_INT,
    appVersion = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty(),
    supported = WearControl.directSupported,
    permission = WearControl.hasBlePermission(context),
    bluetoothOn = BleTransport.isBluetoothOn(context),
    locationPermission = BleTransport.hasLocationPermission(context),
    locationOn = BleTransport.isLocationOn(context),
    vin = WearPrefs.vin(context),
    directMode = WearPrefs.directMode(context),
    paired = WearPrefs.vin(context).isNotBlank() && PairedKeys.isPaired(context, WearPrefs.vin(context)),
    log = BleLog.text(),
    nfc = org.tfv.ata.core.nfc.NfcInfo.hasNfc(context),
    nfcOn = org.tfv.ata.core.nfc.NfcInfo.isEnabled(context),
    cardEmulation = org.tfv.ata.core.nfc.NfcInfo.hasCardEmulation(context),
    keyCard = WearPrefs.keyCard(context),
    nfcContactAgeSec = org.tfv.ata.core.nfc.NfcLog.lastContact.value.let {
      if (it == 0L) -1 else (System.currentTimeMillis() - it) / 1000
    },
    nfcLog = org.tfv.ata.core.nfc.NfcLog.text(context),
  )

  private suspend fun send(context: Context, phone: String, state: WatchState) {
    runCatching {
      com.google.android.gms.tasks.Tasks.await(
        Wearable.getMessageClient(context).sendMessage(phone, WearContract.MSG_WATCH_STATE, state.encode().toByteArray()),
      )
    }
  }
}
