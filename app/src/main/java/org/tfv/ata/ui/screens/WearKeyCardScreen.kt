package org.tfv.ata.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.tfv.ata.R
import org.tfv.ata.core.WearContract
import org.tfv.ata.debug.DebugReport
import org.tfv.ata.ui.theme.AtaColors
import org.tfv.ata.wear.WatchLink

/** Sets up the watch as a Tesla NFC key card, driven from the phone with the watch's live state and card log. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WearKeyCardScreen(onBack: () -> Unit, onReport: () -> Unit) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val watch by WatchLink.state.collectAsStateWithLifecycle()
  val reach by WatchLink.reach.collectAsStateWithLifecycle()
  // The watch only reports when asked: refresh its state every few seconds while this screen is open.
  LaunchedEffect(Unit) {
    DebugReport.refreshWatch(context)
    while (true) {
      delay(3_000)
      WatchLink.send(context, WearContract.WATCH_CMD_STATE)
    }
  }
  fun command(cmd: String) = scope.launch { WatchLink.send(context, cmd) }
  val w = watch

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.wear_keycard_title)) },
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
      Text(stringResource(R.string.wear_keycard_intro), style = MaterialTheme.typography.bodyMedium)

      GuideSection(stringResource(R.string.wear_pair_section_watch))
      StatusRow(
        ok = w != null,
        title = stringResource(R.string.wear_pair_check_reachable),
        hint = when {
          w != null -> "${w.model} · ATA ${w.appVersion}"
          reach == WatchLink.Reach.NO_WATCH -> stringResource(R.string.wear_pair_no_watch)
          reach == WatchLink.Reach.UNKNOWN -> "…"
          else -> stringResource(R.string.wear_pair_no_app)
        },
      )
      if (w != null) {
        StatusRow(ok = w.nfc, title = stringResource(R.string.wear_keycard_check_nfc), hint = if (w.nfc) null else stringResource(R.string.wear_keycard_no_nfc))
        StatusRow(
          ok = w.nfcOn,
          title = stringResource(R.string.wear_keycard_check_nfc_on),
          hint = if (w.nfcOn) null else stringResource(R.string.wear_keycard_nfc_on_hint),
        )
        StatusRow(ok = w.cardEmulation, title = stringResource(R.string.keycard_check_hce))
        StatusRow(ok = w.supported, title = stringResource(R.string.wear_pair_check_supported))
        StatusRow(
          ok = w.keyCard,
          title = stringResource(R.string.wear_keycard_check_enabled),
          actionLabel = stringResource(R.string.pair_action_enable),
          onAction = { command(WearContract.WATCH_CMD_KEYCARD_ON) },
        )
      }

      GuideSection(stringResource(R.string.keycard_section_enroll))
      stringResource(R.string.wear_keycard_enroll_steps).split('\n').filter { it.isNotBlank() }.forEachIndexed { i, line ->
        NumberedStep(i + 1, line.trim())
      }
      val age = w?.nfcContactAgeSec ?: -1
      Text(
        if (age < 0) stringResource(R.string.keycard_no_contact) else stringResource(R.string.keycard_last_contact, age),
        style = MaterialTheme.typography.bodySmall,
        color = if (age < 0) MaterialTheme.colorScheme.onSurfaceVariant else AtaColors.current.success,
      )

      GuideSection(stringResource(R.string.keycard_section_use))
      Text(stringResource(R.string.wear_keycard_use_text), style = MaterialTheme.typography.bodySmall)
      if (w?.keyCard == true) {
        OutlinedButton(onClick = { command(WearContract.WATCH_CMD_KEYCARD_OFF) }) {
          Text(stringResource(R.string.wear_keycard_turn_off))
        }
      }

      DiagnosticsCard(w?.nfcLog?.lines()?.filter { it.isNotBlank() }.orEmpty(), onReport)
    }
  }
}
