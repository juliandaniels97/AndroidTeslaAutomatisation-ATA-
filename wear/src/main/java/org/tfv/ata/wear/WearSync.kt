package org.tfv.ata.wear

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.wear.remote.interactions.RemoteActivityHelper
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import org.tfv.ata.core.AtaAction
import org.tfv.ata.core.WearContract

object WearSync {
  suspend fun sendToPhone(context: Context, action: AtaAction): Boolean = sendId(context, action.id)

  /** Sends to the one phone that runs ATA (capability), so a command is never executed twice. */
  suspend fun sendId(context: Context, actionId: String): Boolean {
    val node = phoneNode(context) ?: return false
    return runCatching {
      Wearable.getMessageClient(context).sendMessage(node.id, WearContract.MSG_RUN, actionId.toByteArray()).await()
    }.isSuccess
  }

  /**
   * Opens the watch-pairing assistant on the phone: directly via Play Services (RemoteActivityHelper), or, if
   * that fails, by asking ATA on the phone to open it.
   */
  suspend fun openPairingOnPhone(context: Context): Boolean {
    val node = phoneNode(context) ?: return false
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(WearContract.DEEP_LINK_WEAR_PAIR))
      .addCategory(Intent.CATEGORY_BROWSABLE)
    val opened = runCatching {
      withContext(Dispatchers.IO) {
        RemoteActivityHelper(context).startRemoteActivity(intent, node.id).get(10, TimeUnit.SECONDS)
      }
    }.isSuccess
    if (opened) return true
    return runCatching {
      Wearable.getMessageClient(context).sendMessage(node.id, WearContract.MSG_OPEN_WEAR_PAIR, ByteArray(0)).await()
    }.isSuccess
  }

  private suspend fun phoneNode(context: Context): Node? {
    val capable = runCatching {
      Wearable.getCapabilityClient(context)
        .getCapability(WearContract.CAPABILITY_PHONE, CapabilityClient.FILTER_REACHABLE)
        .await()
        .nodes
    }.getOrNull().orEmpty()
    if (capable.isNotEmpty()) return capable.firstOrNull { it.isNearby } ?: capable.first()
    // Phone app without the capability (older ATA): fall back to the nearest connected node.
    val connected = runCatching { Wearable.getNodeClient(context).connectedNodes.await() }.getOrNull().orEmpty()
    return connected.firstOrNull { it.isNearby } ?: connected.firstOrNull()
  }
}
