package org.tfv.ata.widget

import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import org.tfv.ata.ui.tiles.formatMinutes
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.flow.first
import org.tfv.ata.R
import org.tfv.ata.core.VehicleSnapshot
import org.tfv.ata.data.AtaStore
import org.tfv.ata.ui.MainActivity

/** What the widget shows; "--" for values that were never read. */
private data class StatusValues(
  val name: String,
  val battery: String,
  val batteryLevel: Float,
  val range: String,
  val inside: String,
  val outside: String,
  val charging: Boolean,
  /** "48 kW · full in 1 h 20 min" while charging. */
  val chargeText: String?,
  val locked: Boolean?,
  val updated: String,
)

/**
 * Vehicle status in the look of the app's tiles: battery (with kW and time to full while charging), range,
 * inside and outside temperature and lock state. Responsive: from a single cell up to a large widget it always
 * shows every value, only arranged differently.
 */
class StatusWidget : GlanceAppWidget() {
  override val sizeMode = SizeMode.Responsive(setOf(TINY, WIDE, MEDIUM, LARGE))

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val store = AtaStore.get(context)
    val look = widgetLook(store.settings.first())
    val vehicles = store.vehicles.first()
    val selectedId = store.selectedVehicleId.first()
    val vehicle = vehicles.firstOrNull { it.id == selectedId } ?: vehicles.firstOrNull()
    val snapshot = vehicle?.let { store.snapshot(it.id).first() }?.takeIf { it.updatedAt > 0 }
    val values = values(context, vehicle?.name ?: "ATA", snapshot)

    provideContent {
      val size = LocalSize.current
      Box(
        modifier = GlanceModifier.fillMaxSize().background(look.background).cornerRadius(20.dp)
          .clickable(actionStartActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))),
        contentAlignment = Alignment.Center,
      ) {
        when {
          size.height < 90.dp && size.width < 200.dp -> Tiny(look, values)
          size.height < 90.dp -> Wide(context, look, values)
          size.width < 230.dp || size.height < 160.dp -> Medium(context, look, values)
          else -> Large(context, look, values)
        }
      }
    }
  }

  companion object {
    val TINY = DpSize(110.dp, 40.dp)
    val WIDE = DpSize(250.dp, 40.dp)
    val MEDIUM = DpSize(150.dp, 110.dp)
    val LARGE = DpSize(250.dp, 180.dp)
  }
}

private fun values(context: Context, name: String, s: VehicleSnapshot?): StatusValues {
  fun temp(c: Float) = "${Math.round(c)}°"
  val charging = s?.charging == true
  return StatusValues(
    name = name,
    battery = s?.let { "${it.batteryPercent}%" } ?: "--",
    batteryLevel = (s?.batteryPercent ?: 0) / 100f,
    range = s?.let { "${it.rangeKm} km" } ?: "--",
    inside = s?.let { temp(it.insideTempC) } ?: "--",
    outside = s?.let { temp(it.outsideTempC) } ?: "--",
    charging = charging,
    chargeText = if (charging && s != null) {
      listOfNotNull(
        "${s.chargingPowerKw} kW".takeIf { s.chargingPowerKw > 0 },
        context.getString(R.string.st_full_in_short, formatMinutes(s.minutesToFull)).takeIf { s.minutesToFull > 0 },
      ).joinToString(" · ").ifEmpty { context.getString(R.string.st_charging) }
    } else {
      null
    },
    locked = s?.locked,
    updated = s?.let {
      DateUtils.getRelativeTimeSpanString(it.updatedAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString()
    } ?: context.getString(R.string.widget_status_unknown),
  )
}

private fun style(size: TextUnit, color: ColorProvider, bold: Boolean = false) =
  TextStyle(color = color, fontSize = size, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)

private fun batteryIcon(v: StatusValues): ImageVector = if (v.charging) Icons.Filled.BatteryChargingFull else Icons.Filled.BatteryStd

@Composable
private fun LockIcon(look: WidgetLook, v: StatusValues, size: androidx.compose.ui.unit.Dp) {
  when (v.locked) {
    true -> WidgetIcon(Icons.Filled.Lock, look.charge, size)
    false -> WidgetIcon(Icons.Filled.LockOpen, look.warning, size)
    null -> Unit
  }
}

@Composable
private fun BatteryLine(look: WidgetLook, v: StatusValues, size: TextUnit, icon: androidx.compose.ui.unit.Dp) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    WidgetIcon(batteryIcon(v), if (v.charging) look.charge else look.accent, icon)
    Text(v.battery, style = style(size, if (v.charging) look.charge else look.text, bold = true), modifier = GlanceModifier.padding(start = 2.dp))
  }
}

/** Value with icon and a small label, in a card like the app's tiles. */
@Composable
private fun Cell(look: WidgetLook, icon: ImageVector, label: String, value: String, modifier: GlanceModifier, valueSize: TextUnit = 16.sp) {
  WidgetCard(look, modifier, radius = 12.dp) {
    Column(GlanceModifier.padding(vertical = 4.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        WidgetIcon(icon, look.accent, 14.dp)
        Text(value, style = style(valueSize, look.text, bold = true).copy(textAlign = TextAlign.Center), maxLines = 1, modifier = GlanceModifier.padding(start = 3.dp))
      }
      Text(label, style = style(10.sp, look.muted).copy(textAlign = TextAlign.Center), maxLines = 1)
    }
  }
}

