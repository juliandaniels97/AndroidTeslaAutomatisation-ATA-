package org.tfv.ata.action

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.tfv.ata.R
import org.tfv.ata.automation.ActionRequest
import org.tfv.ata.automation.BackgroundLaunch
import org.tfv.ata.automation.Notifier
import org.tfv.ata.core.ActionPolicy
import org.tfv.ata.core.AtaAction
import org.tfv.ata.core.after
import org.tfv.ata.core.projected
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
      // Opening another app from the background needs "Display over other apps" (Android's BAL rules).
      val canOpen = inForeground || source == TriggerSource.IN_APP || BackgroundLaunch.allowed(context)
      return ActionExecutor.run(context, action, vehicle, settings, value, foreground = canOpen)
    }

    if (settings.demoMode) return runDemo(context, action, value, source, inForeground)

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
    if (result is CommandResult.Success && vehicle != null) {
      // Show the new state (seat level, lock, volume …) right away; the next status read confirms it.
      val store = org.tfv.ata.data.AtaStore.get(context)
      store.snapshot(vehicle.id).first()?.let { store.saveSnapshot(vehicle.id, it.after(action, number)) }
      runCatching { org.tfv.ata.widget.StatusWidget().updateAll(context) }
    }
    feedback(context, action, result, source, inForeground)
    return result
  }

  /** Demo mode: nothing is sent; after a short "connection" the made-up car takes the new state. */
  private suspend fun runDemo(context: Context, action: AtaAction, value: String?, source: TriggerSource, inForeground: Boolean): CommandResult {
    val number = if (action.needsValue) {
      ActionPolicy.parseValue(action, value) ?: run {
        val result = CommandResult.Failure("invalid value", FailureCode.INVALID_VALUE)
        feedback(context, action, result, source, inForeground)
        return result
      }
    } else {
      null
    }
    kotlinx.coroutines.delay(600)
    val store = org.tfv.ata.data.AtaStore.get(context)
    val now = System.currentTimeMillis()
    val current = (store.snapshot(org.tfv.ata.core.DemoCar.VEHICLE_ID).first() ?: org.tfv.ata.core.DemoCar.start(now)).projected(now).copy(updatedAt = now)
    var next = current.after(action, number)
    // The made-up charger: 22 kW, a 75 kWh pack, so starting or a new limit gives a fitting time to full.
    if (next.charging && action in setOf(AtaAction.CHARGE_START, AtaAction.CHARGE_LIMIT)) {
      val limit = (next.chargeLimit ?: 80).coerceAtLeast(next.batteryPercent)
      next = next.copy(chargingPowerKw = 22, pluggedIn = true, minutesToFull = ((limit - next.batteryPercent) * .75f / 22f * 60f).toInt())
    }
    store.saveSnapshot(org.tfv.ata.core.DemoCar.VEHICLE_ID, next)
    runCatching { org.tfv.ata.widget.StatusWidget().updateAll(context) }
    // The watch shows the demo car too, also right after a tap on the watch.
    runCatching { org.tfv.ata.wear.WearPush.pushStatus(context, next, org.tfv.ata.core.DemoCar.vehicle.name) }
    val result = CommandResult.Success
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
