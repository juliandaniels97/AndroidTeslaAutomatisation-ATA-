package org.tfv.ata.core.nfc

import android.content.Context
import android.nfc.cardemulation.HostApduService
import org.tfv.ata.core.ble.KeyCardCrypto

/**
 * Tesla NFC key-card protocol (APDUs from the car's card reader), shared by the phone's and the watch's card
 * service. Every request and answer goes to [NfcLog].
 *
 * Ref: github.com/pccr10001/TeslaWearKey, gist darconeous/2cd2de11148e3a75685940158bddf933
 */
object KeyCardApdu {
  private const val INS_GET_PUBLIC_KEY = 0x04
  private const val INS_AUTHENTICATE = 0x11
  private const val INS_GET_CARD_INFO = 0x14
  private val SW_SUCCESS = byteArrayOf(0x90.toByte(), 0x00)
  private val SW_CLA_NOT_SUPPORTED = byteArrayOf(0x6E, 0x00)
  private val SW_INS_NOT_SUPPORTED = byteArrayOf(0x6D, 0x00)
  private val SW_UNKNOWN = byteArrayOf(0x6F, 0x00)
  private val SW_WRONG_LENGTH = byteArrayOf(0x67, 0x00)
  private val SW_CONDITIONS = byteArrayOf(0x69, 0x85.toByte())

  /**
   * @param enabled the user turned the key card on.
   * @param locked the device is locked: never answer then, a locked device in a pocket could be relayed to the car.
   */
  fun process(context: Context, apdu: ByteArray?, enabled: Boolean, locked: Boolean): ByteArray {
    if (apdu == null) return SW_UNKNOWN
    val response = respond(context, apdu, enabled, locked)
    NfcLog.add(context, "${describe(apdu)} → ${sw(response)}")
    return response
  }

  fun deactivated(context: Context, reason: Int) {
    NfcLog.add(
      context,
      if (reason == HostApduService.DEACTIVATION_LINK_LOSS) "reader gone (link lost)" else "reader selected another app",
    )
  }

  private fun respond(context: Context, apdu: ByteArray, enabled: Boolean, locked: Boolean): ByteArray {
    if (!enabled) return SW_CONDITIONS.also { NfcLog.add(context, "refused: NFC key card is off in ATA") }
    if (locked) return SW_CONDITIONS.also { NfcLog.add(context, "refused: device is locked") }
    if (apdu.size < 4) return SW_WRONG_LENGTH

    // SELECT by AID
    if (b(apdu, 0) == 0x00 && b(apdu, 1) == 0xA4 && b(apdu, 2) == 0x04 && b(apdu, 3) == 0x00) {
      NfcLog.contact()
      return SW_SUCCESS
    }
    if ((b(apdu, 0) and 0x80) != 0x80) return SW_CLA_NOT_SUPPORTED

    return when (b(apdu, 1)) {
      INS_GET_PUBLIC_KEY -> runCatching { KeyCardCrypto.publicKey() + SW_SUCCESS }
        .onFailure { NfcLog.add(context, "public key error: ${it.message}") }
        .getOrDefault(SW_UNKNOWN)
      INS_AUTHENTICATE -> authenticate(context, apdu)
      INS_GET_CARD_INFO -> byteArrayOf(0x00, 0x01, 0x90.toByte(), 0x00)
      else -> SW_INS_NOT_SUPPORTED
    }
  }

  private fun authenticate(context: Context, apdu: ByteArray): ByteArray {
    val data = apduData(apdu) ?: return SW_WRONG_LENGTH
    if (data.size < 0x51) return SW_WRONG_LENGTH
    // Data = reader's ephemeral public key (65 bytes) || 16-byte challenge; answer AES-ECB(K, challenge).
    val readerPub = data.copyOfRange(0, 65)
    val challenge = data.copyOfRange(65, 81)
    return runCatching { KeyCardCrypto.authenticate(readerPub, challenge) + SW_SUCCESS }
      .onFailure { NfcLog.add(context, "authenticate error: ${it.message}") }
      .getOrDefault(SW_UNKNOWN)
  }

  private fun describe(apdu: ByteArray): String {
    if (apdu.size < 4) return "short APDU ${apdu.hex()}"
    val ins = b(apdu, 1)
    return when {
      b(apdu, 0) == 0x00 && ins == 0xA4 -> "SELECT ${apduData(apdu)?.hex().orEmpty()}"
      ins == INS_GET_PUBLIC_KEY -> "GET PUBLIC KEY"
      ins == INS_AUTHENTICATE -> "AUTHENTICATE (${apduData(apdu)?.size ?: 0} bytes)"
      ins == INS_GET_CARD_INFO -> "GET FORM FACTOR"
      else -> "APDU ${apdu.take(8).toByteArray().hex()}"
    }
  }

  private fun sw(response: ByteArray): String {
    if (response.size < 2) return "?"
    val code = "%02X%02X".format(response[response.size - 2], response[response.size - 1])
    val body = response.size - 2
    return if (body > 0) "$code ($body bytes)" else code
  }

  private fun ByteArray.hex() = joinToString("") { "%02X".format(it) }

  private fun b(a: ByteArray, i: Int): Int = a[i].toInt() and 0xFF

  private fun apduData(apdu: ByteArray): ByteArray? {
    if (apdu.size <= 5) return null
    val lc = b(apdu, 4)
    if (lc == 0 || apdu.size < 5 + lc) return null
    return apdu.copyOfRange(5, 5 + lc)
  }
}
