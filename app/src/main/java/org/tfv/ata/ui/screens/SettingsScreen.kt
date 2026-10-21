package org.tfv.ata.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.content.Intent
import org.tfv.ata.R
import org.tfv.ata.core.NavigationApp
import org.tfv.ata.core.ThemeMode
import org.tfv.ata.geo.Geo
import org.tfv.ata.nfc.NfcWriteActivity
import org.tfv.ata.ui.AtaUiState
import org.tfv.ata.ui.AtaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  vm: AtaViewModel,
  state: AtaUiState,
  onVehicles: () -> Unit,
  onMenu: () -> Unit,
  onWear: () -> Unit,
  onAutomation: () -> Unit,
  onAbout: () -> Unit,
) {
  val s = state.settings
  val context = LocalContext.current
  val locationLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions(),
  ) { result ->
    if (result.values.any { it }) vm.updateSettings { it.copy(directionFx = true) }
  }
  Scaffold(
    topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_settings)) }) },
  ) { padding ->
    Column(
      Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      SectionTitle(stringResource(R.string.section_general))
      LinkRow(stringResource(R.string.settings_vehicles), onVehicles)
      LinkRow(stringResource(R.string.settings_menu), onMenu)
      LinkRow(stringResource(R.string.settings_wear), onWear)
      LinkRow(stringResource(R.string.settings_automation), onAutomation)
      LinkRow(stringResource(R.string.settings_nfc)) {
        context.startActivity(Intent(context, NfcWriteActivity::class.java))
      }

      SwitchRow(stringResource(R.string.settings_active), stringResource(R.string.settings_active_desc), s.active) {
        vm.updateSettings { it.copy(active = it.active.not()) }
      }

      HorizontalDivider()
      SectionTitle(stringResource(R.string.section_appearance))
      Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleSmall)
      Column {
        ThemeMode.entries.forEach { mode ->
          Row(
            Modifier.fillMaxWidth().selectable(s.theme == mode) {
              vm.updateSettings { it.copy(theme = mode) }
            }.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(selected = s.theme == mode, onClick = null)
            Text(
              when (mode) {
                ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                ThemeMode.DARK -> stringResource(R.string.theme_dark)
              },
              Modifier.padding(start = 10.dp),
            )
          }
        }
      }
      SwitchRow(stringResource(R.string.settings_dynamic_color), null, s.dynamicColor) {
        vm.updateSettings { it.copy(dynamicColor = it.dynamicColor.not()) }
      }

      HorizontalDivider()
      SectionTitle(stringResource(R.string.section_navigation))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NavigationApp.entries.forEach { app ->
          FilterChip(
            selected = s.navigationApp == app,
            onClick = { vm.updateSettings { it.copy(navigationApp = app) } },
            label = {
              Text(
                when (app) {
                  NavigationApp.TESLA -> stringResource(R.string.nav_tesla)
                  NavigationApp.GOOGLE_MAPS -> stringResource(R.string.nav_gmaps)
                  NavigationApp.WAZE -> stringResource(R.string.nav_waze)
                },
              )
            },
          )
        }
      }

      HorizontalDivider()
      SectionTitle(stringResource(R.string.section_places))
      Text(
        stringResource(R.string.settings_places_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      AddressField(stringResource(R.string.settings_home_address), s.homeAddress) { v ->
        vm.updateSettings { it.copy(homeAddress = v) }
      }
      AddressField(stringResource(R.string.settings_work_address), s.workAddress) { v ->
        vm.updateSettings { it.copy(workAddress = v) }
      }

      HorizontalDivider()
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

      var pollLocal by remember(s.pollIntervalMinutes) { mutableFloatStateOf(s.pollIntervalMinutes.toFloat()) }
      Text(stringResource(R.string.settings_poll, pollLocal.toInt()), style = MaterialTheme.typography.titleSmall)
      Slider(
        value = pollLocal,
        valueRange = 0f..120f,
        steps = 11,
        onValueChange = { pollLocal = (it / 10f).toInt() * 10f },
        onValueChangeFinished = { vm.updateSettings { it.copy(pollIntervalMinutes = pollLocal.toInt()) } },
      )
      Text(stringResource(R.string.settings_poll_desc), style = MaterialTheme.typography.bodySmall)

      HorizontalDivider()
      SectionTitle(stringResource(R.string.section_experimental))
      SwitchRow(stringResource(R.string.settings_experimental_ble), stringResource(R.string.settings_experimental_ble_desc), s.experimentalBle) {
        vm.updateSettings { it.copy(experimentalBle = it.experimentalBle.not()) }
      }
      SwitchRow(stringResource(R.string.nfc_keycard), stringResource(R.string.nfc_keycard_desc), s.nfcKeyCard) {
        vm.updateSettings { it.copy(nfcKeyCard = it.nfcKeyCard.not()) }
      }

      HorizontalDivider()
      SectionTitle(stringResource(R.string.section_proximity))
      SwitchRow(stringResource(R.string.settings_proximity), stringResource(R.string.settings_proximity_desc), s.proximityUnlock) {
        vm.updateSettings { it.copy(proximityUnlock = it.proximityUnlock.not()) }
      }
      if (s.proximityUnlock) {
        var proxLocal by remember(s.proximityMeters) { mutableFloatStateOf(s.proximityMeters.toFloat()) }
        Text(stringResource(R.string.settings_proximity_meters, proxLocal.toInt()), style = MaterialTheme.typography.titleSmall)
        Slider(
          value = proxLocal,
          valueRange = 5f..100f,
          steps = 18,
          onValueChange = { proxLocal = (it / 5f).toInt() * 5f },
          onValueChangeFinished = { vm.updateSettings { it.copy(proximityMeters = proxLocal.toInt()) } },
        )
      }

      HorizontalDivider()
      LinkRow(stringResource(R.string.settings_about), onAbout)
    }
  }
}

@Composable
private fun SectionTitle(text: String) {
  Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun LinkRow(title: String, onClick: () -> Unit) {
  Text(
    title,
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
    style = MaterialTheme.typography.bodyLarge,
  )
}

@Composable
private fun AddressField(label: String, value: String, onCommit: (String) -> Unit) {
  var text by remember(value) { mutableStateOf(value) }
  OutlinedTextField(
    value = text,
    onValueChange = { text = it },
    label = { Text(label) },
    singleLine = true,
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp)
      .onFocusChanged { focus ->
        if (!focus.isFocused && text != value) onCommit(text.trim())
      },
  )
}

@Composable
private fun SwitchRow(title: String, desc: String?, checked: Boolean, onToggle: () -> Unit) {
  Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
    Column(Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.bodyLarge)
      if (desc != null) Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Switch(checked = checked, onCheckedChange = { onToggle() })
  }
}
