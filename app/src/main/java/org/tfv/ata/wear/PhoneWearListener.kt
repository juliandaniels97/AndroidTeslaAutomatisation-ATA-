package org.tfv.ata.wear

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.runBlocking
import org.tfv.ata.automation.ActionLauncher
import org.tfv.ata.automation.ActionRequest
import org.tfv.ata.automation.ActionRunner
import org.tfv.ata.core.AtaAction
import org.tfv.ata.core.AtaCategory
import org.tfv.ata.core.TriggerSource
import org.tfv.ata.core.WearContract

/**
 * Commands from ATA's watch app. The Data Layer only delivers messages from apps signed with the same key,
 * so these are trusted as [TriggerSource.WATCH]; the watch confirms critical actions before sending.
 */
class PhoneWearListener : WearableListenerService() {
  override fun onMessageReceived(event: MessageEvent) {
    if (event.path != WearContract.MSG_RUN) return
    val action = AtaAction.fromId(String(event.data, Charsets.UTF_8).trim()) ?: return
    val request = ActionRequest(action.id, TriggerSource.WATCH, replyNode = event.sourceNodeId)
    if (action.category == AtaCategory.FUNCTION_B) {
      // Basic actions open an app on the phone: run right here (no worker delay). This callback runs on a
      // background thread, so blocking briefly for the settings read is fine.
      runBlocking { runCatching { ActionRunner.execute(applicationContext, request) } }
    } else {
      ActionLauncher.runNow(this, request)
    }
  }
}
