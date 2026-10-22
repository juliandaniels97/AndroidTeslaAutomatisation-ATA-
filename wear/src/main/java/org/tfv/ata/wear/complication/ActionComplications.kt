package org.tfv.ata.wear.complication

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImage
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.data.SmallImageType
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import org.tfv.ata.core.WearContract
import org.tfv.ata.wear.ComplicationActionActivity
import org.tfv.ata.wear.WatchAction
import org.tfv.ata.wear.WearStore
import org.tfv.ata.wear.iconForId
import org.tfv.ata.wear.toIcon

/**
 * A direct action on the watch face. Which action each of the four slots runs is chosen in the phone app
 * (Settings → Wear OS watch → complications).
 */
abstract class ActionComplication(private val slot: Int) : SuspendingComplicationDataSourceService() {

  private fun action(): WatchAction {
    val chosen = WearStore.currentConfig(this).complications.getOrNull(slot)
    if (chosen != null) return chosen
    val id = WearContract.DEFAULT_COMPLICATION_IDS[slot]
    return WatchAction(id, id.replace('_', ' ').replaceFirstChar { it.uppercase() })
  }

  override fun getPreviewData(type: ComplicationType): ComplicationData? = build(type, action())

  override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? = build(request.complicationType, action())

  private fun build(type: ComplicationType, action: WatchAction): ComplicationData? {
    val icon = iconForId(action.id).toIcon()
    val desc = text(action.label)
    val tap = PendingIntent.getActivity(
      this, slot,
      Intent(this, ComplicationActionActivity::class.java)
        .putExtra(ComplicationActionActivity.EXTRA_ACTION_ID, action.id)
        .putExtra(ComplicationActionActivity.EXTRA_LABEL, action.label)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    return when (type) {
      ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(text(action.label.take(7)), desc)
        .setMonochromaticImage(MonochromaticImage.Builder(icon).build()).setTapAction(tap).build()
      ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(text(action.label), desc)
        .setMonochromaticImage(MonochromaticImage.Builder(icon).build()).setTapAction(tap).build()
      ComplicationType.MONOCHROMATIC_IMAGE -> MonochromaticImageComplicationData.Builder(MonochromaticImage.Builder(icon).build(), desc)
        .setTapAction(tap).build()
      ComplicationType.SMALL_IMAGE -> SmallImageComplicationData.Builder(SmallImage.Builder(icon, SmallImageType.ICON).build(), desc)
        .setTapAction(tap).build()
      else -> null
    }
  }
}

class ActionSlot1Complication : ActionComplication(0)
class ActionSlot2Complication : ActionComplication(1)
class ActionSlot3Complication : ActionComplication(2)
class ActionSlot4Complication : ActionComplication(3)
