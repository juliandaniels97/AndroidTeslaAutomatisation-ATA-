package org.tfv.ata.automation

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.tfv.ata.R
import org.tfv.ata.action.CommandDispatcher
import org.tfv.ata.core.AtaAction
import org.tfv.ata.core.AtaCategory
import org.tfv.ata.core.IntentContract
import org.tfv.ata.core.TriggerSource
import org.tfv.ata.core.ble.TeslaController
import org.tfv.ata.data.AtaStore
import org.tfv.ata.shortcut.ShortcutSync

/**
 * Exported entry point for ATA shortcuts (also what Samsung Routines starts), widgets, NFC tags and other
 * apps. It only decides the trust level from the token, then hands off and closes immediately:
 * BLE actions run in [CommandService], Basic actions open their target right away, delayed ones are scheduled.
 */
class RunActionActivity : ComponentActivity() {
  private var pending: ActionRequest? = null

  private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
    val request = pending
    pending = null
    if (request != null && org.tfv.ata.core.ble.TeslaController.hasBlePermission(this)) {
      ActionLauncher.runNow(this, request)
    } else {
      Toast.makeText(this, R.string.fail_permission, Toast.LENGTH_LONG).show()
    }
    finish()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (savedInstanceState != null) return // recreated while waiting for the permission dialog

    val data = intent.data
    val actionId = intent.getStringExtra(IntentContract.EXTRA_ACTION_ID) ?: data?.getQueryParameter("action")
    val value = intent.getStringExtra(IntentContract.EXTRA_VALUE) ?: data?.getQueryParameter("value")
    val vehicleId = intent.getStringExtra(IntentContract.EXTRA_VEHICLE_ID) ?: data?.getQueryParameter("vehicle")
    val delayMs = intent.getLongExtra(IntentContract.EXTRA_DELAY_MS, 0L)
    val extraToken = intent.getStringExtra(IntentContract.EXTRA_TOKEN)
    val uriToken = data?.getQueryParameter("token")

    lifecycleScope.launch {
      val store = AtaStore.get(this@RunActionActivity)
      val source = store.resolveSource(extraToken, uriToken)

      if (actionId == IntentContract.ACTION_ID_SET_ACTIVE || actionId == IntentContract.ACTION_ID_SET_PASSIVE) {
        setActive(store, source, actionId == IntentContract.ACTION_ID_SET_ACTIVE)
        finish()
        return@launch
      }

      val action = AtaAction.fromId(actionId)
      if (action == null) {
        finish()
        return@launch
      }
      val request = ActionRequest(action.id, source, value, vehicleId)

      if (delayMs > 0) {
        ActionLauncher.schedule(this@RunActionActivity, request, delayMs)
        val minutes = ((delayMs + 59_999) / 60_000).toInt()
        Toast.makeText(this@RunActionActivity, getString(R.string.scheduled_in, minutes), Toast.LENGTH_SHORT).show()
        finish()
        return@launch
      }

      if (action.category == AtaCategory.FUNCTION_B) {
        // Visible activity: Android lets us open the target app directly.
        val settings = store.settings.first()
        val vehicles = store.vehicles.first()
        val selected = vehicleId ?: store.selectedVehicleId.first()
        val vehicle = vehicles.firstOrNull { it.id == selected } ?: vehicles.firstOrNull()
        CommandDispatcher.run(this@RunActionActivity, action, vehicle, settings, value, source)
        finish()
        return@launch
      }

      if (!TeslaController.hasBlePermission(this@RunActionActivity)) {
        pending = request
        permissionLauncher.launch(org.tfv.ata.core.ble.TeslaController.BLE_PERMISSIONS)
        return@launch
      }
      ActionLauncher.runNow(this@RunActionActivity, request)
      finish()
    }
  }

  private suspend fun setActive(store: AtaStore, source: TriggerSource, active: Boolean) {
    if (source != TriggerSource.TRUSTED && source != TriggerSource.NFC) {
      Toast.makeText(this, R.string.deny_external, Toast.LENGTH_LONG).show()
      return
    }
    store.updateSettings { it.copy(active = active) }
    ShortcutSync.refresh(this)
    Toast.makeText(this, if (active) R.string.ata_now_active else R.string.ata_now_passive, Toast.LENGTH_SHORT).show()
  }
}
