package org.tfv.ata.wear

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import org.tfv.ata.automation.ActionLauncher
import org.tfv.ata.automation.ActionRequest
import org.tfv.ata.core.AtaAction
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
    ActionLauncher.runNow(this, ActionRequest(action.id, TriggerSource.WATCH, replyNode = event.sourceNodeId))
  }
}
