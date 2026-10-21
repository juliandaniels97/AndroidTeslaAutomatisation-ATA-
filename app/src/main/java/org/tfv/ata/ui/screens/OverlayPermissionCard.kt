package org.tfv.ata.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import org.tfv.ata.R
import org.tfv.ata.automation.BackgroundLaunch

/**
 * "Open apps on the phone": watch taps and automations can only open the Tesla app / navigation while ATA
 * is in the background if the user allowed "Display over other apps".
 */
@Composable
fun OverlayPermissionCard(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  var granted by remember { mutableStateOf(BackgroundLaunch.allowed(context)) }
  LifecycleResumeEffect(Unit) {
    granted = BackgroundLaunch.allowed(context)
    onPauseOrDispose { }
  }
  Card(
    colors = CardDefaults.cardColors(
      containerColor = if (granted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.tertiaryContainer,
    ),
    modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
  ) {
    Column(Modifier.padding(12.dp)) {
      Text(stringResource(R.string.overlay_title), style = MaterialTheme.typography.titleSmall)
      Text(
        stringResource(if (granted) R.string.overlay_granted else R.string.overlay_desc),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 4.dp),
      )
      if (!granted) {
        Button(
          onClick = { runCatching { context.startActivity(BackgroundLaunch.settingsIntent(context)) } },
          modifier = Modifier.padding(top = 8.dp),
        ) { Text(stringResource(R.string.overlay_button)) }
        Text(
          stringResource(R.string.overlay_restricted_hint),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 6.dp),
        )
      }
    }
  }
}
