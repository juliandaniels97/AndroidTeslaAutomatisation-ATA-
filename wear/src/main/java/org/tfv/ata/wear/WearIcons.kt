package org.tfv.ata.wear

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirlineSeatReclineNormal
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DoorBack
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Masks
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Window
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath
import org.tfv.ata.core.AtaAction

/** Icon of an action id (same icons as in the phone app); a car for unknown ids. */
fun iconForId(id: String): ImageVector = AtaAction.fromId(id)?.let { iconFor(it) } ?: Icons.Filled.DirectionsCar

/**
 * Draws a Material icon into a white bitmap mask for complications (they get no Compose runtime; the watch face
 * tints the image itself).
 */
fun ImageVector.toIcon(sizePx: Int = 96): Icon {
  val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(bitmap)
  canvas.scale(sizePx / viewportWidth, sizePx / viewportHeight)
  val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
  fun draw(group: VectorGroup) {
    for (node in group) {
      when (node) {
        is VectorGroup -> draw(node)
        is VectorPath -> {
          val path = node.pathData.toPath().also { it.fillType = node.pathFillType }.asAndroidPath()
          paint.style = Paint.Style.FILL
          paint.alpha = (255 * node.fillAlpha).toInt()
          canvas.drawPath(path, paint)
        }
      }
    }
  }
  draw(root)
  return Icon.createWithBitmap(bitmap)
}

private fun iconFor(action: AtaAction): ImageVector = when (action) {
  AtaAction.OPEN_TESLA_APP -> Icons.Filled.OpenInNew
  AtaAction.OPEN_CHARGING -> Icons.Filled.EvStation
  AtaAction.OPEN_CLIMATE -> Icons.Filled.Thermostat
  AtaAction.OPEN_CONTROLS -> Icons.Filled.Tune
  AtaAction.OPEN_LOCATION -> Icons.Filled.Map
  AtaAction.NAVIGATE_HOME -> Icons.Filled.Navigation
  AtaAction.NAVIGATE_WORK -> Icons.Filled.Navigation
  AtaAction.NAVIGATE_ADDRESS -> Icons.Filled.Map
  AtaAction.SHARE_LOCATION -> Icons.Filled.Share
  AtaAction.WAKE -> Icons.Filled.PowerSettingsNew
  AtaAction.LOCK -> Icons.Filled.Lock
  AtaAction.UNLOCK -> Icons.Filled.LockOpen
  AtaAction.CLIMATE_ON -> Icons.Filled.Air
  AtaAction.CLIMATE_OFF -> Icons.Filled.PowerSettingsNew
  AtaAction.SET_TEMPERATURE -> Icons.Filled.Thermostat
  AtaAction.STEERING_WHEEL_HEATER -> Icons.Filled.WbSunny
  AtaAction.DEFROST -> Icons.Filled.AcUnit
  AtaAction.CHARGE_START -> Icons.Filled.Bolt
  AtaAction.CHARGE_STOP -> Icons.Filled.Bolt
  AtaAction.CHARGE_LIMIT -> Icons.Filled.EvStation
  AtaAction.CHARGING_AMPS -> Icons.Filled.EvStation
  AtaAction.CHARGE_PORT_OPEN -> Icons.Filled.EvStation
  AtaAction.CHARGE_PORT_CLOSE -> Icons.Filled.EvStation
  AtaAction.TRUNK -> Icons.Filled.Luggage
  AtaAction.FRUNK -> Icons.Filled.Inventory2
  AtaAction.VENT_WINDOWS -> Icons.Filled.Window
  AtaAction.CLOSE_WINDOWS -> Icons.Filled.Window
  AtaAction.HONK -> Icons.Filled.VolumeUp
  AtaAction.FLASH_LIGHTS -> Icons.Filled.FlashOn
  AtaAction.SENTRY_ON -> Icons.Filled.Security
  AtaAction.SENTRY_OFF -> Icons.Filled.Security
  AtaAction.CLIMATE_KEEP -> Icons.Filled.Air
  AtaAction.DOG_MODE -> Icons.Filled.Pets
  AtaAction.CAMP_MODE -> Icons.Filled.Nightlight
  AtaAction.CLIMATE_KEEP_OFF -> Icons.Filled.PowerSettingsNew
  AtaAction.DEFROST_OFF -> Icons.Filled.AcUnit
  AtaAction.STEERING_WHEEL_HEATER_OFF -> Icons.Filled.WbSunny
  AtaAction.BIOWEAPON_ON -> Icons.Filled.Masks
  AtaAction.BIOWEAPON_OFF -> Icons.Filled.Masks
  AtaAction.OVERHEAT_PROTECTION_ON -> Icons.Filled.DeviceThermostat
  AtaAction.OVERHEAT_PROTECTION_OFF -> Icons.Filled.DeviceThermostat
  AtaAction.DOOR_DRIVER_OPEN -> Icons.Filled.DoorFront
  AtaAction.DOOR_PASSENGER_OPEN -> Icons.Filled.DoorBack
  AtaAction.TRUNK_CLOSE -> Icons.Filled.Luggage
  AtaAction.REMOTE_DRIVE -> Icons.Filled.Key
  AtaAction.MEDIA_TOGGLE -> Icons.Filled.PlayArrow
  AtaAction.MEDIA_NEXT -> Icons.Filled.SkipNext
  AtaAction.MEDIA_PREVIOUS -> Icons.Filled.SkipPrevious
  AtaAction.SEAT_HEATER_FRONT_LEFT, AtaAction.SEAT_HEATER_FRONT_RIGHT, AtaAction.SEAT_HEATER_REAR_LEFT,
  AtaAction.SEAT_HEATER_REAR_CENTER, AtaAction.SEAT_HEATER_REAR_RIGHT -> Icons.Filled.AirlineSeatReclineNormal
  AtaAction.SEAT_COOLER_FRONT_LEFT, AtaAction.SEAT_COOLER_FRONT_RIGHT -> Icons.Filled.AcUnit
  AtaAction.SET_VOLUME -> Icons.Filled.Speaker
  AtaAction.VOLUME_UP -> Icons.Filled.VolumeUp
  AtaAction.VOLUME_DOWN -> Icons.Filled.VolumeDown
  AtaAction.SET_TEMPERATURE_DRIVER, AtaAction.SET_TEMPERATURE_PASSENGER -> Icons.Filled.Thermostat
  AtaAction.OVERHEAT_PROTECTION_FAN_ONLY -> Icons.Filled.Air
  AtaAction.SEAT_AUTO_FRONT_LEFT, AtaAction.SEAT_AUTO_FRONT_RIGHT -> Icons.Filled.AirlineSeatReclineNormal
}
