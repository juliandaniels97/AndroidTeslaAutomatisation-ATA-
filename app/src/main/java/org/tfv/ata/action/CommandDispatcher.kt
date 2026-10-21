package org.tfv.ata.action

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tfv.ata.R
import org.tfv.ata.automation.ActionRequest
import org.tfv.ata.automation.Notifier
import org.tfv.ata.core.ActionPolicy
import org.tfv.ata.core.AtaAction
import org.tfv.ata.core.AtaCategory
import org.tfv.ata.core.AtaSettings
import org.tfv.ata.core.CommandResult
import org.tfv.ata.core.DenyReason
import org.tfv.ata.core.FailureCode
import org.tfv.ata.core.PolicyDecision
import org.tfv.ata.core.TriggerSource
import org.tfv.ata.core.Vehicle
import org.tfv.ata.core.ble.TeslaController

/**
 * Single entry point for running an action. Enforces [ActionPolicy] for every source, then runs the
 * action and reports the result (toast in the app, notification for automations).
 */
object CommandDispatcher {

  /** Actions that physically open the car; drawn in red and gated by confirmation. */
  val criticalActions: Set<AtaAction> get() = ActionPolicy.critical

  suspend fun run(
    context: Context,
    action: AtaAction,
    vehicle: Vehicle?,
    settings: AtaSettings,
    value: String? = null,
    source: TriggerSource = TriggerSource.IN_APP,
  ): CommandResult {
    val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    val phoneLocked = keyguard?.isDeviceLocked == true
    val inForeground = context is Activity

    when (val decision = ActionPolicy.decide(action, source, phoneLocked, settings)) {
      is PolicyDecision.Deny -> {
        feedbackDenied(context, action, decision.reason, source, inForeground)
        return CommandResult.Failure(decision.reason.name, FailureCode.OTHER)
      }
      PolicyDecision.Confirm -> {
        Notifier.askConfirmation(context, ActionRequest(action.id, source, value, vehicle?.id))
        if (inForeground) toast(context, R.string.confirm_pending)
        return CommandResult.Failure("awaiting confirmation", FailureCode.OTHER)
      }
      PolicyDecision.Allow -> Unit
    }

    if (action.category == AtaCategory.FUNCTION_B) {
      return ActionExecutor.run(context, action, vehicle, settings, value, foreground = inForeground || source == TriggerSource.IN_APP)
    }

    val vin = vehicle?.vin
    if (vin.isNullOrBlank()) {
      val result = CommandResult.Failure("no vehicle", FailureCode.OTHER)
      if (source == TriggerSource.IN_APP || inForeground) toast(context, R.string.no_vehicle) else Notifier.result(context, action, result)
      return result
    }

    val number = if (action.needsValue) {
      ActionPolicy.parseValue(action, value) ?: run {
        val result = CommandResult.Failure("invalid value", FailureCode.INVALID_VALUE)
        feedback(context, action, result, source, inForeground)
        return result
      }
    } else {
      null
    }

    val result = withContext(Dispatchers.IO) { TeslaController.sendCommand(context, vin, action, number) }
    feedback(context, action, result, source, inForeground)
    return result
  }

  private fun feedback(context: Context, action: AtaAction, result: CommandResult, source: TriggerSource, inForeground: Boolean) {
    if (source == TriggerSource.IN_APP || source == TriggerSource.CONFIRMED && inForeground) {
      val text = when (result) {
        is CommandResult.Success -> context.getString(R.string.cmd_ok)
        is CommandResult.NotImplemented -> context.getString(R.string.in_development)
        is CommandResult.Failure -> context.getString(R.string.cmd_failed) + ": " + Notifier.failureText(context, result)
      }
      toast(context, text)
    } else {
      Notifier.result(context, action, result)
    }
  }

  private fun feedbackDenied(context: Context, action: AtaAction, reason: DenyReason, source: TriggerSource, inForeground: Boolean) {
    if (source == TriggerSource.IN_APP || inForeground) {
      toast(
        context,
        when (reason) {
          DenyReason.INACTIVE -> R.string.deny_inactive
          DenyReason.BLE_DISABLED -> R.string.in_development
          DenyReason.PHONE_LOCKED -> R.string.deny_locked
          DenyReason.NOT_ALLOWED_EXTERNAL -> R.string.deny_external
        },
      )
    } else {
      Notifier.denied(context, action, reason)
    }
  }

  private fun toast(context: Context, res: Int) = toast(context, context.getString(res))

  /** Toasts must be shown from the main thread; services and workers call this from elsewhere. */
  private fun toast(context: Context, text: String) {
    val show = { Toast.makeText(context.applicationContext, text, Toast.LENGTH_LONG).show() }
    if (Looper.myLooper() == Looper.getMainLooper()) show() else Handler(Looper.getMainLooper()).post { show() }
  }
}
