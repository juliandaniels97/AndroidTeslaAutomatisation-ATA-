package org.tfv.ata.core.ble

/** Application-layer meaning of one reply, modelled on vehicle-command's pkg/vehicle/vcsec.go. */
internal sealed interface AppReply {
  /** The command is done and succeeded. */
  data object Done : AppReply

  /** Intermediate message: keep reading. */
  data object KeepWaiting : AppReply

  /** Vehicle subsystem busy (OPERATIONSTATUS_WAIT): resend after a short delay. */
  data object Busy : AppReply

  data class Error(val detail: String) : AppReply
}

internal object VcsecReply {
  private const val VEHICLE_STATUS = 1
  private const val COMMAND_STATUS = 4
  private const val NOMINAL_ERROR = 46

  private const val OP_OK = 0
  private const val OP_WAIT = 1
  private const val OP_ERROR = 2

  /** For RKE / closure commands: a message without commandStatus is terminal (as in executeRKEAction). */
  fun forCommand(payload: ByteArray?): AppReply {
    if (payload == null || payload.isEmpty()) return AppReply.Done
    val msg = ProtoReader(payload)
    msg.message(NOMINAL_ERROR)?.let { return AppReply.Error("VCSEC error ${it.varint(1) ?: 0}") }
    val status = msg.message(COMMAND_STATUS) ?: return AppReply.Done
    return when (status.varint(1)?.toInt() ?: OP_OK) {
      OP_WAIT -> AppReply.Busy
      OP_ERROR -> whitelistError(status)?.let { AppReply.Error(it) } ?: AppReply.KeepWaiting
      else -> AppReply.KeepWaiting
    }
  }

  /**
   * For add-key requests: terminal once whitelistOperationStatus is present. WAIT means "tap the key card"
   * here (protocol.md), so it keeps waiting instead of resending.
   */
  fun forWhitelist(payload: ByteArray?): AppReply {
    if (payload == null || payload.isEmpty()) return AppReply.KeepWaiting
    val msg = ProtoReader(payload)
    msg.message(NOMINAL_ERROR)?.let { return AppReply.Error("VCSEC error ${it.varint(1) ?: 0}") }
    val status = msg.message(COMMAND_STATUS) ?: return AppReply.KeepWaiting
    val whitelist = status.message(3) ?: return AppReply.KeepWaiting
    val info = whitelist.varint(1)?.toInt() ?: 0
    return if (info == 0) AppReply.Done else AppReply.Error("key list error $info")
  }

  /** VCSEC VehicleStatus from a GET_STATUS reply, or null. */
  fun vehicleStatus(payload: ByteArray?): VcsecStatus? {
    if (payload == null) return null
    val status = ProtoReader(payload).message(VEHICLE_STATUS) ?: return null
    return VcsecStatus(
      lockState = status.varint(2)?.toInt() ?: 0,
      sleepStatus = status.varint(3)?.toInt() ?: 0,
    )
  }

  private fun whitelistError(status: ProtoReader): String? {
    val code = status.message(3)?.varint(1)?.toInt() ?: 0
    return if (code != 0) "key list error $code" else null
  }
}

/** VCSEC lock / sleep state; enum values from vcsec.proto. */
internal data class VcsecStatus(val lockState: Int, val sleepStatus: Int) {
  /** LOCKED (1) or INTERNAL_LOCKED (2). */
  val locked: Boolean get() = lockState == 1 || lockState == 2
  val asleep: Boolean get() = sleepStatus == 2
}

internal object CarServerReply {
  /** CarServer.Response.actionStatus: result 0 = OK, 1 = ERROR with an optional plain-text reason. */
  fun parse(payload: ByteArray?): AppReply {
    if (payload == null || payload.isEmpty()) return AppReply.Done
    val actionStatus = ProtoReader(payload).message(1) ?: return AppReply.Done
    val result = actionStatus.varint(1)?.toInt() ?: 0
    if (result == 0) return AppReply.Done
    val reason = actionStatus.message(2)?.bytes(1)?.toString(Charsets.UTF_8)
    return AppReply.Error(reason?.takeIf { it.isNotBlank() } ?: "vehicle rejected the command")
  }
}
