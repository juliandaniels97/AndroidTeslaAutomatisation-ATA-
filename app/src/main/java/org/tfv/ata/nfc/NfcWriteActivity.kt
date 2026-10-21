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
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import org.tfv.ata.ui.theme.AtaAppTheme

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
      AtaAppTheme {
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
          onCancelWrite = {
            armedActionId = null
            armedLabel = null
            disableReader()
          },
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun NfcScreen(
  hasNfc: Boolean,
  armedLabel: String?,
  status: String?,
  actions: List<AtaAction>,
  onPick: (AtaAction) -> Unit,
  onCancelWrite: () -> Unit,
  onBack: () -> Unit,
) {
  val palette = org.tfv.ata.ui.theme.AtaColors.current
  androidx.compose.material3.Scaffold(
    topBar = {
      androidx.compose.material3.TopAppBar(
        title = { Text(stringResource(R.string.settings_nfc)) },
        navigationIcon = {
          androidx.compose.material3.IconButton(onClick = onBack) {
            androidx.compose.material3.Icon(
              androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = stringResource(R.string.back),
            )
          }
        },
      )
    },
  ) { padding ->
    Column(
      Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      if (!hasNfc) {
        Card(
          colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            stringResource(R.string.nfc_unavailable),
            Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.titleSmall,
          )
        }
      }
      Text(stringResource(R.string.nfc_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      if (armedLabel != null) {
        Card(
          colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(Modifier.padding(16.dp)) {
            androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(
              stringResource(R.string.nfc_hold_tag, armedLabel),
              Modifier.padding(top = 12.dp),
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            androidx.compose.material3.TextButton(onClick = onCancelWrite) { Text(stringResource(R.string.cancel)) }
          }
        }
      }
      status?.let {
        Card(
          colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.titleSmall)
        }
      }
      val groups = listOf(
        R.string.nfc_group_basic to actions.filter { it.category == org.tfv.ata.core.AtaCategory.FUNCTION_B },
        R.string.menu_group_direct to actions.filter { it.category == org.tfv.ata.core.AtaCategory.FUNCTION_A },
      )
      groups.forEach { (title, list) ->
        if (list.isEmpty()) return@forEach
        Text(
          stringResource(title),
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(top = 8.dp),
        )
        Card(
          colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
          modifier = Modifier.fillMaxWidth(),
        ) {
          list.forEachIndexed { index, action ->
            val critical = org.tfv.ata.core.ActionPolicy.isCritical(action)
            androidx.compose.material3.ListItem(
              headlineContent = { Text(stringResource(org.tfv.ata.action.ActionInfo.labelRes(action))) },
              supportingContent = if (critical) {
                { Text(stringResource(R.string.nfc_critical_hint), color = palette.critical) }
              } else null,
              leadingContent = {
                androidx.compose.material3.Icon(
                  org.tfv.ata.ui.components.iconFor(action),
                  contentDescription = null,
                  tint = if (critical) palette.critical else palette.accent,
                )
              },
              colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
              modifier = Modifier.clickable(enabled = hasNfc && armedLabel == null) { onPick(action) },
            )
            if (index < list.lastIndex) androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          }
        }
      }
    }
  }
}
