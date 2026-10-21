package org.tfv.ata.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.tfv.ata.R
import org.tfv.ata.core.AtaAction
import org.tfv.ata.data.AtaStore
import org.tfv.ata.ui.theme.AtaTheme

class NfcWriteActivity : ComponentActivity() {
  private var adapter: NfcAdapter? = null
  private var token: String = ""
  private var armedActionId: String? = null
  private var onResult: ((Boolean) -> Unit)? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    adapter = NfcAdapter.getDefaultAdapter(this)
    // Tags get their own token (Settings → Automation can revoke all tags without touching shortcuts).
    lifecycleScope.launch { token = AtaStore.get(this@NfcWriteActivity).nfcToken() }

    setContent {
      AtaTheme(themeMode = org.tfv.ata.core.ThemeMode.DARK, dynamicColor = false) {
        var status by remember { mutableStateOf<String?>(null) }
        var armedLabel by remember { mutableStateOf<String?>(null) }

        val actions = remember {
          AtaAction.entries.filter { it.enabled && !it.needsValue }
        }

        fun arm(action: AtaAction, label: String) {
          armedActionId = action.id
          onResult = { ok ->
            runOnUiThread {
              status = getString(if (ok) R.string.nfc_written else R.string.nfc_write_failed)
              armedLabel = null
              disableReader()
            }
          }
          armedLabel = label
          status = null
          enableReader()
        }

        NfcScreen(
          hasNfc = adapter != null,
          armedLabel = armedLabel,
          status = status,
          actions = actions,
          onPick = { arm(it, resources.getString(org.tfv.ata.action.ActionInfo.labelRes(it))) },
          onBack = { finish() },
        )
      }
    }
  }

  override fun onPause() {
    super.onPause()
    disableReader()
  }

  private fun enableReader() {
    val flags = NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
      NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V or
      NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
    runCatching { adapter?.enableReaderMode(this, { tag -> onTag(tag) }, flags, null) }
  }

  private fun disableReader() {
    runCatching { adapter?.disableReaderMode(this) }
  }

  private fun onTag(tag: Tag) {
    val actionId = armedActionId ?: return
    if (token.isBlank()) {
      onResult?.invoke(false)
      return
    }
    val uri = "ata://run?action=$actionId&token=$token"
    val message = NdefMessage(arrayOf(NdefRecord.createUri(uri)))
    val ok = writeTag(tag, message)
    onResult?.invoke(ok)
  }

  private fun writeTag(tag: Tag, message: NdefMessage): Boolean = runCatching {
    val ndef = Ndef.get(tag)
    if (ndef != null) {
      ndef.connect()
      val writable = ndef.isWritable && ndef.maxSize >= message.toByteArray().size
      if (writable) ndef.writeNdefMessage(message)
      ndef.close()
      writable
    } else {
      val formatable = NdefFormatable.get(tag) ?: return false
      formatable.connect()
      formatable.format(message)
      formatable.close()
      true
    }
  }.getOrDefault(false)
}

@Composable
private fun NfcScreen(
  hasNfc: Boolean,
  armedLabel: String?,
  status: String?,
  actions: List<AtaAction>,
  onPick: (AtaAction) -> Unit,
  onBack: () -> Unit,
) {
  Column(
    Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Text(stringResource(R.string.settings_nfc), style = MaterialTheme.typography.headlineSmall)
    if (!hasNfc) {
      Text(stringResource(R.string.nfc_unavailable), color = MaterialTheme.colorScheme.error)
    } else {
      Text(stringResource(R.string.nfc_desc), style = MaterialTheme.typography.bodyMedium)
      armedLabel?.let {
        Card(Modifier.fillMaxWidth()) {
          Text(
            stringResource(R.string.nfc_hold_tag, it),
            Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }
      status?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
      actions.forEach { action ->
        Button(onClick = { onPick(action) }, modifier = Modifier.fillMaxWidth()) {
          Text(stringResource(org.tfv.ata.action.ActionInfo.labelRes(action)))
        }
      }
    }
    androidx.compose.material3.TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
      Text(stringResource(R.string.cancel))
    }
  }
}
