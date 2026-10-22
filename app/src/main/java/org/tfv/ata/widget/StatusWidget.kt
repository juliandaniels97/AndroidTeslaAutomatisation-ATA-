package org.tfv.ata.widget

import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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
import androidx.glance.color.ColorProvider as DayNight

private val BACKGROUND = DayNight(day = Color(0xFFF4F6F9), night = Color(0xFF1C1F24))
private val CELL = DayNight(day = Color(0xFFE6EBF1), night = Color(0xFF262A31))
private val TEXT = DayNight(day = Color(0xFF161B22), night = Color.White)
private val MUTED = DayNight(day = Color(0xFF5B6B7C), night = Color(0xFFABB6C4))
private val ACCENT = DayNight(day = Color(0xFF0B63BD), night = Color(0xFF4FB4FF))
private val CHARGE = DayNight(day = Color(0xFF1E8E4E), night = Color(0xFF2ECC71))

/** What the widget shows; "--" for values that were never read. */
private data class StatusValues(
  val name: String,
  val battery: String,
  val batteryLevel: Float,
  val range: String,
  val inside: String,
  val outside: String,
  val charging: Boolean,
  val lock: String,
  val updated: String,
)

/**
 * Vehicle status: battery, range, inside and outside temperature, charging and lock state. Responsive: from a
 * single cell up to a large widget it always shows every value, only arranged differently.
 */
class StatusWidget : GlanceAppWidget() {
  override val sizeMode = SizeMode.Responsive(setOf(TINY, WIDE, MEDIUM, LARGE))

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val store = AtaStore.get(context)
    val vehicles = store.vehicles.first()
    val selectedId = store.selectedVehicleId.first()
    val vehicle = vehicles.firstOrNull { it.id == selectedId } ?: vehicles.firstOrNull()
    val snapshot = vehicle?.let { store.snapshot(it.id).first() }?.takeIf { it.updatedAt > 0 }
    val values = values(context, vehicle?.name ?: "ATA", snapshot)

    provideContent {
      val size = LocalSize.current
      Box(
        modifier = GlanceModifier.fillMaxSize().background(BACKGROUND).cornerRadius(16.dp)
          .clickable(actionStartActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))),
        contentAlignment = Alignment.Center,
      ) {
        when {
          size.height < 90.dp && size.width < 200.dp -> Tiny(values)
          size.height < 90.dp -> Wide(context, values)
          size.width < 230.dp || size.height < 160.dp -> Medium(context, values)
          else -> Large(context, values)
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
  return StatusValues(
    name = name,
    battery = s?.let { "${it.batteryPercent}%" } ?: "--",
    batteryLevel = (s?.batteryPercent ?: 0) / 100f,
    range = s?.let { "${it.rangeKm} km" } ?: "--",
    inside = s?.let { temp(it.insideTempC) } ?: "--",
    outside = s?.let { temp(it.outsideTempC) } ?: "--",
    charging = s?.charging == true,
    lock = when (s?.locked) { true -> "🔒"; false -> "🔓"; null -> "" },
    updated = s?.let {
      DateUtils.getRelativeTimeSpanString(it.updatedAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString()
    } ?: context.getString(R.string.widget_status_unknown),
  )
}

private fun style(size: TextUnit, color: ColorProvider = TEXT, bold: Boolean = false) =
  TextStyle(color = color, fontSize = size, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)

@Composable
private fun BatteryText(v: StatusValues, size: TextUnit) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Text(v.battery, style = style(size, if (v.charging) CHARGE else ACCENT, bold = true))
    if (v.charging) Text(" ⚡", style = style(size * .8f, CHARGE))
  }
}

/** Value with a small label underneath, centred in a rounded cell. */
@Composable
private fun Cell(label: String, value: String, modifier: GlanceModifier, valueSize: TextUnit = 16.sp) {
  Column(
    modifier = modifier.background(CELL).cornerRadius(10.dp).padding(vertical = 4.dp, horizontal = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(value, style = style(valueSize, bold = true).copy(textAlign = TextAlign.Center), maxLines = 1)
    Text(label, style = style(10.sp, MUTED).copy(textAlign = TextAlign.Center), maxLines = 1)
  }
}

/** One cell (1×1 / 2×1): two compact lines. */
@Composable
private fun Tiny(v: StatusValues) {
  Column(GlanceModifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      BatteryText(v, 15.sp)
      Text("  ${v.range}", style = style(12.sp), maxLines = 1)
    }
    Text("${v.inside} / ${v.outside}  ${v.lock}", style = style(11.sp, MUTED), maxLines = 1)
  }
}

