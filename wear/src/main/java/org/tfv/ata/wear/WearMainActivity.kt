package org.tfv.ata.wear

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageClient
import org.tfv.ata.core.ble.PairOutcome
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.tfv.ata.core.WearContract

private data class WearStatus(
  val vehicle: String,
  val battery: Int,
  val range: Int,
  val charging: Boolean,
  /** null = unknown. */
  val locked: Boolean?,
  val updatedAt: Long,
)

private const val CONFIRM_WINDOW_MS = 5_000L

class WearMainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { WearApp() }
  }
}

@Composable
private fun WearApp() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var actions by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
  var status by remember { mutableStateOf<WearStatus?>(null) }
  var vin by remember { mutableStateOf(WearPrefs.vin(context)) }
  var directMode by remember { mutableStateOf(WearPrefs.directMode(context)) }
  // Critical actions (unlock, trunk …) need a second tap within a few seconds.
  var armed by remember { mutableStateOf<String?>(null) }
  var armedAt by remember { mutableStateOf(0L) }

  var pendingAfterPerm by remember { mutableStateOf<(() -> Unit)?>(null) }
  val blePermLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions(),
  ) { result ->
    val run = pendingAfterPerm
    pendingAfterPerm = null
    if (WearControl.hasBlePermission(context)) run?.invoke()
  }
  fun withBle(block: () -> Unit) {
    if (WearControl.hasBlePermission(context)) {
      block()
    } else {
      pendingAfterPerm = block
      blePermLauncher.launch(org.tfv.ata.core.ble.TeslaController.BLE_PERMISSIONS)
    }
  }

  // Ask for Bluetooth right away once a car is synced: the phone's watch-pairing assistant needs it.
  LaunchedEffect(vin) {
    if (vin.isNotBlank() && WearControl.directSupported &&
      (!WearControl.hasBlePermission(context) || !org.tfv.ata.core.ble.BleTransport.hasLocationPermission(context))
    ) {
      blePermLauncher.launch(org.tfv.ata.core.ble.TeslaController.BLE_PERMISSIONS)
    }
  }

  DisposableEffect(Unit) {
    val client = Wearable.getDataClient(context)
    fun applyConfig(item: DataItem) {
      actions = parseConfig(item)
      val map = DataMapItem.fromDataItem(item).dataMap
      WearPrefs.setVehicle(context, map.getString(WearContract.KEY_VIN).orEmpty(), map.getString(WearContract.KEY_VEHICLE).orEmpty())
      vin = WearPrefs.vin(context)
    }
    val listener = com.google.android.gms.wearable.DataClient.OnDataChangedListener { events: DataEventBuffer ->
      for (event in events) {
        if (event.type != DataEvent.TYPE_CHANGED) continue
        when (event.dataItem.uri.path) {
          WearContract.PATH_CONFIG -> applyConfig(event.dataItem)
          WearContract.PATH_STATUS -> status = parseStatus(event.dataItem)
        }
      }
    }
    client.addListener(listener)
    val messages = Wearable.getMessageClient(context)
    val resultListener = MessageClient.OnMessageReceivedListener { event ->
      if (event.path == WearContract.MSG_RESULT) {
        val text = String(event.data, Charsets.UTF_8)
        val ok = text.startsWith("1|")
        Toast.makeText(context, (if (ok) "✓ " else "✗ ") + text.substringAfter('|'), Toast.LENGTH_LONG).show()
      }
    }
    messages.addListener(resultListener)
    scope.launch {
      runCatching {
        val buffer = client.dataItems.await()
        for (item in buffer) {
          when (item.uri.path) {
            WearContract.PATH_CONFIG -> applyConfig(item)
            WearContract.PATH_STATUS -> status = parseStatus(item)
          }
        }
        buffer.release()
      }
    }
    onDispose {
      client.removeListener(listener)
      messages.removeListener(resultListener)
    }
  }

  MaterialTheme {
    Scaffold(timeText = { TimeText() }) {
      ScalingLazyColumn(
        state = rememberScalingLazyListState(),
        modifier = Modifier.fillMaxWidth(),
      ) {
        item {
          status?.let { s ->
            Column(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
              Text(
                text = if (s.vehicle.isNotBlank()) s.vehicle else stringResource(R.string.app_name),
                style = MaterialTheme.typography.title3,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
              )
              Text(
                text = "${s.battery}%  ·  ${s.range} km  ·  ${when (s.locked) { true -> "🔒"; false -> "🔓"; null -> "?" }}${if (s.charging) "  ⚡" else ""}",
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
              )
            }
          } ?: ListHeader { Text(stringResource(R.string.app_name)) }
        }

        if (actions.isEmpty()) {
          item {
            Text(
              stringResource(R.string.wear_configure_on_phone),
              style = MaterialTheme.typography.caption1,
              modifier = Modifier.fillMaxWidth().padding(8.dp),
              textAlign = TextAlign.Center,
            )
          }
        } else {
          items(actions) { (id, label) ->
            Chip(
              modifier = Modifier.fillMaxWidth(),
              label = { Text(if (armed == id) stringResource(R.string.wear_confirm_label, label) else label) },
              colors = ChipDefaults.primaryChipColors(),
              onClick = {
                val now = System.currentTimeMillis()
                if (WearControl.isCritical(id) && (armed != id || now - armedAt > CONFIRM_WINDOW_MS)) {
                  armed = id
                  armedAt = now
                  Toast.makeText(context, R.string.wear_tap_again, Toast.LENGTH_SHORT).show()
                } else {
                  armed = null
                  scope.launch {
                    val msg = when (WearControl.run(context, id)) {
                      WearResult.DIRECT -> R.string.wear_sent_car
                      WearResult.RELAYED -> R.string.sent_to_phone
                      WearResult.FAILED -> R.string.wear_failed
                      WearResult.WATCH_LOCKED -> R.string.wear_locked
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                  }
                }
              },
            )
          }
        }

        item { ListHeader { Text(stringResource(R.string.wear_direct_control)) } }
        item {
          Chip(
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(if (directMode) R.string.wear_direct_on else R.string.wear_direct_off)) },
            colors = if (directMode) ChipDefaults.primaryChipColors() else ChipDefaults.secondaryChipColors(),
            onClick = {
              if (directMode) {
                WearPrefs.setDirectMode(context, false)
                directMode = false
              } else if (!WearControl.directSupported) {
                Toast.makeText(context, R.string.wear_direct_unsupported, Toast.LENGTH_LONG).show()
              } else {
                withBle {
                  WearPrefs.setDirectMode(context, true)
                  directMode = true
                }
              }
            },
          )
        }
        if (vin.isNotBlank()) {
          item {
            Chip(
              modifier = Modifier.fillMaxWidth(),
              label = { Text(stringResource(R.string.wear_pair)) },
              colors = ChipDefaults.secondaryChipColors(),
              onClick = {
                withBle {
                  scope.launch {
                    val result = WearControl.pair(context)
                    Toast.makeText(
                      context,
                      when (result.outcome) {
                        PairOutcome.ADDED -> R.string.wear_paired_ok
                        PairOutcome.AWAITING_CONFIRMATION -> R.string.wear_pair_awaiting
                        PairOutcome.FAILED -> R.string.wear_pair_failed
                      },
                      Toast.LENGTH_LONG,
                    ).show()
                  }
                }
              },
            )
          }
        } else {
          item {
            Text(
              stringResource(R.string.wear_pair_hint),
              style = MaterialTheme.typography.caption2,
              modifier = Modifier.fillMaxWidth().padding(8.dp),
              textAlign = TextAlign.Center,
            )
          }
        }
      }
    }
  }
}

private fun parseConfig(item: DataItem): List<Pair<String, String>> {
  val map = DataMapItem.fromDataItem(item).dataMap
  val ids = map.getStringArrayList(WearContract.KEY_ACTION_IDS) ?: return emptyList()
  val labels = map.getStringArrayList(WearContract.KEY_ACTION_LABELS) ?: ArrayList(ids)
  return ids.mapIndexed { i, id -> id to (labels.getOrNull(i) ?: id) }
}

private fun parseStatus(item: DataItem): WearStatus {
  val map = DataMapItem.fromDataItem(item).dataMap
  return WearStatus(
    vehicle = map.getString(WearContract.KEY_VEHICLE) ?: "",
    battery = map.getInt(WearContract.KEY_BATTERY, 0),
    range = map.getInt(WearContract.KEY_RANGE, 0),
    charging = map.getBoolean(WearContract.KEY_CHARGING, false),
    locked = if (map.containsKey(WearContract.KEY_LOCKED)) map.getBoolean(WearContract.KEY_LOCKED) else null,
    updatedAt = map.getLong(WearContract.KEY_UPDATED, 0),
  )
}
