package org.tfv.ata.wear.complication

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import org.tfv.ata.wear.WearMainActivity

class AtaComplicationService : SuspendingComplicationDataSourceService() {

  override fun getPreviewData(type: ComplicationType): ComplicationData? =
    if (type == ComplicationType.SHORT_TEXT) shortText("ATA") else null

  override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? =
    if (request.complicationType == ComplicationType.SHORT_TEXT) shortText("ATA") else null

  private fun shortText(text: String): ShortTextComplicationData {
    val label = PlainComplicationText.Builder(text).build()
    val content = PlainComplicationText.Builder(getString(org.tfv.ata.wear.R.string.app_name)).build()
    return ShortTextComplicationData.Builder(label, content)
      .setTapAction(launchIntent())
      .build()
  }

  private fun launchIntent(): PendingIntent {
    val intent = Intent(this, WearMainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return PendingIntent.getActivity(
      this, 0, intent,
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
  }
}
