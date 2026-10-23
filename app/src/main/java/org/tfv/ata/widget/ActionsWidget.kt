package org.tfv.ata.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.ui.unit.sp
import androidx.glance.LocalSize
import androidx.glance.appwidget.SizeMode
import org.tfv.ata.action.CommandDispatcher
import org.tfv.ata.ui.components.iconFor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
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
import androidx.glance.color.ColorProvider as DayNight
import kotlinx.coroutines.flow.first
import org.tfv.ata.action.ActionInfo
import org.tfv.ata.automation.RunActionActivity
import org.tfv.ata.core.AtaAction
import org.tfv.ata.core.IntentContract
import org.tfv.ata.data.AtaStore

/** The chosen actions as tiles with icon and name, like the app's tiles; 2 to 4 per row depending on the width. */
class ActionsWidget : GlanceAppWidget() {
  override val sizeMode = SizeMode.Exact

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val store = AtaStore.get(context)
    val settings = store.settings.first()
    val look = widgetLook(settings)
    val token = store.triggerToken()
    val actions = settings.widgetActionIds.mapNotNull { AtaAction.fromId(it) }.filter { it.enabled && !it.needsValue }.take(8)
    provideContent {
      val size = LocalSize.current
      val columns = (size.width / 84.dp).toInt().coerceIn(2, 4)
      Column(GlanceModifier.fillMaxSize().background(look.background).cornerRadius(20.dp).padding(8.dp)) {
        Row(GlanceModifier.fillMaxWidth().padding(start = 2.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
          WidgetIcon(Icons.Filled.DirectionsCar, look.accent, 14.dp)
          Text("ATA", style = TextStyle(color = look.accent, fontWeight = FontWeight.Bold, fontSize = 13.sp), modifier = GlanceModifier.padding(start = 4.dp))
        }
        actions.chunked(columns).forEach { row ->
          Row(GlanceModifier.fillMaxWidth().padding(bottom = 6.dp)) {
            for (i in 0 until columns) {
              if (i > 0) Spacer(GlanceModifier.width(6.dp))
              val action = row.getOrNull(i)
              if (action != null) Tile(context, look, action, token, GlanceModifier.defaultWeight()) else Spacer(GlanceModifier.defaultWeight())
            }
          }
        }
      }
    }
  }
}

@Composable
private fun Tile(context: Context, look: WidgetLook, action: AtaAction, token: String, modifier: GlanceModifier) {
  val intent = Intent(context, RunActionActivity::class.java).apply {
    this.action = IntentContract.ACTION_RUN
    putExtra(IntentContract.EXTRA_ACTION_ID, action.id)
    putExtra(IntentContract.EXTRA_TOKEN, token)
  }
  val critical = action in CommandDispatcher.criticalActions
  WidgetCard(look, modifier.height(76.dp).clickable(actionStartActivity(intent)), radius = 16.dp) {
    Column(GlanceModifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
      if (look.neon) {
        WidgetIcon(iconFor(action), if (critical) look.critical else look.accent, 24.dp)
      } else {
        WidgetBadgeIcon(look, iconFor(action), 32.dp, if (critical) look.critical else look.onIconBackground)
      }
      Text(
        context.getString(ActionInfo.labelRes(action)),
        style = TextStyle(color = look.text, textAlign = TextAlign.Center, fontSize = 11.sp),
        maxLines = 2,
        modifier = GlanceModifier.padding(top = 3.dp),
      )
    }
  }
}

class ActionsWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = ActionsWidget()
}
