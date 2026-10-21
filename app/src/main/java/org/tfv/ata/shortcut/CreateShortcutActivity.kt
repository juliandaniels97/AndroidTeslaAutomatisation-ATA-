package org.tfv.ata.shortcut

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.tfv.ata.R
import org.tfv.ata.core.AtaAction
import org.tfv.ata.core.AtaCategory
import org.tfv.ata.core.IntentContract
import org.tfv.ata.ui.screens.ValueInputDialog
import org.tfv.ata.ui.theme.AtaAppTheme

/**
 * Android's "create shortcut" flow (ACTION_CREATE_SHORTCUT): lets Tasker, launchers and automation apps pick
 * any ATA action, including a value (address, charge limit …). The returned shortcut carries ATA's token, so
 * the app the user created it in can run it as a trusted trigger.
 */
class CreateShortcutActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setResult(Activity.RESULT_CANCELED)
    // The shortcut (with ATA's token) goes to the calling app: show the user who that is.
    val caller = callingActivity?.packageName?.let { pkg ->
      runCatching { packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
    }
    val actions = AtaAction.entries.filter { it.enabled }
    val ids = listOf(IntentContract.ACTION_ID_SET_ACTIVE, IntentContract.ACTION_ID_SET_PASSIVE) +
      actions.filter { it.category == AtaCategory.FUNCTION_B }.map { it.id } +
      actions.filter { it.category == AtaCategory.FUNCTION_A }.map { it.id }

    setContent {
      AtaAppTheme {
        var valueAction by remember { mutableStateOf<AtaAction?>(null) }
        Surface(Modifier.fillMaxSize()) {
          Column(Modifier.systemBarsPadding().padding(16.dp)) {
            Text(stringResource(R.string.create_shortcut_title), style = MaterialTheme.typography.titleLarge)
            if (caller != null) {
              Text(
                stringResource(R.string.create_shortcut_for, caller),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
              )
            }
            Text(
              stringResource(R.string.create_shortcut_desc),
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(vertical = 8.dp),
            )
            val palette = org.tfv.ata.ui.theme.AtaColors.current
            LazyColumn {
              items(ids) { id ->
                val action = AtaAction.fromId(id)
                val critical = action != null && org.tfv.ata.core.ActionPolicy.isCritical(action)
                val icon = when {
                  action != null -> org.tfv.ata.ui.components.iconFor(action)
                  id == IntentContract.ACTION_ID_SET_ACTIVE -> androidx.compose.material.icons.Icons.Filled.PlayArrow
                  else -> androidx.compose.material.icons.Icons.Filled.Pause
                }
                androidx.compose.material3.ListItem(
                  headlineContent = { Text(ShortcutSync.label(this@CreateShortcutActivity, id, null)) },
                  leadingContent = {
                    androidx.compose.material3.Icon(icon, contentDescription = null, tint = if (critical) palette.critical else palette.accent)
                  },
                  colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                  modifier = Modifier.clickable {
                    if (action != null && action.needsValue) valueAction = action else finishWith(id, null)
                  },
                )
              }
            }
          }
        }
        valueAction?.let { action ->
          ValueInputDialog(
            action = action,
            onConfirm = { v -> valueAction = null; finishWith(action.id, v) },
            onDismiss = { valueAction = null },
          )
        }
      }
    }
  }

  private fun finishWith(actionId: String, value: String?) {
    lifecycleScope.launch {
      val info = ShortcutSync.forCreateShortcut(this@CreateShortcutActivity, actionId, value)
      setResult(Activity.RESULT_OK, ShortcutManagerCompat.createShortcutResultIntent(this@CreateShortcutActivity, info))
      finish()
    }
  }
}
