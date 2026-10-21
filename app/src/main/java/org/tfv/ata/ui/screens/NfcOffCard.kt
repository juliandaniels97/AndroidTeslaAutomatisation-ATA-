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
import org.tfv.ata.nfc.NfcStatus

/** Shown wherever NFC is needed while it's turned off: explains it and opens the NFC settings. */
@Composable
fun NfcOffCard(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  var off by remember { mutableStateOf(false) }
  LifecycleResumeEffect(Unit) {
    off = NfcStatus.hasNfc(context) && !NfcStatus.isEnabled(context)
    onPauseOrDispose { }
  }
  if (!off) return
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(Modifier.padding(14.dp)) {
      Text(
        stringResource(R.string.nfc_off_title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onErrorContainer,
      )
      Text(
        stringResource(R.string.nfc_off_text),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.padding(top = 4.dp),
      )
      Button(onClick = { openNfcSettings(context) }, modifier = Modifier.padding(top = 8.dp)) {
        Text(stringResource(R.string.nfc_settings_open))
      }
    }
  }
}
