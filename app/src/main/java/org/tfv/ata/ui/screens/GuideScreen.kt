package org.tfv.ata.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import org.tfv.ata.R
import org.tfv.ata.automation.BackgroundLaunch
import org.tfv.ata.nfc.KeyCardRegistration
import org.tfv.ata.nfc.NfcWriteActivity
import org.tfv.ata.ui.AtaUiState
import org.tfv.ata.ui.AtaViewModel
import org.tfv.ata.ui.theme.AtaColors

/** Where a guide button leads. */
data class GuideTargets(
  val vehicles: () -> Unit,
  val pair: () -> Unit,
  val menu: () -> Unit,
  val wear: () -> Unit,
  val automation: () -> Unit,
  val keyCard: () -> Unit,
)

/** Interactive guide: one card per feature with live status, the steps and buttons that do them. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(vm: AtaViewModel, state: AtaUiState, onBack: () -> Unit, go: GuideTargets) {
  val context = LocalContext.current
  val s = state.settings
  var tick by remember { mutableIntStateOf(0) }
  LifecycleResumeEffect(Unit) {
    tick++
    onPauseOrDispose { }
  }
  val overlay = remember(tick) { BackgroundLaunch.allowed(context) }
  val teslaApp = remember(tick) { KeyCardRegistration.teslaAppInstalled(context) }
  var open by rememberSaveable { mutableStateOf("vehicle") }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.guide_title)) },
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
      Text(stringResource(R.string.guide_intro), style = MaterialTheme.typography.bodyMedium)

      GuideCard(
        id = "vehicle", open = open, onOpen = { open = it },
        icon = Icons.Filled.DirectionsCar,
        title = stringResource(R.string.guide_vehicle_title),
        done = state.selected != null,
        steps = stringResource(R.string.guide_vehicle_steps),
        actions = listOf(stringResource(R.string.settings_vehicles) to go.vehicles),
      )
      GuideCard(
        id = "routines", open = open, onOpen = { open = it },
        icon = Icons.Filled.AutoMode,
        title = stringResource(R.string.guide_routines_title),
        done = s.launcherMenuActionIds.isNotEmpty(),
        steps = stringResource(R.string.guide_routines_steps),
        actions = listOf(
          stringResource(R.string.settings_menu) to go.menu,
          stringResource(R.string.settings_automation) to go.automation,
        ),
      )
      GuideCard(
        id = "overlay", open = open, onOpen = { open = it },
        icon = Icons.Filled.Layers,
        title = stringResource(R.string.overlay_title),
        done = overlay,
        steps = stringResource(R.string.guide_overlay_steps),
        actions = listOf(
          stringResource(R.string.overlay_button) to { runCatching { context.startActivity(BackgroundLaunch.settingsIntent(context)) }; Unit },
        ),
      )
      GuideCard(
        id = "widgets", open = open, onOpen = { open = it },
        icon = Icons.Filled.Widgets,
        title = stringResource(R.string.guide_widgets_title),
        done = null,
        steps = stringResource(R.string.guide_widgets_steps),
        actions = listOf(stringResource(R.string.settings_menu) to go.menu),
      )
      GuideCard(
        id = "nfc", open = open, onOpen = { open = it },
        icon = Icons.Filled.Nfc,
        title = stringResource(R.string.settings_nfc),
        done = null,
        steps = stringResource(R.string.guide_nfc_steps),
        actions = listOf(
          stringResource(R.string.guide_nfc_write) to { context.startActivity(Intent(context, NfcWriteActivity::class.java)) },
        ),
      )
      GuideCard(
        id = "wear", open = open, onOpen = { open = it },
        icon = Icons.Filled.Watch,
        title = stringResource(R.string.settings_wear),
        done = null,
        steps = stringResource(R.string.guide_wear_steps),
        actions = listOf(stringResource(R.string.settings_wear) to go.wear),
      )
      GuideCard(
        id = "ble", open = open, onOpen = { open = it },
        icon = Icons.Filled.Bluetooth,
        title = stringResource(R.string.guide_ble_title),
        done = s.experimentalBle && state.selected != null,
        steps = stringResource(R.string.guide_ble_steps),
        actions = listOf(stringResource(R.string.pair_open_assistant) to go.pair),
      )
      GuideCard(
        id = "keycard", open = open, onOpen = { open = it },
        icon = Icons.Filled.CreditCard,
        title = stringResource(R.string.nfc_keycard),
        done = null,
        steps = stringResource(if (teslaApp) R.string.guide_keycard_steps_tesla else R.string.guide_keycard_steps),
        actions = listOf(
          stringResource(R.string.keycard_title) to go.keyCard,
          stringResource(R.string.nfc_settings_open) to { openNfcSettings(context) },
        ),
      )
    }
  }
}

fun openNfcSettings(context: android.content.Context) {
  runCatching { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    .onFailure { runCatching { context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
}

@Composable
private fun GuideCard(
  id: String,
  open: String,
  onOpen: (String) -> Unit,
  icon: ImageVector,
  title: String,
  /** true: set up, false: still to do, null: optional / no status. */
  done: Boolean?,
  steps: String,
  actions: List<Pair<String, () -> Unit>>,
) {
  val palette = AtaColors.current
  val expanded = open == id
  Card(modifier = Modifier.fillMaxWidth().animateContentSize()) {
    Column {
      Row(
        Modifier.fillMaxWidth().clickable { onOpen(if (expanded) "" else id) }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(palette.accent.copy(alpha = 0.14f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(icon, contentDescription = null, tint = palette.accent)
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
          Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          if (done != null) {
            Text(
              stringResource(if (done) R.string.guide_status_done else R.string.guide_status_todo),
              style = MaterialTheme.typography.labelMedium,
              color = if (done) palette.success else palette.warning,
            )
          }
        }
        Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
      }
      if (expanded) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          steps.split('\n').filter { it.isNotBlank() }.forEachIndexed { i, line ->
            NumberedStep(i + 1, line.trim())
          }
          Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
            actions.forEach { (label, action) ->
              FilledTonalButton(onClick = action) { Text(label) }
            }
          }
        }
      }
    }
  }
}

