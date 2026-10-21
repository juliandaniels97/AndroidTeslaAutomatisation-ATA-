package org.tfv.ata.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
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
import kotlinx.coroutines.flow.first
import org.tfv.ata.action.ActionInfo
import org.tfv.ata.automation.RunActionActivity
import org.tfv.ata.core.AtaAction
import org.tfv.ata.core.IntentContract
import org.tfv.ata.data.AtaStore

class ActionsWidget : GlanceAppWidget() {
  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val store = AtaStore.get(context)
    val settings = store.settings.first()
    val token = store.triggerToken()
    val actions = settings.widgetActionIds.mapNotNull { AtaAction.fromId(it) }.filter { it.enabled && !it.needsValue }.take(8)
    provideContent {
      Column(GlanceModifier.fillMaxSize().background(Color(0xFF0B0D11)).padding(8.dp)) {
        Text(
          "ATA",
          style = TextStyle(color = ColorProvider(Color(0xFF4FB4FF)), fontWeight = FontWeight.Bold),
          modifier = GlanceModifier.padding(bottom = 6.dp),
        )
        actions.chunked(2).forEach { pair ->
          Row(GlanceModifier.fillMaxWidth().padding(bottom = 6.dp)) {
            Tile(context, pair[0], token, GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(6.dp))
            if (pair.size > 1) {
              Tile(context, pair[1], token, GlanceModifier.defaultWeight())
            } else {
              Spacer(GlanceModifier.defaultWeight())
            }
          }
        }
      }
    }
  }
}

@Composable
private fun Tile(context: Context, action: AtaAction, token: String, modifier: GlanceModifier) {
  val intent = Intent(context, RunActionActivity::class.java).apply {
    this.action = IntentContract.ACTION_RUN
    putExtra(IntentContract.EXTRA_ACTION_ID, action.id)
    putExtra(IntentContract.EXTRA_TOKEN, token)
  }
  Box(
    modifier = modifier
      .height(54.dp)
      .cornerRadius(14.dp)
      .background(Color(0xFF1B1F27))
      .clickable(actionStartActivity(intent))
      .padding(6.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      context.getString(ActionInfo.labelRes(action)),
      style = TextStyle(color = ColorProvider(Color.White), textAlign = TextAlign.Center),
      maxLines = 2,
    )
  }
}

class ActionsWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = ActionsWidget()
}
