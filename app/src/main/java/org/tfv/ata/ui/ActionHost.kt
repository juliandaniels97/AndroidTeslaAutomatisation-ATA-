package org.tfv.ata.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch
import org.tfv.ata.R
import org.tfv.ata.action.ActionInfo
import org.tfv.ata.action.CommandDispatcher
import org.tfv.ata.core.AtaAction
import org.tfv.ata.core.AtaCategory
import org.tfv.ata.core.CommandResult
import org.tfv.ata.geo.Geo
import org.tfv.ata.geo.rememberDeviceHeading
import org.tfv.ata.ui.screens.ValueInputDialog

/**
 * Hosts the full action-run flow (permissions, confirm, biometric, connection + activation FX)
 * for both Basic and BLE actions, and hands a `run(action)` lambda to [content].
 */
@Composable
fun ActionHost(
  vm: AtaViewModel,
  state: AtaUiState,
  onAddVehicle: () -> Unit = {},
  content: @Composable (run: (AtaAction) -> Unit) -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val heading by rememberDeviceHeading(enabled = state.settings.directionFx)

  var pendingAction by remember { mutableStateOf<AtaAction?>(null) }
  var pendingValue by remember { mutableStateOf<String?>(null) }
  var confirmAction by remember { mutableStateOf<AtaAction?>(null) }
  var valueAction by remember { mutableStateOf<AtaAction?>(null) }
  var noVehicle by remember { mutableStateOf(false) }
  var enableBle by remember { mutableStateOf(false) }

  fun launchCommand(action: AtaAction, value: String?) {
    scope.launch {
      val isBle = action.category == AtaCategory.FUNCTION_A
      if (isBle) vm.setConnecting(true)
      val result = CommandDispatcher.run(context, action, state.selected, state.settings, value)
      if (isBle) vm.setConnecting(false)
      val success = result is CommandResult.Success
      if (success && isBle) {
        vm.recordCarLocation()
        vm.onCarAction(action)
      }
      vm.playFx(success, Geo.screenBearingToCar(context, state.settings, heading))
    }
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions(),
  ) { result ->
    // Location is optional (finding the car without the Tesla app connected); Bluetooth is required.
    val granted = org.tfv.ata.core.ble.TeslaController.hasBlePermission(context)
    val action = pendingAction
    val value = pendingValue
    pendingAction = null
    pendingValue = null
    if (granted && action != null) launchCommand(action, value)
  }

  fun runWithPermissions(action: AtaAction, value: String?) {
    if (action.category == AtaCategory.FUNCTION_A) {
      val perms = org.tfv.ata.core.ble.TeslaController.BLE_PERMISSIONS
      val missing = perms.any { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }
      if (missing) {
        pendingAction = action
        pendingValue = value
        permissionLauncher.launch(perms)
        return
      }
    }
    launchCommand(action, value)
  }

  fun secure(action: AtaAction, value: String?) {
    val activity = context as? FragmentActivity
    val critical = action in CommandDispatcher.criticalActions
    if (state.settings.biometricLock && critical && activity != null) {
      BiometricGate.authenticate(
        activity,
        activity.getString(ActionInfo.labelRes(action)),
        onSuccess = { runWithPermissions(action, value) },
        onFailure = {},
      )
    } else {
      runWithPermissions(action, value)
    }
  }

  fun trigger(action: AtaAction) {
    if (action.category == AtaCategory.FUNCTION_A) {
      if (!state.settings.experimentalBle) {
        enableBle = true
        return
      }
      if (state.selected == null) {
        noVehicle = true
        return
      }
    }
    when {
      action.needsValue -> valueAction = action
      action in CommandDispatcher.criticalActions && state.settings.confirmCritical -> confirmAction = action
      else -> secure(action, null)
    }
  }

  content(::trigger)

  valueAction?.let { action ->
    ValueInputDialog(
      action = action,
      onConfirm = { v -> valueAction = null; secure(action, v) },
      onDismiss = { valueAction = null },
    )
  }

  confirmAction?.let { action ->
    AlertDialog(
      onDismissRequest = { confirmAction = null },
      title = { Text(stringResource(ActionInfo.labelRes(action))) },
      text = { Text(stringResource(R.string.confirm_critical_message)) },
      confirmButton = {
        TextButton(onClick = { confirmAction = null; secure(action, null) }) { Text(stringResource(R.string.ok)) }
      },
      dismissButton = {
        TextButton(onClick = { confirmAction = null }) { Text(stringResource(R.string.cancel)) }
      },
    )
  }

  if (noVehicle) {
    AlertDialog(
      onDismissRequest = { noVehicle = false },
      title = { Text(stringResource(R.string.no_vehicle_title)) },
      text = { Text(stringResource(R.string.no_vehicle_message)) },
      confirmButton = {
        TextButton(onClick = { noVehicle = false; onAddVehicle() }) { Text(stringResource(R.string.add_now)) }
      },
      dismissButton = {
        TextButton(onClick = { noVehicle = false }) { Text(stringResource(R.string.cancel)) }
      },
    )
  }

  if (enableBle) {
    AlertDialog(
      onDismissRequest = { enableBle = false },
      title = { Text(stringResource(R.string.ble_enable_title)) },
      text = { Text(stringResource(R.string.ble_enable_desc)) },
      confirmButton = {
        TextButton(onClick = {
          enableBle = false
          vm.updateSettings { it.copy(experimentalBle = true) }
        }) { Text(stringResource(R.string.activate)) }
      },
      dismissButton = {
        TextButton(onClick = { enableBle = false }) { Text(stringResource(R.string.cancel)) }
      },
    )
  }
}
