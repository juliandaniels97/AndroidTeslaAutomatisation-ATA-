package org.tfv.ata.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import android.widget.Toast
import org.tfv.ata.R
import org.tfv.ata.action.ActionInfo
import org.tfv.ata.automation.Notifier
import org.tfv.ata.core.ActionPolicy
import org.tfv.ata.core.AtaAction
import org.tfv.ata.core.AtaCategory
import org.tfv.ata.data.AtaStore
import org.tfv.ata.ui.AtaUiState
import org.tfv.ata.ui.AtaViewModel

/** Settings → Automation: what Samsung Routines, Tasker, widgets, NFC tags and other apps may do. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationScreen(vm: AtaViewModel, state: AtaUiState, onBack: () -> Unit) {
  val s = state.settings
  val context = LocalContext.current
  val clipboard = LocalClipboardManager.current
  var tokenVersion by remember { mutableIntStateOf(0) }
  val token by produceState<String?>(initialValue = null, tokenVersion) {
    value = AtaStore.get(context).triggerToken()
  }
  var notifyAllowed by remember { mutableStateOf(Notifier.canNotify(context)) }
  val notifyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
    notifyAllowed = Notifier.canNotify(context)
  }
  var confirmRotate by remember { mutableStateOf(false) }
  var confirmNfc by remember { mutableStateOf(false) }

  val critical = AtaAction.entries.filter { it.enabled && ActionPolicy.isCritical(it) }
  val nonCritical = AtaAction.entries.filter {
    it.enabled && it.category == AtaCategory.FUNCTION_A && !ActionPolicy.isCritical(it)
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.settings_automation)) },
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
      RoutinesHint()
      Text(stringResource(R.string.automation_intro), style = MaterialTheme.typography.bodyMedium)

      if (!notifyAllowed) {
        Text(stringResource(R.string.automation_notify_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        Button(onClick = { notifyLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
          Text(stringResource(R.string.automation_notify_allow))
        }
      }

      HorizontalDivider()
      Title(stringResource(R.string.automation_section_trusted))
      Toggle(stringResource(R.string.automation_when_locked), stringResource(R.string.automation_when_locked_desc), s.automationWhenLocked) {
        vm.updateSettings { it.copy(automationWhenLocked = !it.automationWhenLocked) }
      }

      HorizontalDivider()
      Title(stringResource(R.string.automation_section_critical))
      Hint(stringResource(R.string.automation_critical_desc))
      critical.forEach { action ->
        Check(stringResource(ActionInfo.labelRes(action)), action.id in s.criticalDirectIds) {
          vm.updateSettings {
            val ids = if (action.id in it.criticalDirectIds) it.criticalDirectIds - action.id else it.criticalDirectIds + action.id
            it.copy(criticalDirectIds = ids, criticalDirectWhenLocked = it.criticalDirectWhenLocked && ids.isNotEmpty())
          }
        }
      }
      if (s.criticalDirectIds.isNotEmpty()) {
        Toggle(stringResource(R.string.automation_critical_locked), stringResource(R.string.automation_critical_locked_desc), s.criticalDirectWhenLocked) {
          vm.updateSettings { it.copy(criticalDirectWhenLocked = !it.criticalDirectWhenLocked) }
        }
      }

      HorizontalDivider()
      Title(stringResource(R.string.automation_section_external))
      Hint(stringResource(R.string.automation_external_desc))
      nonCritical.forEach { action ->
        Check(stringResource(ActionInfo.labelRes(action)), action.id in s.externalActionIds) {
          vm.updateSettings {
            val ids = if (action.id in it.externalActionIds) it.externalActionIds - action.id else it.externalActionIds + action.id
            it.copy(externalActionIds = ids)
          }
        }
      }

      HorizontalDivider()
      Toggle(stringResource(R.string.automation_status_broadcast), stringResource(R.string.automation_status_broadcast_desc), s.statusBroadcast) {
        vm.updateSettings { it.copy(statusBroadcast = !it.statusBroadcast) }
      }

      HorizontalDivider()
      Title(stringResource(R.string.automation_section_token))
      Hint(stringResource(R.string.automation_token_desc))
      Text(
        token?.let { it.take(8) + "…" + it.takeLast(4) } ?: "…",
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.primary,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = {
          token?.let {
            clipboard.setText(AnnotatedString(it))
            Toast.makeText(context, R.string.automation_token_copied, Toast.LENGTH_SHORT).show()
          }
        }) { Text(stringResource(R.string.automation_token_copy)) }
        OutlinedButton(onClick = { confirmRotate = true }) { Text(stringResource(R.string.automation_token_rotate)) }
      }
      OutlinedButton(onClick = { confirmNfc = true }) { Text(stringResource(R.string.automation_nfc_revoke)) }
    }
  }

  if (confirmRotate) {
    AlertDialog(
      onDismissRequest = { confirmRotate = false },
      title = { Text(stringResource(R.string.automation_token_rotate)) },
      text = { Text(stringResource(R.string.automation_token_rotate_desc)) },
      confirmButton = {
        TextButton(onClick = {
          confirmRotate = false
          vm.rotateTriggerToken { tokenVersion++ }
        }) { Text(stringResource(R.string.ok)) }
      },
      dismissButton = { TextButton(onClick = { confirmRotate = false }) { Text(stringResource(R.string.cancel)) } },
    )
  }
  if (confirmNfc) {
    AlertDialog(
      onDismissRequest = { confirmNfc = false },
      title = { Text(stringResource(R.string.automation_nfc_revoke)) },
      text = { Text(stringResource(R.string.automation_nfc_revoke_desc)) },
      confirmButton = {
        TextButton(onClick = {
          confirmNfc = false
          vm.rotateNfcToken()
        }) { Text(stringResource(R.string.ok)) }
      },
      dismissButton = { TextButton(onClick = { confirmNfc = false }) { Text(stringResource(R.string.cancel)) } },
    )
  }
}

@Composable
private fun Title(text: String) {
  Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun Hint(text: String) {
  Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Toggle(title: String, desc: String?, checked: Boolean, onToggle: () -> Unit) {
  Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
    Column(Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.bodyLarge)
      if (desc != null) Hint(desc)
    }
    Switch(checked = checked, onCheckedChange = { onToggle() })
  }
}

@Composable
private fun Check(label: String, checked: Boolean, onToggle: () -> Unit) {
  Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
    Checkbox(checked = checked, onCheckedChange = { onToggle() })
    Text(label, Modifier.padding(start = 6.dp))
  }
}
