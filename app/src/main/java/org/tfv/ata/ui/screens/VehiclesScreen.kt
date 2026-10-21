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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import org.tfv.ata.R
import org.tfv.ata.core.Vehicle
import org.tfv.ata.core.Vin
import org.tfv.ata.ui.AtaUiState
import org.tfv.ata.ui.AtaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehiclesScreen(vm: AtaViewModel, state: AtaUiState, onBack: () -> Unit) {
  var editing by remember { mutableStateOf<Vehicle?>(null) }
  var showForm by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.settings_vehicles)) },
        navigationIcon = {
          IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
        },
      )
    },
    floatingActionButton = {
      FloatingActionButton(onClick = { editing = null; showForm = true }) {
        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.vehicle_add))
      }
    },
  ) { padding ->
    Column(
      Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      if (state.vehicles.isEmpty()) {
        Text(stringResource(R.string.vehicle_empty), style = MaterialTheme.typography.bodyLarge)
      }
      state.vehicles.forEach { v ->
        Card(Modifier.fillMaxWidth()) {
          Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
              Text(v.name, style = MaterialTheme.typography.titleMedium)
              Text(v.vin, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { editing = v; showForm = true }) {
              Icon(Icons.Filled.Add, contentDescription = null)
            }
            IconButton(onClick = { vm.deleteVehicle(v.id) }) {
              Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
            }
          }
        }
      }
    }
  }

  if (showForm) {
    VehicleFormDialog(
      existing = editing,
      onSave = { name, vin, home, work ->
        val e = editing
        if (e == null) vm.addVehicle(name, vin, home, work)
        else vm.saveVehicle(e.copy(name = name, vin = vin, navHome = home, navWork = work))
        showForm = false
      },
      onDismiss = { showForm = false },
    )
  }
}

@Composable
private fun VehicleFormDialog(
  existing: Vehicle?,
  onSave: (String, String, String, String) -> Unit,
  onDismiss: () -> Unit,
) {
  var name by remember { mutableStateOf(existing?.name ?: "") }
  var vin by remember { mutableStateOf(existing?.vin ?: "") }
  var home by remember { mutableStateOf(existing?.navHome ?: "") }
  var work by remember { mutableStateOf(existing?.navWork ?: "") }
  val vinValid = Vin.isValid(vin)

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(if (existing == null) R.string.vehicle_add else R.string.vehicle_edit)) },
    text = {
      Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.vehicle_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
          value = vin,
          onValueChange = { vin = Vin.normalize(it) },
          label = { Text(stringResource(R.string.vehicle_vin)) },
          singleLine = true,
          isError = vin.isNotEmpty() && !vinValid,
          supportingText = {
            if (vin.isNotEmpty() && !vinValid) Text(stringResource(R.string.vehicle_vin_invalid))
            else if (vinValid) Text(stringResource(R.string.vehicle_ble_name, Vin.bleLocalName(vin)))
          },
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(home, { home = it }, label = { Text(stringResource(R.string.vehicle_home)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(work, { work = it }, label = { Text(stringResource(R.string.vehicle_work)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
      }
    },
    confirmButton = {
      TextButton(
        enabled = name.isNotBlank() && vinValid,
        onClick = { onSave(name.trim(), vin, home.trim(), work.trim()) },
      ) { Text(stringResource(R.string.save)) }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
  )
}
