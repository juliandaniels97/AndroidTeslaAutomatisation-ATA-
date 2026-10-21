package org.tfv.ata.automation

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.tfv.ata.core.ble.TeslaController

/**
 * Short-lived foreground service for BLE commands (scan + connect + handshake can take 20–40 s). Not
 * exported: requests only arrive through [ActionLauncher] after the entry point decided their source.
 */
class CommandService : Service() {
  private val errors = CoroutineExceptionHandler { _, _ -> }
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main + errors)
  private var running = 0

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (!startForegroundSafely()) {
      stopSelf(startId)
      return START_NOT_STICKY
    }
    val request = ActionRequest.from(intent)
    if (request == null) {
      stopIfIdle(startId)
      return START_NOT_STICKY
    }
    running++
    scope.launch {
      try {
        ActionRunner.execute(this@CommandService, request)
      } finally {
        running--
        stopIfIdle(startId)
      }
    }
    return START_NOT_STICKY
  }

  private fun stopIfIdle(startId: Int) {
    if (running <= 0) stopSelf(startId)
  }

  /** Android 14 requires Bluetooth permission for a connectedDevice foreground service. */
  private fun startForegroundSafely(): Boolean {
    if (!TeslaController.hasBlePermission(this)) return false
    Notifier.ensureChannels(this)
    return try {
      startForeground(Notifier.NOTIF_RUNNING, Notifier.runningNotification(this), ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
      true
    } catch (e: Exception) {
      false
    }
  }

  override fun onDestroy() {
    scope.cancel()
    super.onDestroy()
  }
}
