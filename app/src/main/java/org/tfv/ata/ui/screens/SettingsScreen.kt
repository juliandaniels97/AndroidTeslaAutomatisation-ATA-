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
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.content.Intent
import org.tfv.ata.R
import org.tfv.ata.core.NavigationApp
import org.tfv.ata.core.ThemeMode
import org.tfv.ata.core.UiDesign
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
  onDesign: () -> Unit,
  onBehavior: () -> Unit,
  onWear: () -> Unit,
  onAutomation: () -> Unit,
  onAbout: () -> Unit,
  onGuide: () -> Unit,
  onReport: () -> Unit,
  onBackup: () -> Unit,
  onKeyCard: () -> Unit,
) {
  val s = state.settings
  val context = LocalContext.current
  Scaffold(
    topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_settings)) }) },
  ) { padding ->
    Column(
      Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      LinkRow(stringResource(R.string.guide_title), Icons.AutoMirrored.Filled.MenuBook, onGuide)
      HorizontalDivider()
      SectionTitle(stringResource(R.string.section_general))
      LinkRow(stringResource(R.string.settings_vehicles), Icons.Filled.DirectionsCar, onVehicles)
      LinkRow(stringResource(R.string.settings_menu), Icons.Filled.Widgets, onMenu)
      LinkRow(stringResource(R.string.settings_wear), Icons.Filled.Watch, onWear)
      LinkRow(stringResource(R.string.settings_automation), Icons.Filled.AutoMode, onAutomation)
      LinkRow(stringResource(R.string.settings_nfc), Icons.Filled.Nfc) {
        context.startActivity(Intent(context, NfcWriteActivity::class.java))
      }

      SwitchRow(stringResource(R.string.settings_active), stringResource(R.string.settings_active_desc), s.active) {
        vm.updateSettings { it.copy(active = it.active.not()) }
      }

      HorizontalDivider()
      SectionTitle(stringResource(R.string.section_appearance))
      LinkRow(stringResource(R.string.settings_design_page), Icons.Filled.Palette, onDesign)

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
      SectionTitle(stringResource(R.string.section_behavior_connections))
      LinkRow(stringResource(R.string.settings_behavior_page), Icons.Filled.Tune, onBehavior)
      SwitchRow(stringResource(R.string.settings_demo), stringResource(R.string.settings_demo_desc), s.demoMode) {
        vm.setDemoMode(!s.demoMode)
      }

      HorizontalDivider()
      LinkRow(stringResource(R.string.settings_backup), Icons.Filled.SettingsBackupRestore, onBackup)
      LinkRow(stringResource(R.string.report_title), Icons.Filled.BugReport, onReport)
      LinkRow(stringResource(R.string.settings_about), Icons.Filled.Info, onAbout)
    }
  }
}

@Composable
internal fun SectionTitle(text: String) {
  Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
internal fun LinkRow(title: String, icon: ImageVector, onClick: () -> Unit) {
  Row(
    Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).padding(start = 16.dp))
    Icon(
      Icons.AutoMirrored.Filled.KeyboardArrowRight,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
internal fun AddressField(label: String, value: String, onCommit: (String) -> Unit) {
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
internal fun SwitchRow(title: String, desc: String?, checked: Boolean, onToggle: () -> Unit) {
  Row(
    Modifier.fillMaxWidth().toggleable(checked, role = Role.Switch) { onToggle() }.padding(vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.bodyLarge)
      if (desc != null) Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Switch(checked = checked, onCheckedChange = null)
  }
}

/** Explains how the key card and the Tesla app share the car's card reader. */
@Composable
internal fun KeyCardHint() {
  val context = LocalContext.current
  NfcOffCard()
  val teslaApp = remember { org.tfv.ata.nfc.KeyCardRegistration.teslaAppInstalled(context) }
  androidx.compose.material3.Card(
    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(Modifier.padding(12.dp)) {
      Text(
        stringResource(if (teslaApp) R.string.nfc_keycard_tesla_hint else R.string.nfc_keycard_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
      )
      androidx.compose.material3.TextButton(onClick = { openNfcSettings(context) }) {
        Text(stringResource(R.string.nfc_settings_open))
      }
    }
  }
}