/** Wide and flat (3×1, 4×1): one row of values. */
@Composable
private fun Wide(context: Context, v: StatusValues) {
  Row(GlanceModifier.fillMaxSize().padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
    Column(GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
      BatteryText(v, 17.sp)
      Text("${v.lock} ${context.getString(R.string.widget_status_battery)}", style = style(10.sp, MUTED), maxLines = 1)
    }
    Cell(context.getString(R.string.widget_status_range), v.range, GlanceModifier.defaultWeight().height(44.dp), 14.sp)
    Spacer(GlanceModifier.width(4.dp))
    Cell(context.getString(R.string.widget_status_inside), v.inside, GlanceModifier.defaultWeight().height(44.dp), 14.sp)
    Spacer(GlanceModifier.width(4.dp))
    Cell(context.getString(R.string.widget_status_outside), v.outside, GlanceModifier.defaultWeight().height(44.dp), 14.sp)
  }
}

/** 2×2: name, battery with bar, then three cells. */
@Composable
private fun Medium(context: Context, v: StatusValues) {
  Column(GlanceModifier.fillMaxSize().padding(10.dp)) {
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Text(v.name, style = style(12.sp, MUTED), maxLines = 1, modifier = GlanceModifier.defaultWeight())
      Text(v.lock, style = style(12.sp))
    }
    BatteryText(v, 24.sp)
    LinearProgressIndicator(
      progress = v.batteryLevel,
      modifier = GlanceModifier.fillMaxWidth().height(5.dp),
      color = if (v.charging) CHARGE else ACCENT,
      backgroundColor = CELL,
    )
    Spacer(GlanceModifier.height(6.dp))
    Row(GlanceModifier.fillMaxWidth().defaultWeight()) {
      Cell(context.getString(R.string.widget_status_range), v.range, GlanceModifier.defaultWeight().fillMaxHeight(), 13.sp)
      Spacer(GlanceModifier.width(4.dp))
      Cell(context.getString(R.string.widget_status_inside), v.inside, GlanceModifier.defaultWeight().fillMaxHeight(), 13.sp)
      Spacer(GlanceModifier.width(4.dp))
      Cell(context.getString(R.string.widget_status_outside), v.outside, GlanceModifier.defaultWeight().fillMaxHeight(), 13.sp)
    }
  }
}

/** 4×2 and bigger: like the medium one, larger, with the time of the last reading. */
@Composable
private fun Large(context: Context, v: StatusValues) {
  Column(GlanceModifier.fillMaxSize().padding(14.dp)) {
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Text(v.name, style = style(15.sp, bold = true), maxLines = 1, modifier = GlanceModifier.defaultWeight())
      Text(v.lock, style = style(16.sp))
    }
    Row(verticalAlignment = Alignment.Bottom) {
      BatteryText(v, 34.sp)
      Text("  ${v.range}", style = style(16.sp, MUTED), maxLines = 1)
    }
    LinearProgressIndicator(
      progress = v.batteryLevel,
      modifier = GlanceModifier.fillMaxWidth().height(7.dp),
      color = if (v.charging) CHARGE else ACCENT,
      backgroundColor = CELL,
    )
    Spacer(GlanceModifier.height(8.dp))
    Row(GlanceModifier.fillMaxWidth().defaultWeight()) {
      Cell(context.getString(R.string.widget_status_range), v.range, GlanceModifier.defaultWeight().fillMaxHeight(), 18.sp)
      Spacer(GlanceModifier.width(6.dp))
      Cell(context.getString(R.string.widget_status_inside), v.inside, GlanceModifier.defaultWeight().fillMaxHeight(), 18.sp)
      Spacer(GlanceModifier.width(6.dp))
      Cell(context.getString(R.string.widget_status_outside), v.outside, GlanceModifier.defaultWeight().fillMaxHeight(), 18.sp)
    }
    Text(v.updated, style = style(10.sp, MUTED), modifier = GlanceModifier.padding(top = 4.dp))
  }
}

class StatusWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = StatusWidget()
}
