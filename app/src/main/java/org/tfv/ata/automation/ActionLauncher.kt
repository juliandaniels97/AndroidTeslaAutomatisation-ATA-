package org.tfv.ata.automation

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import org.tfv.ata.R
import org.tfv.ata.core.AtaCategory
import org.tfv.ata.core.ble.TeslaController
import java.util.concurrent.TimeUnit

/** Picks the right way to run a request, depending on whether ATA is in the foreground. */
object ActionLauncher {
  const val MAX_DELAY_MS = 6 * 60 * 60 * 1000L

  /**
   * Runs [request] now. BLE actions go to the foreground service (allowed while an ATA activity is visible
   * or for exempt callers); when Android refuses that, an expedited worker runs the command instead.
   */
  fun runNow(context: Context, request: ActionRequest) {
    val action = request.action ?: return
    if (action.category == AtaCategory.FUNCTION_A) {
      if (!TeslaController.hasBlePermission(context)) {
        Notifier.needsSetup(context, context.getString(R.string.setup_ble_permission))
        return
      }
      try {
        ContextCompat.startForegroundService(context, request.putInto(Intent(context, CommandService::class.java)))
        return
      } catch (e: IllegalStateException) {
        // ForegroundServiceStartNotAllowedException (background start, Android 12+): fall back to a worker.
      } catch (e: SecurityException) {
        Notifier.needsSetup(context, context.getString(R.string.setup_ble_permission))
        return
      }
    }
    enqueue(context, request, delayMs = 0)
  }

  /** Runs [request] after [delayMs] (approximately: Android may batch background work). */
  fun schedule(context: Context, request: ActionRequest, delayMs: Long) {
    enqueue(context, request, delayMs.coerceIn(0, MAX_DELAY_MS))
  }

  private fun enqueue(context: Context, request: ActionRequest, delayMs: Long) {
    val builder = OneTimeWorkRequestBuilder<ActionWorker>().setInputData(request.toData())
    if (delayMs > 0) {
      builder.setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
    } else {
      builder.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
    }
    WorkManager.getInstance(context).enqueueUniqueWork(
      "ata-action-${request.actionId}-${System.nanoTime()}",
      ExistingWorkPolicy.KEEP,
      builder.build(),
    )
  }
}
