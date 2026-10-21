package org.tfv.ata.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.tfv.ata.R
import org.tfv.ata.core.AtaAction
import org.tfv.ata.core.AtaCategory
import org.tfv.ata.ui.AtaUiState
import org.tfv.ata.ui.AtaViewModel
import org.tfv.ata.ui.components.ActionCheckRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WearConfigScreen(
  vm: AtaViewModel,
  state: AtaUiState,
  onBack: () -> Unit,
  onPairWatch: () -> Unit = {},
  onWatchKeyCard: () -> Unit = {},
) {
  // The watch can't ask for a value (address, percent …), so those actions aren't offered there.
  val funcB = AtaAction.entries.filter { it.category == AtaCategory.FUNCTION_B && it.enabled && !it.needsValue }
  val funcA = AtaAction.entries.filter { it.category == AtaCategory.FUNCTION_A && it.enabled && !it.needsValue }
  val defaultIds = funcB.filter { !it.beta }.map { it.id }
  val current = state.settings.wearActionIds.ifEmpty { defaultIds }

  fun toggle(action: AtaAction) {
    val next = if (action.id in current) current - action.id else current + action.id
    vm.setWearActions(next.mapNotNull { AtaAction.fromId(it) })
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.settings_wear)) },
        navigationIcon = {
          IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
        },
      )
    },
  ) { padding ->
    Column(
      Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(stringResource(R.string.settings_wear_desc), style = MaterialTheme.typography.bodyMedium)
      WatchSetupHint()
      OverlayPermissionCard()
      WatchPairCard(onPairWatch)
      WatchFeatureCard(
        title = stringResource(R.string.wear_keycard_title),
        text = stringResource(R.string.wear_keycard_card_desc),
        onOpen = onWatchKeyCard,
      )
      funcB.forEach { action -> ActionCheckRow(action, action.id in current) { toggle(action) } }
      Text(
        stringResource(R.string.menu_group_direct),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
      )
      funcA.forEach { action -> ActionCheckRow(action, action.id in current) { toggle(action) } }
    }
  }
}

/** Entry to the watch-pairing assistant (direct Bluetooth control from the watch). */
@Composable
private fun WatchPairCard(onOpen: () -> Unit) =
  WatchFeatureCard(stringResource(R.string.wear_pair_title), stringResource(R.string.wear_pair_card_desc), onOpen)

/**
 * Tells what to do when no watch is connected or ATA isn't installed on it: everything on this screen
 * needs ATA running on the watch.
 */
@Composable
private fun WatchSetupHint() {
  val context = androidx.compose.ui.platform.LocalContext.current
  var reach by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(org.tfv.ata.wear.WatchLink.Reach.UNKNOWN) }
  androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
    val job = kotlinx.coroutines.MainScope().launch {
      val state = org.tfv.ata.debug.DebugReport.refreshWatch(context)
      reach = if (state != null) org.tfv.ata.wear.WatchLink.Reach.OK else org.tfv.ata.wear.WatchLink.reach.value
    }
    onPauseOrDispose { job.cancel() }
  }
  val (title, text) = when (reach) {
    org.tfv.ata.wear.WatchLink.Reach.NO_WATCH -> R.string.wear_setup_no_watch_title to R.string.wear_setup_no_watch_text
    org.tfv.ata.wear.WatchLink.Reach.NO_APP -> R.string.wear_setup_no_app_title to R.string.wear_setup_no_app_text
    else -> return
  }
  androidx.compose.material3.Card(
    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
  ) {
    Column(Modifier.padding(14.dp)) {
      Text(stringResource(title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
      stringResource(text).split('\n').filter { it.isNotBlank() }.forEachIndexed { i, line ->
        Text("${i + 1}. ${line.trim()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(top = 4.dp))
      }
      androidx.compose.material3.Button(
        onClick = {
          runCatching {
            context.startActivity(
              android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(RELEASES_URL))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
            )
          }
        },
        modifier = Modifier.padding(top = 8.dp),
      ) { Text(stringResource(R.string.wear_setup_download)) }
    }
  }
}

private const val RELEASES_URL = "https://github.com/juliandaniels97/AndroidTeslaAutomatisation-ATA-/releases/latest"

@Composable
private fun WatchFeatureCard(title: String, text: String, onOpen: () -> Unit) {
  androidx.compose.material3.Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
      }
      androidx.compose.material3.Button(onClick = onOpen, modifier = Modifier.padding(start = 10.dp)) {
        Text(stringResource(R.string.pair_open_assistant_short))
      }
    }
  }
}
