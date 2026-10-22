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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.tfv.ata.R
import org.tfv.ata.core.AtaAction
import org.tfv.ata.core.AtaCategory
import org.tfv.ata.ui.AtaUiState
import org.tfv.ata.ui.AtaViewModel
import org.tfv.ata.ui.components.ActionCheckRow

/** Sub-menus of "Menu & widgets". */
enum class MenuSection(val route: String) { ICON("icon"), ROUTINES("routines"), PRESETS("presets"), WIDGET("widget") }

/**
 * "Menu & widgets": an overview ([section] = null) that leads to the sub-menus for the app-icon menu, Samsung
 * Routines, fixed-value shortcuts and the actions widget.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuConfigScreen(
  vm: AtaViewModel,
  state: AtaUiState,
  onBack: () -> Unit,
  section: MenuSection? = null,
  onOpen: (MenuSection) -> Unit = {},
) {
  val funcB = AtaAction.entries.filter { it.category == AtaCategory.FUNCTION_B && it.enabled }
  val funcA = AtaAction.entries.filter { it.category == AtaCategory.FUNCTION_A && it.enabled }
  val menu = state.settings.launcherMenuActionIds
  val widget = state.settings.widgetActionIds
  val routine = state.settings.routineActionIds
  val context = androidx.compose.ui.platform.LocalContext.current
  val maxExtra = (org.tfv.ata.shortcut.ShortcutSync.maxPublished(context) - menu.size).coerceAtLeast(0)
  val routineChoices = (funcB + funcA).filter { !it.needsValue && it.id !in menu }
  val presets = state.settings.presets
  var pickPreset by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
  var presetAction by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<AtaAction?>(null) }
  val routinesUsed = routine.count { it !in menu }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            stringResource(
              when (section) {
                null -> R.string.settings_menu
                MenuSection.ICON -> R.string.menu_launcher_title
                MenuSection.ROUTINES -> R.string.menu_routines_title
                MenuSection.PRESETS -> R.string.presets_title
                MenuSection.WIDGET -> R.string.menu_widget_title
              },
            ),
          )
        },
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
      when (section) {
        null -> {
          SectionLink(Icons.Filled.Apps, stringResource(R.string.menu_launcher_title), stringResource(R.string.menu_count, menu.size, 4)) { onOpen(MenuSection.ICON) }
          SectionLink(Icons.Filled.AutoMode, stringResource(R.string.menu_routines_title), stringResource(R.string.menu_count, routinesUsed, maxExtra)) { onOpen(MenuSection.ROUTINES) }
          SectionLink(Icons.Filled.Tune, stringResource(R.string.presets_title), stringResource(R.string.menu_count_plain, presets.size)) { onOpen(MenuSection.PRESETS) }
          SectionLink(Icons.Filled.Widgets, stringResource(R.string.menu_widget_title), stringResource(R.string.menu_count, widget.size, 8)) { onOpen(MenuSection.WIDGET) }
        }
        MenuSection.ICON -> {
          Text(stringResource(R.string.menu_launcher_desc), style = MaterialTheme.typography.bodySmall)
          funcB.forEach { action ->
            val checked = action.id in menu
            ActionCheckRow(action, checked, enabled = checked || menu.size < 4) { toggleMenu(vm, action, checked) }
          }
          GroupLabel(stringResource(R.string.menu_group_direct))
          funcA.forEach { action ->
            val checked = action.id in menu
            ActionCheckRow(action, checked, enabled = checked || menu.size < 4) { toggleMenu(vm, action, checked) }
          }
        }
        MenuSection.ROUTINES -> {
          RoutinesHint()
          Text(stringResource(R.string.menu_routines_desc, maxExtra), style = MaterialTheme.typography.bodySmall)
          routineChoices.forEach { action ->
            val checked = action.id in routine
            ActionCheckRow(action, checked, enabled = checked || routinesUsed + presets.size < maxExtra) {
              vm.updateSettings { s ->
                s.copy(routineActionIds = if (checked) s.routineActionIds - action.id else s.routineActionIds + action.id)
              }
            }
          }
        }
        MenuSection.PRESETS -> {
          Text(stringResource(R.string.presets_desc), style = MaterialTheme.typography.bodySmall)
          presets.forEach { entry ->
            val (id, value) = org.tfv.ata.shortcut.ShortcutSync.parsePreset(entry) ?: return@forEach
            val action = AtaAction.fromId(id) ?: return@forEach
            PresetRow(action, org.tfv.ata.shortcut.ShortcutSync.label(context, id, value)) {
              vm.updateSettings { s -> s.copy(presets = s.presets - entry) }
            }
          }
          androidx.compose.material3.OutlinedButton(
            onClick = { pickPreset = true },
            enabled = routinesUsed + presets.size < maxExtra,
          ) { Text(stringResource(R.string.presets_add)) }
        }
        MenuSection.WIDGET -> {
          Text(stringResource(R.string.menu_widget_desc), style = MaterialTheme.typography.bodySmall)
          funcB.forEach { action ->
            val checked = action.id in widget
            ActionCheckRow(action, checked, enabled = checked || widget.size < 8) { toggleWidget(vm, action, checked) }
          }
          GroupLabel(stringResource(R.string.menu_group_direct))
          funcA.forEach { action ->
            val checked = action.id in widget
            ActionCheckRow(action, checked, enabled = checked || widget.size < 8) { toggleWidget(vm, action, checked) }
          }
        }
      }
    }
  }

  if (pickPreset) {
    ActionPickerDialog(
      title = stringResource(R.string.presets_add),
      actions = AtaAction.entries.filter { it.enabled && it.needsValue },
      onPick = { pickPreset = false; presetAction = it },
      onDismiss = { pickPreset = false },
    )
  }
  presetAction?.let { action ->
    ValueInputDialog(
      action = action,
      onConfirm = { v ->
        presetAction = null
        if (v.isNotBlank()) {
          vm.updateSettings { s -> s.copy(presets = (s.presets + org.tfv.ata.shortcut.ShortcutSync.presetEntry(action.id, v)).distinct()) }
        }
      },
      onDismiss = { presetAction = null },
    )
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
    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
  ) {
    Column(Modifier.padding(12.dp)) {
      Text(
        stringResource(R.string.routines_hint_title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
      )
      Text(
        stringResource(R.string.routines_hint_text),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.padding(top = 4.dp),
      )
    }
  }
}

@Composable
private fun PresetRow(action: AtaAction, label: String, onDelete: () -> Unit) {
  val palette = org.tfv.ata.ui.theme.AtaColors.current
  Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
    Icon(org.tfv.ata.ui.components.iconFor(action), contentDescription = null, tint = palette.accent, modifier = Modifier.padding(horizontal = 12.dp))
    Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    IconButton(onClick = onDelete) {
      Icon(androidx.compose.material.icons.Icons.Filled.Delete, contentDescription = stringResource(R.string.presets_delete))
    }
  }
}

@Composable
private fun SectionLink(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, summary: String, onClick: () -> Unit) {
  androidx.compose.material3.ListItem(
    headlineContent = { Text(title) },
    supportingContent = { Text(summary) },
    leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
    colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
  )
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

