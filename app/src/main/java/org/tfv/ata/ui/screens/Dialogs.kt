package org.tfv.ata.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.tfv.ata.R
import org.tfv.ata.action.ActionInfo
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import org.tfv.ata.core.ActionPolicy
import org.tfv.ata.core.AtaAction
import kotlin.math.roundToInt

@Composable
fun ActionPickerDialog(
  title: String,
  actions: List<AtaAction>,
  onPick: (AtaAction) -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = {
      Column(Modifier.verticalScroll(rememberScrollState()).heightIn(max = 400.dp)) {
        actions.forEach { action ->
          Text(
            text = stringResource(ActionInfo.labelRes(action)),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onPick(action) }
              .padding(vertical = 14.dp),
          )
        }
      }
    },
    confirmButton = {},
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
  )
}

@Composable
fun ValueInputDialog(
  action: AtaAction,
  onConfirm: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val range = ActionPolicy.valueRange(action)
  if (range != null) {
    // Numbers: a slider in the action's range (seat level, volume, temperature, limit, amps).
    var pick by remember { mutableStateOf(((range.first + range.last) / 2).toFloat()) }
    val n = pick.toInt()
    val shown = when (action) {
      AtaAction.CHARGE_LIMIT -> "$n %"
      AtaAction.CHARGING_AMPS -> "$n A"
      AtaAction.SET_TEMPERATURE, AtaAction.SET_TEMPERATURE_DRIVER, AtaAction.SET_TEMPERATURE_PASSENGER -> "$n °C"
      AtaAction.SET_VOLUME -> String.format(java.util.Locale.ROOT, "%.1f", n / 10f)
      else -> if (n == 0) stringResource(R.string.st_off) else n.toString()
    }
    AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text(stringResource(ActionInfo.labelRes(action))) },
      text = {
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
          Text(shown, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
          androidx.compose.material3.Slider(
            value = pick,
            onValueChange = { pick = it.roundToInt().toFloat() },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (range.last - range.first - 1).coerceIn(0, 200),
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = { TextButton(onClick = { onConfirm(n.toString()) }) { Text(stringResource(R.string.ok)) } },
      dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
    return
  }
  var value by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(ActionInfo.labelRes(action))) },
    text = {
      OutlinedTextField(
        value = value,
        onValueChange = { value = it },
        label = { Text(stringResource(R.string.address_or_place)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
      )
    },
    confirmButton = {
      TextButton(enabled = value.isNotBlank(), onClick = { onConfirm(value.trim()) }) {
        Text(stringResource(R.string.ok))
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
  )
}
