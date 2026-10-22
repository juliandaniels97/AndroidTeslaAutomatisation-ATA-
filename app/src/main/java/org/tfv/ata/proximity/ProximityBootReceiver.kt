package org.tfv.ata.proximity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.tfv.ata.data.AtaStore

/** Restarts proximity unlock after a reboot or an app update, if it is turned on. */
class ProximityBootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
    val pending = goAsync()
    CoroutineScope(Dispatchers.Default).launch {
      try {
        val store = AtaStore.get(context)
        val settings = store.settings.first()
        val vehicles = store.vehicles.first()
        val id = store.selectedVehicleId.first()
        ProximityService.sync(context, settings, vehicles.firstOrNull { it.id == id } ?: vehicles.firstOrNull())
      } finally {
        pending.finish()
      }
    }
  }
}
