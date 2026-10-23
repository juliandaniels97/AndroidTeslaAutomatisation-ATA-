package org.tfv.ata.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import org.tfv.ata.R
import org.tfv.ata.core.AtaAction
import org.tfv.ata.shortcut.ShortcutSync
import org.tfv.ata.ui.ActionHost
import org.tfv.ata.ui.AtaUiState
import org.tfv.ata.ui.AtaViewModel
import org.tfv.ata.ui.tiles.MultiTileView
import org.tfv.ata.ui.tiles.TileBoard
import org.tfv.ata.ui.tiles.TileCatalog
import org.tfv.ata.ui.tiles.TilePickerDialog
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: AtaViewModel, state: AtaUiState, onAddVehicle: () -> Unit = {}) {
  val context = LocalContext.current
  var vehicleMenu by remember { mutableStateOf(false) }
  var pinDialog by remember { mutableStateOf(false) }
  var addDialog by remember { mutableStateOf(false) }
  var editMode by remember { mutableStateOf(false) }

  val allActions = remember { AtaAction.entries.filter { it.enabled } }
  val multi = state.settings.multiTiles
  val savedActions = remember(state.settings.homeActionIds, multi) {
    TileCatalog.resolve(state.settings.homeActionIds.ifEmpty { TileCatalog.favoriteDefaults }, multi)
      .ifEmpty { TileCatalog.resolve(TileCatalog.favoriteDefaults, multi) }
  }
  var order by remember { mutableStateOf(savedActions) }
  val shown = if (editMode) order else savedActions
  val hidden = TileCatalog.all(multi).filter { t -> shown.none { it.key == t.key } }
  val carReady = state.settings.experimentalBle && state.selected != null
  val liveSnapshot = org.tfv.ata.ui.tiles.rememberLiveSnapshot(state.snapshot)

  ActionHost(vm, state, onAddVehicle = onAddVehicle) { run ->
    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            if (editMode) {
              Text(stringResource(R.string.edit_layout_title))
            } else {
              Column {
                Text(
                  stringResource(R.string.app_name),
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  stringResource(R.string.app_subtitle),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          },
          actions = {
            if (editMode) {
              TextButton(onClick = { vm.setFavoriteTiles(order.map { it.key }); editMode = false }) {
                Text(stringResource(R.string.save_layout))
              }
            } else {
              if (state.vehicles.size > 1) {
                IconButton(onClick = { vehicleMenu = true }) {
                  Icon(Icons.Filled.DirectionsCar, contentDescription = null)
                }
                DropdownMenu(expanded = vehicleMenu, onDismissRequest = { vehicleMenu = false }) {
                  state.vehicles.forEach { v ->
                    DropdownMenuItem(
                      text = { Text(v.name) },
                      onClick = { vm.selectVehicle(v.id); vehicleMenu = false },
                    )
                  }
                }
              }
              IconButton(onClick = { pinDialog = true }) {
                Icon(Icons.Filled.AddToHomeScreen, contentDescription = stringResource(R.string.add_shortcut))
              }
              IconButton(onClick = { order = savedActions; editMode = true }) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_layout_title))
              }
            }
          },
        )
      },
    ) { padding ->
      Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(12.dp)) {
        TileBoard(
          tiles = shown,
          columnSetting = state.settings.tileColumns,
          editMode = editMode,
          enabled = { !TileCatalog.needsCar(it) || carReady },
          onRun = { run(it) },
          onEnterEdit = { order = savedActions; editMode = true },
          onMove = { from, to -> order = order.toMutableList().apply { add(to, removeAt(from)) } },
          onDelete = { t -> if (order.size > 1) order = order.filter { it.key != t.key } },
          onAdd = { addDialog = true },
          canDelete = shown.size > 1,
          showAddTile = hidden.isNotEmpty(),
          haptics = state.settings.haptics,
          multi = { tile, enabled, interactive, compact ->
            MultiTileView(tile, liveSnapshot, enabled, interactive, compact, state.settings.haptics, run)
          },
        )
      }
    }

    if (pinDialog) {
      ActionPickerDialog(
        title = stringResource(R.string.add_shortcut),
        actions = allActions,
        onPick = { ShortcutSync.pinToHome(context, it); pinDialog = false },
        onDismiss = { pinDialog = false },
      )
    }

    if (addDialog) {
      TilePickerDialog(
        title = stringResource(R.string.add_action),
        tiles = hidden,
        onPick = { order = order + it; addDialog = false },
        onDismiss = { addDialog = false },
      )
    }
  }
}
