package org.tfv.ata.wear

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tfv.ata.core.AtaAction
import org.tfv.ata.core.AtaCategory
import org.tfv.ata.core.CommandResult
import org.tfv.ata.core.ble.TeslaController

/** Where a watch action ended up. */
enum class WearResult { DIRECT, RELAYED, FAILED }

/**
 * Runs a watch action either directly over BLE (when direct control is set up) or by relaying it to
 * the phone. The watch uses its own hardware-backed key — the phone's key cannot be exported — so
 * direct control needs the watch paired with the car once.
 */
object WearControl {

  fun hasBlePermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
      ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED

  /** True when a BLE action can run straight from the watch right now. */
  fun canRunDirect(context: Context): Boolean =
    WearPrefs.directMode(context) && WearPrefs.vin(context).isNotBlank() && hasBlePermission(context)

  suspend fun run(context: Context, actionId: String): WearResult {
    val action = AtaAction.fromId(actionId)
    if (action != null && action.category == AtaCategory.FUNCTION_A && canRunDirect(context)) {
      val result = withContext(Dispatchers.IO) { TeslaController.sendCommand(context, WearPrefs.vin(context), action) }
      return if (result is CommandResult.Success) WearResult.DIRECT else WearResult.FAILED
    }
    // Basic actions, or no direct setup → relay to the phone, which runs the command.
    return if (WearSync.sendId(context, actionId)) WearResult.RELAYED else WearResult.FAILED
  }

  /** Enrol the watch's own key with the car. The car asks to trust the key — confirm on the console. */
  suspend fun pair(context: Context): Boolean {
    if (!hasBlePermission(context) || WearPrefs.vin(context).isBlank()) return false
    return withContext(Dispatchers.IO) {
      TeslaController.pairKey(context, WearPrefs.vin(context)) is CommandResult.Success
    }
  }
}
