package org.tfv.ata.status

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.glance.appwidget.updateAll
import org.tfv.ata.widget.StatusWidget

class StatusPollWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
  override suspend fun doWork(): Result {
    val snapshot = runCatching { StatusPoller.pollNow(applicationContext) }.getOrNull()
    if (snapshot != null) runCatching { StatusWidget().updateAll(applicationContext) }
    return Result.success()
  }
}
