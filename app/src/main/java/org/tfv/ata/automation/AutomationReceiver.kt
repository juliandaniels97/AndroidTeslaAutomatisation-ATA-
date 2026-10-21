package org.tfv.ata.automation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tfv.ata.core.AtaAction
import org.tfv.ata.core.IntentContract
import org.tfv.ata.core.TriggerSource
import org.tfv.ata.data.AtaStore

/** Broadcast entry point for Tasker & co. Without ATA's token a caller is EXTERNAL (see ActionPolicy). */
class AutomationReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val action = intent.action ?: return
    if (action != IntentContract.ACTION_SET_ACTIVE && action != IntentContract.ACTION_RUN) return
    val pending = goAsync()
    CoroutineScope(Dispatchers.Default).launch {
      try {
        val store = AtaStore.get(context)
        val source = store.resolveSource(intent.getStringExtra(IntentContract.EXTRA_TOKEN), null)
        when (action) {
          IntentContract.ACTION_SET_ACTIVE -> {
            if (source == TriggerSource.TRUSTED) {
              val active = intent.getBooleanExtra(IntentContract.EXTRA_ACTIVE, true)
              store.updateSettings { it.copy(active = active) }
            }
          }
          IntentContract.ACTION_RUN -> {
            val ataAction = AtaAction.fromId(intent.getStringExtra(IntentContract.EXTRA_ACTION_ID)) ?: return@launch
            val request = ActionRequest(
              ataAction.id,
              source,
              intent.getStringExtra(IntentContract.EXTRA_VALUE),
              intent.getStringExtra(IntentContract.EXTRA_VEHICLE_ID),
            )
            val delayMs = intent.getLongExtra(IntentContract.EXTRA_DELAY_MS, 0L)
            if (delayMs > 0) ActionLauncher.schedule(context, request, delayMs) else ActionLauncher.runNow(context, request)
          }
        }
      } finally {
        pending.finish()
      }
    }
  }
}
