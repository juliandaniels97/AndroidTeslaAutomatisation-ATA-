package org.tfv.ata.status

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import org.tfv.ata.data.AtaStore
import org.tfv.ata.widget.StatusWidget

/** One planned status read (see StatusPoller.sync); plans the next one when done. */
class StatusPollWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
  override suspend fun doWork(): Result {
    val settings = AtaStore.get(applicationContext).settings.first()
    val minutes = StatusPoller.minutesFor(settings) ?: return Result.success()
    if (StatusPoller.shouldReadNow(applicationContext, settings)) {
      val snapshot = runCatching { StatusPoller.pollNow(applicationContext) }.getOrNull()
      if (snapshot != null) runCatching { StatusWidget().updateAll(applicationContext) }
    }
    StatusPoller.schedule(applicationContext, minutes, ExistingWorkPolicy.APPEND_OR_REPLACE)
    return Result.success()
  }
}
