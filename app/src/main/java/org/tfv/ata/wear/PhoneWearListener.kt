package org.tfv.ata.wear

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import org.tfv.ata.automation.CommandService

class PhoneWearListener : WearableListenerService() {
  override fun onMessageReceived(event: MessageEvent) {
    if (event.path != "/ata/run") return
    val actionId = String(event.data)
    CommandService.startAction(this, actionId, trusted = true)
  }
}
