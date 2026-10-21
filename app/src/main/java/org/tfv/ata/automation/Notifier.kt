package org.tfv.ata.automation

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.tfv.ata.R
import org.tfv.ata.action.ActionInfo
import org.tfv.ata.core.AtaAction
import org.tfv.ata.core.CommandResult
import org.tfv.ata.core.DenyReason
import org.tfv.ata.core.FailureCode
import org.tfv.ata.ui.MainActivity

/** Feedback for commands that ran without the ATA UI (Routines, Tasker, widgets, watch, NFC). */
object Notifier {
  const val CHANNEL_RUNNING = "ata_commands"
  private const val CHANNEL_RESULTS = "ata_results"
  private const val CHANNEL_CONFIRM = "ata_confirm"
  private const val RESULT_TIMEOUT_MS = 15_000L

  fun ensureChannels(context: Context) {
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    manager.createNotificationChannel(
      NotificationChannel(CHANNEL_RUNNING, context.getString(R.string.notif_channel_running), NotificationManager.IMPORTANCE_LOW),
    )
    manager.createNotificationChannel(
      NotificationChannel(CHANNEL_RESULTS, context.getString(R.string.notif_channel_results), NotificationManager.IMPORTANCE_DEFAULT),
    )
    manager.createNotificationChannel(
      NotificationChannel(CHANNEL_CONFIRM, context.getString(R.string.notif_channel_confirm), NotificationManager.IMPORTANCE_HIGH),
    )
  }

  fun canNotify(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED &&
      NotificationManagerCompat.from(context).areNotificationsEnabled()

  /** Result of a background command. Successes vanish after a few seconds. */
  fun result(context: Context, action: AtaAction, result: CommandResult) {
    val label = context.getString(ActionInfo.labelRes(action))
    val (title, text) = when (result) {
      is CommandResult.Success -> context.getString(R.string.result_ok, label) to null
      is CommandResult.NotImplemented -> context.getString(R.string.result_failed, label) to context.getString(R.string.in_development)
      is CommandResult.Failure -> context.getString(R.string.result_failed, label) to failureText(context, result)
    }
    val builder = base(context, CHANNEL_RESULTS, title, text).setContentIntent(openApp(context))
    if (result is CommandResult.Success) builder.setTimeoutAfter(RESULT_TIMEOUT_MS)
    post(context, NOTIF_RESULT_BASE + action.ordinal, builder)
  }

  fun denied(context: Context, action: AtaAction, reason: DenyReason) {
    val label = context.getString(ActionInfo.labelRes(action))
    val text = context.getString(
      when (reason) {
        DenyReason.INACTIVE -> R.string.deny_inactive
        DenyReason.BLE_DISABLED -> R.string.deny_ble_disabled
        DenyReason.PHONE_LOCKED -> R.string.deny_locked
        DenyReason.NOT_ALLOWED_EXTERNAL -> R.string.deny_external
      },
    )
    post(context, NOTIF_RESULT_BASE + action.ordinal, base(context, CHANNEL_RESULTS, context.getString(R.string.result_blocked, label), text).setContentIntent(openApp(context)))
  }

  /** Asks the user to confirm a critical command; tapping opens [ConfirmActionActivity]. */
  fun askConfirmation(context: Context, request: ActionRequest) {
    val action = request.action ?: return
    val label = context.getString(ActionInfo.labelRes(action))
    val intent = request.putInto(Intent(context, ConfirmActionActivity::class.java))
      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    val pending = PendingIntent.getActivity(
      context, NOTIF_CONFIRM_BASE + action.ordinal, intent,
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    val builder = base(context, CHANNEL_CONFIRM, context.getString(R.string.confirm_title, label), context.getString(R.string.confirm_text))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_REMINDER)
      .setContentIntent(pending)
      .addAction(0, context.getString(R.string.confirm_action), pending)
      .setTimeoutAfter(CONFIRM_TIMEOUT_MS)
    post(context, NOTIF_CONFIRM_BASE + action.ordinal, builder)
  }

  fun cancelConfirmation(context: Context, action: AtaAction) {
    NotificationManagerCompat.from(context).cancel(NOTIF_CONFIRM_BASE + action.ordinal)
  }

  /** Android doesn't let a background app open other apps; offer a tap instead. */
  fun openPrompt(context: Context, action: AtaAction, target: Intent) {
    val label = context.getString(ActionInfo.labelRes(action))
    val pending = PendingIntent.getActivity(
      context, NOTIF_OPEN_BASE + action.ordinal, target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    val builder = base(context, CHANNEL_CONFIRM, label, context.getString(R.string.open_prompt_text))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setContentIntent(pending)
    post(context, NOTIF_OPEN_BASE + action.ordinal, builder)
  }

  /** Something ATA needs before automations can run (e.g. Bluetooth permission). */
  fun needsSetup(context: Context, text: String) {
    post(context, NOTIF_SETUP, base(context, CHANNEL_RESULTS, context.getString(R.string.app_name), text).setContentIntent(openApp(context)))
  }

  fun runningNotification(context: Context) =
    NotificationCompat.Builder(context, CHANNEL_RUNNING)
      .setSmallIcon(R.drawable.ic_launcher_monochrome)
      .setContentTitle(context.getString(R.string.app_name))
      .setContentText(context.getString(R.string.cmd_running))
      .setOngoing(true)
      .build()

  fun failureText(context: Context, failure: CommandResult.Failure): String =
    context.getString(
      when (failure.code) {
        FailureCode.NOT_REACHABLE -> R.string.fail_not_reachable
        FailureCode.PERMISSION -> R.string.fail_permission
        FailureCode.KEY_NOT_PAIRED -> R.string.fail_key_not_paired
        FailureCode.VEHICLE_REJECTED -> R.string.fail_rejected
        FailureCode.NO_RESPONSE -> R.string.fail_no_response
        FailureCode.UNSUPPORTED_DEVICE -> R.string.fail_unsupported
        FailureCode.INVALID_VALUE -> R.string.fail_invalid_value
        FailureCode.OTHER -> R.string.fail_other
      },
    ) + if (failure.code == FailureCode.VEHICLE_REJECTED || failure.code == FailureCode.OTHER) " (${failure.reason})" else ""

  private fun base(context: Context, channel: String, title: String, text: String?): NotificationCompat.Builder {
    ensureChannels(context)
    return NotificationCompat.Builder(context, channel)
      .setSmallIcon(R.drawable.ic_launcher_monochrome)
      .setContentTitle(title)
      .apply { if (text != null) setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text)) }
      .setAutoCancel(true)
  }

  private fun openApp(context: Context): PendingIntent =
    PendingIntent.getActivity(
      context, NOTIF_SETUP, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

  @android.annotation.SuppressLint("MissingPermission") // checked in canNotify()
  private fun post(context: Context, id: Int, builder: NotificationCompat.Builder) {
    if (!canNotify(context)) return
    try {
      NotificationManagerCompat.from(context).notify(id, builder.build())
    } catch (e: SecurityException) {
      // Permission revoked between the check and the call.
    }
  }

  const val NOTIF_RUNNING = 42
  private const val NOTIF_SETUP = 43
  private const val NOTIF_RESULT_BASE = 1_000
  private const val NOTIF_CONFIRM_BASE = 2_000
  private const val NOTIF_OPEN_BASE = 3_000
  private const val CONFIRM_TIMEOUT_MS = 5 * 60 * 1000L
}
