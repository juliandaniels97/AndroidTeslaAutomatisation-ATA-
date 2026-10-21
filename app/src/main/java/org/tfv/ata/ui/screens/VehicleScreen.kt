package org.tfv.ata.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tfv.ata.R
import org.tfv.ata.core.AtaAction
import org.tfv.ata.core.AtaCategory
import org.tfv.ata.core.ble.TeslaController
import org.tfv.ata.core.ble.VehicleKey
import org.tfv.ata.ui.ActionHost
import org.tfv.ata.ui.AtaUiState
import org.tfv.ata.ui.AtaViewModel
import org.tfv.ata.ui.components.EditableActionGrid
import org.tfv.ata.ui.components.VehicleArt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleScreen(vm: AtaViewModel, state: AtaUiState, onAddVehicle: () -> Unit = {}) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val snapshot = state.snapshot
  val experimental = state.settings.experimentalBle
  val refreshing by vm.refreshing.collectAsStateWithLifecycle()

  val allBle = remember { AtaAction.entries.filter { it.category == AtaCategory.FUNCTION_A && it.enabled } }
  val savedBle = remember(state.settings.vehicleActionIds) {
    val ids = state.settings.vehicleActionIds
    if (ids.isEmpty()) allBle
    else ids.mapNotNull { AtaAction.fromId(it) }
      .filter { it.category == AtaCategory.FUNCTION_A && it.enabled }
      .ifEmpty { allBle }
  }
  var editMode by remember { mutableStateOf(false) }
  var order by remember { mutableStateOf(savedBle) }
  val shown = if (editMode) order else savedBle
  val hidden = allBle.filter { it !in shown }
  var addDialog by remember { mutableStateOf(false) }

  ActionHost(vm, state, onAddVehicle = onAddVehicle) { run ->
    Scaffold(
      topBar = {
        TopAppBar(
          title = { Text(if (editMode) stringResource(R.string.edit_layout_title) else state.selected?.name ?: stringResource(R.string.tab_vehicle)) },
          actions = {
            when {
              editMode -> TextButton(onClick = { vm.setVehicleActions(order); editMode = false }) {
                Text(stringResource(R.string.save_layout))
              }
              experimental -> {
                IconButton(onClick = { if (!refreshing) vm.refreshStatus() }, enabled = !refreshing) {
                  if (refreshing) CircularProgressIndicator(modifier = Modifier.padding(4.dp), strokeWidth = 2.dp)
                  else Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.refresh))
                }
                IconButton(onClick = { order = savedBle; editMode = true }) {
                  Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_layout_title))
                }
              }
            }
          },
        )
      },
    ) { padding ->
      EditableActionGrid(
        actions = shown,
        editMode = editMode,
        showAddTile = hidden.isNotEmpty(),
        canDelete = shown.size > 1,
        tilesEnabled = experimental && state.selected != null,
        haptics = state.settings.haptics,
        favorite = { it.id in state.settings.homeActionIds },
        onToggleFavorite = { vm.toggleHomeFavorite(it) },
        onRun = { run(it) },
        onEnterEdit = { if (experimental) { order = savedBle; editMode = true } },
        onDelete = { a -> if (order.size > 1) order = order - a },
        onMove = { from, to -> order = order.toMutableList().apply { add(to, removeAt(from)) } },
        onAdd = { addDialog = true },
        modifier = Modifier.fillMaxSize().padding(padding),
        header = {
          Column(Modifier.fillMaxWidth()) {
            VehicleArt(snapshot)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
              StatBox(stringResource(R.string.stat_battery), snapshot?.let { "${it.batteryPercent}%" } ?: "--")
              StatBox(stringResource(R.string.stat_range), snapshot?.let { "${it.rangeKm} km" } ?: "--")
              StatBox(stringResource(R.string.stat_inside), snapshot?.let { "${it.insideTempC.toInt()}°" } ?: "--")
              StatBox(stringResource(R.string.stat_outside), snapshot?.let { "${it.outsideTempC.toInt()}°" } ?: "--")
            }
            snapshot?.takeIf { it.updatedAt > 0 }?.let {
              Text(
                stringResource(R.string.last_updated, formatRelative(it.updatedAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                textAlign = TextAlign.Center,
              )
            }
            if (!experimental) {
              BleEnableCard(
                onEnable = { vm.updateSettings { it.copy(experimentalBle = true) } },
                modifier = Modifier.padding(top = 10.dp),
              )
            } else if (state.selected == null) {
              AddVehicleCard(onAdd = onAddVehicle, modifier = Modifier.padding(top = 10.dp))
            } else {
              PairCard(
                onPair = {
                  val vin = state.selected?.vin
                  if (vin != null) scope.launch { withContext(Dispatchers.IO) { TeslaController.pairKey(context, vin) } }
                },
                modifier = Modifier.padding(top = 10.dp),
              )
            }
          }
        },
      )
    }

    if (addDialog) {
      ActionPickerDialog(
        title = stringResource(R.string.add_action),
        actions = hidden,
        onPick = { order = order + it; addDialog = false },
        onDismiss = { addDialog = false },
      )
    }
  }
}

@Composable
private fun BleEnableCard(onEnable: () -> Unit, modifier: Modifier = Modifier) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f)) {
        Text(
          stringResource(R.string.ble_enable_title),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
        )
        Text(
          stringResource(R.string.ble_enable_desc),
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(top = 4.dp),
        )
      }
      Switch(checked = false, onCheckedChange = { onEnable() })
    }
  }
}

@Composable
private fun AddVehicleCard(onAdd: () -> Unit, modifier: Modifier = Modifier) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(Modifier.padding(16.dp)) {
      Text(
        stringResource(R.string.no_vehicle_title),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
      )
      Text(
        stringResource(R.string.no_vehicle_message),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 4.dp),
      )
      Button(onClick = onAdd, modifier = Modifier.padding(top = 10.dp)) {
        Text(stringResource(R.string.add_now))
      }
    }
  }
}

@Composable
private fun PairCard(onPair: () -> Unit, modifier: Modifier = Modifier) {
  val fingerprint by produceState<String?>(initialValue = null) {
    value = withContext(Dispatchers.IO) { runCatching { VehicleKey.publicKeyFingerprint() }.getOrNull() }
  }
  Card(modifier = modifier.fillMaxWidth()) {
    Column(Modifier.padding(14.dp)) {
      Text(stringResource(R.string.ble_pair_desc), style = MaterialTheme.typography.bodySmall)
      SelectionContainer {
        Text(fingerprint ?: "…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
      }
      Button(onClick = onPair, modifier = Modifier.padding(top = 8.dp)) {
        Icon(Icons.Filled.Key, contentDescription = null)
        Text(stringResource(R.string.ble_pair), modifier = Modifier.padding(start = 8.dp))
      }
    }
  }
}

private fun formatRelative(timestamp: Long): String {
  val seconds = ((System.currentTimeMillis() - timestamp) / 1000).coerceAtLeast(0)
  return when {
    seconds < 60 -> "${seconds}s"
    seconds < 3600 -> "${seconds / 60}m"
    seconds < 86400 -> "${seconds / 3600}h"
    else -> "${seconds / 86400}d"
  }
}

@Composable
private fun StatBox(label: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text(label, style = MaterialTheme.typography.labelSmall)
  }
}
