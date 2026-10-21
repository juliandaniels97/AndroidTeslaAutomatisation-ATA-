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
  var value by remember { mutableStateOf("") }
  val range = ActionPolicy.valueRange(action)
  val valid = if (range != null) ActionPolicy.parseValue(action, value) != null else value.isNotBlank()
  val label = when (action) {
    AtaAction.CHARGE_LIMIT -> stringResource(R.string.value_percent, range?.first ?: 0, range?.last ?: 0)
    AtaAction.CHARGING_AMPS -> stringResource(R.string.value_amps, range?.first ?: 0, range?.last ?: 0)
    AtaAction.SET_TEMPERATURE -> stringResource(R.string.value_celsius, range?.first ?: 0, range?.last ?: 0)
    else -> stringResource(R.string.address_or_place)
  }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(ActionInfo.labelRes(action))) },
    text = {
      OutlinedTextField(
        value = value,
        onValueChange = { value = it },
        label = { Text(label) },
        isError = value.isNotEmpty() && !valid,
        keyboardOptions = if (range != null) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
      )
    },
    confirmButton = {
      TextButton(enabled = valid, onClick = { onConfirm(value.trim()) }) {
        Text(stringResource(R.string.ok))
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
  )
}
