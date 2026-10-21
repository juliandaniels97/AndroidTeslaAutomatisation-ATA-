package org.tfv.ata.automation

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import org.tfv.ata.action.CommandDispatcher
import org.tfv.ata.action.TriggerSource
import org.tfv.ata.core.AtaAction
import org.tfv.ata.core.AtaCategory
import org.tfv.ata.core.IntentContract
import org.tfv.ata.data.AtaStore

class RunActionActivity : Activity() {
  private var action: AtaAction? = null
  private var value: String? = null
  private var vehicleId: String? = null
  private var delayMs: Long = 0L
  private var token: String? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val data = intent.data
    val actionId = intent.getStringExtra(IntentContract.EXTRA_ACTION_ID)
      ?: data?.getQueryParameter("action")
    value = intent.getStringExtra(IntentContract.EXTRA_VALUE) ?: data?.getQueryParameter("value")
    vehicleId = intent.getStringExtra(IntentContract.EXTRA_VEHICLE_ID) ?: data?.getQueryParameter("vehicle")
    delayMs = intent.getLongExtra(IntentContract.EXTRA_DELAY_MS, 0L)
    token = intent.getStringExtra(IntentContract.EXTRA_TOKEN) ?: data?.getQueryParameter("token")
    val resolved = AtaAction.fromId(actionId)

    if (resolved == null) {
      finish()
      return
    }
    action = resolved

    if (resolved.category == AtaCategory.FUNCTION_A && !hasBlePermission()) {
      requestPermissions(BLE_PERMISSIONS, REQ_BLE)
      return
    }
    runAction()
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == REQ_BLE && grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
      runAction()
    } else {
      finish()
    }
  }

  private fun runAction() {
    val current = action ?: run { finish(); return }
    val store = AtaStore.get(this)
    CoroutineScope(Dispatchers.Main).launch {
      val settings = store.settings.first()
      if (!settings.active) {
        finish()
        return@launch
      }
      val source =
        if (!token.isNullOrBlank() && token == store.triggerToken()) TriggerSource.TRUSTED
        else TriggerSource.EXTERNAL
      if (delayMs in 1..MAX_DELAY_MS) delay(delayMs)
      val vehicles = store.vehicles.first()
      val selected = vehicleId ?: store.selectedVehicleId.first()
      val vehicle = vehicles.firstOrNull { it.id == selected } ?: vehicles.firstOrNull()
      CommandDispatcher.run(this@RunActionActivity, current, vehicle, settings, value, source)
      withContext(Dispatchers.Main) { finish() }
    }
  }

  private fun hasBlePermission(): Boolean =
    BLE_PERMISSIONS.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

  private companion object {
    const val MAX_DELAY_MS = 10 * 60 * 1000L
    const val REQ_BLE = 71
    val BLE_PERMISSIONS = arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
  }
}
