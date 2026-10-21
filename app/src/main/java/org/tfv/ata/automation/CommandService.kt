package org.tfv.ata.automation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.tfv.ata.R
import org.tfv.ata.action.CommandDispatcher
import org.tfv.ata.action.TriggerSource
import org.tfv.ata.core.AtaAction
import org.tfv.ata.core.IntentContract
import org.tfv.ata.data.AtaStore

class CommandService : Service() {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  private var running = 0

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    startForegroundSafely()
    val action = AtaAction.fromId(intent?.getStringExtra(IntentContract.EXTRA_ACTION_ID))
    if (action == null) {
      stopIfIdle()
      return START_NOT_STICKY
    }
    val value = intent?.getStringExtra(IntentContract.EXTRA_VALUE)
    val vehicleId = intent?.getStringExtra(IntentContract.EXTRA_VEHICLE_ID)
    val token = intent?.getStringExtra(IntentContract.EXTRA_TOKEN)
    val delayMs = intent?.getLongExtra(IntentContract.EXTRA_DELAY_MS, 0L) ?: 0L
    val trusted = intent?.getBooleanExtra(EXTRA_TRUSTED, false) ?: false

    running++
    scope.launch {
      try {
        val store = AtaStore.get(this@CommandService)
        val settings = store.settings.first()
        if (!settings.active) return@launch
        val source = when {
          trusted -> TriggerSource.TRUSTED
          !token.isNullOrBlank() && token == store.triggerToken() -> TriggerSource.TRUSTED
          else -> TriggerSource.EXTERNAL
        }
        if (delayMs in 1..MAX_DELAY_MS) delay(delayMs)
        val vehicles = store.vehicles.first()
        val selected = vehicleId ?: store.selectedVehicleId.first()
        val vehicle = vehicles.firstOrNull { it.id == selected } ?: vehicles.firstOrNull()
        CommandDispatcher.run(this@CommandService, action, vehicle, settings, value, source)
      } finally {
        running--
        stopIfIdle()
      }
    }
    return START_NOT_STICKY
  }

  private fun stopIfIdle() {
    if (running <= 0) stopSelf()
  }

  private fun startForegroundSafely() {
    val manager = getSystemService(NotificationManager::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      manager.createNotificationChannel(
        NotificationChannel(CHANNEL, "ATA", NotificationManager.IMPORTANCE_LOW),
      )
    }
    val notification: Notification = Notification.Builder(this, CHANNEL)
      .setContentTitle(getString(R.string.app_name))
      .setContentText(getString(R.string.cmd_running))
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .build()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
    } else {
      startForeground(NOTIF_ID, notification)
    }
  }

  override fun onDestroy() {
    scope.cancel()
    super.onDestroy()
  }

  companion object {
    private const val CHANNEL = "ata_commands"
    private const val NOTIF_ID = 42
    private const val MAX_DELAY_MS = 10 * 60 * 1000L
    const val EXTRA_TRUSTED = "trusted"

    fun start(context: Context, source: Intent, trusted: Boolean) {
      val intent = Intent(context, CommandService::class.java).apply {
        putExtra(IntentContract.EXTRA_ACTION_ID, source.getStringExtra(IntentContract.EXTRA_ACTION_ID))
        putExtra(IntentContract.EXTRA_VALUE, source.getStringExtra(IntentContract.EXTRA_VALUE))
        putExtra(IntentContract.EXTRA_VEHICLE_ID, source.getStringExtra(IntentContract.EXTRA_VEHICLE_ID))
        putExtra(IntentContract.EXTRA_TOKEN, source.getStringExtra(IntentContract.EXTRA_TOKEN))
        putExtra(IntentContract.EXTRA_DELAY_MS, source.getLongExtra(IntentContract.EXTRA_DELAY_MS, 0L))
        putExtra(EXTRA_TRUSTED, trusted)
      }
      androidx.core.content.ContextCompat.startForegroundService(context, intent)
    }

    fun startAction(context: Context, actionId: String, trusted: Boolean) {
      val intent = Intent(context, CommandService::class.java).apply {
        putExtra(IntentContract.EXTRA_ACTION_ID, actionId)
        putExtra(EXTRA_TRUSTED, trusted)
      }
      androidx.core.content.ContextCompat.startForegroundService(context, intent)
    }
  }
}
