package org.tfv.ata.automation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tfv.ata.core.IntentContract
import org.tfv.ata.data.AtaStore

class AutomationReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    when (intent.action) {
      IntentContract.ACTION_SET_ACTIVE -> {
        val active = intent.getBooleanExtra(IntentContract.EXTRA_ACTIVE, true)
        val store = AtaStore.get(context)
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
          try {
            val token = intent.getStringExtra(IntentContract.EXTRA_TOKEN)
            if (token.isNullOrBlank() || token == store.triggerToken()) {
              store.updateSettings { it.copy(active = active) }
            }
          } finally {
            pending.finish()
          }
        }
      }
      IntentContract.ACTION_RUN -> CommandService.start(context, intent, trusted = false)
    }
  }
}
