package org.tfv.ata.core

object Ble {
  val SERVICE_UUID = "00000211-b2d1-43f0-9b88-960cebf8b91e"
  val WRITE_UUID = "00000212-b2d1-43f0-9b88-960cebf8b91e"
  val READ_UUID = "00000213-b2d1-43f0-9b88-960cebf8b91e"
}

enum class FailureCode {
  NOT_REACHABLE,
  PERMISSION,
  KEY_NOT_PAIRED,
  VEHICLE_REJECTED,
  NO_RESPONSE,
  UNSUPPORTED_DEVICE,
  INVALID_VALUE,
  OTHER,
}

sealed interface CommandResult {
  data object Success : CommandResult
  data object NotImplemented : CommandResult
  data class Failure(val reason: String, val code: FailureCode = FailureCode.OTHER) : CommandResult
}
