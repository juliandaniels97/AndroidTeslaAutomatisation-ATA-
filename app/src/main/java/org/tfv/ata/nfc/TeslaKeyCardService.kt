package org.tfv.ata.nfc

import android.app.KeyguardManager
import android.content.Context
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tfv.ata.core.ble.KeyCardCrypto
import org.tfv.ata.data.AtaStore
import java.security.SecureRandom

/**
 * Emulates a Tesla NFC key card over HCE, reusing the phone's enrolled EC key.
 * Only responds when the user turned on "NFC key card" in settings.
 */
class TeslaKeyCardService : HostApduService() {
  @Volatile private var enabled = false
  @Volatile private var requireUnlock = true
  private val random = SecureRandom()
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  override fun onCreate() {
    super.onCreate()
    val store = AtaStore.get(this)
    runCatching {
      runBlocking {
        val s = store.settings.first()
        enabled = s.nfcKeyCard
        requireUnlock = s.nfcKeyCardRequireUnlock
      }
    }
    scope.launch {
      store.settings.collect {
        enabled = it.nfcKeyCard
        requireUnlock = it.nfcKeyCardRequireUnlock
      }
    }
  }

  override fun onDestroy() {
    scope.cancel()
    super.onDestroy()
  }

  override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
    val apdu = commandApdu ?: return SW_UNKNOWN
    if (!enabled) return SW_CONDITIONS
    if (requireUnlock && isLocked()) return SW_CONDITIONS
    if (apdu.size < 4) return SW_WRONG_LENGTH

    // SELECT by AID
    if (b(apdu, 0) == 0x00 && b(apdu, 1) == 0xA4 && b(apdu, 2) == 0x04 && b(apdu, 3) == 0x00) {
      return SW_SUCCESS
    }
    if ((b(apdu, 0) and 0x80) != 0x80) return SW_CLA_NOT_SUPPORTED

    return when (b(apdu, 1)) {
      INS_GET_PUBLIC_KEY -> runCatching { KeyCardCrypto.publicKey() + SW_SUCCESS }.getOrDefault(SW_UNKNOWN)
      INS_AUTHENTICATE -> authenticate(apdu)
      INS_GET_CARD_INFO -> byteArrayOf(0x00, 0x01, 0x90.toByte(), 0x00)
      else -> SW_INS_NOT_SUPPORTED
    }
  }

  private fun authenticate(apdu: ByteArray): ByteArray {
    val data = apduData(apdu) ?: return SW_WRONG_LENGTH
    if (data.size < 0x51) return SW_WRONG_LENGTH
    val readerPub = data.copyOfRange(0, 65)
    val challenge = ByteArray(16)
    random.nextBytes(challenge)
    System.arraycopy(data, 69, challenge, 4, 12)
    return runCatching { KeyCardCrypto.authenticate(readerPub, challenge) + SW_SUCCESS }
      .getOrDefault(SW_UNKNOWN)
  }

  override fun onDeactivated(reason: Int) {}

  private fun isLocked(): Boolean {
    val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    return km != null && km.isDeviceLocked
  }

  private fun b(a: ByteArray, i: Int): Int = a[i].toInt() and 0xFF

  private fun apduData(apdu: ByteArray): ByteArray? {
    if (apdu.size <= 5) return null
    val lc = b(apdu, 4)
    if (lc == 0 || apdu.size < 5 + lc) return null
    return apdu.copyOfRange(5, 5 + lc)
  }

  private companion object {
    const val INS_GET_PUBLIC_KEY = 0x04
    const val INS_AUTHENTICATE = 0x11
    const val INS_GET_CARD_INFO = 0x14
    val SW_SUCCESS = byteArrayOf(0x90.toByte(), 0x00)
    val SW_CLA_NOT_SUPPORTED = byteArrayOf(0x6E, 0x00)
    val SW_INS_NOT_SUPPORTED = byteArrayOf(0x6D, 0x00)
    val SW_UNKNOWN = byteArrayOf(0x6F, 0x00)
    val SW_WRONG_LENGTH = byteArrayOf(0x67, 0x00)
    val SW_CONDITIONS = byteArrayOf(0x69, 0x85.toByte())
  }
}
