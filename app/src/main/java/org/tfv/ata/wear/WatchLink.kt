package org.tfv.ata.wear

import android.content.Context
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tfv.ata.core.WatchState
import org.tfv.ata.core.WearContract

/** Phone side of the watch-pairing assistant: sends commands to ATA on the watch and keeps its latest report. */
object WatchLink {
  enum class Reach { UNKNOWN, NO_WATCH, NO_APP, OK }

  private val _state = MutableStateFlow<WatchState?>(null)
  /** Latest report from the watch (null: none yet). */
  val state: StateFlow<WatchState?> = _state.asStateFlow()

  private val _reach = MutableStateFlow(Reach.UNKNOWN)
  val reach: StateFlow<Reach> = _reach.asStateFlow()

  private val _watchName = MutableStateFlow("")
  val watchName: StateFlow<String> = _watchName.asStateFlow()

  /** Called by [PhoneWearListener] for every report the watch sends. */
  fun onState(data: ByteArray) {
    _state.value = WatchState.decode(String(data, Charsets.UTF_8))
  }

  /** Forgets the result of the previous run, so the assistant shows the new one from its start. */
  fun clearRun() {
    _state.value = _state.value?.copy(running = "", step = "", outcome = "", code = "", probe = "")
  }

  /** Sends a WATCH_CMD_* command; false if no watch with ATA is reachable. */
  suspend fun send(context: Context, command: String): Boolean {
    val node = watchNode(context) ?: return false
    return runCatching {
      Wearable.getMessageClient(context).sendMessage(node.id, WearContract.MSG_WATCH_COMMAND, command.toByteArray()).await()
    }.isSuccess
  }

  private suspend fun watchNode(context: Context): Node? {
    val withApp = runCatching {
      Wearable.getCapabilityClient(context)
        .getCapability(WearContract.CAPABILITY_WATCH, CapabilityClient.FILTER_REACHABLE)
        .await()
        .nodes
    }.getOrNull().orEmpty()
    val node = withApp.firstOrNull { it.isNearby } ?: withApp.firstOrNull()
    if (node != null) {
      _reach.value = Reach.OK
      _watchName.value = node.displayName
      return node
    }
    val connected = runCatching { Wearable.getNodeClient(context).connectedNodes.await() }.getOrNull().orEmpty()
    _reach.value = if (connected.isEmpty()) Reach.NO_WATCH else Reach.NO_APP
    _watchName.value = connected.firstOrNull()?.displayName.orEmpty()
    // Older watch app without the capability: try the connected watch anyway.
    return connected.firstOrNull { it.isNearby } ?: connected.firstOrNull()
  }

  private suspend fun <T> Task<T>.await(): T = withContext(Dispatchers.IO) { Tasks.await(this@await) }
}
