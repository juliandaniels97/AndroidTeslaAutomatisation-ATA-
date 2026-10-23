package org.tfv.ata.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.glance.appwidget.cornerRadius
import androidx.glance.layout.Box
import androidx.glance.text.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlinx.coroutines.flow.first
import org.tfv.ata.R
import org.tfv.ata.data.AtaStore
import org.tfv.ata.geo.CarFinder
import org.tfv.ata.geo.Geo
import org.tfv.ata.ui.MainActivity

/**
 * "Find my car": distance and direction (north-up arrow) from the phone's last position to where the car was
 * last reached. Widgets can't follow the compass live, so a tap opens the live compass in the app.
 */
class CarFinderWidget : GlanceAppWidget() {
  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val settings = AtaStore.get(context).settings.first()
    val car = CarFinder.carLocation(settings)
    val me = Geo.lastLocation(context)
    val (big, small) = when {
      car == null -> "?" to context.getString(R.string.finder_widget_no_car)
      me == null -> "?" to context.getString(R.string.finder_widget_no_gps)
      me.distanceTo(car) < 15f -> "✓" to context.getString(R.string.finder_here)
      else -> {
        val bearing = me.bearingTo(car)
        "${CarFinder.arrow(bearing)} ${CarFinder.distanceText(me.distanceTo(car))}" to
          CarFinder.cardinal(context, bearing) + " · " + context.getString(R.string.finder_widget_tap)
      }
    }
    val open = Intent(Intent.ACTION_VIEW, Uri.parse(MainActivity.DEEP_LINK_FIND_CAR))
      .setClass(context, MainActivity::class.java)
      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    val look = widgetLook(settings)
    provideContent {
      Box(
        GlanceModifier.fillMaxSize().background(look.background).cornerRadius(20.dp).padding(6.dp).clickable(actionStartActivity(open)),
      ) {
        WidgetCard(look, GlanceModifier.fillMaxSize(), radius = 16.dp) {
          Column(
            modifier = GlanceModifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            WidgetIcon(Icons.Filled.Explore, look.accent, 22.dp)
            Text(text = big, style = TextStyle(color = look.accent, fontWeight = FontWeight.Bold, fontSize = 22.sp))
            Text(text = small, style = TextStyle(color = look.muted, fontSize = 12.sp, textAlign = TextAlign.Center))
          }
        }
      }
    }
  }
}

class CarFinderWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = CarFinderWidget()
}
