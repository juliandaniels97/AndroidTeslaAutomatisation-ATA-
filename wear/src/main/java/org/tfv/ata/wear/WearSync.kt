package org.tfv.ata.wear

import android.content.Context
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
