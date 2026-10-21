package org.tfv.ata.action

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import org.tfv.ata.R
import org.tfv.ata.automation.Notifier
import org.tfv.ata.core.AtaAction
import org.tfv.ata.core.AtaCategory
import org.tfv.ata.core.AtaSettings
import org.tfv.ata.core.CommandResult
import org.tfv.ata.core.DeepLinks
import org.tfv.ata.core.FailureCode
import org.tfv.ata.core.Vehicle

/** Runs Basic ("Function B") actions: opens the Tesla app, navigation or the share sheet. */
object ActionExecutor {

  /**
   * @param foreground true when called from a visible activity. Android blocks background apps from
   *   opening other apps, so in the background the action is offered as a tappable notification instead.
   */
  fun run(
    context: Context,
    action: AtaAction,
    vehicle: Vehicle?,
    settings: AtaSettings,
    value: String? = null,
    foreground: Boolean = true,
  ): CommandResult {
    if (action.category == AtaCategory.FUNCTION_A) return CommandResult.NotImplemented
    val intent = buildIntent(context, action, vehicle, settings, value)
    if (intent == null) {
      val result = CommandResult.Failure("address missing", FailureCode.INVALID_VALUE)
      if (foreground) toast(context, R.string.address_missing) else Notifier.result(context, action, result)
      return result
    }
    if (!foreground) {
      Notifier.openPrompt(context, action, intent)
      return CommandResult.Failure("tap the notification to open", FailureCode.OPEN_ON_PHONE)
    }
    return try {
      context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
      CommandResult.Success
    } catch (e: Exception) {
      toast(context, R.string.error_no_app)
      CommandResult.Failure(e.message ?: "error")
    }
  }

  /** Toasts only work on the main thread; background callers (watch, workers) reach this from elsewhere. */
  private fun toast(context: Context, res: Int) {
    val app = context.applicationContext
    val show = { Toast.makeText(app, res, Toast.LENGTH_SHORT).show() }
    if (Looper.myLooper() == Looper.getMainLooper()) show() else Handler(Looper.getMainLooper()).post { show() }
  }

  private fun buildIntent(
    context: Context,
    action: AtaAction,
    vehicle: Vehicle?,
    settings: AtaSettings,
    value: String?,
  ): Intent? {
    val navApp = settings.navigationApp
    val home = settings.homeAddress.ifBlank { vehicle?.navHome.orEmpty() }
    val work = settings.workAddress.ifBlank { vehicle?.navWork.orEmpty() }
    return when (action) {
      AtaAction.OPEN_TESLA_APP -> DeepLinks.launchTeslaApp(context)
      AtaAction.OPEN_CHARGING -> DeepLinks.teslaSection(context, "charging", "charge", "car/charging")
      AtaAction.OPEN_CLIMATE -> DeepLinks.teslaSection(context, "climate", "hvac", "car/climate")
      AtaAction.OPEN_CONTROLS -> DeepLinks.teslaSection(context, "controls", "control", "car/controls")
      AtaAction.OPEN_LOCATION -> DeepLinks.teslaSection(context, "location", "map", "car/location")
      AtaAction.NAVIGATE_HOME -> home.takeIf { it.isNotBlank() }?.let { DeepLinks.navigate(context, navApp, it) }
      AtaAction.NAVIGATE_WORK -> work.takeIf { it.isNotBlank() }?.let { DeepLinks.navigate(context, navApp, it) }
      AtaAction.NAVIGATE_ADDRESS -> value?.takeIf { it.isNotBlank() }?.let { DeepLinks.navigate(context, navApp, it) }
      AtaAction.SHARE_LOCATION -> value?.takeIf { it.isNotBlank() }?.let { DeepLinks.shareDestination(it) }
      else -> null
    }
  }
}
