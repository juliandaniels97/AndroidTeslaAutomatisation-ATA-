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
fun WearConfigScreen(vm: AtaViewModel, state: AtaUiState, onBack: () -> Unit, onPairWatch: () -> Unit = {}) {
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
      OverlayPermissionCard()
      WatchPairCard(onPairWatch)
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
private fun WatchPairCard(onOpen: () -> Unit) {
  androidx.compose.material3.Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f)) {
        Text(stringResource(R.string.wear_pair_title), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.wear_pair_card_desc), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
      }
      androidx.compose.material3.Button(onClick = onOpen, modifier = Modifier.padding(start = 10.dp)) {
        Text(stringResource(R.string.pair_open_assistant_short))
      }
    }
  }
}
