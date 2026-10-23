package org.tfv.ata.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.tfv.ata.R
import org.tfv.ata.core.ThemeMode
import org.tfv.ata.core.UiDesign
import org.tfv.ata.ui.AtaUiState
import org.tfv.ata.ui.AtaViewModel

/** Everything about the look: design, theme, colours, car view and the tile grid. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DesignScreen(vm: AtaViewModel, state: AtaUiState, onBack: () -> Unit) {
  val s = state.settings
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.settings_design_page)) },
        navigationIcon = {
          IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
        },
      )
    },
  ) { padding ->
    Column(
      Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(stringResource(R.string.settings_design), style = MaterialTheme.typography.titleSmall)
      Text(stringResource(R.string.settings_design_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) {
        UiDesign.entries.forEach { design ->
          FilterChip(
            selected = s.design == design,
            onClick = { vm.updateSettings { it.copy(design = design) } },
            label = { Text(stringResource(if (design == UiDesign.NEON) R.string.design_neon else R.string.design_material)) },
          )
        }
      }
      Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleSmall)
      Column {
        ThemeMode.entries.forEach { mode ->
          Row(
            Modifier.fillMaxWidth().selectable(s.theme == mode) { vm.updateSettings { it.copy(theme = mode) } }.padding(vertical = 6.dp),
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
      SwitchRow(stringResource(R.string.settings_car3d), stringResource(R.string.settings_car3d_desc), s.car3d) {
        vm.updateSettings { it.copy(car3d = it.car3d.not()) }
      }

      HorizontalDivider()
      SwitchRow(stringResource(R.string.settings_multi_tiles), stringResource(R.string.settings_multi_tiles_desc), s.multiTiles) {
        vm.updateSettings { it.copy(multiTiles = it.multiTiles.not()) }
      }
      Text(stringResource(R.string.settings_columns), style = MaterialTheme.typography.titleSmall)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (listOf(0) + (2..6)).forEach { n ->
          FilterChip(
            selected = s.tileColumns == n,
            onClick = { vm.updateSettings { it.copy(tileColumns = n) } },
            label = { Text(if (n == 0) stringResource(R.string.settings_columns_auto) else n.toString()) },
          )
        }
      }
      Text(stringResource(R.string.settings_columns_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}
