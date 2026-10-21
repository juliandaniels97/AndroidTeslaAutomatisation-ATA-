package org.tfv.ata.ui.screens

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.widget.Toast
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tfv.ata.BuildConfig
import org.tfv.ata.R
import org.tfv.ata.automation.Notifier
import org.tfv.ata.core.CommandResult
import org.tfv.ata.core.FailureCode
import org.tfv.ata.core.Vin
import org.tfv.ata.core.ble.BleLog
import org.tfv.ata.core.ble.BleTransport
import org.tfv.ata.core.ble.PairOutcome
import org.tfv.ata.core.ble.PairStep
import org.tfv.ata.core.ble.ProbeResult
import org.tfv.ata.core.ble.TeslaController
import org.tfv.ata.core.ble.VehicleKey
import org.tfv.ata.ui.AtaUiState
import org.tfv.ata.ui.AtaViewModel
import org.tfv.ata.ui.PairUiState
import org.tfv.ata.ui.theme.AtaColors

/** Step-by-step assistant that pairs this phone as a key with the selected car. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairScreen(vm: AtaViewModel, state: AtaUiState, onBack: () -> Unit, onVehicles: () -> Unit, onReport: () -> Unit) {
  val context = LocalContext.current
  val pairState by vm.pairState.collectAsStateWithLifecycle()
  val vehicle = state.selected

  // Re-check permissions and Bluetooth whenever the user comes back from a system screen.
  var tick by remember { mutableIntStateOf(0) }
  LifecycleResumeEffect(Unit) {
    tick++
    onPauseOrDispose { }
  }
  val permissionOk = remember(tick) { TeslaController.hasBlePermission(context) }
  val bluetoothOn = remember(tick) { BleTransport.isBluetoothOn(context) }
  val locationPermOk = remember(tick) { BleTransport.hasLocationPermission(context) }
  val locationOn = remember(tick) { BleTransport.isLocationOn(context) }
  val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { tick++ }
  val bluetoothLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { tick++ }

  val supported = VehicleKey.isSupported
  val bleOn = state.settings.experimentalBle
  val vinOk = vehicle != null && Vin.isValid(Vin.normalize(vehicle.vin))
  val ready = supported && bleOn && vinOk && permissionOk && bluetoothOn && locationPermOk && locationOn

  var nearCar by rememberSaveable { mutableStateOf(false) }
  var carAwake by rememberSaveable { mutableStateOf(false) }
  var cardReady by rememberSaveable { mutableStateOf(false) }
  val running = pairState is PairUiState.Running
  val probe by vm.probe.collectAsStateWithLifecycle()
  val probing by vm.probing.collectAsStateWithLifecycle()
  val pairedVins by remember { org.tfv.ata.core.ble.PairedKeys.vins(context) }.collectAsStateWithLifecycle()
  // Already paired: no pairing steps, only the result and the connection test (re-pairing on request).
  val alreadyPaired = vehicle != null && Vin.normalize(vehicle.vin) in pairedVins.orEmpty()
  var repair by rememberSaveable { mutableStateOf(false) }
  val showPairing = !alreadyPaired || repair || running

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.pair_title)) },
        navigationIcon = {
          IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
        },
      )
    },
  ) { padding ->
    Column(
      Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(stringResource(R.string.pair_intro), style = MaterialTheme.typography.bodyMedium)

      GuideSection(stringResource(R.string.pair_section_phone))
      StatusRow(
        ok = supported,
        title = stringResource(R.string.pair_check_android),
        hint = if (supported) null else stringResource(R.string.fail_unsupported),
      )
      StatusRow(
        ok = bleOn,
        title = stringResource(R.string.pair_check_ble),
        actionLabel = stringResource(R.string.pair_action_enable),
        onAction = { vm.updateSettings { it.copy(experimentalBle = true) } },
      )
      StatusRow(
        ok = vinOk,
        title = stringResource(R.string.pair_check_vehicle),
        hint = when {
          vehicle == null -> stringResource(R.string.pair_check_vehicle_missing)
          !vinOk -> stringResource(R.string.pair_check_vin_invalid, vehicle.vin)
          else -> "${vehicle.name} · ${Vin.normalize(vehicle.vin)}"
        },
        actionLabel = stringResource(if (vehicle == null) R.string.add_now else R.string.pair_action_edit),
        onAction = onVehicles,
      )
      StatusRow(
        ok = permissionOk,
        title = stringResource(R.string.pair_check_permission),
        actionLabel = stringResource(R.string.pair_action_allow),
        onAction = {
          permissionLauncher.launch(org.tfv.ata.core.ble.TeslaController.BLE_PERMISSIONS)
        },
      )
      StatusRow(
        ok = locationPermOk,
        title = stringResource(R.string.pair_check_location_permission),
        hint = if (locationPermOk) null else stringResource(R.string.pair_location_why),
        actionLabel = stringResource(R.string.pair_action_allow),
        onAction = { permissionLauncher.launch(org.tfv.ata.core.ble.TeslaController.BLE_PERMISSIONS) },
      )
      StatusRow(
        ok = locationOn,
        title = stringResource(R.string.pair_check_location_on),
        hint = if (locationOn) null else stringResource(R.string.pair_location_on_why),
        actionLabel = stringResource(R.string.pair_action_bluetooth),
        onAction = { runCatching { bluetoothLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) } },
      )
      StatusRow(
        ok = bluetoothOn,
        title = stringResource(R.string.pair_check_bluetooth),
        actionLabel = stringResource(R.string.pair_action_bluetooth),
        onAction = {
          val intent = if (permissionOk) Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE) else Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
          runCatching { bluetoothLauncher.launch(intent) }
        },
      )

      if (alreadyPaired && !running) PairedCard(showRepair = !repair, onRepair = { repair = true })

      if (showPairing) {
        GuideSection(stringResource(R.string.pair_section_car))
        TickRow(stringResource(R.string.pair_tick_near), nearCar) { nearCar = it }
        TickRow(stringResource(R.string.pair_tick_awake), carAwake) { carAwake = it }
        TickRow(stringResource(R.string.pair_tick_card), cardReady) { cardReady = it }

        GuideSection(stringResource(R.string.pair_section_pair))
        Button(
          onClick = { vehicle?.let { vm.pairKey(it.vin) } },
          enabled = ready && nearCar && carAwake && cardReady && !running && !probing,
          modifier = Modifier.fillMaxWidth(),
        ) {
          if (running) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
          else Icon(Icons.Filled.Key, contentDescription = null)
          Text(
            stringResource(if (running) R.string.pair_in_progress else R.string.pair_start),
            modifier = Modifier.padding(start = 8.dp),
          )
        }
        if (!ready || !(nearCar && carAwake && cardReady)) {
          Text(
            stringResource(R.string.pair_start_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        PairProgress(pairState, onRetry = { vm.resetPairing() })
      }

      GuideSection(stringResource(R.string.pair_section_test))
      Text(stringResource(R.string.pair_test_desc), style = MaterialTheme.typography.bodySmall)
      OutlinedButton(
        onClick = { vehicle?.let { vm.probeVehicle(it.vin) } },
        enabled = ready && !running && !probing,
      ) {
        if (probing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        else Icon(Icons.Filled.Bluetooth, contentDescription = null)
        Text(stringResource(R.string.pair_test), modifier = Modifier.padding(start = 8.dp))
      }
      if (!probing) probe?.let { ProbeResultText(it) }

      DiagnosticsCard(BleLog.lines.collectAsStateWithLifecycle().value, onReport)

      Fingerprint()

      NfcChooserNote()
    }
  }
}

@Composable
internal fun PairProgress(
  pairState: PairUiState,
  onRetry: () -> Unit,
  addedNext: String = stringResource(R.string.pair_added_next),
) {
  val step = (pairState as? PairUiState.Running)?.step
  if (pairState is PairUiState.Idle) return
  val finished = pairState as? PairUiState.Finished
  val added = finished?.result?.outcome == PairOutcome.ADDED
  Card(modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      PairStep.entries.forEach { s ->
        val reached = step ?: finished?.reached
        ProgressLine(
          text = stringResource(
            when (s) {
              PairStep.SEARCHING -> R.string.pair_step_search
              PairStep.CONNECTING -> R.string.pair_step_connect
              PairStep.WAITING_FOR_CARD -> R.string.pair_step_card
            },
          ),
          done = added || (reached != null && s.ordinal < reached.ordinal),
          active = step == s,
          failed = finished != null && !added && reached == s,
        )
      }
      if (step == PairStep.WAITING_FOR_CARD) TapCardCallout()
      if (pairState is PairUiState.Finished) {
        val result = pairState.result
        val palette = AtaColors.current
        val (title, color) = when (result.outcome) {
          PairOutcome.ADDED -> stringResource(R.string.pair_added) to palette.success
          PairOutcome.AWAITING_CONFIRMATION -> stringResource(R.string.pair_awaiting) to palette.warning
          PairOutcome.FAILED -> stringResource(R.string.pair_failed) to palette.danger
        }
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
        val context = LocalContext.current
        val detail = when (result.outcome) {
          PairOutcome.ADDED -> addedNext
          PairOutcome.AWAITING_CONFIRMATION -> stringResource(R.string.pair_awaiting_next)
          PairOutcome.FAILED -> Notifier.failureText(context, CommandResult.Failure(result.detail, result.code ?: FailureCode.OTHER)) +
            "\n\n" + stringResource(troubleshootingFor(result.code))
        }
        Text(detail, style = MaterialTheme.typography.bodySmall)
        if (result.outcome != PairOutcome.ADDED) {
          OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.pair_retry)) }
        }
      }
    }
  }
}

/** Shown instead of the pairing steps once this phone's key works with the car. */
@Composable
private fun PairedCard(showRepair: Boolean, onRepair: () -> Unit) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(
        stringResource(R.string.pair_done_title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
      )
      Text(
        stringResource(R.string.pair_done_text),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
      )
      if (showRepair) TextButton(onClick = onRepair) { Text(stringResource(R.string.pair_repair)) }
    }
  }
}

