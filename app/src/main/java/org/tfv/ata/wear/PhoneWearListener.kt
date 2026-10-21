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
    if (event.path == WearContract.MSG_OPEN_WEAR_PAIR) {
      openWearPairing()
      return
    }
    if (event.path == WearContract.MSG_WATCH_STATE) {
      WatchLink.onState(event.data)
      return
    }
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

  /** Fallback when the watch couldn't open the assistant itself: open it here, or offer it as a notification. */
  private fun openWearPairing() {
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(WearContract.DEEP_LINK_WEAR_PAIR))
      .setClass(this, org.tfv.ata.ui.MainActivity::class.java)
      .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    val started = org.tfv.ata.automation.BackgroundLaunch.allowed(this) && runCatching { startActivity(intent) }.isSuccess
    if (!started) org.tfv.ata.automation.Notifier.openScreen(this, getString(org.tfv.ata.R.string.wear_pair_title), intent)
  }
}
