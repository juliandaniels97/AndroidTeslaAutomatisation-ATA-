package org.tfv.ata.action

import android.content.Context
import android.content.Intent
import android.widget.Toast
import org.tfv.ata.R
import org.tfv.ata.core.AtaAction
import org.tfv.ata.core.AtaSettings
import org.tfv.ata.core.CommandResult
import org.tfv.ata.core.DeepLinks
import org.tfv.ata.core.NavigationApp
import org.tfv.ata.core.Vehicle

object ActionExecutor {

  fun run(
    context: Context,
    action: AtaAction,
    vehicle: Vehicle?,
    settings: AtaSettings,
    value: String? = null,
  ): CommandResult {
    if (action.category == org.tfv.ata.core.AtaCategory.FUNCTION_A) {
      Toast.makeText(context, R.string.in_development, Toast.LENGTH_SHORT).show()
      return CommandResult.NotImplemented
    }
    val intent = buildIntent(context, action, vehicle, settings, value)
    if (intent == null) {
      Toast.makeText(context, R.string.address_missing, Toast.LENGTH_SHORT).show()
      return CommandResult.Failure("no intent")
    }
    return try {
      context.startActivity(intent)
      CommandResult.Success
    } catch (e: Exception) {
      Toast.makeText(context, R.string.error_no_app, Toast.LENGTH_SHORT).show()
      CommandResult.Failure(e.message ?: "error")
    }
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