@Composable
private fun ThreeCells(context: Context, look: WidgetLook, v: StatusValues, modifier: GlanceModifier, valueSize: TextUnit, gap: androidx.compose.ui.unit.Dp) {
  Row(modifier) {
    Cell(look, Icons.Filled.Route, context.getString(R.string.widget_status_range), v.range, GlanceModifier.defaultWeight().fillMaxHeight(), valueSize)
    Spacer(GlanceModifier.width(gap))
    Cell(look, Icons.Filled.Thermostat, context.getString(R.string.widget_status_inside), v.inside, GlanceModifier.defaultWeight().fillMaxHeight(), valueSize)
    Spacer(GlanceModifier.width(gap))
    Cell(look, Icons.Filled.WbSunny, context.getString(R.string.widget_status_outside), v.outside, GlanceModifier.defaultWeight().fillMaxHeight(), valueSize)
  }
}

/** One cell (1×1 / 2×1): two compact lines. */
@Composable
private fun Tiny(look: WidgetLook, v: StatusValues) {
  Column(GlanceModifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      BatteryLine(look, v, 15.sp, 16.dp)
      Text("  ${v.range}", style = style(12.sp, look.text), maxLines = 1)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(v.chargeText ?: "${v.inside} / ${v.outside}", style = style(11.sp, if (v.charging) look.charge else look.muted), maxLines = 1)
      Spacer(GlanceModifier.width(4.dp))
      LockIcon(look, v, 12.dp)
    }
  }
}

/** Wide and flat (3×1, 4×1): one row of cards. */
@Composable
private fun Wide(context: Context, look: WidgetLook, v: StatusValues) {
  Row(GlanceModifier.fillMaxSize().padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
    WidgetCard(look, GlanceModifier.defaultWeight().fillMaxHeight(), radius = 12.dp) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BatteryLine(look, v, 16.sp, 15.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
          LockIcon(look, v, 11.dp)
          Text(
            v.chargeText ?: context.getString(R.string.widget_status_battery),
            style = style(10.sp, if (v.charging) look.charge else look.muted),
            maxLines = 1,
            modifier = GlanceModifier.padding(start = 2.dp),
          )
        }
      }
    }
    Spacer(GlanceModifier.width(4.dp))
    ThreeCells(context, look, v, GlanceModifier.defaultWeight().fillMaxHeight(), 14.sp, 4.dp)
  }
}

@Composable
private fun Header(look: WidgetLook, v: StatusValues, size: TextUnit) {
  Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    WidgetIcon(Icons.Filled.DirectionsCar, look.accent, 16.dp)
    Text(v.name, style = style(size, look.text, bold = true), maxLines = 1, modifier = GlanceModifier.defaultWeight().padding(start = 5.dp))
    LockIcon(look, v, 16.dp)
  }
}

/** 2×2: name, battery with bar and charging state, then three cards. */
@Composable
private fun Medium(context: Context, look: WidgetLook, v: StatusValues) {
  Column(GlanceModifier.fillMaxSize().padding(10.dp)) {
    Header(look, v, 12.sp)
    BatteryLine(look, v, 24.sp, 22.dp)
    if (v.chargeText != null) Text(v.chargeText, style = style(11.sp, look.charge, bold = true), maxLines = 1)
    LinearProgressIndicator(
      progress = v.batteryLevel,
      modifier = GlanceModifier.fillMaxWidth().height(5.dp).cornerRadius(3.dp),
      color = if (v.charging) look.charge else look.accent,
      backgroundColor = look.track,
    )
    Spacer(GlanceModifier.height(6.dp))
    ThreeCells(context, look, v, GlanceModifier.fillMaxWidth().defaultWeight(), 13.sp, 4.dp)
  }
}

/** 4×2 and bigger: like the medium one, larger, with the time of the last reading. */
@Composable
private fun Large(context: Context, look: WidgetLook, v: StatusValues) {
  Column(GlanceModifier.fillMaxSize().padding(14.dp)) {
    Header(look, v, 15.sp)
    Row(verticalAlignment = Alignment.CenterVertically) {
      BatteryLine(look, v, 34.sp, 30.dp)
      Column(GlanceModifier.padding(start = 10.dp)) {
        Text(v.range, style = style(16.sp, look.muted), maxLines = 1)
        if (v.chargeText != null) Text(v.chargeText, style = style(12.sp, look.charge, bold = true), maxLines = 1)
      }
    }
    LinearProgressIndicator(
      progress = v.batteryLevel,
      modifier = GlanceModifier.fillMaxWidth().height(7.dp).cornerRadius(4.dp),
      color = if (v.charging) look.charge else look.accent,
      backgroundColor = look.track,
    )
    Spacer(GlanceModifier.height(8.dp))
    ThreeCells(context, look, v, GlanceModifier.fillMaxWidth().defaultWeight(), 18.sp, 6.dp)
    Text(v.updated, style = style(10.sp, look.muted), modifier = GlanceModifier.padding(top = 4.dp))
  }
}

class StatusWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = StatusWidget()
}
