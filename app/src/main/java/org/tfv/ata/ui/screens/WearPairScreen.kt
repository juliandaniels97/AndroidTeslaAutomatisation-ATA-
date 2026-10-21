package org.tfv.ata.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.tfv.ata.R
import org.tfv.ata.core.FailureCode
import org.tfv.ata.core.Vin
import org.tfv.ata.core.WatchState
import org.tfv.ata.core.WearContract
import org.tfv.ata.core.ble.PairOutcome
import org.tfv.ata.core.ble.PairResult
import org.tfv.ata.core.ble.PairStep
import org.tfv.ata.debug.DebugReport
import org.tfv.ata.ui.AtaUiState
import org.tfv.ata.ui.AtaViewModel
import org.tfv.ata.ui.PairUiState
import org.tfv.ata.ui.theme.AtaColors
import org.tfv.ata.wear.WatchLink

/**
 * Pairs the watch's own key with the car, driven from the phone: the watch runs the Bluetooth part and reports
 * every step and its log back here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WearPairScreen(vm: AtaViewModel, state: AtaUiState, onBack: () -> Unit, onReport: () -> Unit) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val watch by WatchLink.state.collectAsStateWithLifecycle()
  val reach by WatchLink.reach.collectAsStateWithLifecycle()
  val watchName by WatchLink.watchName.collectAsStateWithLifecycle()
  var asked by rememberSaveable { mutableStateOf(false) }
  var started by rememberSaveable { mutableStateOf("") }

  fun refresh() = scope.launch { DebugReport.refreshWatch(context); asked = true }
  fun command(cmd: String) = scope.launch {
    started = cmd
    WatchLink.clearRun()
    if (!WatchLink.send(context, cmd)) started = ""
  }
  LaunchedEffect(Unit) { refresh() }
  // While the watch is busy, poll its state now and then in case a step message got lost.
  LaunchedEffect(watch?.running) {
    while (watch?.running?.isNotEmpty() == true) {
      delay(5_000)
      WatchLink.send(context, WearContract.WATCH_CMD_STATE)
    }
  }

  val vehicle = state.selected
  val w = watch
  val vinOnWatch = w != null && vehicle != null && Vin.normalize(w.vin) == Vin.normalize(vehicle.vin)
  val ready = w != null && w.supported && w.permission && w.locationPermission && w.locationOn && w.bluetoothOn && vinOnWatch
  val running = w?.running.orEmpty()
  var nearCar by rememberSaveable { mutableStateOf(false) }
  var cardReady by rememberSaveable { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.wear_pair_title)) },
        navigationIcon = {
          IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
        },
        actions = {
          IconButton(onClick = { refresh() }) { Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.refresh)) }
        },
      )
    },
  ) { padding ->
    Column(
      Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(stringResource(R.string.wear_pair_intro), style = MaterialTheme.typography.bodyMedium)

      GuideSection(stringResource(R.string.wear_pair_section_watch))
      StatusRow(
        ok = w != null,
        title = stringResource(R.string.wear_pair_check_reachable),
        hint = when {
          w != null -> "$watchName · ${w.model} · ATA ${w.appVersion}"
          !asked -> "…"
          reach == WatchLink.Reach.NO_WATCH -> stringResource(R.string.wear_pair_no_watch)
          else -> stringResource(R.string.wear_pair_no_app)
        },
        actionLabel = stringResource(R.string.wear_pair_retry_check),
        onAction = { refresh() },
      )
      if (w != null) {
        StatusRow(
          ok = w.supported,
          title = stringResource(R.string.wear_pair_check_supported),
          hint = if (w.supported) null else stringResource(R.string.wear_direct_unsupported_phone, w.sdk),
        )
        StatusRow(
          ok = vinOnWatch,
          title = stringResource(R.string.wear_pair_check_vin),
          hint = if (vinOnWatch) DebugReport.maskVin(w.vin) else stringResource(R.string.wear_pair_vin_hint),
          actionLabel = stringResource(R.string.wear_pair_sync),
          onAction = { vm.resyncWear(); scope.launch { delay(2_000); refresh() } },
        )
        StatusRow(
          ok = w.permission,
          title = stringResource(R.string.wear_pair_check_permission),
          hint = if (w.permission) null else stringResource(R.string.wear_pair_permission_hint),
          actionLabel = stringResource(R.string.wear_pair_retry_check),
          onAction = { refresh() },
        )
        StatusRow(
          ok = w.locationPermission,
          title = stringResource(R.string.wear_pair_check_location_permission),
          hint = if (w.locationPermission) null else stringResource(R.string.wear_pair_permission_hint),
          actionLabel = stringResource(R.string.wear_pair_retry_check),
          onAction = { refresh() },
        )
        StatusRow(
          ok = w.locationOn,
          title = stringResource(R.string.wear_pair_check_location_on),
          hint = if (w.locationOn) null else stringResource(R.string.wear_pair_location_on_hint),
          actionLabel = stringResource(R.string.wear_pair_retry_check),
          onAction = { refresh() },
        )
        StatusRow(
          ok = w.bluetoothOn,
          title = stringResource(R.string.wear_pair_check_bluetooth),
          hint = if (w.bluetoothOn) null else stringResource(R.string.wear_pair_bluetooth_hint),
          actionLabel = stringResource(R.string.wear_pair_retry_check),
          onAction = { refresh() },
        )
      }

      GuideSection(stringResource(R.string.pair_section_car))
      TickRow(stringResource(R.string.wear_pair_tick_near), nearCar) { nearCar = it }
      TickRow(stringResource(R.string.pair_tick_card), cardReady) { cardReady = it }

      GuideSection(stringResource(R.string.pair_section_test))
      Text(stringResource(R.string.wear_pair_test_desc), style = MaterialTheme.typography.bodySmall)
      OutlinedButton(onClick = { command(WearContract.WATCH_CMD_PROBE) }, enabled = ready && running.isEmpty()) {
        if (running == "probe") CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        else Icon(Icons.Filled.Bluetooth, contentDescription = null)
        Text(stringResource(R.string.pair_test), modifier = Modifier.padding(start = 8.dp))
      }
      if (started == WearContract.WATCH_CMD_PROBE && running.isEmpty() && w != null && w.probe.isNotEmpty()) {
        val palette = AtaColors.current
        val reached = w.probe == "ok" || w.probe == "paired"
        Text(
          when (w.probe) {
            "paired" -> stringResource(R.string.wear_pair_test_paired)
            "ok" -> stringResource(R.string.wear_pair_test_ok)
            else -> stringResource(R.string.wear_pair_test_failed) + "\n\n" + stringResource(troubleshootingFor(code(w.code)))
          },
          style = MaterialTheme.typography.bodySmall,
          color = if (reached) palette.success else palette.danger,
        )
      }

      GuideSection(stringResource(R.string.pair_section_pair))
      Button(
        onClick = { command(WearContract.WATCH_CMD_PAIR) },
        enabled = ready && nearCar && cardReady && running.isEmpty(),
        modifier = Modifier.fillMaxWidth(),
      ) {
        if (running == "pair") CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        else Icon(Icons.Filled.Key, contentDescription = null)
        Text(
          stringResource(if (running == "pair") R.string.pair_in_progress else R.string.wear_pair_start),
          modifier = Modifier.padding(start = 8.dp),
        )
      }
      if (w != null && started == WearContract.WATCH_CMD_PAIR) {
        PairProgress(
          pairState = pairUiState(w),
          onRetry = { started = "" },
          addedNext = stringResource(R.string.wear_paired_next),
        )
      }

      DiagnosticsCard(w?.log?.lines()?.filter { it.isNotBlank() }.orEmpty(), onReport)
    }
  }
}

private fun code(name: String): FailureCode? = FailureCode.entries.firstOrNull { it.name == name }

/** Maps the watch's report onto the phone assistant's progress model. */
private fun pairUiState(w: WatchState): PairUiState {
  val step = PairStep.entries.firstOrNull { it.name == w.step }
  if (w.running == "pair") return PairUiState.Running(step ?: PairStep.SEARCHING)
  val outcome = PairOutcome.entries.firstOrNull { it.name == w.outcome } ?: return PairUiState.Running(PairStep.SEARCHING)
  return PairUiState.Finished(PairResult(outcome, code(w.code)), reached = step)
}
