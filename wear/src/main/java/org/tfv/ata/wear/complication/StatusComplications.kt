package org.tfv.ata.wear.complication

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationText
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import org.tfv.ata.wear.R
import org.tfv.ata.wear.WatchStatus
import org.tfv.ata.wear.WearMainActivity
import org.tfv.ata.wear.WearStore
import org.tfv.ata.wear.toIcon
import kotlin.math.roundToInt

internal fun text(value: String): ComplicationText = PlainComplicationText.Builder(value).build()

internal fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
  context, 0,
  Intent(context, WearMainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
  PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
)

/** One status value of the car as a complication; every value is its own data source in the watch-face picker. */
abstract class StatusComplication : SuspendingComplicationDataSourceService() {
  /** Short value ("80%", "412", "21°"), or "--" when unknown. */
  protected abstract fun value(s: WatchStatus): String
  /** Small title under the value ("km", "Innen" …). */
  protected abstract fun title(s: WatchStatus): String
  /** Label for the long form. */
  protected abstract fun name(): String
  protected abstract fun icon(s: WatchStatus): ImageVector
  /** 0 … 100 for a ranged value (battery), null if the value has no range. */
  protected open fun ranged(s: WatchStatus): Float? = null

  override fun getPreviewData(type: ComplicationType): ComplicationData? =
    build(type, WatchStatus(vehicle = "Tesla", battery = 80, range = 412, inside = 21f, outside = 12f, locked = true, updatedAt = 1))

  override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? =
    build(request.complicationType, WearStore.currentStatus(this))

  private fun build(type: ComplicationType, s: WatchStatus): ComplicationData? {
    val value = if (s.known) value(s) else "--"
    val desc = text("${name()} $value")
    val image = MonochromaticImage.Builder(icon(s).toIcon()).build()
    return when (type) {
      ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(text(value), desc)
        .setTitle(text(title(s))).setMonochromaticImage(image).setTapAction(openAppIntent(this)).build()
      ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(text("${name()}: $value"), desc)
        .setTitle(text(s.vehicle.ifBlank { getString(R.string.app_name) })).setMonochromaticImage(image).setTapAction(openAppIntent(this)).build()
      ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder((if (s.known) ranged(s) else null) ?: 0f, 0f, 100f, desc)
        .setText(text(value)).setMonochromaticImage(image).setTapAction(openAppIntent(this)).build()
      ComplicationType.MONOCHROMATIC_IMAGE -> MonochromaticImageComplicationData.Builder(image, desc).setTapAction(openAppIntent(this)).build()
      else -> null
    }
  }
}

class BatteryComplication : StatusComplication() {
  override fun value(s: WatchStatus) = "${s.battery}%"
  override fun title(s: WatchStatus) = if (s.charging) "⚡" else getString(R.string.c_battery)
  override fun name() = getString(R.string.c_battery)
  override fun icon(s: WatchStatus) = if (s.charging) Icons.Filled.Bolt else Icons.Filled.BatteryFull
  override fun ranged(s: WatchStatus) = s.battery.toFloat()
}

class RangeComplication : StatusComplication() {
  override fun value(s: WatchStatus) = "${s.range}"
  override fun title(s: WatchStatus) = "km"
  override fun name() = getString(R.string.c_range)
  override fun icon(s: WatchStatus) = Icons.Filled.Route
}

class InsideTempComplication : StatusComplication() {
  override fun value(s: WatchStatus) = "${s.inside.roundToInt()}°"
  override fun title(s: WatchStatus) = getString(R.string.c_inside)
  override fun name() = getString(R.string.c_inside)
  override fun icon(s: WatchStatus) = Icons.Filled.Thermostat
}

class OutsideTempComplication : StatusComplication() {
  override fun value(s: WatchStatus) = "${s.outside.roundToInt()}°"
  override fun title(s: WatchStatus) = getString(R.string.c_outside)
  override fun name() = getString(R.string.c_outside)
  override fun icon(s: WatchStatus) = Icons.Filled.DeviceThermostat
}

class LockComplication : StatusComplication() {
  override fun value(s: WatchStatus) = when (s.locked) {
    true -> getString(R.string.c_locked)
    false -> getString(R.string.c_unlocked)
    null -> "?"
  }
  override fun title(s: WatchStatus) = s.vehicle.ifBlank { getString(R.string.app_name) }.take(8)
  override fun name() = getString(R.string.c_lock)
  override fun icon(s: WatchStatus) = if (s.locked == false) Icons.Filled.LockOpen else Icons.Filled.Lock
}
