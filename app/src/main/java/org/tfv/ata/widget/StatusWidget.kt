package org.tfv.ata.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.layout.Row
import kotlinx.coroutines.flow.first
import org.tfv.ata.data.AtaStore
import org.tfv.ata.ui.MainActivity

class StatusWidget : GlanceAppWidget() {
  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val store = AtaStore.get(context)
    val vehicles = store.vehicles.first()
    val selectedId = store.selectedVehicleId.first()
    val vehicle = vehicles.firstOrNull { it.id == selectedId } ?: vehicles.firstOrNull()
    val snapshot = vehicle?.let { store.snapshot(it.id).first() }

    provideContent {
      Column(
        modifier = GlanceModifier.fillMaxSize()
          .background(Color(0xFF1C1F24)).padding(12.dp)
          .clickable(actionStartActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = vehicle?.name ?: "ATA",
          style = TextStyle(color = ColorProvider(Color.White), fontWeight = FontWeight.Medium),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = snapshot?.let { "${it.batteryPercent}%" } ?: "--",
            style = TextStyle(
              color = ColorProvider(Color(0xFF4FB4FF)),
              fontWeight = FontWeight.Bold,
              fontSize = 22.sp,
            ),
          )
          if (snapshot?.charging == true) {
            Text(text = "  ⚡", style = TextStyle(color = ColorProvider(Color(0xFFFF4D40))))
          }
        }
        Text(
          text = snapshot?.let { "${it.rangeKm} km  ·  ${if (it.locked) "🔒" else "🔓"}" } ?: "--",
          style = TextStyle(color = ColorProvider(Color(0xFFABB6C4)), fontSize = 12.sp),
        )
      }
    }
  }
}

class StatusWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = StatusWidget()
}
