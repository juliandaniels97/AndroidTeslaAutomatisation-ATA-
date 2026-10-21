package org.tfv.ata.automation

import android.content.Context
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.first
import org.tfv.ata.action.ActionInfo
import org.tfv.ata.action.CommandDispatcher
import org.tfv.ata.core.CommandResult
import org.tfv.ata.core.WearContract
import org.tfv.ata.data.AtaStore

/** Runs an [ActionRequest] outside the UI (service or worker) and reports back to the watch if needed. */
object ActionRunner {
  suspend fun execute(context: Context, request: ActionRequest): CommandResult {
    val action = request.action ?: return CommandResult.Failure("unknown action")
    val store = AtaStore.get(context)
    val settings = store.settings.first()
    val vehicles = store.vehicles.first()
    val selected = request.vehicleId ?: store.selectedVehicleId.first()
    val vehicle = vehicles.firstOrNull { it.id == selected } ?: vehicles.firstOrNull()
    val result = CommandDispatcher.run(context, action, vehicle, settings, request.value, request.source)
    request.replyNode?.let { replyToWatch(context, it, action.id, result) }
    return result
  }

  private fun replyToWatch(context: Context, node: String, actionId: String, result: CommandResult) {
    val label = org.tfv.ata.core.AtaAction.fromId(actionId)?.let { context.getString(ActionInfo.labelRes(it)) } ?: actionId
    val text = when (result) {
      is CommandResult.Success -> "1|$label"
      is CommandResult.NotImplemented -> "0|$label"
      is CommandResult.Failure -> "0|$label: ${Notifier.failureText(context, result)}"
    }
    runCatching { Wearable.getMessageClient(context).sendMessage(node, WearContract.MSG_RESULT, text.toByteArray()) }
  }
}
