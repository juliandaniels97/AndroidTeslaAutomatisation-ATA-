package org.tfv.ata.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Runs the action of a watch-face complication. Critical actions (unlock, trunk …) wait for a confirming tap,
 * the others start right away. Shows the result briefly, then closes.
 */
class ComplicationActionActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val id = intent.getStringExtra(EXTRA_ACTION_ID)
    if (id == null) {
      finish()
      return
    }
    val label = intent.getStringExtra(EXTRA_LABEL) ?: id
    setContent { MaterialTheme { RunAction(id, label) { finish() } } }
  }

  companion object {
    const val EXTRA_ACTION_ID = "action_id"
    const val EXTRA_LABEL = "label"
  }
}

private enum class Phase { CONFIRM, RUNNING, DONE }

@androidx.compose.runtime.Composable
private fun RunAction(id: String, label: String, onClose: () -> Unit) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val scope = rememberCoroutineScope()
  val critical = WearControl.isCritical(id)
  var phase by remember { mutableStateOf(if (critical) Phase.CONFIRM else Phase.RUNNING) }
  var message by remember { mutableStateOf("") }
  var ok by remember { mutableStateOf(false) }

  fun start() {
    phase = Phase.RUNNING
    scope.launch {
      val result = WearControl.run(context, id)
      ok = result == WearResult.DIRECT || result == WearResult.RELAYED
      message = context.getString(
        when (result) {
          WearResult.DIRECT -> R.string.wear_sent_car
          WearResult.RELAYED -> R.string.sent_to_phone
          WearResult.FAILED -> R.string.wear_failed
          WearResult.WATCH_LOCKED -> R.string.wear_locked
        },
      )
      phase = Phase.DONE
    }
  }

  LaunchedEffect(Unit) { if (!critical) start() }
  LaunchedEffect(phase) {
    when (phase) {
      Phase.CONFIRM -> { delay(6_000); onClose() }
      Phase.DONE -> { delay(1_800); onClose() }
      Phase.RUNNING -> Unit
    }
  }

  val accent = if (critical) Color(0xFFFF4D40) else MaterialTheme.colors.primary
  Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Box(
        Modifier.size(84.dp).clip(CircleShape).background(accent.copy(alpha = if (phase == Phase.CONFIRM) .9f else .25f))
          .clickable(enabled = phase == Phase.CONFIRM) { start() },
        contentAlignment = Alignment.Center,
      ) {
        if (phase == Phase.RUNNING) CircularProgressIndicator(Modifier.size(84.dp), indicatorColor = accent)
        Icon(iconForId(id), contentDescription = label, tint = Color.White, modifier = Modifier.size(40.dp))
      }
      Text(label, textAlign = TextAlign.Center, style = MaterialTheme.typography.title3, modifier = Modifier.padding(horizontal = 24.dp))
      Text(
        when (phase) {
          Phase.CONFIRM -> stringResource(R.string.wear_tap_to_confirm)
          Phase.RUNNING -> ""
          Phase.DONE -> (if (ok) "✓ " else "✗ ") + message
        },
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.caption2,
        modifier = Modifier.padding(horizontal = 28.dp),
      )
    }
  }
}