@Composable
private fun ProbeResultText(result: ProbeResult) {
  val palette = AtaColors.current
  val context = LocalContext.current
  val text = if (result.reached) {
    stringResource(
      when (result.paired) {
        true -> R.string.pair_test_ok_paired
        else -> R.string.pair_test_ok
      },
      stringResource(if (result.locked == true) R.string.pair_test_locked else R.string.pair_test_unlocked),
    )
  } else {
    Notifier.failureText(context, CommandResult.Failure("", result.code ?: FailureCode.OTHER)) +
      "\n\n" + stringResource(troubleshootingFor(result.code))
  }
  Text(
    text,
    style = MaterialTheme.typography.bodySmall,
    color = if (result.reached) palette.success else palette.danger,
  )
}

/** Technical log of the last pairing / connection test, to copy and send when something doesn't work. */
@Composable
internal fun DiagnosticsCard(lines: List<String>, onReport: () -> Unit) {
  if (lines.isEmpty()) return
  var open by rememberSaveable { mutableStateOf(false) }
  val clipboard = LocalClipboardManager.current
  val context = LocalContext.current
  Card(modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.pair_diagnostics), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        TextButton(onClick = { open = !open }) {
          Text(stringResource(if (open) R.string.pair_diagnostics_hide else R.string.pair_diagnostics_show))
        }
      }
      Text(stringResource(R.string.pair_diagnostics_desc), style = MaterialTheme.typography.bodySmall)
      if (open) {
        SelectionContainer {
          Text(
            lines.joinToString("\n"),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 8.dp),
          )
        }
      }
      Row {
        TextButton(onClick = {
          clipboard.setText(AnnotatedString("ATA ${BuildConfig.VERSION_NAME}\n" + lines.joinToString("\n")))
          Toast.makeText(context, R.string.pair_diagnostics_copied, Toast.LENGTH_SHORT).show()
        }) { Text(stringResource(R.string.pair_diagnostics_copy)) }
        TextButton(onClick = onReport) { Text(stringResource(R.string.report_send)) }
      }
    }
  }
}

