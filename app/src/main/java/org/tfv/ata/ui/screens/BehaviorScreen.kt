package org.tfv.ata.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.tfv.ata.R
import org.tfv.ata.core.StatusIntervals
import org.tfv.ata.core.StatusMode
import org.tfv.ata.geo.Geo
import org.tfv.ata.ui.AtaUiState
import org.tfv.ata.ui.AtaViewModel

/** Behaviour, status reading, Bluetooth & NFC and unlocking when approaching. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BehaviorScreen(vm: AtaViewModel, state: AtaUiState, onBack: () -> Unit, onKeyCard: () -> Unit) {
  // The stored settings, not the demo's (which shows Bluetooth as on).
  val s = state.stored ?: state.settings
  val context = LocalContext.current
  val locationLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions(),
  ) { result ->
    if (result.values.any { it }) vm.updateSettings { it.copy(directionFx = true) }
  }
  val proximityLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions(),
  ) { result ->
    if (result.values.all { it }) vm.updateSettings { it.copy(proximityUnlock = true) }
  }
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.settings_behavior_page)) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
      )
    },
  ) { padding ->
    Column(
      Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      SectionTitle(stringResource(R.string.section_behavior))
      SwitchRow(stringResource(R.string.settings_confirm), stringResource(R.string.settings_confirm_desc), s.confirmCritical) {
        vm.updateSettings { it.copy(confirmCritical = it.confirmCritical.not()) }
      }
      SwitchRow(stringResource(R.string.settings_haptics), null, s.haptics) {
        vm.updateSettings { it.copy(haptics = it.haptics.not()) }
      }
      SwitchRow(stringResource(R.string.settings_biometric), stringResource(R.string.settings_biometric_desc), s.biometricLock) {
        vm.updateSettings { it.copy(biometricLock = it.biometricLock.not()) }
      }
      SwitchRow(stringResource(R.string.settings_direction_fx), stringResource(R.string.settings_direction_fx_desc), s.directionFx) {
        if (s.directionFx) {
          vm.updateSettings { it.copy(directionFx = false) }
        } else if (Geo.hasLocationPermission(context)) {
          vm.updateSettings { it.copy(directionFx = true) }
        } else {
          locationLauncher.launch(
            arrayOf(
              android.Manifest.permission.ACCESS_FINE_LOCATION,
              android.Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
          )
        }
      }

      HorizontalDivider()
      SectionTitle(stringResource(R.string.section_status))
      Column {
        StatusMode.entries.forEach { mode ->
          Row(
            Modifier.fillMaxWidth().selectable(s.statusMode == mode) { vm.updateSettings { it.copy(statusMode = mode) } }.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(selected = s.statusMode == mode, onClick = null)
            Column(Modifier.padding(start = 10.dp)) {
              Text(
                stringResource(
                  when (mode) {
                    StatusMode.BATTERY_SAVER -> R.string.status_mode_saver
                    StatusMode.SMART -> R.string.status_mode_smart
                    StatusMode.INTERVAL -> R.string.status_mode_interval
                  },
                ),
                style = MaterialTheme.typography.bodyLarge,
              )
              Text(
                stringResource(
                  when (mode) {
                    StatusMode.BATTERY_SAVER -> R.string.status_mode_saver_desc
                    StatusMode.SMART -> R.string.status_mode_smart_desc
                    StatusMode.INTERVAL -> R.string.status_mode_interval_desc
                  },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
      if (s.statusMode != StatusMode.BATTERY_SAVER) {
        val smart = s.statusMode == StatusMode.SMART
        val current = if (smart) s.smartMinutes else s.intervalMinutes
        var index by remember(current, smart) { mutableFloatStateOf(StatusIntervals.indexOf(current).toFloat()) }
        val minutes = StatusIntervals.steps[index.toInt().coerceIn(0, StatusIntervals.steps.lastIndex)]
        Text(stringResource(R.string.status_every, formatInterval(minutes)), style = MaterialTheme.typography.titleSmall)
        Slider(
          value = index,
          valueRange = 0f..StatusIntervals.steps.lastIndex.toFloat(),
          steps = StatusIntervals.steps.size - 2,
          onValueChange = { index = it },
          onValueChangeFinished = {
            vm.updateSettings { st -> if (smart) st.copy(smartMinutes = minutes) else st.copy(intervalMinutes = minutes) }
          },
        )
      }
      Text(stringResource(R.string.status_never_wakes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

      HorizontalDivider()
      SectionTitle(stringResource(R.string.section_experimental))
      SwitchRow(stringResource(R.string.settings_experimental_ble), stringResource(R.string.settings_experimental_ble_desc), s.experimentalBle) {
        vm.updateSettings { it.copy(experimentalBle = it.experimentalBle.not()) }
      }
      SwitchRow(stringResource(R.string.nfc_keycard), stringResource(R.string.nfc_keycard_desc), s.nfcKeyCard) {
        vm.updateSettings { it.copy(nfcKeyCard = it.nfcKeyCard.not()) }
      }
      LinkRow(stringResource(R.string.keycard_title), Icons.Filled.CreditCard, onKeyCard)
      if (s.nfcKeyCard) KeyCardHint()

      HorizontalDivider()
      SectionTitle(stringResource(R.string.section_proximity))
      SwitchRow(stringResource(R.string.settings_proximity), stringResource(R.string.settings_proximity_desc), s.proximityUnlock && s.experimentalBle) {
        when {
          s.proximityUnlock -> vm.updateSettings { it.copy(proximityUnlock = false) }
          !s.experimentalBle -> android.widget.Toast.makeText(context, R.string.proximity_needs_ble, android.widget.Toast.LENGTH_LONG).show()
          // Bluetooth scanning and location (Android only reports the car's advertisement with location access).
          org.tfv.ata.core.ble.TeslaController.BLE_PERMISSIONS.any {
            androidx.core.content.ContextCompat.checkSelfPermission(context, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
          } -> proximityLauncher.launch(org.tfv.ata.core.ble.TeslaController.BLE_PERMISSIONS)
          else -> vm.updateSettings { it.copy(proximityUnlock = true) }
        }
      }
      if (s.proximityUnlock && s.experimentalBle) {
        var meters by remember(s.proximityMeters) { mutableFloatStateOf(s.proximityMeters.coerceIn(2, 20).toFloat()) }
        Text(stringResource(R.string.settings_proximity_meters, meters.toInt()), style = MaterialTheme.typography.titleSmall)
        Slider(
          value = meters,
          valueRange = 2f..20f,
          steps = 17,
          onValueChange = { meters = it },
          onValueChangeFinished = { vm.updateSettings { it.copy(proximityMeters = meters.toInt()) } },
        )
        SwitchRow(stringResource(R.string.settings_proximity_locked), stringResource(R.string.settings_proximity_locked_desc), s.proximityWhenLocked) {
          vm.updateSettings { it.copy(proximityWhenLocked = it.proximityWhenLocked.not()) }
        }
        Text(stringResource(R.string.settings_proximity_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }

    }
  }
}

/** "45 min", "1 h", "2 h 30 min". */
private fun formatInterval(minutes: Int): String = when {
  minutes < 60 -> "$minutes min"
  minutes % 60 == 0 -> "${minutes / 60} h"
  else -> "${minutes / 60} h ${minutes % 60} min"
}
