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
import androidx.compose.material3.Checkbox
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
import org.tfv.ata.action.ActionInfo
import org.tfv.ata.core.AtaAction
import org.tfv.ata.core.AtaCategory
import org.tfv.ata.ui.AtaUiState
import org.tfv.ata.ui.AtaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WearConfigScreen(vm: AtaViewModel, state: AtaUiState, onBack: () -> Unit) {
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
      funcB.forEach { action -> WearCheck(stringResource(ActionInfo.labelRes(action)), action.id in current) { toggle(action) } }
      Text(
        stringResource(R.string.menu_group_direct),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
      )
      funcA.forEach { action -> WearCheck(stringResource(ActionInfo.labelRes(action)), action.id in current) { toggle(action) } }
    }
  }
}

@Composable
private fun WearCheck(label: String, checked: Boolean, onToggle: () -> Unit) {
  Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
    Checkbox(checked = checked, onCheckedChange = { onToggle() })
    Text(label, Modifier.padding(start = 6.dp))
  }
}
