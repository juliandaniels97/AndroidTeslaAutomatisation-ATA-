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
fun MenuConfigScreen(vm: AtaViewModel, state: AtaUiState, onBack: () -> Unit) {
  val funcB = AtaAction.entries.filter { it.category == AtaCategory.FUNCTION_B && it.enabled }
  val funcA = AtaAction.entries.filter { it.category == AtaCategory.FUNCTION_A && it.enabled }
  val menu = state.settings.launcherMenuActionIds
  val widget = state.settings.widgetActionIds
  val routine = state.settings.routineActionIds
  val context = androidx.compose.ui.platform.LocalContext.current
  val maxExtra = (org.tfv.ata.shortcut.ShortcutSync.maxPublished(context) - menu.size).coerceAtLeast(0)
  val routineChoices = (funcB + funcA).filter { !it.needsValue && it.id !in menu }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.settings_menu)) },
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
      Text(stringResource(R.string.menu_launcher_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
      Text(stringResource(R.string.menu_launcher_desc), style = MaterialTheme.typography.bodySmall)
      funcB.forEach { action ->
        val checked = action.id in menu
        CheckRow(stringResource(ActionInfo.labelRes(action)), checked, enabled = checked || menu.size < 4) {
          toggleMenu(vm, action, checked)
        }
      }
      GroupLabel(stringResource(R.string.menu_group_direct))
      funcA.forEach { action ->
        val checked = action.id in menu
        CheckRow(stringResource(ActionInfo.labelRes(action)), checked, enabled = checked || menu.size < 4) {
          toggleMenu(vm, action, checked)
        }
      }

      Text(stringResource(R.string.menu_routines_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp))
      RoutinesHint()
      Text(stringResource(R.string.menu_routines_desc, maxExtra), style = MaterialTheme.typography.bodySmall)
      routineChoices.forEach { action ->
        val checked = action.id in routine
        CheckRow(stringResource(ActionInfo.labelRes(action)), checked, enabled = checked || routine.count { it !in menu } < maxExtra) {
          vm.updateSettings { s ->
            s.copy(routineActionIds = if (checked) s.routineActionIds - action.id else s.routineActionIds + action.id)
          }
        }
      }

      Text(stringResource(R.string.menu_widget_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp))
      Text(stringResource(R.string.menu_widget_desc), style = MaterialTheme.typography.bodySmall)
      funcB.forEach { action ->
        val checked = action.id in widget
        CheckRow(stringResource(ActionInfo.labelRes(action)), checked, enabled = checked || widget.size < 8) {
          toggleWidget(vm, action, checked)
        }
      }
      GroupLabel(stringResource(R.string.menu_group_direct))
      funcA.forEach { action ->
        val checked = action.id in widget
        CheckRow(stringResource(ActionInfo.labelRes(action)), checked, enabled = checked || widget.size < 8) {
          toggleWidget(vm, action, checked)
        }
      }
    }
  }
}

private fun toggleMenu(vm: AtaViewModel, action: AtaAction, checked: Boolean) {
  vm.updateSettings { s ->
    val list = if (checked) s.launcherMenuActionIds - action.id
    else (s.launcherMenuActionIds + action.id).take(4)
    s.copy(launcherMenuActionIds = list)
  }
}

private fun toggleWidget(vm: AtaViewModel, action: AtaAction, checked: Boolean) {
  vm.updateSettings { s ->
    val list = if (checked) s.widgetActionIds - action.id else s.widgetActionIds + action.id
    s.copy(widgetActionIds = list)
  }
}

/** Prominent note: Samsung Routines can only start actions ATA publishes as app shortcuts. */
@Composable
fun RoutinesHint(modifier: Modifier = Modifier) {
  androidx.compose.material3.Card(
    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
  ) {
    Column(Modifier.padding(12.dp)) {
      Text(
        stringResource(R.string.routines_hint_title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
      )
      Text(
        stringResource(R.string.routines_hint_text),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier.padding(top = 4.dp),
      )
    }
  }
}

@Composable
private fun GroupLabel(text: String) {
  Text(
    text,
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.secondary,
    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
  )
}

@Composable
private fun CheckRow(label: String, checked: Boolean, enabled: Boolean, onToggle: () -> Unit) {
  Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
    Checkbox(checked = checked, onCheckedChange = { onToggle() }, enabled = enabled)
    Text(label, Modifier.padding(start = 6.dp))
  }
}
