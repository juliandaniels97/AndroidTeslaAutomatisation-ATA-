package org.tfv.ata.automation

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Runs a request in the background without a foreground service: used for delayed actions and when Android
 * doesn't allow ATA to start its foreground service (e.g. a Tasker broadcast while ATA is in the background).
 */
class ActionWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
  override suspend fun doWork(): Result {
    val request = ActionRequest.from(inputData) ?: return Result.failure()
    runCatching { ActionRunner.execute(applicationContext, request) }
    return Result.success()
  }
}
