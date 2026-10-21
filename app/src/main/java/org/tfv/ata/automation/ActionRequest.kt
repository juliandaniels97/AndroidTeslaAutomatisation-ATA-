package org.tfv.ata.automation

import android.content.Intent
import androidx.work.Data
import org.tfv.ata.core.AtaAction
import org.tfv.ata.core.TriggerSource

/**
 * A command request handed between ATA's own (non-exported) components. The [source] is decided once at the
 * exported entry point (RunActionActivity, AutomationReceiver, PhoneWearListener) and never read from a
 * caller-supplied extra.
 */
data class ActionRequest(
  val actionId: String,
  val source: TriggerSource,
  val value: String? = null,
  val vehicleId: String? = null,
  /** Wear OS node to report the result to. */
  val replyNode: String? = null,
) {
  val action: AtaAction? get() = AtaAction.fromId(actionId)

  fun putInto(intent: Intent): Intent = intent.apply {
    putExtra(K_ACTION, actionId)
    putExtra(K_SOURCE, source.name)
    value?.let { putExtra(K_VALUE, it) }
    vehicleId?.let { putExtra(K_VEHICLE, it) }
    replyNode?.let { putExtra(K_REPLY, it) }
  }

  fun toData(): Data = Data.Builder()
    .putString(K_ACTION, actionId)
    .putString(K_SOURCE, source.name)
    .putString(K_VALUE, value)
    .putString(K_VEHICLE, vehicleId)
    .putString(K_REPLY, replyNode)
    .build()

  companion object {
    private const val K_ACTION = "ata.req.action"
    private const val K_SOURCE = "ata.req.source"
    private const val K_VALUE = "ata.req.value"
    private const val K_VEHICLE = "ata.req.vehicle"
    private const val K_REPLY = "ata.req.reply"

    /** Only for intents addressed to ATA's own non-exported components. */
    fun from(intent: Intent?): ActionRequest? {
      intent ?: return null
      val id = intent.getStringExtra(K_ACTION) ?: return null
      val source = sourceOf(intent.getStringExtra(K_SOURCE))
      return ActionRequest(id, source, intent.getStringExtra(K_VALUE), intent.getStringExtra(K_VEHICLE), intent.getStringExtra(K_REPLY))
    }

    fun from(data: Data): ActionRequest? {
      val id = data.getString(K_ACTION) ?: return null
      return ActionRequest(id, sourceOf(data.getString(K_SOURCE)), data.getString(K_VALUE), data.getString(K_VEHICLE), data.getString(K_REPLY))
    }

    private fun sourceOf(name: String?): TriggerSource =
      TriggerSource.entries.firstOrNull { it.name == name } ?: TriggerSource.EXTERNAL
  }
}
