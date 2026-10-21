package org.tfv.ata.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.tfv.ata.R
import org.tfv.ata.core.WatchState
import org.tfv.ata.debug.DebugReport

/** Debug report: preview, then send it as a GitHub issue, share it or copy it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(onBack: () -> Unit) {
  val context = LocalContext.current
  val clipboard = LocalClipboardManager.current
  val scope = rememberCoroutineScope()
  var version by remember { mutableIntStateOf(0) }
  var description by rememberSaveable { mutableStateOf("") }
  val data by produceState<Pair<WatchState?, String>?>(initialValue = null, version) {
    value = null
    val watch = DebugReport.refreshWatch(context)
    value = watch to DebugReport.build(context, watch)
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.report_title)) },
        navigationIcon = {
          IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
        },
        actions = {
          IconButton(onClick = { version++ }) { Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.refresh)) }
        },
      )
    },
  ) { padding ->
    Column(
      Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(stringResource(R.string.report_intro), style = MaterialTheme.typography.bodyMedium)
      OutlinedTextField(
        value = description,
        onValueChange = { description = it },
        label = { Text(stringResource(R.string.report_description)) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2,
      )
      val ready = data != null
      Button(
        onClick = {
          val (watch, report) = data ?: return@Button
          clipboard.setText(AnnotatedString(report))
          scope.launch {
            DebugReport.openIssue(context, watch, "Bug report ATA: " + description.lines().first().take(60).ifBlank { "pairing" }, description)
          }
          Toast.makeText(context, R.string.report_github_toast, Toast.LENGTH_LONG).show()
        },
        enabled = ready,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Icon(Icons.Filled.BugReport, contentDescription = null)
        Text(stringResource(R.string.report_github), modifier = Modifier.padding(start = 8.dp))
      }
      OutlinedButton(
        onClick = { data?.let { DebugReport.share(context, (description.ifBlank { null }?.plus("\n\n") ?: "") + it.second) } },
        enabled = ready,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Icon(Icons.Filled.Share, contentDescription = null)
        Text(stringResource(R.string.report_share), modifier = Modifier.padding(start = 8.dp))
      }
      OutlinedButton(
        onClick = {
          data?.let {
            clipboard.setText(AnnotatedString(it.second))
            Toast.makeText(context, R.string.pair_diagnostics_copied, Toast.LENGTH_SHORT).show()
          }
        },
        enabled = ready,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Icon(Icons.Filled.ContentCopy, contentDescription = null)
        Text(stringResource(R.string.pair_diagnostics_copy), modifier = Modifier.padding(start = 8.dp))
      }
      Text(
        stringResource(R.string.report_privacy),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth(),
      ) {
        val report = data?.second
        if (report == null) {
          CircularProgressIndicator(Modifier.padding(16.dp).size(24.dp), strokeWidth = 2.dp)
        } else {
          SelectionContainer {
            Text(
              report,
              style = MaterialTheme.typography.bodySmall,
              fontFamily = FontFamily.Monospace,
              modifier = Modifier.padding(12.dp),
            )
          }
        }
      }
    }
  }
}
