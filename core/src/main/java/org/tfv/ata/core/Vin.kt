package org.tfv.ata.core

import java.security.MessageDigest

object Vin {
  private val ALLOWED = Regex("^[A-HJ-NPR-Z0-9]{17}$")

  fun isValid(vin: String): Boolean = ALLOWED.matches(vin.uppercase())

  fun normalize(vin: String): String = vin.trim().uppercase()

  fun bleLocalName(vin: String): String {
    val digest = MessageDigest.getInstance("SHA-1").digest(vin.toByteArray())
    val hex = digest.take(8).joinToString("") { "%02x".format(it) }
    return "S${hex}C"
  }
}
