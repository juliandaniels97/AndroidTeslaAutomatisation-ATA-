package org.tfv.ata.action

import android.app.KeyguardManager
import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tfv.ata.R
import org.tfv.ata.core.AtaAction
import org.tfv.ata.core.AtaCategory
import org.tfv.ata.core.AtaSettings
import org.tfv.ata.core.CommandResult
import org.tfv.ata.core.Vehicle
import org.tfv.ata.core.ble.TeslaController

enum class TriggerSource { IN_APP, TRUSTED, EXTERNAL }

object CommandDispatcher {

  /**
   * Actions that physically open something on the car. These are the only ones that get the
   * optional confirmation / biometric gate in-app and are drawn in red everywhere else.
   */
  val criticalActions = setOf(
    AtaAction.UNLOCK, AtaAction.CHARGE_PORT_OPEN, AtaAction.TRUNK, AtaAction.FRUNK,
  )

  suspend fun run(
    context: Context,
    action: AtaAction,
    vehicle: Vehicle?,
    settings: AtaSettings,
    value: String? = null,
    source: TriggerSource = TriggerSource.IN_APP,
  ): CommandResult {
    if (action.category == AtaCategory.FUNCTION_B) {
      return ActionExecutor.run(context, action, vehicle, settings, value)
    }
    if (!settings.experimentalBle) {
      toast(context, R.string.in_development)
      return CommandResult.NotImplemented
    }
    val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    if (keyguard?.isDeviceLocked == true) {
      toast(context, R.string.blocked_locked)
      return CommandResult.Failure("device locked")
    }
    val vin = vehicle?.vin
    if (vin.isNullOrBlank()) {
      toast(context, R.string.no_vehicle)
      return CommandResult.Failure("no vehicle")
    }
    val result = withContext(Dispatchers.IO) {
      TeslaController.sendCommand(context, vin, action, value?.toIntOrNull())
    }
    toast(
      context,
      when (result) {
        is CommandResult.Success -> R.string.cmd_ok
        is CommandResult.NotImplemented -> R.string.in_development
        is CommandResult.Failure -> R.string.cmd_failed
      },
    )
    return result
  }

  private fun toast(context: Context, res: Int) =
    Toast.makeText(context, res, Toast.LENGTH_SHORT).show()
}