internal fun troubleshootingFor(code: FailureCode?): Int = when (code) {
  FailureCode.NOT_REACHABLE -> R.string.pair_help_not_found
  FailureCode.BLUETOOTH_OFF -> R.string.pair_help_bluetooth
  FailureCode.PERMISSION -> R.string.pair_help_permission
  FailureCode.VEHICLE_REJECTED -> R.string.pair_help_rejected
  FailureCode.NO_RESPONSE -> R.string.pair_help_no_response
  else -> R.string.pair_help_general
}

@Composable
internal fun TapCardCallout() {
  val palette = AtaColors.current
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.Filled.CreditCard, contentDescription = null, tint = palette.accent, modifier = Modifier.size(32.dp))
      Column(Modifier.padding(start = 12.dp)) {
        Text(
          stringResource(R.string.pair_tap_card_title),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
          stringResource(R.string.pair_tap_card_text),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
      }
    }
  }
}

@Composable
internal fun ProgressLine(text: String, done: Boolean, active: Boolean, failed: Boolean) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    if (active) {
      CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
    } else {
      StatusIcon(ok = done, pending = !done && !failed)
    }
    Text(
      text,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
      modifier = Modifier.padding(start = 10.dp),
    )
  }
}

@Composable
internal fun TickRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
  Row(
    Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Checkbox(checked = checked, onCheckedChange = null)
    Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 10.dp))
  }
}

@Composable
internal fun Fingerprint() {
  val unsupported = stringResource(R.string.fail_unsupported)
  val fingerprint by produceState<String?>(initialValue = null) {
    value = if (!VehicleKey.isSupported) unsupported
    else withContext(Dispatchers.IO) { runCatching { VehicleKey.publicKeyFingerprint() }.getOrNull() }
  }
  Column {
    Text(stringResource(R.string.pair_fingerprint), style = MaterialTheme.typography.labelLarge)
    SelectionContainer {
      Text(fingerprint ?: "…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
    }
  }
}

/** Explains the "Tesla or ATA" NFC chooser some users see when they hold the phone to the card reader. */
@Composable
fun NfcChooserNote(modifier: Modifier = Modifier) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(Modifier.padding(12.dp)) {
      Text(
        stringResource(R.string.nfc_chooser_title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
      )
      Text(
        stringResource(R.string.nfc_chooser_text),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.padding(top = 4.dp),
      )
    }
  }
}
