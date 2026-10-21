package org.tfv.ata.core

object Ble {
  const val IN_DEVELOPMENT = true

  val SERVICE_UUID = "00000211-b2d1-43f0-9b88-960cebf8b91e"
  val WRITE_UUID = "00000212-b2d1-43f0-9b88-960cebf8b91e"
  val READ_UUID = "00000213-b2d1-43f0-9b88-960cebf8b91e"

  fun supports(action: AtaAction): Boolean = action.category == AtaCategory.FUNCTION_A && !IN_DEVELOPMENT
}

sealed interface CommandResult {
  data object Success : CommandResult
  data object NotImplemented : CommandResult
  data class Failure(val reason: String) : CommandResult
}