@Composable
internal fun NumberedStep(n: Int, text: String) {
  val palette = AtaColors.current
  Row(verticalAlignment = Alignment.Top) {
    Box(
      Modifier.size(22.dp).clip(CircleShape).background(palette.accent),
      contentAlignment = Alignment.Center,
    ) {
      Text("$n", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
    }
    Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 10.dp))
  }
}

@Composable
fun GuideSection(text: String) {
  Text(
    text,
    style = MaterialTheme.typography.titleMedium,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(top = 8.dp),
  )
}

/** Status icon: green check (ok), grey circle (pending) or amber warning (needs attention). */
@Composable
fun StatusIcon(ok: Boolean, pending: Boolean = false) {
  val palette = AtaColors.current
  when {
    ok -> Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = palette.success, modifier = Modifier.size(20.dp))
    pending -> Icon(Icons.Filled.RadioButtonUnchecked, contentDescription = null, tint = palette.muted, modifier = Modifier.size(20.dp))
    else -> Icon(Icons.Filled.Warning, contentDescription = null, tint = palette.warning, modifier = Modifier.size(20.dp))
  }
}

/** A precondition with live status and, while it isn't met, a button that fixes it. */
@Composable
fun StatusRow(
  ok: Boolean,
  title: String,
  hint: String? = null,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
) {
  Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    StatusIcon(ok = ok)
    Column(Modifier.weight(1f).padding(start = 10.dp)) {
      Text(title, style = MaterialTheme.typography.bodyMedium)
      if (hint != null) {
        Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
    if (!ok && actionLabel != null && onAction != null) {
      TextButton(onClick = onAction) { Text(actionLabel) }
    }
  }
}
