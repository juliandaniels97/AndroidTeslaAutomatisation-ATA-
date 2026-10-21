package org.tfv.ata.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.tfv.ata.R
import org.tfv.ata.core.ble.KeyCardKey
import org.tfv.ata.nfc.KeyCardRegistration
import org.tfv.ata.core.nfc.NfcLog
import org.tfv.ata.nfc.NfcStatus
import org.tfv.ata.ui.AtaUiState
import org.tfv.ata.ui.AtaViewModel
import org.tfv.ata.ui.theme.AtaColors

/**
 * Sets up the phone as a Tesla NFC key card: live checks (NFC on, card emulation, ATA answers the reader),
 * the steps to add it in the car, live reader contact and the card log.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyCardScreen(vm: AtaViewModel, state: AtaUiState, onBack: () -> Unit, onReport: () -> Unit) {
  val context = LocalContext.current
  // NFC state changes outside the app (settings, other apps): re-check regularly while this screen is open.
  var tick by remember { mutableIntStateOf(0) }
  LaunchedEffect(Unit) {
    while (true) {
      delay(1_500)
      tick++
    }
  }
  val hasNfc = remember(tick) { NfcStatus.hasNfc(context) }
  val nfcOn = remember(tick) { NfcStatus.isEnabled(context) }
  val hce = remember(tick) { NfcStatus.hasCardEmulation(context) }
  val answers = remember(tick) { NfcStatus.ataAnswersReader(context) }
  val teslaApp = remember { KeyCardRegistration.teslaAppInstalled(context) }
  val cardOn = state.settings.nfcKeyCard
  val lastContact by NfcLog.lastContact.collectAsStateWithLifecycle()
  val lines by remember { NfcLog.lines(context) }.collectAsStateWithLifecycle()
  val fingerprint by produceState<String?>(initialValue = null) {
    value = if (!KeyCardKey.isSupported) null
    else withContext(Dispatchers.IO) { runCatching { KeyCardKey.publicKeyFingerprint() }.getOrNull() }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.keycard_title)) },
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
      Text(stringResource(R.string.keycard_intro), style = MaterialTheme.typography.bodyMedium)
      NfcOffCard()

      GuideSection(stringResource(R.string.pair_section_phone))
      StatusRow(ok = hasNfc, title = stringResource(R.string.keycard_check_nfc), hint = if (hasNfc) null else stringResource(R.string.nfc_unavailable))
      StatusRow(
        ok = nfcOn,
        title = stringResource(R.string.keycard_check_nfc_on),
        actionLabel = stringResource(R.string.pair_action_enable),
        onAction = { openNfcSettings(context) },
      )
      StatusRow(ok = hce, title = stringResource(R.string.keycard_check_hce))
      StatusRow(ok = KeyCardKey.isSupported, title = stringResource(R.string.pair_check_android))
      StatusRow(
        ok = cardOn,
        title = stringResource(R.string.keycard_check_enabled),
        actionLabel = stringResource(R.string.pair_action_enable),
        onAction = { vm.updateSettings { it.copy(nfcKeyCard = true) } },
      )
      if (cardOn && nfcOn) {
        StatusRow(
          ok = answers,
          title = stringResource(R.string.keycard_check_answers),
          hint = stringResource(
            when {
              !answers -> R.string.keycard_answers_no
              teslaApp -> R.string.keycard_answers_tesla
              else -> R.string.keycard_answers_yes
            },
          ),
          actionLabel = stringResource(R.string.nfc_settings_open),
          onAction = { openNfcSettings(context) },
        )
      }

      GuideSection(stringResource(R.string.keycard_section_enroll))
      stringResource(R.string.keycard_enroll_steps).split('\n').filter { it.isNotBlank() }.forEachIndexed { i, line ->
        NumberedStep(i + 1, line.trim())
      }
      val palette = AtaColors.current
      Text(
        if (lastContact == 0L) stringResource(R.string.keycard_no_contact)
        else stringResource(R.string.keycard_last_contact, ((System.currentTimeMillis() - lastContact) / 1000).coerceAtLeast(0)),
        style = MaterialTheme.typography.bodySmall,
        color = if (lastContact == 0L) MaterialTheme.colorScheme.onSurfaceVariant else palette.success,
      )

      GuideSection(stringResource(R.string.keycard_section_use))
      Text(stringResource(R.string.keycard_use_text), style = MaterialTheme.typography.bodySmall)

      Column {
        Text(stringResource(R.string.keycard_fingerprint), style = MaterialTheme.typography.labelLarge)
        SelectionContainer {
          Text(fingerprint ?: "…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
      }

      DiagnosticsCard(lines.orEmpty(), onReport)
      NfcChooserNote()
    }
  }
}
