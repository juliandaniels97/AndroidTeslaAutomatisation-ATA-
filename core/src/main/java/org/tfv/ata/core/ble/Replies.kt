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
  private const val WL_ALREADY_ON_LIST = 13

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
    val info = status.message(3)?.varint(1)?.toInt() ?: 0
    // The car reports ERROR when the key is already on its list: for pairing that means "done".
    if (info == WL_ALREADY_ON_LIST) return AppReply.Done
    // CommandStatus.operationStatus: 0 OK, 1 WAIT (e.g. waiting for the key card tap), 2 ERROR.
    when (status.varint(1)?.toInt() ?: 0) {
      1 -> return AppReply.KeepWaiting
      2 -> return AppReply.Error(whitelistInfoName(info))
    }
    if (!status.has(3)) return AppReply.KeepWaiting
    return if (info == 0) AppReply.Done else AppReply.Error(whitelistInfoName(info))
  }

  /** True if the reply says the key is already on the car's key list. */
  fun alreadyOnWhitelist(payload: ByteArray?): Boolean =
    payload != null && ProtoReader(payload).message(COMMAND_STATUS)?.message(3)?.varint(1)?.toInt() == WL_ALREADY_ON_LIST

  /** WhitelistOperation_information_E as readable text. */
  fun whitelistInfoName(info: Int): String {
    val name = when (info) {
      0 -> "none"
      1 -> "undocumented error"
      2 -> "no permission to remove oneself"
      3 -> "key fob slots full"
      4 -> "key list full"
      5 -> "no permission to add"
      6 -> "invalid public key"
      7 -> "no permission to remove"
      8 -> "no permission to change permissions"
      9 -> "attempting to elevate other above oneself"
      10 -> "attempting to demote superior"
      11 -> "attempting to remove own permissions"
      12 -> "public key not on key list"
      WL_ALREADY_ON_LIST -> "key already on key list"
      14 -> "not allowed to add unless on reader"
      15 -> "factory mode: modifying outside of factory mode"
      16 -> "factory mode: attempting to add permanent key"
      17 -> "factory mode: attempting to remove permanent key"
      18 -> "keychain full"
      19 -> "attempting to add key without role"
      20 -> "attempting to add key with service role"
      21 -> "non-service key attempting to add service tech"
      22 -> "service key attempting to add service tech outside service mode"
      else -> "unknown"
    }
    return "key list $info ($name)"
  }

  /** One-line description of a VCSEC reply for the log. */
  fun describe(payload: ByteArray?): String {
    if (payload == null) return "empty"
    val msg = ProtoReader(payload)
    msg.message(NOMINAL_ERROR)?.let { return "nominal error ${it.varint(1) ?: 0}" }
    if (msg.has(3)) return "authentication request to another key (ignored)"
    if (msg.has(VEHICLE_STATUS)) return "vehicle status"
    val status = msg.message(COMMAND_STATUS) ?: return "no command status"
    val op = when (status.varint(1)?.toInt() ?: 0) { 0 -> "OK"; 1 -> "WAIT"; 2 -> "ERROR"; else -> "?" }
    val info = status.message(3)?.varint(1)?.toInt()
    return "operation=$op" + (info?.let { " " + whitelistInfoName(it) } ?: "")
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
    return if (code != 0) whitelistInfoName(code) else null
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
