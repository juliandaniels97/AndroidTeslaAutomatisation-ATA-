package org.tfv.ata.wear

import android.content.Context
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import org.tfv.ata.core.AtaAction
import org.tfv.ata.core.WearContract

object WearSync {
  suspend fun sendToPhone(context: Context, action: AtaAction): Boolean = sendId(context, action.id)

  suspend fun sendId(context: Context, actionId: String): Boolean {
    val nodeClient = Wearable.getNodeClient(context)
    val messageClient = Wearable.getMessageClient(context)
    val nodes = runCatching { nodeClient.connectedNodes.await() }.getOrNull().orEmpty()
    if (nodes.isEmpty()) return false
    var sent = false
    for (node in nodes) {
      val ok = runCatching {
        messageClient.sendMessage(node.id, WearContract.MSG_RUN, actionId.toByteArray()).await()
      }.isSuccess
      sent = sent || ok
    }
    return sent
  }
}
