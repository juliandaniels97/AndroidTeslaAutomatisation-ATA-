package org.tfv.ata.wear

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tfv.ata.core.ActionPolicy
import org.tfv.ata.core.AtaAction
import org.tfv.ata.core.AtaCategory
import org.tfv.ata.core.CommandResult
import org.tfv.ata.core.FailureCode
import org.tfv.ata.core.ble.PairOutcome
import org.tfv.ata.core.ble.PairResult
import org.tfv.ata.core.ble.TeslaController
import org.tfv.ata.core.ble.VehicleKey

/** Where a watch action ended up. */
enum class WearResult { DIRECT, RELAYED, FAILED, WATCH_LOCKED }

/**
 * Runs a watch action either directly over BLE (when direct control is set up) or by relaying it to
 * the phone. The watch uses its own hardware-backed key — the phone's key cannot be exported — so
 * direct control needs the watch paired with the car once.
 */
object WearControl {

  private val RELAY_ON = setOf(
    FailureCode.NOT_REACHABLE,
    FailureCode.BLUETOOTH_OFF,
    FailureCode.KEY_NOT_PAIRED,
    FailureCode.PERMISSION,
    FailureCode.UNSUPPORTED_DEVICE,
  )

  fun hasBlePermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
      ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED

  /** Keystore ECDH (needed for the watch's own key) exists from Wear OS 4 / API 31 on. */
  val directSupported: Boolean get() = VehicleKey.isSupported

  /** True when a BLE action can run straight from the watch right now. */
  fun canRunDirect(context: Context): Boolean =
    directSupported && WearPrefs.directMode(context) && WearPrefs.vin(context).isNotBlank() && hasBlePermission(context)

  fun isCritical(actionId: String): Boolean = AtaAction.fromId(actionId)?.let { ActionPolicy.isCritical(it) } == true

  private fun watchLocked(context: Context): Boolean =
    (context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.isDeviceLocked == true

  /** The UI has already asked for confirmation of critical actions; a locked watch never opens the car. */
  suspend fun run(context: Context, actionId: String): WearResult {
    val action = AtaAction.fromId(actionId)
    if (isCritical(actionId) && watchLocked(context)) return WearResult.WATCH_LOCKED
    if (action != null && action.category == AtaCategory.FUNCTION_A && canRunDirect(context)) {
      val result = withContext(Dispatchers.IO) { TeslaController.sendCommand(context, WearPrefs.vin(context), action) }
      if (result is CommandResult.Success) return WearResult.DIRECT
      // Nothing reached the car (not found / not paired / no permission): let the phone try instead.
      // Other failures (car rejected, no answer) are not retried, so a command never runs twice.
      val code = (result as? CommandResult.Failure)?.code
      if (code == null || code !in RELAY_ON) return WearResult.FAILED
    }
    // Basic actions, or no direct setup → relay to the phone, which runs the command and reports back.
    return if (WearSync.sendId(context, actionId)) WearResult.RELAYED else WearResult.FAILED
  }

  /** Enrol the watch's own key (Driver role) with the car. The car asks to confirm with a key card. */
  suspend fun pair(context: Context): PairResult {
    if (!directSupported) return PairResult(PairOutcome.FAILED, detail = "unsupported")
    if (!hasBlePermission(context) || WearPrefs.vin(context).isBlank()) return PairResult(PairOutcome.FAILED, detail = "setup")
    return withContext(Dispatchers.IO) { TeslaController.pairKey(context, WearPrefs.vin(context)) }
  }
}
