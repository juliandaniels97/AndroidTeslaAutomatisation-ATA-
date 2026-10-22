package org.tfv.ata.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import org.tfv.ata.R
import org.tfv.ata.core.BodyStyle
import org.tfv.ata.core.CarPaint
import org.tfv.ata.core.NeonColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import org.tfv.ata.core.Vehicle
import org.tfv.ata.core.Vin
import org.tfv.ata.ui.AtaUiState
import org.tfv.ata.ui.AtaViewModel
import org.tfv.ata.ui.components.car.CarCamera
import org.tfv.ata.ui.components.car.CarFxState
import org.tfv.ata.ui.components.car.CarModel
import org.tfv.ata.ui.components.car.CarStatus
import org.tfv.ata.ui.components.car.drawNeonCar
import org.tfv.ata.ui.components.car.drawFlatCar
import org.tfv.ata.ui.components.car.rememberCarLook

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
              Text("${bodyLabel(v.body)} · ${v.vin}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { editing = v; showForm = true }) {
              Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.vehicle_edit))
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
      threeD = state.settings.car3d,
      onSave = { draft ->
        val e = editing
        if (e == null) vm.saveVehicle(draft.copy(id = java.util.UUID.randomUUID().toString()))
        else vm.saveVehicle(
          e.copy(
            name = draft.name, vin = draft.vin, navHome = draft.navHome, navWork = draft.navWork,
            bodyStyle = draft.bodyStyle, paint = draft.paint, neonColor = draft.neonColor,
          ),
        )
        showForm = false
      },
      onDismiss = { showForm = false },
    )
  }
}

/** Round colour buttons; the chosen one gets a ring. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Swatches(colors: List<Pair<String, Color>>, selected: String, onPick: (String) -> Unit) {
  FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    colors.forEach { (id, color) ->
      val chosen = id == selected
      androidx.compose.foundation.layout.Box(
        Modifier.size(34.dp)
          .border(if (chosen) 3.dp else 1.dp, if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape)
          .padding(if (chosen) 5.dp else 2.dp)
          .background(color, CircleShape)
          .clickable { onPick(id) },
      )
    }
  }
}

/** Product names are not translated. */
fun bodyLabel(style: BodyStyle): String = when (style) {
  BodyStyle.MODEL_3 -> "Model 3"
  BodyStyle.MODEL_Y -> "Model Y"
  BodyStyle.MODEL_S -> "Model S"
  BodyStyle.MODEL_X -> "Model X"
  BodyStyle.CYBERTRUCK -> "Cybertruck"
  BodyStyle.ROADSTER -> "Roadster"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VehicleFormDialog(
  existing: Vehicle?,
  threeD: Boolean,
  onSave: (Vehicle) -> Unit,
  onDismiss: () -> Unit,
) {
  var name by remember { mutableStateOf(existing?.name ?: "") }
  var vin by remember { mutableStateOf(existing?.vin ?: "") }
  var home by remember { mutableStateOf(existing?.navHome ?: "") }
  var work by remember { mutableStateOf(existing?.navWork ?: "") }
  var body by remember { mutableStateOf(existing?.bodyStyle ?: "") }
  var paint by remember { mutableStateOf(existing?.paint ?: "") }
  var neon by remember { mutableStateOf(existing?.neonColor ?: "") }
  val vinValid = Vin.isValid(vin)
  val draft = Vehicle("", name, vin, bodyStyle = body, paint = paint, neonColor = neon)
  val shown = draft.body
  val look = rememberCarLook(draft, lineWidth = 2f)

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
        Text(stringResource(R.string.vehicle_body), style = MaterialTheme.typography.titleSmall)
        Canvas(Modifier.fillMaxWidth().height(80.dp).clipToBounds()) {
          val shape = CarModel.shape(shown)
          // About three quarters of the width, whatever the dialog height.
          val zoom = 1.45f * size.width / minOf(size.width * 1.25f, size.height * 1.9f) * shape.fit
          val camera = CarCamera.side(size.width, size.height, zoom)
          if (threeD) drawNeonCar(shape, camera, CarCamera.SIDE_YAW, CarFxState(), CarStatus(), 0L, look.style.copy(glow = false))
          else drawFlatCar(shape, camera, CarFxState(), CarStatus(), 0L, look.flat)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          val auto = BodyStyle.fromVin(vin)
          FilterChip(
            selected = body.isEmpty(),
            onClick = { body = "" },
            label = { Text(stringResource(R.string.vehicle_body_auto, auto?.let(::bodyLabel) ?: "–")) },
          )
          BodyStyle.entries.forEach { style ->
            FilterChip(selected = body == style.name, onClick = { body = style.name }, label = { Text(bodyLabel(style)) })
          }
        }
        // Colour: Tesla paint for the Material design, line colour for the neon design.
        if (look.material) {
          Text(stringResource(R.string.vehicle_paint), style = MaterialTheme.typography.titleSmall)
          Swatches(CarPaint.entries.map { it.name to Color(it.argb) }, draft.carPaint.name) { paint = it }
        } else {
          Text(stringResource(R.string.vehicle_neon_color), style = MaterialTheme.typography.titleSmall)
          Swatches(NeonColor.entries.map { it.name to Color(it.argb) }, draft.carNeon.name) { neon = it }
        }
      }
    },
    confirmButton = {
      TextButton(
        enabled = name.isNotBlank() && vinValid,
        onClick = { onSave(draft.copy(name = name.trim(), navHome = home.trim(), navWork = work.trim())) },
      ) { Text(stringResource(R.string.save)) }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
  )
}
